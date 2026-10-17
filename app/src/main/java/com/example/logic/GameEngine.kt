package com.example.logic

import com.example.model.*
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

object GameEngine {

    /**
     * Resolves end of turn for all factions:
     * - Collects tile yields with season multipliers
     * - Updates research
     * - Resets and heals units
     * - Executes AI faction turns
     * - Updates season
     */
    fun processEndTurn(
        currentTurn: Int,
        currentSeason: Season,
        tiles: List<GameTile>,
        factions: Map<FactionId, Faction>,
        mapWidth: Int,
        mapHeight: Int
    ): Quadruple<List<GameTile>, Map<FactionId, Faction>, Season, List<BattleEvent>> {
        val updatedTiles = tiles.toMutableList()
        val updatedFactions = factions.toMutableMap()
        val events = mutableListOf<BattleEvent>()
        val nextTurn = currentTurn + 1

        // 1. Season progression every 8 turns
        val nextSeason = if (nextTurn % 8 == 1 && nextTurn > 1) {
            val s = currentSeason.next()
            events.add(
                BattleEvent(
                    turn = nextTurn,
                    message = "${s.icon} Mudança de Estação: Chegou o ${s.displayName}! ${s.description}",
                    isPlayerInvolved = true,
                    isVictory = true
                )
            )
            s
        } else {
            currentSeason
        }

        // 2. Resource collection per faction
        for ((factionId, faction) in updatedFactions) {
            var totalFood = 0
            var totalWood = 0
            var totalStone = 0
            var totalGold = 0
            var totalScience = 2 // base science per turn

            // Calculate yields from owned tiles
            for (tile in updatedTiles) {
                if (tile.ownerFaction == factionId) {
                    val base = tile.terrain.baseYield
                    totalFood += base.food
                    totalWood += base.wood
                    totalStone += base.stone
                    totalGold += base.gold
                    totalScience += base.science

                    // Building yields
                    tile.building?.let { b ->
                        val bYield = b.type.yieldBonus
                        totalFood += bYield.food
                        totalWood += bYield.wood
                        totalStone += bYield.stone
                        totalGold += bYield.gold
                        totalScience += bYield.science
                    }
                }
            }

            // Apply Season multipliers
            val finalFood = (totalFood * nextSeason.foodMultiplier).toInt()
            val finalGold = (totalGold * nextSeason.goldMultiplier).toInt()

            // Unit upkeep: units cost food & gold each turn
            var unitCount = 0
            for (tile in updatedTiles) {
                if (tile.unit?.factionId == factionId) {
                    unitCount++
                }
            }
            val foodUpkeep = (unitCount * 2 * (if (nextSeason.winterCostPenalty) 1.25f else 1.0f)).toInt()
            val goldUpkeep = unitCount * 1

            val netFood = max(0, finalFood - foodUpkeep)
            val netGold = max(0, finalGold - goldUpkeep)

            // Update Research
            var newScience = faction.science + totalScience
            var currentResearch = faction.currentResearchId
            var currentProgress = faction.researchProgress + totalScience
            var researchedTechs = faction.researchedTechs

            if (currentResearch != null) {
                val tech = TechRegistry.getTech(currentResearch)
                if (tech != null && currentProgress >= tech.scienceCost) {
                    researchedTechs = researchedTechs + currentResearch
                    if (faction.isHuman) {
                        events.add(
                            BattleEvent(
                                turn = nextTurn,
                                message = "💡 Descoberta Científica: Você pesquisou ${tech.name} (${tech.icon})!",
                                isPlayerInvolved = true,
                                isVictory = true
                            )
                        )
                    }
                    currentResearch = null
                    currentProgress = 0
                }
            }

            updatedFactions[factionId] = faction.copy(
                food = faction.food + netFood,
                wood = faction.wood + totalWood,
                stone = faction.stone + totalStone,
                gold = faction.gold + netGold,
                science = newScience,
                researchedTechs = researchedTechs,
                currentResearchId = currentResearch,
                researchProgress = currentProgress
            )
        }

        // 3. Reset unit movements & heal in friendly territory
        for (i in updatedTiles.indices) {
            val tile = updatedTiles[i]
            val unit = tile.unit
            if (unit != null) {
                var healedUnit = unit.resetTurn()
                // Heal +3 troops if stationed inside friendly territory or on a city
                if (tile.ownerFaction == unit.factionId) {
                    val healAmount = if (tile.hasCity) 6 else 3
                    healedUnit = healedUnit.heal(healAmount)
                }
                updatedTiles[i] = tile.copy(unit = healedUnit)
            }
        }

        // 4. Process AI Turns
        processAITurns(nextTurn, updatedTiles, updatedFactions, mapWidth, mapHeight, events)

        // 5. Random kingdom events for human player
        val random = Random(nextTurn * 1337L)
        if (random.nextFloat() < 0.20f) {
            when (random.nextInt(3)) {
                0 -> {
                    val giftGold = random.nextInt(30, 70)
                    val p = updatedFactions[FactionId.PLAYER]!!
                    updatedFactions[FactionId.PLAYER] = p.copy(gold = p.gold + giftGold)
                    events.add(
                        BattleEvent(
                            turn = nextTurn,
                            message = "💰 Caravana Mercante: Comerciantes itinerantes pagaram taxas de pedágio (+${giftGold} Ouro)!",
                            isPlayerInvolved = true,
                            isVictory = true
                        )
                    )
                }
                1 -> {
                    val harvestFood = random.nextInt(40, 80)
                    val p = updatedFactions[FactionId.PLAYER]!!
                    updatedFactions[FactionId.PLAYER] = p.copy(food = p.food + harvestFood)
                    events.add(
                        BattleEvent(
                            turn = nextTurn,
                            message = "🌾 Safra Excepcional: As aldeias do reino entregaram tributos agrícolas extras (+${harvestFood} Comida)!",
                            isPlayerInvolved = true,
                            isVictory = true
                        )
                    )
                }
                2 -> {
                    val bonusWood = random.nextInt(35, 65)
                    val p = updatedFactions[FactionId.PLAYER]!!
                    updatedFactions[FactionId.PLAYER] = p.copy(wood = p.wood + bonusWood)
                    events.add(
                        BattleEvent(
                            turn = nextTurn,
                            message = "🌲 Expedição Florestal: Lenhadores encontraram excelente madeira de lei (+${bonusWood} Madeira)!",
                            isPlayerInvolved = true,
                            isVictory = true
                        )
                    )
                }
            }
        }

        return Quadruple(updatedTiles, updatedFactions, nextSeason, events)
    }

    private fun processAITurns(
        turn: Int,
        tiles: MutableList<GameTile>,
        factions: MutableMap<FactionId, Faction>,
        mapWidth: Int,
        mapHeight: Int,
        events: MutableList<BattleEvent>
    ) {
        val aiFactions = listOf(FactionId.CRIMSON, FactionId.VERDANT, FactionId.AZURE)
        val random = Random(turn * 733L)

        fun getIndex(x: Int, y: Int) = y * mapWidth + x

        for (aiId in aiFactions) {
            val faction = factions[aiId] ?: continue
            if (faction.isDefeated) continue

            // 1. Move and attack with AI units
            val aiUnitTileIndices = tiles.indices.filter { tiles[it].unit?.factionId == aiId }

            for (idx in aiUnitTileIndices) {
                val currentTile = tiles[idx]
                val unit = currentTile.unit ?: continue
                if (unit.movementRemaining <= 0 || unit.hasAttacked) continue

                // Check adjacent tiles for enemies or neutral expansion
                val neighbors = getAdjacentIndices(currentTile.x, currentTile.y, mapWidth, mapHeight)
                val enemyTarget = neighbors.map { tiles[it] }.find { it.unit != null && it.unit.factionId != aiId }

                if (enemyTarget != null && !unit.hasAttacked) {
                    // Attack enemy!
                    val defenderUnit = enemyTarget.unit!!
                    val combatResult = resolveCombat(unit, defenderUnit, enemyTarget.terrain, enemyTarget.hasCity)

                    if (defenderUnit.factionId == FactionId.PLAYER || unit.factionId == FactionId.PLAYER) {
                        events.add(
                            BattleEvent(
                                turn = turn,
                                message = "⚔️ Batalha em (${enemyTarget.x}, ${enemyTarget.y}): ${unit.type.displayName} (${faction.name}) atacou ${defenderUnit.type.displayName}! Baixas: -${combatResult.defenderCasualties} defensores, -${combatResult.attackerCasualties} atacantes.",
                                isPlayerInvolved = defenderUnit.factionId == FactionId.PLAYER,
                                isVictory = defenderUnit.factionId == FactionId.PLAYER && combatResult.defenderSurvives
                            )
                        )
                    }

                    val updatedAttacker = unit.copy(
                        troops = unit.troops - combatResult.attackerCasualties,
                        hasAttacked = true,
                        movementRemaining = 0
                    )
                    val updatedDefender = defenderUnit.copy(
                        troops = defenderUnit.troops - combatResult.defenderCasualties
                    )

                    val targetIdx = getIndex(enemyTarget.x, enemyTarget.y)
                    if (updatedDefender.troops <= 0) {
                        // Defender destroyed!
                        if (unit.type.attackRange == 1 && updatedAttacker.troops > 0) {
                            // Melee unit moves into captured tile
                            tiles[targetIdx] = enemyTarget.copy(
                                unit = updatedAttacker,
                                ownerFaction = if (enemyTarget.ownerFaction != null) aiId else enemyTarget.ownerFaction
                            )
                            tiles[idx] = currentTile.copy(unit = null)
                        } else {
                            tiles[targetIdx] = enemyTarget.copy(unit = null)
                            tiles[idx] = currentTile.copy(unit = if (updatedAttacker.troops > 0) updatedAttacker else null)
                        }
                    } else {
                        tiles[targetIdx] = enemyTarget.copy(unit = updatedDefender)
                        tiles[idx] = currentTile.copy(unit = if (updatedAttacker.troops > 0) updatedAttacker else null)
                    }
                } else {
                    // Move AI unit toward unowned tile or player
                    val validMoves = neighbors.filter {
                        val t = tiles[it]
                        t.terrain.isPassable && t.unit == null
                    }
                    if (validMoves.isNotEmpty() && random.nextFloat() < 0.70f) {
                        val chosenIdx = validMoves[random.nextInt(validMoves.size)]
                        val destTile = tiles[chosenIdx]
                        tiles[chosenIdx] = destTile.copy(
                            unit = unit.copy(movementRemaining = 0),
                            ownerFaction = if (destTile.ownerFaction == null) aiId else destTile.ownerFaction
                        )
                        tiles[idx] = currentTile.copy(unit = null)
                    }
                }
            }

            // 2. AI recruits new unit if has resources and capital is empty
            val capitalTileIdx = tiles.indices.find {
                val t = tiles[it]
                t.ownerFaction == aiId && t.hasCity && t.unit == null
            }

            if (capitalTileIdx != null) {
                val unitToBuild = when {
                    faction.food >= 45 && faction.gold >= 35 && random.nextFloat() < 0.4f -> UnitType.KNIGHT
                    faction.food >= 30 && faction.wood >= 25 && random.nextFloat() < 0.5f -> UnitType.ARCHER
                    faction.food >= 35 && faction.wood >= 20 -> UnitType.WARRIOR
                    else -> null
                }

                if (unitToBuild != null && faction.canAfford(unitToBuild.foodCost, unitToBuild.woodCost, unitToBuild.stoneCost, unitToBuild.goldCost)) {
                    factions[aiId] = faction.deduct(unitToBuild.foodCost, unitToBuild.woodCost, unitToBuild.stoneCost, unitToBuild.goldCost)
                    val capTile = tiles[capitalTileIdx]
                    tiles[capitalTileIdx] = capTile.copy(
                        unit = GameUnit(
                            id = UUID.randomUUID().toString(),
                            type = unitToBuild,
                            factionId = aiId,
                            troops = unitToBuild.maxTroops
                        )
                    )
                }
            }
        }
    }

    data class CombatResult(
        val attackerCasualties: Int,
        val defenderCasualties: Int,
        val defenderSurvives: Boolean
    )

    fun resolveCombat(
        attacker: GameUnit,
        defender: GameUnit,
        defenderTerrain: TerrainType,
        defenderInCity: Boolean
    ): CombatResult {
        val rand = Random.Default
        val attackRoll = 0.85f + rand.nextFloat() * 0.30f
        val defenseRoll = 0.85f + rand.nextFloat() * 0.30f

        val cityDefense = if (defenderInCity) 1.35f else 1.0f
        val terrainDefense = defenderTerrain.defenseBonus * cityDefense

        val effectiveAtk = attacker.effectiveAttack * attackRoll
        val effectiveDef = defender.effectiveDefense * terrainDefense * defenseRoll

        // Attacker deals damage to defender
        val rawDefDamage = ((effectiveAtk / effectiveDef) * 10f).toInt().coerceAtLeast(2)
        val defenderCasualties = rawDefDamage.coerceAtMost(defender.troops)

        // Defender counters if attacker is in melee range (range == 1)
        val attackerCasualties = if (attacker.type.attackRange == 1 && (defender.troops - defenderCasualties) > 0) {
            val counterAtk = defender.effectiveDefense * 0.75f * defenseRoll
            val counterDef = attacker.effectiveDefense * attackRoll
            val rawAtkDamage = ((counterAtk / counterDef) * 8f).toInt().coerceAtLeast(1)
            rawAtkDamage.coerceAtMost(attacker.troops)
        } else {
            0 // Ranged attacks take zero retaliation
        }

        val defenderSurvives = (defender.troops - defenderCasualties) > 0

        return CombatResult(
            attackerCasualties = attackerCasualties,
            defenderCasualties = defenderCasualties,
            defenderSurvives = defenderSurvives
        )
    }

    /**
     * Executes player movement of unit from startTile to destTile
     */
    fun moveUnit(
        fromTile: GameTile,
        toTile: GameTile,
        tiles: List<GameTile>,
        factions: Map<FactionId, Faction>,
        mapWidth: Int,
        mapHeight: Int
    ): MoveUnitResult {
        val unit = fromTile.unit ?: return MoveUnitResult.Failure("Nenhuma unidade selecionada.")
        if (unit.movementRemaining <= 0) return MoveUnitResult.Failure("Unidade sem pontos de movimento neste turno.")
        if (!toTile.terrain.isPassable && toTile.terrain != TerrainType.WATER_RIVER) {
            return MoveUnitResult.Failure("Terreno intransponível.")
        }

        val dist = getDistance(fromTile.x, fromTile.y, toTile.x, toTile.y)
        if (dist > unit.movementRemaining) {
            return MoveUnitResult.Failure("Destino fora do alcance de movimento.")
        }

        val updatedTiles = tiles.toMutableList()
        val updatedFactions = factions.toMutableMap()
        val events = mutableListOf<BattleEvent>()

        fun getIdx(x: Int, y: Int) = y * mapWidth + x
        val fromIdx = getIdx(fromTile.x, fromTile.y)
        val toIdx = getIdx(toTile.x, toTile.y)

        // Case 1: Friendly tile or empty tile
        if (toTile.unit == null) {
            val moveCost = toTile.terrain.movementCost
            val remainingMove = max(0, unit.movementRemaining - moveCost)

            // Ancient ruins reward check
            var ruinsFound = false
            if (toTile.hasAncientRuins) {
                ruinsFound = true
                val player = updatedFactions[FactionId.PLAYER]!!
                val goldBonus = 60
                val scienceBonus = 30
                updatedFactions[FactionId.PLAYER] = player.copy(
                    gold = player.gold + goldBonus,
                    science = player.science + scienceBonus
                )
                events.add(
                    BattleEvent(
                        turn = 0,
                        message = "🏛️ Ruínas Ancestrais Descobertas! Seus soldados recuperaram relíquias antigas (+${goldBonus} Ouro, +${scienceBonus} Ciência)!",
                        isPlayerInvolved = true,
                        isVictory = true
                    )
                )
            }

            // Move unit & capture unowned tile if entering
            val capturedOwner = if (toTile.ownerFaction == null) FactionId.PLAYER else toTile.ownerFaction
            val movedUnit = unit.copy(movementRemaining = remainingMove)

            updatedTiles[fromIdx] = fromTile.copy(unit = null)
            updatedTiles[toIdx] = toTile.copy(
                unit = movedUnit,
                ownerFaction = capturedOwner,
                hasAncientRuins = false
            )

            // Reveal fog of war around destination
            revealFog(toTile.x, toTile.y, unit.type.sightRange, updatedTiles, mapWidth, mapHeight)

            return MoveUnitResult.Success(
                updatedTiles = updatedTiles,
                updatedFactions = updatedFactions,
                events = events,
                selectedTile = updatedTiles[toIdx]
            )
        }

        // Case 2: Target tile has enemy unit -> COMBAT!
        val targetUnit = toTile.unit
        if (targetUnit.factionId != unit.factionId) {
            if (unit.hasAttacked) {
                return MoveUnitResult.Failure("Esta tropa já realizou um ataque neste turno.")
            }

            val combat = resolveCombat(unit, targetUnit, toTile.terrain, toTile.hasCity)
            events.add(
                BattleEvent(
                    turn = 0,
                    message = "⚔️ Ataque em (${toTile.x}, ${toTile.y}): Seu ${unit.type.displayName} atacou ${targetUnit.type.displayName}! O inimigo perdeu ${combat.defenderCasualties} soldados (você perdeu ${combat.attackerCasualties}).",
                    isPlayerInvolved = true,
                    isVictory = !combat.defenderSurvives
                )
            )

            val newAttacker = unit.copy(
                troops = unit.troops - combat.attackerCasualties,
                hasAttacked = true,
                movementRemaining = 0
            )
            val newDefender = targetUnit.copy(
                troops = targetUnit.troops - combat.defenderCasualties
            )

            if (!combat.defenderSurvives) {
                // Enemy eliminated!
                if (unit.type.attackRange == 1 && newAttacker.troops > 0) {
                    // Melee marches into conquered tile and claims territory
                    updatedTiles[fromIdx] = fromTile.copy(unit = null)
                    updatedTiles[toIdx] = toTile.copy(
                        unit = newAttacker,
                        ownerFaction = FactionId.PLAYER
                    )
                    revealFog(toTile.x, toTile.y, unit.type.sightRange, updatedTiles, mapWidth, mapHeight)
                } else {
                    // Ranged: defender removed, attacker stays
                    updatedTiles[toIdx] = toTile.copy(unit = null)
                    updatedTiles[fromIdx] = fromTile.copy(unit = if (newAttacker.troops > 0) newAttacker else null)
                }
            } else {
                // Defender survived
                updatedTiles[toIdx] = toTile.copy(unit = newDefender)
                updatedTiles[fromIdx] = fromTile.copy(unit = if (newAttacker.troops > 0) newAttacker else null)
            }

            val currentSelected = if (newAttacker.troops > 0) updatedTiles[fromIdx] else updatedTiles[toIdx]
            return MoveUnitResult.Success(
                updatedTiles = updatedTiles,
                updatedFactions = updatedFactions,
                events = events,
                selectedTile = currentSelected
            )
        }

        return MoveUnitResult.Failure("Casa já ocupada por tropas aliadas.")
    }

    fun revealFog(
        centerX: Int,
        centerY: Int,
        radius: Int,
        tiles: MutableList<GameTile>,
        mapWidth: Int,
        mapHeight: Int
    ) {
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = centerX + dx
                val ny = centerY + dy
                if (nx in 0 until mapWidth && ny in 0 until mapHeight) {
                    val dist = Math.hypot(dx.toDouble(), dy.toDouble())
                    if (dist <= radius + 0.3) {
                        val idx = ny * mapWidth + nx
                        tiles[idx] = tiles[idx].copy(isExplored = true)
                    }
                }
            }
        }
    }

    fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        return max(abs(x1 - x2), abs(y1 - y2))
    }

    fun getAdjacentIndices(x: Int, y: Int, width: Int, height: Int): List<Int> {
        val list = mutableListOf<Int>()
        val directions = listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1),
            Pair(-1, -1), Pair(1, -1), Pair(-1, 1), Pair(1, 1)
        )
        for ((dx, dy) in directions) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until width && ny in 0 until height) {
                list.add(ny * width + nx)
            }
        }
        return list
    }
}

sealed class MoveUnitResult {
    data class Success(
        val updatedTiles: List<GameTile>,
        val updatedFactions: Map<FactionId, Faction>,
        val events: List<BattleEvent>,
        val selectedTile: GameTile?
    ) : MoveUnitResult()

    data class Failure(val message: String) : MoveUnitResult()
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
