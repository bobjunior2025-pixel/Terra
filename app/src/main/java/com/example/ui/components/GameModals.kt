package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun TechTreeDialog(
    playerFaction: Faction,
    onSelectTech: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFFBBF24))
                Text(text = "Árvore de Tecnologias", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TechRegistry.ALL_TECHS) { tech ->
                    val isResearched = playerFaction.researchedTechs.contains(tech.id)
                    val isCurrent = playerFaction.currentResearchId == tech.id
                    val canResearch = !isResearched && (tech.prerequisiteId == null || playerFaction.researchedTechs.contains(tech.prerequisiteId))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) Color(0xFF38BDF8) else if (isResearched) Color(0xFF10B981) else Color(0xFF334155),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = canResearch) {
                                onSelectTech(tech.id)
                            },
                        color = if (isResearched) Color(0xFF064E3B) else if (isCurrent) Color(0xFF0C4A6E) else Color(0xFF1E293B)
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
                                    Text(text = tech.icon, fontSize = 18.sp)
                                    Text(
                                        text = tech.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }

                                if (isResearched) {
                                    Surface(
                                        color = Color(0xFF10B981),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Concluído",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isCurrent) {
                                    Text(
                                        text = "${playerFaction.researchProgress}/${tech.scienceCost} 🧪",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                } else {
                                    Text(
                                        text = "${tech.scienceCost} 🧪",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tech.description,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )

                            if (isCurrent) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        (playerFaction.researchProgress.toFloat() / tech.scienceCost.toFloat()).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF38BDF8),
                                    trackColor = Color(0xFF1E293B),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun MinimapDialog(
    tiles: List<GameTile>,
    mapWidth: Int,
    mapHeight: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF60A5FA))
                Text(text = "Radar & Visão Geral do Mundo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mini 2D pixel grid representing the map
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(4.dp)
                ) {
                    for (y in 0 until mapHeight) {
                        Row {
                            for (x in 0 until mapWidth) {
                                val tile = tiles[y * mapWidth + x]
                                val pixelColor = when {
                                    !tile.isExplored -> Color(0xFF1E293B)
                                    tile.unit?.factionId == FactionId.PLAYER -> Color(0xFFA78BFA)
                                    tile.unit != null -> Color(tile.unit.factionId.colorHex)
                                    tile.ownerFaction == FactionId.PLAYER -> Color(0xFF7C3AED)
                                    tile.ownerFaction != null -> Color(tile.ownerFaction.colorHex).copy(alpha = 0.7f)
                                    tile.terrain == TerrainType.WATER_RIVER || tile.terrain == TerrainType.WATER_OCEAN -> Color(0xFF38BDF8)
                                    tile.terrain == TerrainType.MOUNTAIN || tile.terrain == TerrainType.MOUNTAIN_SNOW || tile.terrain == TerrainType.MOUNTAIN_GOLD -> Color(0xFF78716C)
                                    tile.terrain == TerrainType.FOREST_PINE || tile.terrain == TerrainType.FOREST_OAK -> Color(0xFF15803D)
                                    else -> Color(0xFF84CC16)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(0.5.dp)
                                        .background(pixelColor)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(color = Color(0xFF7C3AED), text = "Seu Reino")
                    LegendItem(color = Color(0xFFEF4444), text = "Carmesim")
                    LegendItem(color = Color(0xFF10B981), text = "Esmeralda")
                    LegendItem(color = Color(0xFF3B82F6), text = "Celeste")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = text, fontSize = 10.sp, color = Color(0xFF94A3B8))
    }
}

@Composable
fun KingdomStatsDialog(
    factions: Map<FactionId, Faction>,
    tiles: List<GameTile>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF38BDF8))
                Text(text = "Poder dos Reinos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                factions.values.forEach { faction ->
                    val territoryCount = tiles.count { it.ownerFaction == faction.id }
                    val armyStrength = tiles.filter { it.unit?.factionId == faction.id }.sumOf { it.unit?.troops ?: 0 }
                    val citiesCount = tiles.count { it.ownerFaction == faction.id && it.hasCity }
                    val totalScore = territoryCount * 10 + armyStrength * 5 + citiesCount * 50 + faction.researchedTechs.size * 25

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(faction.colorHex))
                                    )
                                    Text(
                                        text = faction.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "$totalScore pts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Territórios: $territoryCount • Cidades: $citiesCount • Tropas: $armyStrength • Tesouro: ${faction.gold}🪙",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun VictoryDialog(
    playerFaction: Faction,
    tiles: List<GameTile>,
    onDismiss: () -> Unit
) {
    val totalTerritory = tiles.count { it.ownerFaction == FactionId.PLAYER }
    val isDominationWon = totalTerritory >= 50
    val isScienceWon = playerFaction.researchedTechs.size >= 8
    val isEconomicWon = playerFaction.gold >= 800

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24))
                Text(text = "Condições de Vitória", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VictoryGoalCard(
                    title = "⚔️ Vitória por Dominação",
                    desc = "Controle ao menos 50 territórios no continente.",
                    current = totalTerritory,
                    target = 50,
                    isAchieved = isDominationWon
                )
                VictoryGoalCard(
                    title = "📜 Vitória Científica",
                    desc = "Pesquise 8 tecnologias na Árvore de Inovação.",
                    current = playerFaction.researchedTechs.size,
                    target = 8,
                    isAchieved = isScienceWon
                )
                VictoryGoalCard(
                    title = "💰 Vitória Econômica",
                    desc = "Acumule 800 Moedas de Ouro no tesouro imperial.",
                    current = playerFaction.gold,
                    target = 800,
                    isAchieved = isEconomicWon
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Continuar Conquista")
            }
        }
    )
}

@Composable
private fun VictoryGoalCard(
    title: String,
    desc: String,
    current: Int,
    target: Int,
    isAchieved: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAchieved) Color(0xFF064E3B) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                if (isAchieved) {
                    Text(text = "👑 VENCEDOR!", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF34D399))
                } else {
                    Text(text = "$current / $target", fontSize = 12.sp, color = Color(0xFFFBBF24))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun EventLogDialog(
    events: List<BattleEvent>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "📜 Crônicas & Relatos de Batalha", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(events.reversed()) { event ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            if (event.turn > 0) {
                                Text(
                                    text = "Turno ${event.turn}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                text = event.message,
                                fontSize = 12.sp,
                                color = if (event.isVictory) Color(0xFFF8FAFC) else Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun SettingsDialog(
    isSoundEnabled: Boolean,
    isFogEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleFog: () -> Unit,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "⚙️ Configurações do Jogo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sons & Efeitos Hápticos", fontSize = 14.sp)
                    Switch(checked = isSoundEnabled, onCheckedChange = { onToggleSound() })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Névoa de Guerra (Fog of War)", fontSize = 14.sp)
                    Switch(checked = isFogEnabled, onCheckedChange = { onToggleFog() })
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onNewGame()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Reiniciar / Novo Mapa Procedural")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
