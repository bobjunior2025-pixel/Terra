package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun TileActionPanel(
    selectedTile: GameTile?,
    playerFaction: Faction,
    isTargetingMode: Boolean,
    onToggleTargeting: () -> Unit,
    onFortifyUnit: () -> Unit,
    onHealUnit: () -> Unit,
    onFoundSettlement: () -> Unit,
    onRecruitUnit: (UnitType) -> Unit,
    onBuildStructure: (BuildingType) -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedTile == null) return

    var showRecruitDialog by remember { mutableStateOf(false) }
    var showBuildDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tile_action_panel"),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color(0xFF1E293B),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Terrain & Coordinates + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (selectedTile.terrain) {
                                TerrainType.PLAINS -> "🌿"
                                TerrainType.FARMLAND_WHEAT -> "🌾"
                                TerrainType.FARMLAND_CORN -> "🌽"
                                TerrainType.ORCHARD_APPLE -> "🍎"
                                TerrainType.FOREST_PINE, TerrainType.FOREST_OAK -> "🌲"
                                TerrainType.PASTURE_HORSES -> "🐴"
                                TerrainType.MOUNTAIN, TerrainType.MOUNTAIN_SNOW -> "⛰️"
                                TerrainType.MOUNTAIN_GOLD -> "💎"
                                TerrainType.WATER_RIVER, TerrainType.WATER_OCEAN -> "🐟"
                                TerrainType.SPICE_GROVE -> "✨"
                            },
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Text(
                            text = selectedTile.terrain.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF8FAFC)
                        )
                        val ownerText = when (selectedTile.ownerFaction) {
                            FactionId.PLAYER -> "Território Imperial de Eldoria"
                            FactionId.CRIMSON -> "Fronteira da Legião Carmesim"
                            FactionId.VERDANT -> "Terras da Aliança Esmeralda"
                            FactionId.AZURE -> "Domínio da Dinastia Celeste"
                            else -> "Terras Livres Não Reivindicadas"
                        }
                        Text(
                            text = "(${selectedTile.x}, ${selectedTile.y}) • $ownerText",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onDeselect,
                    modifier = Modifier.testTag("btn_close_panel")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Unit or Building Information
            val unit = selectedTile.unit
            if (unit != null) {
                val isPlayerUnit = unit.factionId == FactionId.PLAYER
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = unit.type.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isPlayerUnit) Color(0xFFA78BFA) else Color(0xFFF87171)
                                )
                                if (unit.isFortified) {
                                    Surface(
                                        color = Color(0xFF1E3A8A),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "🛡️ Fortificada",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Regimento: ${unit.troops}/${unit.maxTroops} soldados",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ataque: ${unit.effectiveAttack} • Defesa: ${unit.effectiveDefense}",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "Movimento: ${unit.movementRemaining}/${unit.type.maxMovement}",
                                fontSize = 12.sp,
                                color = if (unit.movementRemaining > 0) Color(0xFF4ADE80) else Color(0xFFF87171)
                            )
                        }

                        if (isPlayerUnit) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onToggleTargeting,
                                    modifier = Modifier.weight(1f).testTag("btn_unit_move_attack"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isTargetingMode) Color(0xFFDC2626) else Color(0xFF7C3AED)
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isTargetingMode) Icons.Default.Close else Icons.Default.Navigation,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (isTargetingMode) "Cancelar" else "Mover / Atacar", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = onFortifyUnit,
                                    modifier = Modifier.weight(1f).testTag("btn_unit_fortify"),
                                    enabled = !unit.isFortified && unit.movementRemaining > 0
                                ) {
                                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Fortificar", fontSize = 12.sp)
                                }

                                if (unit.troops < unit.maxTroops) {
                                    FilledTonalButton(
                                        onClick = onHealUnit,
                                        modifier = Modifier.weight(1f).testTag("btn_unit_heal"),
                                        enabled = playerFaction.food >= 15
                                    ) {
                                        Text(text = "❤️ Curar", fontSize = 12.sp)
                                    }
                                }

                                if (unit.type == UnitType.SETTLER) {
                                    Button(
                                        onClick = onFoundSettlement,
                                        modifier = Modifier.weight(1f).testTag("btn_found_city"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Text(text = "🏛️ Fundar", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // City / Territory Actions
            val isPlayerTerritory = selectedTile.ownerFaction == FactionId.PLAYER
            if (isPlayerTerritory) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTile.hasCity) {
                        Button(
                            onClick = { showRecruitDialog = true },
                            modifier = Modifier.weight(1f).testTag("btn_open_recruit"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Recrutar Tropas", fontSize = 13.sp)
                        }
                    }

                    if (selectedTile.building == null || !selectedTile.hasCity) {
                        FilledTonalButton(
                            onClick = { showBuildDialog = true },
                            modifier = Modifier.weight(1f).testTag("btn_open_build")
                        ) {
                            Icon(imageVector = Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Construir", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal dialog for Recruiting Units
    if (showRecruitDialog) {
        AlertDialog(
            onDismissRequest = { showRecruitDialog = false },
            title = {
                Text(text = "⚔️ Recrutar Tropas no Reino", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnitType.entries.forEach { unitType ->
                        val hasTech = unitType.requiredTechId == null || playerFaction.researchedTechs.contains(unitType.requiredTechId)
                        val canAfford = playerFaction.canAfford(unitType.foodCost, unitType.woodCost, unitType.stoneCost, unitType.goldCost)
                        val enabled = hasTech && canAfford && selectedTile.unit == null

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = enabled) {
                                    onRecruitUnit(unitType)
                                    showRecruitDialog = false
                                },
                            color = if (enabled) Color(0xFF1E293B) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = unitType.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (hasTech) Color.White else Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "⚔️ ${unitType.attackPower} • 🛡️ ${unitType.defensePower} • 👟 ${unitType.maxMovement}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    if (!hasTech) {
                                        Text(
                                            text = "Requer Pesquisa: ${unitType.requiredTechId}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFF87171)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${unitType.foodCost} 🌾 ${unitType.goldCost} 🪙",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAfford) Color(0xFFFBBF24) else Color(0xFFEF4444)
                                    )
                                    if (unitType.woodCost > 0 || unitType.stoneCost > 0) {
                                        Text(
                                            text = "${unitType.woodCost} 🪵 ${unitType.stoneCost} ⛏️",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecruitDialog = false }) {
                    Text("Voltar")
                }
            }
        )
    }

    // Modal dialog for Building Improvements
    if (showBuildDialog) {
        AlertDialog(
            onDismissRequest = { showBuildDialog = false },
            title = {
                Text(text = "🏗️ Construir Estrutura no Terreno", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val buildableTypes = listOf(
                        BuildingType.FARMSTEAD,
                        BuildingType.LUMBER_MILL,
                        BuildingType.QUARRY,
                        BuildingType.WATCHTOWER,
                        BuildingType.MARKET,
                        BuildingType.ACADEMY
                    )

                    buildableTypes.forEach { bType ->
                        val hasTech = bType.requiredTechId == null || playerFaction.researchedTechs.contains(bType.requiredTechId)
                        val canAfford = playerFaction.canAfford(bType.foodCost, bType.woodCost, bType.stoneCost, bType.goldCost)
                        val enabled = hasTech && canAfford

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = enabled) {
                                    onBuildStructure(bType)
                                    showBuildDialog = false
                                },
                            color = if (enabled) Color(0xFF1E293B) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bType.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (hasTech) Color.White else Color(0xFF64748B)
                                    )
                                    Text(
                                        text = bType.description,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Text(
                                    text = "${bType.woodCost}🪵 ${bType.stoneCost}⛏️ ${bType.goldCost}🪙",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAfford) Color(0xFFFBBF24) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBuildDialog = false }) {
                    Text("Voltar")
                }
            }
        )
    }
}
