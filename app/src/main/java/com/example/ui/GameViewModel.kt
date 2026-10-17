package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logic.GameEngine
import com.example.logic.MapGenerator
import com.example.logic.MoveUnitResult
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class GameUiState(
    val tiles: List<GameTile> = emptyList(),
    val factions: Map<FactionId, Faction> = emptyMap(),
    val currentTurn: Int = 1,
    val currentSeason: Season = Season.SPRING,
    val selectedTile: GameTile? = null,
    val isTargetingMode: Boolean = false,
    val reachableCoords: Set<Pair<Int, Int>> = emptySet(),
    val attackableCoords: Set<Pair<Int, Int>> = emptySet(),
    val battleEvents: List<BattleEvent> = emptyList(),
    val isSoundEnabled: Boolean = true,
    val isFogEnabled: Boolean = true,
    val zoomScale: Float = 1.0f,
    val panOffset: Offset = Offset.Zero,
    val mapWidth: Int = MapGenerator.DEFAULT_WIDTH,
    val mapHeight: Int = MapGenerator.DEFAULT_HEIGHT,
    val snackbarMessage: String? = null
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        startNewGame()
    }

    fun startNewGame(seed: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val (tiles, factions, logs) = MapGenerator.generateMap(
                width = MapGenerator.DEFAULT_WIDTH,
                height = MapGenerator.DEFAULT_HEIGHT,
                seed = seed
            )
            _uiState.value = GameUiState(
                tiles = tiles,
                factions = factions,
                currentTurn = 1,
                currentSeason = Season.SPRING,
                battleEvents = logs,
                mapWidth = MapGenerator.DEFAULT_WIDTH,
                mapHeight = MapGenerator.DEFAULT_HEIGHT
            )
            recenterOnCapital()
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun onTileClicked(x: Int, y: Int) {
        val state = _uiState.value
        val clickedTile = state.tiles.find { it.x == x && it.y == y } ?: return

        if (state.isTargetingMode && state.selectedTile?.unit != null) {
            val selectedUnit = state.selectedTile.unit
            if (selectedUnit.factionId == FactionId.PLAYER) {
                // Check if target is in reachable or attackable
                val isReachable = state.reachableCoords.contains(Pair(x, y))
                val isAttackable = state.attackableCoords.contains(Pair(x, y))

                if (isReachable || isAttackable) {
                    executeMoveOrAttack(state.selectedTile, clickedTile)
                    return
                }
            }
        }

        // Standard tile selection
        selectTile(clickedTile)
    }

    private fun selectTile(tile: GameTile) {
        val state = _uiState.value
        val unit = tile.unit

        if (unit != null && unit.factionId == FactionId.PLAYER && unit.movementRemaining > 0) {
            val (reachable, attackable) = computeMovementAndAttackTargets(tile, unit, state.tiles, state.mapWidth, state.mapHeight)
            _uiState.value = state.copy(
                selectedTile = tile,
                isTargetingMode = false,
                reachableCoords = reachable,
                attackableCoords = attackable
            )
        } else {
            _uiState.value = state.copy(
                selectedTile = tile,
                isTargetingMode = false,
                reachableCoords = emptySet(),
                attackableCoords = emptySet()
            )
        }
    }

    fun toggleTargetingMode() {
        val state = _uiState.value
        val currentTargeting = state.isTargetingMode
        _uiState.value = state.copy(isTargetingMode = !currentTargeting)
    }

    fun deselectTile() {
        _uiState.value = _uiState.value.copy(
            selectedTile = null,
            isTargetingMode = false,
            reachableCoords = emptySet(),
            attackableCoords = emptySet()
        )
    }

    private fun executeMoveOrAttack(fromTile: GameTile, toTile: GameTile) {
        val state = _uiState.value
        val result = GameEngine.moveUnit(
            fromTile = fromTile,
            toTile = toTile,
            tiles = state.tiles,
            factions = state.factions,
            mapWidth = state.mapWidth,
            mapHeight = state.mapHeight
        )

        when (result) {
            is MoveUnitResult.Success -> {
                val updatedLogs = state.battleEvents + result.events
                val newSelected = result.selectedTile
                val (reachable, attackable) = if (newSelected?.unit != null && newSelected.unit.movementRemaining > 0) {
                    computeMovementAndAttackTargets(newSelected, newSelected.unit, result.updatedTiles, state.mapWidth, state.mapHeight)
                } else {
                    Pair(emptySet(), emptySet())
                }

                _uiState.value = state.copy(
                    tiles = result.updatedTiles,
                    factions = result.updatedFactions,
                    battleEvents = updatedLogs,
                    selectedTile = newSelected,
                    isTargetingMode = false,
                    reachableCoords = reachable,
                    attackableCoords = attackable,
                    snackbarMessage = result.events.lastOrNull()?.message
                )
            }
            is MoveUnitResult.Failure -> {
                _uiState.value = state.copy(snackbarMessage = result.message)
            }
        }
    }

    private fun computeMovementAndAttackTargets(
        startTile: GameTile,
        unit: GameUnit,
        tiles: List<GameTile>,
        mapWidth: Int,
        mapHeight: Int
    ): Pair<Set<Pair<Int, Int>>, Set<Pair<Int, Int>>> {
        val reachable = mutableSetOf<Pair<Int, Int>>()
        val attackable = mutableSetOf<Pair<Int, Int>>()
        val maxDist = unit.movementRemaining

        for (t in tiles) {
            val dist = GameEngine.getDistance(startTile.x, startTile.y, t.x, t.y)
            if (dist in 1..maxDist) {
                if (t.unit == null && (t.terrain.isPassable || t.terrain == TerrainType.WATER_RIVER)) {
                    reachable.add(Pair(t.x, t.y))
                } else if (t.unit != null && t.unit.factionId != unit.factionId && !unit.hasAttacked) {
                    if (dist <= unit.type.attackRange) {
                        attackable.add(Pair(t.x, t.y))
                    }
                }
            }
        }

        return Pair(reachable, attackable)
    }

    fun onNextTurn() {
        val state = _uiState.value
        val (updatedTiles, updatedFactions, nextSeason, events) = GameEngine.processEndTurn(
            currentTurn = state.currentTurn,
            currentSeason = state.currentSeason,
            tiles = state.tiles,
            factions = state.factions,
            mapWidth = state.mapWidth,
            mapHeight = state.mapHeight
        )

        val updatedSelected = if (state.selectedTile != null) {
            updatedTiles.find { it.x == state.selectedTile.x && it.y == state.selectedTile.y }
        } else null

        _uiState.value = state.copy(
            tiles = updatedTiles,
            factions = updatedFactions,
            currentTurn = state.currentTurn + 1,
            currentSeason = nextSeason,
            battleEvents = state.battleEvents + events,
            selectedTile = updatedSelected,
            isTargetingMode = false,
            reachableCoords = emptySet(),
            attackableCoords = emptySet(),
            snackbarMessage = events.firstOrNull()?.message ?: "Turno ${state.currentTurn + 1} iniciado!"
        )
    }

    fun fortifySelectedUnit() {
        val state = _uiState.value
        val tile = state.selectedTile ?: return
        val unit = tile.unit ?: return
        if (unit.factionId != FactionId.PLAYER) return

        val fortifiedUnit = unit.copy(isFortified = true, movementRemaining = 0)
        val updatedTiles = state.tiles.map {
            if (it.x == tile.x && it.y == tile.y) it.copy(unit = fortifiedUnit) else it
        }

        _uiState.value = state.copy(
            tiles = updatedTiles,
            selectedTile = tile.copy(unit = fortifiedUnit),
            reachableCoords = emptySet(),
            attackableCoords = emptySet(),
            snackbarMessage = "🛡️ ${unit.type.displayName} fortificado! (+35% defesa)"
        )
    }

    fun healSelectedUnit() {
        val state = _uiState.value
        val tile = state.selectedTile ?: return
        val unit = tile.unit ?: return
        val player = state.factions[FactionId.PLAYER] ?: return
        if (unit.factionId != FactionId.PLAYER) return

        val foodCost = 15
        if (player.food < foodCost) {
            _uiState.value = state.copy(snackbarMessage = "Comida insuficiente para curar tropas.")
            return
        }

        val healedUnit = unit.heal(6)
        val updatedTiles = state.tiles.map {
            if (it.x == tile.x && it.y == tile.y) it.copy(unit = healedUnit) else it
        }

        val updatedFactions = state.factions.toMutableMap()
        updatedFactions[FactionId.PLAYER] = player.copy(food = player.food - foodCost)

        _uiState.value = state.copy(
            tiles = updatedTiles,
            factions = updatedFactions,
            selectedTile = tile.copy(unit = healedUnit),
            snackbarMessage = "❤️ Tropas curadas (+6 soldados)!"
        )
    }

    fun foundSettlement() {
        val state = _uiState.value
        val tile = state.selectedTile ?: return
        val unit = tile.unit ?: return
        if (unit.type != UnitType.SETTLER || unit.factionId != FactionId.PLAYER) return

        val updatedTiles = state.tiles.toMutableList()
        fun getIdx(x: Int, y: Int) = y * state.mapWidth + x

        val centerIdx = getIdx(tile.x, tile.y)
        updatedTiles[centerIdx] = tile.copy(
            unit = null,
            ownerFaction = FactionId.PLAYER,
            building = Building(BuildingType.TOWN, FactionId.PLAYER)
        )

        // Claim 3x3 territory around new settlement
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = tile.x + dx
                val ny = tile.y + dy
                if (nx in 0 until state.mapWidth && ny in 0 until state.mapHeight) {
                    val idx = getIdx(nx, ny)
                    val t = updatedTiles[idx]
                    if (t.ownerFaction == null) {
                        updatedTiles[idx] = t.copy(ownerFaction = FactionId.PLAYER, isExplored = true)
                    }
                }
            }
        }

        GameEngine.revealFog(tile.x, tile.y, 3, updatedTiles, state.mapWidth, state.mapHeight)

        _uiState.value = state.copy(
            tiles = updatedTiles,
            selectedTile = updatedTiles[centerIdx],
            reachableCoords = emptySet(),
            attackableCoords = emptySet(),
            snackbarMessage = "🏛️ Novo Assentamento Fundado! As fronteiras do império se expandiram!"
        )
    }

    fun recruitUnit(type: UnitType) {
        val state = _uiState.value
        val tile = state.selectedTile ?: return
        val player = state.factions[FactionId.PLAYER] ?: return
        if (!tile.hasCity || tile.unit != null) return

        if (!player.canAfford(type.foodCost, type.woodCost, type.stoneCost, type.goldCost)) {
            _uiState.value = state.copy(snackbarMessage = "Recursos insuficientes para recrutar ${type.displayName}.")
            return
        }

        val updatedFactions = state.factions.toMutableMap()
        updatedFactions[FactionId.PLAYER] = player.deduct(type.foodCost, type.woodCost, type.stoneCost, type.goldCost)

        val newUnit = GameUnit(
            id = UUID.randomUUID().toString(),
            type = type,
            factionId = FactionId.PLAYER,
            troops = type.maxTroops,
            movementRemaining = 0 // Just recruited, can move next turn
        )

        val updatedTiles = state.tiles.map {
            if (it.x == tile.x && it.y == tile.y) it.copy(unit = newUnit) else it
        }

        _uiState.value = state.copy(
            tiles = updatedTiles,
            factions = updatedFactions,
            selectedTile = tile.copy(unit = newUnit),
            snackbarMessage = "⚔️ Novo regimento de ${type.displayName} recrutado na cidade!"
        )
    }

    fun buildStructure(type: BuildingType) {
        val state = _uiState.value
        val tile = state.selectedTile ?: return
        val player = state.factions[FactionId.PLAYER] ?: return
        if (tile.ownerFaction != FactionId.PLAYER || tile.building != null) return

        if (!player.canAfford(type.foodCost, type.woodCost, type.stoneCost, type.goldCost)) {
            _uiState.value = state.copy(snackbarMessage = "Recursos insuficientes para construir ${type.displayName}.")
            return
        }

        val updatedFactions = state.factions.toMutableMap()
        updatedFactions[FactionId.PLAYER] = player.deduct(type.foodCost, type.woodCost, type.stoneCost, type.goldCost)

        val newBuilding = Building(type, FactionId.PLAYER)
        val updatedTiles = state.tiles.map {
            if (it.x == tile.x && it.y == tile.y) it.copy(building = newBuilding) else it
        }

        _uiState.value = state.copy(
            tiles = updatedTiles,
            factions = updatedFactions,
            selectedTile = tile.copy(building = newBuilding),
            snackbarMessage = "🏗️ Construção de ${type.displayName} concluída!"
        )
    }

    fun selectTech(techId: String) {
        val state = _uiState.value
        val player = state.factions[FactionId.PLAYER] ?: return
        val tech = TechRegistry.getTech(techId) ?: return

        val updatedFactions = state.factions.toMutableMap()
        updatedFactions[FactionId.PLAYER] = player.copy(
            currentResearchId = techId
        )

        _uiState.value = state.copy(
            factions = updatedFactions,
            snackbarMessage = "🔬 Pesquisando agora: ${tech.name} (${tech.icon})"
        )
    }

    fun zoomIn() {
        val current = _uiState.value.zoomScale
        _uiState.value = _uiState.value.copy(zoomScale = (current + 0.15f).coerceAtMost(1.8f))
    }

    fun zoomOut() {
        val current = _uiState.value.zoomScale
        _uiState.value = _uiState.value.copy(zoomScale = (current - 0.15f).coerceAtLeast(0.65f))
    }

    fun setPanOffset(offset: Offset) {
        _uiState.value = _uiState.value.copy(panOffset = offset)
    }

    fun setZoomScale(scale: Float) {
        _uiState.value = _uiState.value.copy(zoomScale = scale.coerceIn(0.65f, 1.8f))
    }

    fun recenterOnCapital() {
        val state = _uiState.value
        val cap = state.tiles.find { it.ownerFaction == FactionId.PLAYER && it.building?.type == BuildingType.CAPITAL_CASTLE }
        if (cap != null) {
            _uiState.value = state.copy(
                panOffset = Offset.Zero,
                zoomScale = 1.0f,
                selectedTile = cap
            )
        }
    }

    fun toggleSound() {
        _uiState.value = _uiState.value.copy(isSoundEnabled = !_uiState.value.isSoundEnabled)
    }

    fun toggleFog() {
        val newFog = !_uiState.value.isFogEnabled
        val state = _uiState.value
        val updatedTiles = state.tiles.map {
            if (!newFog) it.copy(isExplored = true) else it
        }
        _uiState.value = state.copy(isFogEnabled = newFog, tiles = updatedTiles)
    }
}
