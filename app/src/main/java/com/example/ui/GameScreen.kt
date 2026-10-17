package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Faction
import com.example.model.FactionId
import com.example.ui.components.*
import kotlin.math.floor

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val textMeasurer = rememberTextMeasurer()
    val haptics = LocalHapticFeedback.current

    // Dialog state toggles
    var showTechDialog by remember { mutableStateOf(false) }
    var showMinimapDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showVictoryDialog by remember { mutableStateOf(false) }
    var showEventLogDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Wave and pulse animation timer
    val infiniteTransition = rememberInfiniteTransition(label = "game_animation")
    val animTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animTick"
    )

    val playerFaction = uiState.factions[FactionId.PLAYER] ?: Faction(
        id = FactionId.PLAYER,
        name = FactionId.PLAYER.defaultName,
        colorHex = FactionId.PLAYER.colorHex,
        isHuman = true
    )

    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopResourceBar(
                currentTurn = uiState.currentTurn,
                currentSeason = uiState.currentSeason,
                playerFaction = playerFaction,
                isSoundEnabled = uiState.isSoundEnabled,
                onNextTurn = {
                    viewModel.onNextTurn()
                },
                onOpenMap = { showMinimapDialog = true },
                onOpenVictories = { showVictoryDialog = true },
                onOpenStats = { showStatsDialog = true },
                onOpenTech = { showTechDialog = true },
                onToggleSound = { viewModel.toggleSound() },
                onOpenSave = { showSettingsDialog = true },
                onOpenSettings = { showSettingsDialog = true },
                onOpenDiplomacy = { showStatsDialog = true }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            val screenWidth = maxWidth.value
            val screenHeight = maxHeight.value

            // Base tile size in points
            val baseTileSize = (screenWidth / uiState.mapWidth.toFloat()).coerceAtLeast(46f)
            val currentTileSize = baseTileSize * uiState.zoomScale

            // Interactive Pan & Zoom Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("game_canvas")
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewModel.setZoomScale(uiState.zoomScale * zoom)
                            viewModel.setPanOffset(uiState.panOffset + pan)
                        }
                    }
                    .pointerInput(uiState.zoomScale, uiState.panOffset) {
                        detectTapGestures { tapOffset ->
                            // Convert screen tap coordinates to grid (x, y)
                            val relativeX = tapOffset.x - uiState.panOffset.x
                            val relativeY = tapOffset.y - uiState.panOffset.y

                            val gridX = floor(relativeX / currentTileSize).toInt()
                            val gridY = floor(relativeY / currentTileSize).toInt()

                            if (gridX in 0 until uiState.mapWidth && gridY in 0 until uiState.mapHeight) {
                                viewModel.onTileClicked(gridX, gridY)
                            }
                        }
                    }
            ) {
                // Render all tiles
                uiState.tiles.forEach { tile ->
                    val xPos = tile.x * currentTileSize + uiState.panOffset.x
                    val yPos = tile.y * currentTileSize + uiState.panOffset.y

                    // Culling: only draw tiles within visible screen bounds
                    if (xPos + currentTileSize >= 0 && xPos <= size.width &&
                        yPos + currentTileSize >= 0 && yPos <= size.height
                    ) {
                        val isSelected = uiState.selectedTile?.let { it.x == tile.x && it.y == tile.y } ?: false
                        val isInMoveRange = uiState.reachableCoords.contains(Pair(tile.x, tile.y))
                        val isInAttackRange = uiState.attackableCoords.contains(Pair(tile.x, tile.y))

                        TileCanvasRenderer.drawTile(
                            scope = this,
                            tile = tile,
                            xPos = xPos,
                            yPos = yPos,
                            tileSize = currentTileSize,
                            isSelected = isSelected,
                            isInMoveRange = isInMoveRange,
                            isInAttackRange = isInAttackRange,
                            textMeasurer = textMeasurer,
                            animationTick = animTick,
                            season = uiState.currentSeason
                        )
                    }
                }
            }

            // Floating Controls: Zoom In (+), Zoom Out (-), Recenter on Capital
            // Exactly matching the bottom right layout in the user's screenshot!
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (uiState.selectedTile != null) 220.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingMapButton(
                    icon = Icons.Default.Add,
                    contentDesc = "Aproximar Zoom",
                    onClick = { viewModel.zoomIn() },
                    tag = "btn_zoom_in"
                )
                FloatingMapButton(
                    icon = Icons.Default.Remove,
                    contentDesc = "Afastar Zoom",
                    onClick = { viewModel.zoomOut() },
                    tag = "btn_zoom_out"
                )
                FloatingMapButton(
                    icon = Icons.Default.MyLocation,
                    contentDesc = "Recentrar no Castelo da Capital",
                    onClick = { viewModel.recenterOnCapital() },
                    tag = "btn_recenter"
                )
            }

            // Bottom Action Drawer: Selected Tile, Unit Orders, Recruitment & Building
            AnimatedVisibility(
                visible = uiState.selectedTile != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                TileActionPanel(
                    selectedTile = uiState.selectedTile,
                    playerFaction = playerFaction,
                    isTargetingMode = uiState.isTargetingMode,
                    onToggleTargeting = { viewModel.toggleTargetingMode() },
                    onFortifyUnit = { viewModel.fortifySelectedUnit() },
                    onHealUnit = { viewModel.healSelectedUnit() },
                    onFoundSettlement = { viewModel.foundSettlement() },
                    onRecruitUnit = { unitType -> viewModel.recruitUnit(unitType) },
                    onBuildStructure = { buildingType -> viewModel.buildStructure(buildingType) },
                    onDeselect = { viewModel.deselectTile() }
                )
            }
        }
    }

    // Modal Dialogs
    if (showTechDialog) {
        TechTreeDialog(
            playerFaction = playerFaction,
            onSelectTech = { techId ->
                viewModel.selectTech(techId)
                showTechDialog = false
            },
            onDismiss = { showTechDialog = false }
        )
    }

    if (showMinimapDialog) {
        MinimapDialog(
            tiles = uiState.tiles,
            mapWidth = uiState.mapWidth,
            mapHeight = uiState.mapHeight,
            onDismiss = { showMinimapDialog = false }
        )
    }

    if (showStatsDialog) {
        KingdomStatsDialog(
            factions = uiState.factions,
            tiles = uiState.tiles,
            onDismiss = { showStatsDialog = false }
        )
    }

    if (showVictoryDialog) {
        VictoryDialog(
            playerFaction = playerFaction,
            tiles = uiState.tiles,
            onDismiss = { showVictoryDialog = false }
        )
    }

    if (showEventLogDialog) {
        EventLogDialog(
            events = uiState.battleEvents,
            onDismiss = { showEventLogDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            isSoundEnabled = uiState.isSoundEnabled,
            isFogEnabled = uiState.isFogEnabled,
            onToggleSound = { viewModel.toggleSound() },
            onToggleFog = { viewModel.toggleFog() },
            onNewGame = { viewModel.startNewGame() },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun FloatingMapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .shadow(4.dp, shape = CircleShape)
            .clip(CircleShape)
            .testTag(tag),
        color = Color(0xFFF8FAFC),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
