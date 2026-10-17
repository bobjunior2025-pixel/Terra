package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlin.math.cos
import kotlin.math.sin

object TileCanvasRenderer {

    // Palettes matching and enhancing the screenshot
    private val COLOR_PLAINS_1 = Color(0xFFD6E9B9)
    private val COLOR_PLAINS_2 = Color(0xFFCEE4AC)
    private val COLOR_WATER_RIVER = Color(0xFF75BEE9)
    private val COLOR_WATER_DEEP = Color(0xFF5BAEDF)
    private val COLOR_FOREST_BG = Color(0xFFA5D19A)
    private val COLOR_MOUNTAIN_BG = Color(0xFFD3BBA9)
    private val COLOR_GRID_LINE = Color(0x22374151)
    private val COLOR_FOG = Color(0xCC1E293B)
    private val COLOR_FOG_STROKE = Color(0xFF334155)

    fun drawTile(
        scope: DrawScope,
        tile: GameTile,
        xPos: Float,
        yPos: Float,
        tileSize: Float,
        isSelected: Boolean,
        isInMoveRange: Boolean,
        isInAttackRange: Boolean,
        textMeasurer: TextMeasurer,
        animationTick: Float = 0f
    ) {
        with(scope) {
            val rect = Size(tileSize, tileSize)
            val isEven = (tile.x + tile.y) % 2 == 0

            // 1. Base Ground Color based on terrain
            val groundColor = when (tile.terrain) {
                TerrainType.WATER_RIVER -> COLOR_WATER_RIVER
                TerrainType.WATER_OCEAN -> COLOR_WATER_DEEP
                TerrainType.FOREST_PINE, TerrainType.FOREST_OAK -> COLOR_FOREST_BG
                TerrainType.MOUNTAIN, TerrainType.MOUNTAIN_SNOW, TerrainType.MOUNTAIN_GOLD -> COLOR_MOUNTAIN_BG
                else -> if (isEven) COLOR_PLAINS_1 else COLOR_PLAINS_2
            }

            drawRect(
                color = groundColor,
                topLeft = Offset(xPos, yPos),
                size = rect
            )

            // 2. Subtle decorative wheat/leaf pattern in plains (like the subtle watermark in screenshot)
            if (tile.terrain == TerrainType.PLAINS || tile.terrain == TerrainType.FARMLAND_WHEAT || tile.terrain == TerrainType.FARMLAND_CORN) {
                drawSubtleLeafPattern(xPos, yPos, tileSize)
            }

            // 3. Draw Terrain Feature Graphic
            when (tile.terrain) {
                TerrainType.FARMLAND_WHEAT -> drawWheatStalk(xPos, yPos, tileSize)
                TerrainType.FARMLAND_CORN -> drawCornCob(xPos, yPos, tileSize)
                TerrainType.ORCHARD_APPLE -> drawAppleFruit(xPos, yPos, tileSize)
                TerrainType.FOREST_PINE -> drawPineTree(xPos, yPos, tileSize)
                TerrainType.FOREST_OAK -> drawOakTree(xPos, yPos, tileSize)
                TerrainType.PASTURE_HORSES -> drawHorseHead(xPos, yPos, tileSize)
                TerrainType.MOUNTAIN -> drawMountainPeak(xPos, yPos, tileSize, hasSnow = false, hasGold = false)
                TerrainType.MOUNTAIN_SNOW -> drawMountainPeak(xPos, yPos, tileSize, hasSnow = true, hasGold = false)
                TerrainType.MOUNTAIN_GOLD -> drawMountainPeak(xPos, yPos, tileSize, hasSnow = false, hasGold = true)
                TerrainType.WATER_RIVER, TerrainType.WATER_OCEAN -> drawWaterWavesAndFish(xPos, yPos, tileSize, animationTick)
                TerrainType.SPICE_GROVE -> drawSpiceBag(xPos, yPos, tileSize)
                TerrainType.PLAINS -> {} // clean meadow
            }

            // 4. Draw Ancient Ruins if present
            if (tile.hasAncientRuins && tile.isExplored) {
                drawAncientRuins(xPos, yPos, tileSize, animationTick)
            }

            // 5. Draw Building (Castle / Town / Farmstead, etc.)
            tile.building?.let { building ->
                drawBuilding(building, xPos, yPos, tileSize)
            }

            // 6. Draw Unit badge & troop count (e.g. 25, 20, 11)
            tile.unit?.let { unit ->
                drawUnitBadge(unit, xPos, yPos, tileSize, textMeasurer)
            }

            // 7. Territory Border Lines & Shield Insignia (like purple borders in user screenshot)
            tile.ownerFaction?.let { factionId ->
                drawTerritoryBorder(factionId, xPos, yPos, tileSize)
            }

            // 8. Grid Border Lines
            drawRect(
                color = COLOR_GRID_LINE,
                topLeft = Offset(xPos, yPos),
                size = rect,
                style = Stroke(width = 1f)
            )

            // 9. Highlights: Selection, Move Range, Attack Range
            if (isSelected) {
                drawRect(
                    color = Color(0xFFFBBF24), // Vibrant Gold selection
                    topLeft = Offset(xPos + 2, yPos + 2),
                    size = Size(tileSize - 4, tileSize - 4),
                    style = Stroke(width = 3.5f)
                )
            } else if (isInMoveRange) {
                // Friendly reachable tile
                drawRect(
                    color = Color(0x4460A5FA),
                    topLeft = Offset(xPos, yPos),
                    size = rect
                )
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = tileSize * 0.12f,
                    center = Offset(xPos + tileSize / 2, yPos + tileSize / 2)
                )
            } else if (isInAttackRange) {
                // Enemy attackable tile
                drawRect(
                    color = Color(0x66EF4444),
                    topLeft = Offset(xPos, yPos),
                    size = rect
                )
                drawCircle(
                    color = Color(0xFFDC2626),
                    radius = tileSize * 0.15f,
                    center = Offset(xPos + tileSize / 2, yPos + tileSize / 2),
                    style = Stroke(width = 2.5f)
                )
            }

            // 10. Fog of War (Unexplored)
            if (!tile.isExplored) {
                drawRect(
                    color = COLOR_FOG,
                    topLeft = Offset(xPos, yPos),
                    size = rect
                )
                // Ancient map parchment texture dots
                drawCircle(
                    color = COLOR_FOG_STROKE,
                    radius = tileSize * 0.10f,
                    center = Offset(xPos + tileSize * 0.5f, yPos + tileSize * 0.5f)
                )
            }
        }
    }

    private fun DrawScope.drawSubtleLeafPattern(x: Float, y: Float, size: Float) {
        val path = Path().apply {
            moveTo(x + size * 0.25f, y + size * 0.75f)
            quadraticTo(x + size * 0.5f, y + size * 0.5f, x + size * 0.75f, y + size * 0.25f)
        }
        drawPath(path, color = Color(0x18000000), style = Stroke(width = 3f, cap = StrokeCap.Round))
    }

    private fun DrawScope.drawWheatStalk(x: Float, y: Float, size: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.55f
        val stalkColor = Color(0xFFD97706)
        val grainColor = Color(0xFFFBBF24)

        // Stem
        drawLine(stalkColor, Offset(cx, cy + size * 0.25f), Offset(cx, cy - size * 0.2f), strokeWidth = 2.5f)
        // Wheat kernels
        for (i in -2..2) {
            val ky = cy + i * (size * 0.08f)
            drawOval(
                color = grainColor,
                topLeft = Offset(cx - size * 0.12f, ky - size * 0.04f),
                size = Size(size * 0.10f, size * 0.07f)
            )
            drawOval(
                color = grainColor,
                topLeft = Offset(cx + size * 0.02f, ky - size * 0.04f),
                size = Size(size * 0.10f, size * 0.07f)
            )
        }
    }

    private fun DrawScope.drawCornCob(x: Float, y: Float, size: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.55f
        // Green leaves
        val huskPath = Path().apply {
            moveTo(cx - size * 0.15f, cy + size * 0.2f)
            quadraticTo(cx - size * 0.25f, cy, cx, cy + size * 0.25f)
            quadraticTo(cx + size * 0.25f, cy, cx + size * 0.15f, cy + size * 0.2f)
        }
        drawPath(huskPath, color = Color(0xFF4ADE80))

        // Golden corn cob
        drawRoundRect(
            color = Color(0xFFFACC15),
            topLeft = Offset(cx - size * 0.10f, cy - size * 0.22f),
            size = Size(size * 0.20f, size * 0.38f),
            cornerRadius = CornerRadius(size * 0.09f, size * 0.09f)
        )
    }

    private fun DrawScope.drawAppleFruit(x: Float, y: Float, size: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.55f
        // Red juicy apple
        drawCircle(
            color = Color(0xFFEF4444),
            radius = size * 0.22f,
            center = Offset(cx, cy)
        )
        // Little leaf
        drawOval(
            color = Color(0xFF22C55E),
            topLeft = Offset(cx, cy - size * 0.32f),
            size = Size(size * 0.12f, size * 0.08f)
        )
        // Apple shine
        drawCircle(
            color = Color(0x66FFFFFF),
            radius = size * 0.06f,
            center = Offset(cx - size * 0.08f, cy - size * 0.08f)
        )
    }

    private fun DrawScope.drawPineTree(x: Float, y: Float, size: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f
        // Trunk
        drawRect(
            color = Color(0xFF78350F),
            topLeft = Offset(cx - size * 0.04f, cy + size * 0.15f),
            size = Size(size * 0.08f, size * 0.20f)
        )
        // 3 Tiers of conifer pine triangles
        val tiers = listOf(
            Triple(cy - size * 0.28f, size * 0.18f, Color(0xFF15803D)),
            Triple(cy - size * 0.10f, size * 0.26f, Color(0xFF166534)),
            Triple(cy + size * 0.08f, size * 0.34f, Color(0xFF14532D))
        )
        for ((tierY, w, c) in tiers) {
            val tri = Path().apply {
                moveTo(cx, tierY)
                lineTo(cx - w, tierY + size * 0.20f)
                lineTo(cx + w, tierY + size * 0.20f)
                close()
            }
            drawPath(tri, color = c)
        }
    }

    private fun DrawScope.drawOakTree(x: Float, y: Float, size: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f
        // Trunk
        drawRect(
            color = Color(0xFF78350F),
            topLeft = Offset(cx - size * 0.05f, cy + size * 0.10f),
            size = Size(size * 0.10f, size * 0.25f)
        )
        // Fluffy leafy crown
        drawCircle(Color(0xFF15803D), radius = size * 0.22f, center = Offset(cx, cy - size * 0.08f))
        drawCircle(Color(0xFF16A34A), radius = size * 0.18f, center = Offset(cx - size * 0.12f, cy - size * 0.04f))
        drawCircle(Color(0xFF22C55E), radius = size * 0.18f, center = Offset(cx + size * 0.12f, cy - size * 0.04f))
    }

    private fun DrawScope.drawHorseHead(x: Float, y: Float, size: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.52f
        // Brown horse head silhouette
        val horsePath = Path().apply {
            moveTo(cx - size * 0.18f, cy + size * 0.25f)
            lineTo(cx - size * 0.18f, cy - size * 0.05f)
            lineTo(cx - size * 0.12f, cy - size * 0.25f) // ear
            lineTo(cx - size * 0.04f, cy - size * 0.15f)
            lineTo(cx + size * 0.18f, cy - size * 0.05f) // muzzle
            lineTo(cx + size * 0.16f, cy + size * 0.10f)
            lineTo(cx + size * 0.05f, cy + size * 0.12f) // jaw
            lineTo(cx + size * 0.08f, cy + size * 0.25f)
            close()
        }
        drawPath(horsePath, color = Color(0xFFB45309))
        // Mane
        val manePath = Path().apply {
            moveTo(cx - size * 0.18f, cy - size * 0.05f)
            lineTo(cx - size * 0.25f, cy + size * 0.05f)
            lineTo(cx - size * 0.20f, cy + size * 0.15f)
            lineTo(cx - size * 0.16f, cy + size * 0.20f)
            close()
        }
        drawPath(manePath, color = Color(0xFF78350F))
        // Eye
        drawCircle(Color(0xFF1F2937), radius = size * 0.03f, center = Offset(cx, cy - size * 0.04f))
    }

    private fun DrawScope.drawMountainPeak(x: Float, y: Float, size: Float, hasSnow: Boolean, hasGold: Boolean) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.55f

        // Shaded Mountain Body
        val leftSlope = Path().apply {
            moveTo(cx, cy - size * 0.32f)
            lineTo(cx - size * 0.38f, cy + size * 0.26f)
            lineTo(cx, cy + size * 0.26f)
            close()
        }
        drawPath(leftSlope, color = Color(0xFF0D9488)) // Teal/greenish rock as in screenshot

        val rightSlope = Path().apply {
            moveTo(cx, cy - size * 0.32f)
            lineTo(cx + size * 0.38f, cy + size * 0.26f)
            lineTo(cx, cy + size * 0.26f)
            close()
        }
        drawPath(rightSlope, color = Color(0xFF0F766E)) // Darker slope

        // Snow Cap
        if (hasSnow) {
            val snowPath = Path().apply {
                moveTo(cx, cy - size * 0.32f)
                lineTo(cx - size * 0.16f, cy - size * 0.12f)
                lineTo(cx - size * 0.05f, cy - size * 0.08f)
                lineTo(cx + size * 0.06f, cy - size * 0.14f)
                lineTo(cx + size * 0.16f, cy - size * 0.12f)
                close()
            }
            drawPath(snowPath, color = Color(0xFFF8FAFC))
        }

        // Sparkling Gold / Gem icon (like in user screenshot)
        if (hasGold) {
            val gemX = cx + size * 0.22f
            val gemY = cy - size * 0.05f
            val gemRadius = size * 0.12f

            val diamondPath = Path().apply {
                moveTo(gemX, gemY - gemRadius)
                lineTo(gemX + gemRadius, gemY)
                lineTo(gemX, gemY + gemRadius)
                lineTo(gemX - gemRadius, gemY)
                close()
            }
            drawPath(diamondPath, color = Color(0xFFFBBF24))
            drawPath(diamondPath, color = Color(0xFFD97706), style = Stroke(width = 1.5f))
        }
    }

    private fun DrawScope.drawWaterWavesAndFish(x: Float, y: Float, size: Float, tick: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f
        val waveOffset = (sin(tick + x) * size * 0.05f)

        // Water Ripple
        val wavePath = Path().apply {
            moveTo(x + size * 0.15f, cy + waveOffset)
            quadraticTo(x + size * 0.35f, cy + size * 0.1f + waveOffset, x + size * 0.55f, cy + waveOffset)
            quadraticTo(x + size * 0.75f, cy - size * 0.1f + waveOffset, x + size * 0.85f, cy + waveOffset)
        }
        drawPath(wavePath, color = Color(0x55FFFFFF), style = Stroke(width = 2f, cap = StrokeCap.Round))

        // Red/Orange Leaping Fish (like screenshot)
        val fishPath = Path().apply {
            moveTo(cx - size * 0.15f, cy)
            quadraticTo(cx, cy - size * 0.12f, cx + size * 0.15f, cy)
            lineTo(cx + size * 0.22f, cy - size * 0.08f) // tail top
            lineTo(cx + size * 0.18f, cy)
            lineTo(cx + size * 0.22f, cy + size * 0.08f) // tail bottom
            quadraticTo(cx, cy + size * 0.12f, cx - size * 0.15f, cy)
            close()
        }
        drawPath(fishPath, color = Color(0xFFFB7185))
    }

    private fun DrawScope.drawSpiceBag(x: Float, y: Float, size: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.55f
        // Burlap spice sacks with cinnamon/paprika
        drawCircle(Color(0xFFC2410C), radius = size * 0.16f, center = Offset(cx - size * 0.08f, cy))
        drawCircle(Color(0xFFD97706), radius = size * 0.14f, center = Offset(cx + size * 0.10f, cy + size * 0.04f))
    }

    private fun DrawScope.drawAncientRuins(x: Float, y: Float, size: Float, tick: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.45f
        // Marble columns
        drawRect(Color(0xFFE2E8F0), topLeft = Offset(cx - size * 0.18f, cy - size * 0.10f), size = Size(size * 0.08f, size * 0.25f))
        drawRect(Color(0xFFE2E8F0), topLeft = Offset(cx + size * 0.10f, cy - size * 0.10f), size = Size(size * 0.08f, size * 0.25f))
        drawRect(Color(0xFFCBD5E1), topLeft = Offset(cx - size * 0.22f, cy - size * 0.18f), size = Size(size * 0.44f, size * 0.08f))

        // Golden relic pulse
        val pulse = (sin(tick * 2f) * 0.5f + 0.5f)
        drawCircle(
            color = Color(0xFFFBBF24).copy(alpha = 0.4f * pulse),
            radius = size * 0.25f,
            center = Offset(cx, cy)
        )
    }

    private fun DrawScope.drawBuilding(building: Building, x: Float, y: Float, size: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.55f

        when (building.type) {
            BuildingType.CAPITAL_CASTLE -> {
                // Majestic medieval fortress castle (like the bottom-center castle in user screenshot!)
                val castleW = size * 0.70f
                val castleH = size * 0.55f

                // Base stone keep
                drawRoundRect(
                    color = Color(0xFF94A3B8),
                    topLeft = Offset(cx - castleW / 2, cy - castleH / 2),
                    size = Size(castleW, castleH),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Shaded castle roofs & towers
                // Left tower
                drawRect(Color(0xFF64748B), topLeft = Offset(cx - castleW / 2, cy - castleH / 2 - size * 0.15f), size = Size(size * 0.20f, size * 0.20f))
                // Right tower
                drawRect(Color(0xFF64748B), topLeft = Offset(cx + castleW / 2 - size * 0.20f, cy - castleH / 2 - size * 0.15f), size = Size(size * 0.20f, size * 0.20f))
                // Central Keep
                drawRect(Color(0xFF475569), topLeft = Offset(cx - size * 0.15f, cy - castleH / 2 - size * 0.22f), size = Size(size * 0.30f, size * 0.25f))

                // Red/Purple Roof Cones
                val roofLeft = Path().apply {
                    moveTo(cx - castleW / 2 + size * 0.10f, cy - castleH / 2 - size * 0.28f)
                    lineTo(cx - castleW / 2, cy - castleH / 2 - size * 0.15f)
                    lineTo(cx - castleW / 2 + size * 0.20f, cy - castleH / 2 - size * 0.15f)
                    close()
                }
                drawPath(roofLeft, color = Color(0xFFDC2626))

                val roofRight = Path().apply {
                    moveTo(cx + castleW / 2 - size * 0.10f, cy - castleH / 2 - size * 0.28f)
                    lineTo(cx + castleW / 2 - size * 0.20f, cy - castleH / 2 - size * 0.15f)
                    lineTo(cx + castleW / 2, cy - castleH / 2 - size * 0.15f)
                    close()
                }
                drawPath(roofRight, color = Color(0xFFDC2626))

                // Fortress Gate
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(cx - size * 0.10f, cy + castleH / 2 - size * 0.22f),
                    size = Size(size * 0.20f, size * 0.22f),
                    cornerRadius = CornerRadius(size * 0.08f, size * 0.08f)
                )

                // Fluttering Pennant Flag on top
                drawLine(Color(0xFF334155), Offset(cx, cy - castleH / 2 - size * 0.22f), Offset(cx, cy - castleH / 2 - size * 0.40f), strokeWidth = 2f)
                val flagPath = Path().apply {
                    moveTo(cx, cy - castleH / 2 - size * 0.40f)
                    lineTo(cx + size * 0.18f, cy - castleH / 2 - size * 0.35f)
                    lineTo(cx, cy - castleH / 2 - size * 0.30f)
                    close()
                }
                drawPath(flagPath, color = Color(0xFF9333EA)) // Royal Purple Banner
            }
            BuildingType.TOWN -> {
                // Stone village houses
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(cx - size * 0.25f, cy - size * 0.15f),
                    size = Size(size * 0.50f, size * 0.40f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Red peaked roof
                val roof = Path().apply {
                    moveTo(cx, cy - size * 0.35f)
                    lineTo(cx - size * 0.30f, cy - size * 0.15f)
                    lineTo(cx + size * 0.30f, cy - size * 0.15f)
                    close()
                }
                drawPath(roof, color = Color(0xFFEA580C))
            }
            BuildingType.FARMSTEAD -> {
                drawRect(Color(0xFFB45309), topLeft = Offset(cx - size * 0.15f, cy - size * 0.10f), size = Size(size * 0.30f, size * 0.25f))
            }
            BuildingType.LUMBER_MILL -> {
                drawCircle(Color(0xFF78350F), radius = size * 0.18f, center = Offset(cx, cy))
            }
            BuildingType.QUARRY -> {
                drawRect(Color(0xFF64748B), topLeft = Offset(cx - size * 0.15f, cy - size * 0.15f), size = Size(size * 0.30f, size * 0.30f))
            }
            BuildingType.WATCHTOWER -> {
                drawRect(Color(0xFF475569), topLeft = Offset(cx - size * 0.10f, cy - size * 0.30f), size = Size(size * 0.20f, size * 0.50f))
            }
            BuildingType.MARKET -> {
                drawCircle(Color(0xFFFBBF24), radius = size * 0.18f, center = Offset(cx, cy))
            }
            BuildingType.ACADEMY -> {
                drawCircle(Color(0xFF3B82F6), radius = size * 0.18f, center = Offset(cx, cy))
            }
        }
    }

    private fun DrawScope.drawUnitBadge(
        unit: GameUnit,
        x: Float,
        y: Float,
        size: Float,
        textMeasurer: TextMeasurer
    ) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f
        val badgeRadius = size * 0.30f

        // Outer faction color ring
        val factionColor = Color(unit.factionId.colorHex)
        drawCircle(
            color = Color(0xEEFFFFFF),
            radius = badgeRadius + 3f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = factionColor,
            radius = badgeRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 3.5f)
        )

        // Semi-transparent unit disk (matching the circular troop badges in screenshot!)
        drawCircle(
            color = Color(0xDD859F87), // muted tactical sage green disk from screenshot
            radius = badgeRadius - 2f,
            center = Offset(cx, cy)
        )

        // Silhouette of troops
        when (unit.type) {
            UnitType.SCOUT -> {
                // Compass / scout runner
                drawLine(Color(0xFF1E293B), Offset(cx, cy - badgeRadius * 0.5f), Offset(cx, cy + badgeRadius * 0.5f), strokeWidth = 2.5f)
                drawLine(Color(0xFF1E293B), Offset(cx - badgeRadius * 0.5f, cy), Offset(cx + badgeRadius * 0.5f, cy), strokeWidth = 2.5f)
            }
            UnitType.ARCHER -> {
                // Bow and arrow
                val bowPath = Path().apply {
                    moveTo(cx - badgeRadius * 0.4f, cy - badgeRadius * 0.4f)
                    quadraticTo(cx + badgeRadius * 0.1f, cy, cx - badgeRadius * 0.4f, cy + badgeRadius * 0.4f)
                }
                drawPath(bowPath, color = Color(0xFF1E293B), style = Stroke(width = 2.5f))
                drawLine(Color(0xFFDC2626), Offset(cx - badgeRadius * 0.3f, cy), Offset(cx + badgeRadius * 0.4f, cy), strokeWidth = 2f)
            }
            UnitType.KNIGHT -> {
                // Steed silhouette
                drawCircle(Color(0xFF1E293B), radius = badgeRadius * 0.35f, center = Offset(cx, cy))
            }
            UnitType.CATAPULT -> {
                // Siege wheel
                drawCircle(Color(0xFF1E293B), radius = badgeRadius * 0.35f, center = Offset(cx, cy), style = Stroke(width = 3f))
            }
            else -> {
                // Soldier squad ranks (like the tiny squad illustration in screenshot!)
                for (r in -1..1) {
                    for (c in -1..1) {
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = 2.5f,
                            center = Offset(cx + c * 6f, cy - 4f + r * 6f)
                        )
                    }
                }
            }
        }

        // Troop Count Badge on Top Right (like the "25", "20", "11" badges in the screenshot!)
        val countX = cx + badgeRadius * 0.70f
        val countY = cy - badgeRadius * 0.70f
        val countRadius = size * 0.16f

        drawCircle(
            color = Color(0xFFF1F5F9),
            radius = countRadius,
            center = Offset(countX, countY)
        )
        drawCircle(
            color = factionColor,
            radius = countRadius,
            center = Offset(countX, countY),
            style = Stroke(width = 1.5f)
        )

        val countText = unit.troops.toString()
        val textLayout = textMeasurer.measure(
            text = countText,
            style = TextStyle(
                fontSize = (size * 0.18f).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        )

        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                countX - textLayout.size.width / 2f,
                countY - textLayout.size.height / 2f
            )
        )
    }

    private fun DrawScope.drawTerritoryBorder(factionId: FactionId, x: Float, y: Float, size: Float) {
        val color = Color(factionId.colorHex)
        // Draw elegant glowing border lines (like the purple outlined tiles in user screenshot)
        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(size, size),
            style = Stroke(width = 2.5f)
        )

        // Small shield crest emblem in the top-left corner (visible in screenshot!)
        val shieldX = x + size * 0.08f
        val shieldY = y + size * 0.08f
        val shieldW = size * 0.18f
        val shieldH = size * 0.22f

        val shieldPath = Path().apply {
            moveTo(shieldX, shieldY)
            lineTo(shieldX + shieldW, shieldY)
            lineTo(shieldX + shieldW, shieldY + shieldH * 0.6f)
            quadraticTo(shieldX + shieldW * 0.5f, shieldY + shieldH, shieldX + shieldW * 0.5f, shieldY + shieldH)
            quadraticTo(shieldX, shieldY + shieldH * 0.6f, shieldX, shieldY + shieldH * 0.6f)
            close()
        }
        drawPath(shieldPath, color = color.copy(alpha = 0.25f), style = Fill)
        drawPath(shieldPath, color = color, style = Stroke(width = 1.5f))
    }
}
