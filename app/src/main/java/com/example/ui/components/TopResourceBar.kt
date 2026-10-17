package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Faction
import com.example.model.Season

@Composable
fun TopResourceBar(
    currentTurn: Int,
    currentSeason: Season,
    playerFaction: Faction,
    isSoundEnabled: Boolean,
    onNextTurn: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenVictories: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenTech: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenSave: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiplomacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF3EED9), // Classic warm antique beige parchment bar matching screenshot
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Row 1: Actions & Turn Button (exact match to screenshot layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Icon Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TopIconButton(
                        icon = Icons.Default.FavoriteBorder,
                        contentDesc = "Diplomacia & Paz",
                        onClick = onOpenDiplomacy,
                        tag = "btn_diplomacy"
                    )
                    TopIconButton(
                        icon = Icons.Default.Map,
                        contentDesc = "Minimapa",
                        onClick = onOpenMap,
                        tag = "btn_minimap"
                    )
                    TopIconButton(
                        icon = Icons.Default.EmojiEvents,
                        contentDesc = "Vitórias & Metas",
                        onClick = onOpenVictories,
                        tag = "btn_victories"
                    )
                    TopIconButton(
                        icon = Icons.Default.BarChart,
                        contentDesc = "Estatísticas do Reino",
                        onClick = onOpenStats,
                        tag = "btn_stats"
                    )
                    TopIconButton(
                        icon = Icons.Default.MilitaryTech,
                        contentDesc = "Árvore de Tecnologia",
                        onClick = onOpenTech,
                        tag = "btn_tech"
                    )
                    TopIconButton(
                        icon = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDesc = "Som & Efeitos",
                        onClick = onToggleSound,
                        tag = "btn_sound"
                    )
                    TopIconButton(
                        icon = Icons.Default.Save,
                        contentDesc = "Salvar / Carregar",
                        onClick = onOpenSave,
                        tag = "btn_save"
                    )
                    TopIconButton(
                        icon = Icons.Default.Settings,
                        contentDesc = "Configurações",
                        onClick = onOpenSettings,
                        tag = "btn_settings"
                    )
                }

                // Right Button: Turn Counter & Season Indicator (e.g., "32 ❄️ ->")
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = EaseInOutQuad),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Surface(
                    modifier = Modifier
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onNextTurn() }
                        .testTag("next_turn_button"),
                    color = Color(0xFF6AA77E), // Mint/Sage green button from screenshot
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$currentTurn",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = currentSeason.icon,
                            fontSize = 15.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Avançar Turno",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Resources Tally (Food, Wood, Stone, Gold, Science)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ResourcePill(icon = "🌾", label = "Comida", value = playerFaction.food, color = Color(0xFF15803D))
                ResourcePill(icon = "🪵", label = "Madeira", value = playerFaction.wood, color = Color(0xFF92400E))
                ResourcePill(icon = "⛏️", label = "Pedra", value = playerFaction.stone, color = Color(0xFF475569))
                ResourcePill(icon = "🪙", label = "Ouro", value = playerFaction.gold, color = Color(0xFFB45309))
                ResourcePill(icon = "🧪", label = "Ciência", value = playerFaction.science, color = Color(0xFF2563EB))
            }
        }
    }
}

@Composable
private fun TopIconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = Color(0xFF4B5563),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ResourcePill(
    icon: String,
    label: String,
    value: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x33FFFFFF))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = icon, fontSize = 12.sp)
        Text(
            text = "$value",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
