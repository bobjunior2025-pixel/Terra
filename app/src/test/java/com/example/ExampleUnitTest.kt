package com.example

import com.example.logic.GameEngine
import com.example.logic.MapGenerator
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun mapGenerator_initializesValidMapAndFactions() {
        val (tiles, factions, logs) = MapGenerator.generateMap(width = 14, height = 18, seed = 42L)
        assertEquals(14 * 18, tiles.size)
        assertTrue(factions.containsKey(FactionId.PLAYER))
        assertTrue(factions.containsKey(FactionId.CRIMSON))

        val playerTiles = tiles.filter { it.ownerFaction == FactionId.PLAYER }
        assertTrue("Player should start with territory", playerTiles.isNotEmpty())

        val capital = playerTiles.find { it.hasCity }
        assertNotNull("Player should start with capital castle", capital)
        assertEquals(BuildingType.CAPITAL_CASTLE, capital?.building?.type)
        assertTrue(logs.isNotEmpty())
    }

    @Test
    fun gameEngine_endTurn_progressesTurnAndCollectsResources() {
        val (tiles, factions, _) = MapGenerator.generateMap(width = 14, height = 18, seed = 42L)
        val initialPlayer = factions[FactionId.PLAYER]!!

        val (nextTiles, nextFactions, nextSeason, events) = GameEngine.processEndTurn(
            currentTurn = 1,
            currentSeason = Season.SPRING,
            tiles = tiles,
            factions = factions,
            mapWidth = 14,
            mapHeight = 18
        )

        assertEquals(tiles.size, nextTiles.size)
        val updatedPlayer = nextFactions[FactionId.PLAYER]!!
        assertTrue("Player should gain science or food", updatedPlayer.food >= initialPlayer.food || updatedPlayer.science >= initialPlayer.science)
        assertEquals(Season.SPRING, nextSeason)
    }

    @Test
    fun combat_resolvesDamageAccurately() {
        val warrior = GameUnit(
            id = "w1",
            type = UnitType.WARRIOR,
            factionId = FactionId.PLAYER,
            troops = 25
        )
        val scout = GameUnit(
            id = "s1",
            type = UnitType.SCOUT,
            factionId = FactionId.CRIMSON,
            troops = 15
        )

        val result = GameEngine.resolveCombat(warrior, scout, TerrainType.PLAINS, defenderInCity = false)
        assertTrue("Scout defender should suffer casualties", result.defenderCasualties > 0)
    }
}
