package com.example.logic

import com.example.model.*
import java.util.UUID
import kotlin.random.Random

object MapGenerator {

    const val DEFAULT_WIDTH = 14
    const val DEFAULT_HEIGHT = 18

    fun generateMap(
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        seed: Long = System.currentTimeMillis()
    ): Triple<List<GameTile>, Map<FactionId, Faction>, List<BattleEvent>> {
        val random = Random(seed)
        val tiles = mutableListOf<GameTile>()

        // 1. Generate base terrain using clustered probabilities
        for (y in 0 until height) {
            for (x in 0 until width) {
                val terrain = pickTerrainForCoordinate(x, y, width, height, random)
                val hasRuins = random.nextFloat() < 0.04f && terrain.isPassable
                tiles.add(
                    GameTile(
                        x = x,
                        y = y,
                        terrain = terrain,
                        ownerFaction = null,
                        building = null,
                        unit = null,
                        isExplored = false,
                        hasAncientRuins = hasRuins
                    )
                )
            }
        }

        // Helper 2D index lookup
        fun getTileIndex(x: Int, y: Int) = y * width + x

        // 2. Set up initial factions
        val factions = mutableMapOf(
            FactionId.PLAYER to Faction(
                id = FactionId.PLAYER,
                name = FactionId.PLAYER.defaultName,
                colorHex = FactionId.PLAYER.colorHex,
                isHuman = true,
                food = 120,
                wood = 100,
                stone = 80,
                gold = 150,
                science = 10,
                researchedTechs = setOf("AGRICULTURE")
            ),
            FactionId.CRIMSON to Faction(
                id = FactionId.CRIMSON,
                name = FactionId.CRIMSON.defaultName,
                colorHex = FactionId.CRIMSON.colorHex,
                isHuman = false,
                food = 100,
                wood = 80,
                stone = 60,
                gold = 100,
                science = 0
            ),
            FactionId.VERDANT to Faction(
                id = FactionId.VERDANT,
                name = FactionId.VERDANT.defaultName,
                colorHex = FactionId.VERDANT.colorHex,
                isHuman = false,
                food = 110,
                wood = 90,
                stone = 50,
                gold = 90,
                science = 0
            ),
            FactionId.AZURE to Faction(
                id = FactionId.AZURE,
                name = FactionId.AZURE.defaultName,
                colorHex = FactionId.AZURE.colorHex,
                isHuman = false,
                food = 100,
                wood = 70,
                stone = 70,
                gold = 120,
                science = 0
            )
        )

        // 3. Position capital cities for each faction
        // Player at bottom-center (like the user screenshot!)
        val capitalPositions = mapOf(
            FactionId.PLAYER to Pair(width / 2 - 1, height - 3),
            FactionId.CRIMSON to Pair(width / 2 + 1, 2),
            FactionId.VERDANT to Pair(2, height / 2 - 2),
            FactionId.AZURE to Pair(width - 3, height / 2 + 1)
        )

        capitalPositions.forEach { (factionId, pos) ->
            val cx = pos.first
            val cy = pos.second
            val idx = getTileIndex(cx, cy)
            val currentTile = tiles[idx]

            // Ensure capital tile is passable grassland/plains
            val capitalTile = currentTile.copy(
                terrain = TerrainType.PLAINS,
                ownerFaction = factionId,
                building = Building(BuildingType.CAPITAL_CASTLE, factionId),
                unit = GameUnit(
                    id = UUID.randomUUID().toString(),
                    type = UnitType.WARRIOR,
                    factionId = factionId,
                    troops = UnitType.WARRIOR.maxTroops
                )
            )
            tiles[idx] = capitalTile

            // Spawn adjacent scout unit for player & AI
            val scoutX = (cx + 1).coerceAtMost(width - 1)
            val scoutY = cy
            val scoutIdx = getTileIndex(scoutX, scoutY)
            if (tiles[scoutIdx].unit == null && tiles[scoutIdx].terrain.isPassable) {
                tiles[scoutIdx] = tiles[scoutIdx].copy(
                    unit = GameUnit(
                        id = UUID.randomUUID().toString(),
                        type = UnitType.SCOUT,
                        factionId = factionId,
                        troops = UnitType.SCOUT.maxTroops
                    )
                )
            }

            // Claim initial 3x3 territory around capital
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val nx = cx + dx
                    val ny = cy + dy
                    if (nx in 0 until width && ny in 0 until height) {
                        val nIdx = getTileIndex(nx, ny)
                        val t = tiles[nIdx]
                        if (t.ownerFaction == null) {
                            tiles[nIdx] = t.copy(ownerFaction = factionId)
                        }
                    }
                }
            }
        }

        // 4. Reveal fog of war around player territory & units
        val playerCap = capitalPositions[FactionId.PLAYER]!!
        val playerStartX = playerCap.first
        val playerStartY = playerCap.second

        for (i in tiles.indices) {
            val t = tiles[i]
            val dist = Math.hypot((t.x - playerStartX).toDouble(), (t.y - playerStartY).toDouble())
            if (dist <= 4.0 || t.ownerFaction == FactionId.PLAYER) {
                tiles[i] = t.copy(isExplored = true)
            }
        }

        val initialLogs = listOf(
            BattleEvent(
                turn = 1,
                message = "👑 Bem-vindo ao Terra Imperium! Seu reino floresce ao sul. Explore as terras desconhecidas e expanda seus domínios!",
                isPlayerInvolved = true,
                isVictory = true
            ),
            BattleEvent(
                turn = 1,
                message = "🌸 Primavera: A estação do florescer chegou! Colheitas aumentadas em +30%.",
                isPlayerInvolved = true,
                isVictory = true
            )
        )

        return Triple(tiles, factions, initialLogs)
    }

    private fun pickTerrainForCoordinate(x: Int, y: Int, width: Int, height: Int, random: Random): TerrainType {
        // Natural distribution with variety:
        // Central river or water pools
        val isRiverPath = (x == (width * 0.7 + Math.sin(y * 0.8) * 2.0).toInt()) ||
                (x == (width * 0.25 + Math.cos(y * 0.6) * 1.5).toInt() && y < height / 2)

        if (isRiverPath) {
            return if (random.nextFloat() < 0.85f) TerrainType.WATER_RIVER else TerrainType.WATER_OCEAN
        }

        val roll = random.nextFloat()
        return when {
            roll < 0.22f -> TerrainType.PLAINS
            roll < 0.35f -> TerrainType.FARMLAND_WHEAT
            roll < 0.45f -> TerrainType.FARMLAND_CORN
            roll < 0.55f -> TerrainType.FOREST_OAK
            roll < 0.65f -> TerrainType.FOREST_PINE
            roll < 0.73f -> TerrainType.PASTURE_HORSES
            roll < 0.80f -> TerrainType.ORCHARD_APPLE
            roll < 0.87f -> {
                // Mountains: gold or snowy
                if (random.nextFloat() < 0.35f) TerrainType.MOUNTAIN_GOLD
                else if (random.nextFloat() < 0.5f) TerrainType.MOUNTAIN_SNOW
                else TerrainType.MOUNTAIN
            }
            roll < 0.94f -> TerrainType.WATER_OCEAN
            else -> TerrainType.SPICE_GROVE
        }
    }
}
