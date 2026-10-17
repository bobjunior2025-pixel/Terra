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

    // Natural realistic terrain color palettes
    private val COLOR_SUMMER_GRASS_LIGHT = Color(0xFFC7E298)
    private val COLOR_SUMMER_GRASS_DARK = Color(0xFFB5D480)
    private val COLOR_SPRING_GRASS = Color(0xFFBCE68E)
    private val COLOR_AUTUMN_GRASS = Color(0xFFD6C88D)
    private val COLOR_WINTER_GRASS = Color(0xFFDCE6DE)

    private val COLOR_DIRT_PATH = Color(0xFFBFA077)
    private val COLOR_SAND_SHORE = Color(0xFFE5D5A4)

    private val COLOR_WATER_SHALLOW = Color(0xFF5AB6E5)
    private val COLOR_WATER_DEEP = Color(0xFF297FB9)
    private val COLOR_WATER_ICE = Color(0xFFB9E5F3)

    private val COLOR_ROCK_LIGHT = Color(0xFF8C9BAE)
    private val COLOR_ROCK_DARK = Color(0xFF4C5866)
    private val COLOR_ROCK_SHADOW = Color(0xFF2E3844)

    private val COLOR_FOG_PAPER = Color(0xF21C2433)
    private val COLOR_FOG_DETAILS = Color(0xFF334155)

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
        animationTick: Float = 0f,
        season: Season = Season.SPRING
    ) {
        with(scope) {
            val rect = Size(tileSize, tileSize)
            val isEven = (tile.x + tile.y) % 2 == 0

            // 1. Realistic Base Ground with Elevation & Relief Gradients
            drawRealisticGround(tile, xPos, yPos, tileSize, isEven, season)

            // 2. Draw Realistic Shorelines & Sandy Riverbanks
            if (tile.terrain == TerrainType.WATER_RIVER || tile.terrain == TerrainType.WATER_OCEAN) {
                drawRealisticWater(xPos, yPos, tileSize, tile.terrain, animationTick, season)
            }

            // 3. Draw Realistic Natural Terrains
            when (tile.terrain) {
                TerrainType.PLAINS -> drawPlainsVegetation(xPos, yPos, tileSize, season, isEven)
                TerrainType.FARMLAND_WHEAT -> drawRealisticWheatField(xPos, yPos, tileSize, season)
                TerrainType.FARMLAND_CORN -> drawRealisticCornField(xPos, yPos, tileSize, season)
                TerrainType.ORCHARD_APPLE -> drawRealisticAppleOrchard(xPos, yPos, tileSize, season)
                TerrainType.FOREST_PINE -> drawRealisticPineForest(xPos, yPos, tileSize, season)
                TerrainType.FOREST_OAK -> drawRealisticOakForest(xPos, yPos, tileSize, season)
                TerrainType.PASTURE_HORSES -> drawRealisticHorsePasture(xPos, yPos, tileSize, season)
                TerrainType.MOUNTAIN -> drawRealisticMountain(xPos, yPos, tileSize, hasSnow = false, hasGold = false, season = season)
                TerrainType.MOUNTAIN_SNOW -> drawRealisticMountain(xPos, yPos, tileSize, hasSnow = true, hasGold = false, season = season)
                TerrainType.MOUNTAIN_GOLD -> drawRealisticMountain(xPos, yPos, tileSize, hasSnow = false, hasGold = true, season = season, animTick = animationTick)
                TerrainType.SPICE_GROVE -> drawRealisticSpiceGrove(xPos, yPos, tileSize, season)
                TerrainType.WATER_RIVER, TerrainType.WATER_OCEAN -> {
                    // River & ocean features already rendered in water shader
                }
            }

            // 4. Draw Ancient Ruins with 3D Marble & Mystical Aura
            if (tile.hasAncientRuins && tile.isExplored) {
                drawRealisticAncientRuins(xPos, yPos, tileSize, animationTick)
            }

            // 5. Draw High-Detail Medieval Buildings
            tile.building?.let { building ->
                drawRealisticBuilding(building, xPos, yPos, tileSize, season)
            }

            // 6. Draw Realistic Unit Medallions & Military Regiments
            tile.unit?.let { unit ->
                drawRealisticUnitBadge(unit, xPos, yPos, tileSize, textMeasurer)
            }

            // 7. Realistic Territory Borders & Enamelled Crest Seals
            tile.ownerFaction?.let { factionId ->
                drawRealisticTerritoryBorder(factionId, xPos, yPos, tileSize)
            }

            // 8. Subtle Tile Relief Edge (Simulating 3D Hex/Grid Tabletop Contour)
            drawRect(
                color = Color(0x1F000000),
                topLeft = Offset(xPos, yPos + tileSize - 1.5f),
                size = Size(tileSize, 1.5f)
            )
            drawRect(
                color = Color(0x1AFFFFFF),
                topLeft = Offset(xPos, yPos),
                size = Size(tileSize, 1.2f)
            )
            drawRect(
                color = Color(0x12000000),
                topLeft = Offset(xPos, yPos),
                size = rect,
                style = Stroke(width = 0.8f)
            )

            // 9. Tactical Highlights: Selection Glow, Movement Range, Attack Indicator
            if (isSelected) {
                // Outer Golden Glow
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B)),
                        start = Offset(xPos, yPos),
                        end = Offset(xPos + tileSize, yPos + tileSize)
                    ),
                    topLeft = Offset(xPos + 1.5f, yPos + 1.5f),
                    size = Size(tileSize - 3f, tileSize - 3f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 3.5f)
                )
            } else if (isInMoveRange) {
                // Friendly reachable territory pulse
                drawRect(
                    color = Color(0x353B82F6),
                    topLeft = Offset(xPos, yPos),
                    size = rect
                )
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = tileSize * 0.11f,
                    center = Offset(xPos + tileSize / 2, yPos + tileSize / 2)
                )
                drawCircle(
                    color = Color(0x80FFFFFF),
                    radius = tileSize * 0.05f,
                    center = Offset(xPos + tileSize / 2, yPos + tileSize / 2)
                )
            } else if (isInAttackRange) {
                // Hostile target banner
                drawRect(
                    color = Color(0x45DC2626),
                    topLeft = Offset(xPos, yPos),
                    size = rect
                )
                drawCircle(
                    color = Color(0xFFDC2626),
                    radius = tileSize * 0.14f,
                    center = Offset(xPos + tileSize / 2, yPos + tileSize / 2),
                    style = Stroke(width = 2.5f)
                )
                // Crosshairs
                val cx = xPos + tileSize / 2
                val cy = yPos + tileSize / 2
                drawLine(Color(0xFFDC2626), Offset(cx - tileSize * 0.18f, cy), Offset(cx + tileSize * 0.18f, cy), strokeWidth = 2f)
                drawLine(Color(0xFFDC2626), Offset(cx, cy - tileSize * 0.18f), Offset(cx, cy + tileSize * 0.18f), strokeWidth = 2f)
            }

            // 10. Atmospheric Cartographic Fog of War
            if (!tile.isExplored) {
                drawRealisticFogOfWar(xPos, yPos, tileSize)
            }
        }
    }

    private fun DrawScope.drawRealisticGround(
        tile: GameTile,
        x: Float,
        y: Float,
        size: Float,
        isEven: Boolean,
        season: Season
    ) {
        val baseGradient = when (tile.terrain) {
            TerrainType.WATER_RIVER, TerrainType.WATER_OCEAN -> {
                if (season == Season.WINTER) {
                    Brush.verticalGradient(listOf(COLOR_WATER_ICE, Color(0xFFA5D5E8)), y, y + size)
                } else {
                    Brush.verticalGradient(listOf(COLOR_WATER_SHALLOW, COLOR_WATER_DEEP), y, y + size)
                }
            }
            TerrainType.MOUNTAIN, TerrainType.MOUNTAIN_SNOW, TerrainType.MOUNTAIN_GOLD -> {
                Brush.linearGradient(
                    colors = listOf(Color(0xFFC7B19E), Color(0xFFA89481)),
                    start = Offset(x, y),
                    end = Offset(x + size, y + size)
                )
            }
            TerrainType.FOREST_PINE, TerrainType.FOREST_OAK -> {
                val forestFloor = when (season) {
                    Season.WINTER -> Color(0xFFC2D6C5)
                    Season.AUTUMN -> Color(0xFFB8986B)
                    else -> Color(0xFF86BC75)
                }
                Brush.radialGradient(
                    colors = listOf(forestFloor, forestFloor.copy(alpha = 0.85f)),
                    center = Offset(x + size * 0.5f, y + size * 0.5f),
                    radius = size * 0.8f
                )
            }
            else -> {
                // Grasslands according to season
                val (c1, c2) = when (season) {
                    Season.SPRING -> Pair(COLOR_SPRING_GRASS, Color(0xFFAEDC7C))
                    Season.SUMMER -> if (isEven) Pair(COLOR_SUMMER_GRASS_LIGHT, COLOR_SUMMER_GRASS_DARK) else Pair(COLOR_SUMMER_GRASS_DARK, COLOR_SUMMER_GRASS_LIGHT)
                    Season.AUTUMN -> Pair(COLOR_AUTUMN_GRASS, Color(0xFFC8B677))
                    Season.WINTER -> Pair(COLOR_WINTER_GRASS, Color(0xFFC8D6CD))
                }
                Brush.linearGradient(
                    colors = listOf(c1, c2),
                    start = Offset(x, y),
                    end = Offset(x + size, y + size)
                )
            }
        }

        drawRect(brush = baseGradient, topLeft = Offset(x, y), size = Size(size, size))
    }

    private fun DrawScope.drawRealisticWater(
        x: Float,
        y: Float,
        size: Float,
        type: TerrainType,
        tick: Float,
        season: Season
    ) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f

        // Coastal Sand Fringe
        val sandWidth = size * 0.08f
        drawRect(
            color = COLOR_SAND_SHORE.copy(alpha = 0.45f),
            topLeft = Offset(x, y),
            size = Size(size, sandWidth)
        )

        if (season == Season.WINTER) {
            // Frozen water with ice cracks
            val iceColor = Color(0xEEF8FAFC)
            drawCircle(iceColor, radius = size * 0.28f, center = Offset(cx, cy))
            // Crack lines
            drawLine(Color(0xFF7DD3FC), Offset(cx - size * 0.2f, cy), Offset(cx + size * 0.2f, cy), strokeWidth = 1.5f)
            drawLine(Color(0xFF7DD3FC), Offset(cx, cy - size * 0.15f), Offset(cx + size * 0.08f, cy + size * 0.15f), strokeWidth = 1.2f)
        } else {
            // Flowing Translucent Water Waves
            val wave1 = (sin(tick * 1.5f + x * 0.1f) * size * 0.04f)
            val wave2 = (cos(tick * 1.8f + y * 0.1f) * size * 0.04f)

            val wavePath = Path().apply {
                moveTo(x + size * 0.12f, cy + wave1)
                quadraticTo(x + size * 0.35f, cy + size * 0.08f + wave2, x + size * 0.55f, cy + wave1)
                quadraticTo(x + size * 0.75f, cy - size * 0.08f + wave2, x + size * 0.88f, cy + wave1)
            }
            drawPath(wavePath, color = Color(0x66FFFFFF), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            val secondWave = Path().apply {
                moveTo(x + size * 0.22f, cy + size * 0.22f + wave2)
                quadraticTo(x + size * 0.5f, cy + size * 0.14f + wave1, x + size * 0.78f, cy + size * 0.22f + wave2)
            }
            drawPath(secondWave, color = Color(0x44FFFFFF), style = Stroke(width = 2f, cap = StrokeCap.Round))

            // Leaping Salmon with shimmering scales and splash
            val fishX = cx + (cos(tick * 0.8f) * size * 0.08f)
            val fishY = cy + (sin(tick * 0.8f) * size * 0.06f)

            // Splash Ripple Rings
            drawOval(
                color = Color(0x44FFFFFF),
                topLeft = Offset(fishX - size * 0.16f, fishY + size * 0.08f),
                size = Size(size * 0.32f, size * 0.10f),
                style = Stroke(width = 1.5f)
            )

            val fishPath = Path().apply {
                moveTo(fishX - size * 0.14f, fishY)
                quadraticTo(fishX, fishY - size * 0.12f, fishX + size * 0.14f, fishY)
                lineTo(fishX + size * 0.20f, fishY - size * 0.09f) // top fin
                lineTo(fishX + size * 0.16f, fishY)
                lineTo(fishX + size * 0.20f, fishY + size * 0.09f) // bottom fin
                quadraticTo(fishX, fishY + size * 0.10f, fishX - size * 0.14f, fishY)
                close()
            }
            drawPath(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFF43F5E), Color(0xFFFB923C)),
                    start = Offset(fishX - size * 0.15f, fishY),
                    end = Offset(fishX + size * 0.15f, fishY)
                ),
                path = fishPath
            )
            // Fish Eye & Gills
            drawCircle(Color.White, radius = size * 0.025f, center = Offset(fishX - size * 0.08f, fishY - size * 0.02f))
            drawCircle(Color(0xFF1E293B), radius = size * 0.015f, center = Offset(fishX - size * 0.08f, fishY - size * 0.02f))
        }
    }

    private fun DrawScope.drawPlainsVegetation(x: Float, y: Float, size: Float, season: Season, isEven: Boolean) {
        // Natural wildflowers and grass clumps
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f

        if (season == Season.SPRING) {
            // Tiny blooming spring flowers
            drawCircle(Color(0xFFEC4899), radius = size * 0.035f, center = Offset(cx - size * 0.2f, cy - size * 0.15f))
            drawCircle(Color(0xFFFBBF24), radius = size * 0.035f, center = Offset(cx + size * 0.25f, cy + size * 0.2f))
        } else if (season == Season.WINTER) {
            // Snow patch
            drawOval(
                color = Color(0xB0F8FAFC),
                topLeft = Offset(cx - size * 0.25f, cy - size * 0.15f),
                size = Size(size * 0.5f, size * 0.3f)
            )
        }

        // Subtle curved blade of grass
        val grassPath = Path().apply {
            moveTo(cx - size * 0.1f, cy + size * 0.2f)
            quadraticTo(cx - size * 0.05f, cy, cx, cy - size * 0.15f)
        }
        drawPath(grassPath, color = Color(0x30000000), style = Stroke(width = 2f, cap = StrokeCap.Round))
    }

    private fun DrawScope.drawRealisticWheatField(x: Float, y: Float, size: Float, season: Season) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f

        // 1. Perspective Plowed Furrows (Parallel Agricultural Rows)
        for (i in -3..3) {
            val offsetY = cy + i * (size * 0.11f)
            drawLine(
                color = Color(0x2B78350F),
                start = Offset(x + size * 0.10f, offsetY),
                end = Offset(x + size * 0.90f, offsetY),
                strokeWidth = size * 0.045f,
                cap = StrokeCap.Round
            )
        }

        // 2. Dense Golden Wheat Sheaves with Drop Shadow
        val grainColor = when (season) {
            Season.AUTUMN -> Color(0xFFD97706) // Ripe golden bronze
            Season.WINTER -> Color(0xFFCA8A04) // Dry winter stalk
            else -> Color(0xFFFBBF24) // Radiant golden yellow
        }

        // Drop shadow under wheat stalks
        drawOval(
            color = Color(0x35000000),
            topLeft = Offset(cx - size * 0.22f, cy + size * 0.22f),
            size = Size(size * 0.44f, size * 0.12f)
        )

        // Multiple waving wheat stems
        for (dx in listOf(-0.16f, -0.04f, 0.08f, 0.20f)) {
            val wx = cx + dx * size
            drawLine(
                color = Color(0xFFB45309),
                start = Offset(wx, cy + size * 0.24f),
                end = Offset(wx + size * 0.04f, cy - size * 0.18f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )

            // Plump ripe grain kernels
            for (step in 0..3) {
                val ky = cy - size * 0.18f + step * (size * 0.07f)
                drawOval(
                    color = grainColor,
                    topLeft = Offset(wx - size * 0.06f, ky),
                    size = Size(size * 0.07f, size * 0.05f)
                )
                drawOval(
                    color = grainColor,
                    topLeft = Offset(wx + size * 0.02f, ky),
                    size = Size(size * 0.07f, size * 0.05f)
                )
            }
        }
    }

    private fun DrawScope.drawRealisticCornField(x: Float, y: Float, size: Float, season: Season) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f

        // Ground shadow
        drawOval(
            color = Color(0x30000000),
            topLeft = Offset(cx - size * 0.24f, cy + size * 0.20f),
            size = Size(size * 0.48f, size * 0.12f)
        )

        // Large vibrant green corn leaves / husks
        val leafPath = Path().apply {
            moveTo(cx - size * 0.22f, cy + size * 0.18f)
            quadraticTo(cx - size * 0.35f, cy - size * 0.05f, cx, cy - size * 0.25f)
            quadraticTo(cx + size * 0.35f, cy - size * 0.05f, cx + size * 0.22f, cy + size * 0.18f)
            close()
        }
        drawPath(leafPath, color = Color(0xFF22C55E))
        drawPath(leafPath, color = Color(0xFF15803D), style = Stroke(width = 1.5f))

        // Golden Corn Cob with individual kernel texture
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFEF08A), Color(0xFFEAB308)),
                startY = cy - size * 0.22f,
                endY = cy + size * 0.14f
            ),
            topLeft = Offset(cx - size * 0.11f, cy - size * 0.22f),
            size = Size(size * 0.22f, size * 0.36f),
            cornerRadius = CornerRadius(size * 0.10f, size * 0.10f)
        )

        // Corn silk (brown tassel on top)
        drawLine(Color(0xFF92400E), Offset(cx, cy - size * 0.22f), Offset(cx - size * 0.06f, cy - size * 0.32f), strokeWidth = 2f)
        drawLine(Color(0xFF92400E), Offset(cx, cy - size * 0.22f), Offset(cx + size * 0.06f, cy - size * 0.32f), strokeWidth = 2f)
    }

    private fun DrawScope.drawRealisticAppleOrchard(x: Float, y: Float, size: Float, season: Season) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.55f

        // Cast tree shadow on grass
        drawOval(
            color = Color(0x35000000),
            topLeft = Offset(cx - size * 0.30f, cy + size * 0.16f),
            size = Size(size * 0.60f, size * 0.15f)
        )

        // Sturdy Oak/Fruit Tree Trunk with bark texture
        val trunkPath = Path().apply {
            moveTo(cx - size * 0.08f, cy + size * 0.24f)
            lineTo(cx - size * 0.05f, cy)
            lineTo(cx + size * 0.05f, cy)
            lineTo(cx + size * 0.08f, cy + size * 0.24f)
            close()
        }
        drawPath(trunkPath, color = Color(0xFF5A2E0F))

        // Multi-layered rounded lush tree canopy with ambient lighting
        val foliageColor = when (season) {
            Season.AUTUMN -> Color(0xFFD97706)
            Season.WINTER -> Color(0xFF9CA3AF)
            else -> Color(0xFF15803D)
        }
        val foliageHighlight = when (season) {
            Season.AUTUMN -> Color(0xFFF59E0B)
            Season.WINTER -> Color(0xFFE5E7EB)
            else -> Color(0xFF22C55E)
        }

        drawCircle(foliageColor, radius = size * 0.24f, center = Offset(cx, cy - size * 0.10f))
        drawCircle(foliageHighlight, radius = size * 0.18f, center = Offset(cx - size * 0.10f, cy - size * 0.16f))
        drawCircle(foliageHighlight, radius = size * 0.18f, center = Offset(cx + size * 0.10f, cy - size * 0.16f))

        // Glossy 3D Red Apples with specular shine
        val applePositions = listOf(
            Pair(-0.12f, -0.16f), Pair(0.12f, -0.14f),
            Pair(-0.04f, -0.06f), Pair(0.06f, -0.22f), Pair(-0.14f, -0.04f)
        )
        for ((ax, ay) in applePositions) {
            val appX = cx + ax * size
            val appY = cy + ay * size
            drawCircle(Color(0xFFDC2626), radius = size * 0.045f, center = Offset(appX, appY))
            // Specular highlight dot
            drawCircle(Color.White, radius = size * 0.012f, center = Offset(appX - size * 0.015f, appY - size * 0.015f))
        }

        // Small rustic wooden harvest bushel on the grass
        drawRoundRect(
            color = Color(0xFF92400E),
            topLeft = Offset(cx + size * 0.18f, cy + size * 0.12f),
            size = Size(size * 0.12f, size * 0.10f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawCircle(Color(0xFFDC2626), radius = size * 0.025f, center = Offset(cx + size * 0.24f, cy + size * 0.11f))
    }

    private fun DrawScope.drawRealisticPineForest(x: Float, y: Float, size: Float, season: Season) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.52f

        // Render a cluster of 3 staggered evergreen conifers for realistic depth
        val trees = listOf(
            Triple(cx - size * 0.20f, cy - size * 0.04f, 0.85f), // background tree
            Triple(cx + size * 0.22f, cy - size * 0.02f, 0.88f), // background tree
            Triple(cx, cy + size * 0.10f, 1.05f)                  // foreground hero tree
        )

        for ((tx, ty, scale) in trees) {
            val treeW = size * 0.32f * scale
            val treeH = size * 0.52f * scale

            // Drop shadow
            drawOval(
                color = Color(0x35000000),
                topLeft = Offset(tx - treeW * 0.45f, ty + treeH * 0.28f),
                size = Size(treeW * 0.9f, size * 0.10f * scale)
            )

            // Conifer trunk
            drawRect(
                color = Color(0xFF451A03),
                topLeft = Offset(tx - size * 0.035f * scale, ty + treeH * 0.15f),
                size = Size(size * 0.07f * scale, treeH * 0.22f)
            )

            // 3 Triangular foliage tiers with dark shadow and light crest
            val tiers = listOf(
                Triple(ty - treeH * 0.45f, treeW * 0.55f, treeH * 0.30f),
                Triple(ty - treeH * 0.20f, treeW * 0.75f, treeH * 0.32f),
                Triple(ty + treeH * 0.05f, treeW * 0.95f, treeH * 0.34f)
            )

            for ((tierTop, w, h) in tiers) {
                val tierPath = Path().apply {
                    moveTo(tx, tierTop)
                    lineTo(tx - w / 2, tierTop + h)
                    lineTo(tx + w / 2, tierTop + h)
                    close()
                }

                val tierColor = when (season) {
                    Season.WINTER -> Color(0xFF1B4332)
                    else -> Color(0xFF14532D)
                }
                drawPath(tierPath, color = tierColor)

                // Sunlit left facet highlight
                val highlightPath = Path().apply {
                    moveTo(tx, tierTop)
                    lineTo(tx - w / 2, tierTop + h)
                    lineTo(tx, tierTop + h)
                    close()
                }
                drawPath(highlightPath, color = Color(0x28FFFFFF))

                // Snow crest in winter
                if (season == Season.WINTER) {
                    val snowCap = Path().apply {
                        moveTo(tx, tierTop)
                        lineTo(tx - w * 0.3f, tierTop + h * 0.4f)
                        lineTo(tx + w * 0.3f, tierTop + h * 0.4f)
                        close()
                    }
                    drawPath(snowCap, color = Color(0xF5F8FAFC))
                }
            }
        }
    }

    private fun DrawScope.drawRealisticOakForest(x: Float, y: Float, size: Float, season: Season) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.52f

        // Staggered cluster of 3 broadleaf oak trees
        val oaks = listOf(
            Pair(cx - size * 0.18f, cy - size * 0.08f),
            Pair(cx + size * 0.18f, cy - size * 0.06f),
            Pair(cx, cy + size * 0.06f)
        )

        for ((ox, oy) in oaks) {
            // Drop shadow
            drawOval(
                color = Color(0x35000000),
                topLeft = Offset(ox - size * 0.24f, oy + size * 0.20f),
                size = Size(size * 0.48f, size * 0.12f)
            )

            // Gnarled oak trunk
            drawRect(
                color = Color(0xFF451A03),
                topLeft = Offset(ox - size * 0.045f, oy + size * 0.08f),
                size = Size(size * 0.09f, size * 0.20f)
            )

            val (cBase, cLight) = when (season) {
                Season.AUTUMN -> Pair(Color(0xFFC2410C), Color(0xFFF97316)) // Fiery autumn amber
                Season.WINTER -> Pair(Color(0xFF64748B), Color(0xFF94A3B8)) // Dormant bare winter
                else -> Pair(Color(0xFF15803D), Color(0xFF22C55E))          // Lush summer emerald
            }

            drawCircle(cBase, radius = size * 0.22f, center = Offset(ox, oy - size * 0.04f))
            drawCircle(cLight, radius = size * 0.17f, center = Offset(ox - size * 0.08f, oy - size * 0.10f))
            drawCircle(cLight.copy(alpha = 0.9f), radius = size * 0.15f, center = Offset(ox + size * 0.08f, oy - size * 0.10f))
        }
    }

    private fun DrawScope.drawRealisticHorsePasture(x: Float, y: Float, size: Float, season: Season) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.52f

        // 1. Rustic Wooden Paddock Fence
        val fenceColor = Color(0xFF78350F)
        // Horizontal fence rails
        drawLine(fenceColor, Offset(x + size * 0.10f, y + size * 0.25f), Offset(x + size * 0.90f, y + size * 0.25f), strokeWidth = 2.5f)
        drawLine(fenceColor, Offset(x + size * 0.10f, y + size * 0.35f), Offset(x + size * 0.90f, y + size * 0.35f), strokeWidth = 2f)
        // Vertical fence posts
        for (fx in listOf(0.15f, 0.40f, 0.65f, 0.85f)) {
            val px = x + fx * size
            drawLine(fenceColor, Offset(px, y + size * 0.20f), Offset(px, y + size * 0.40f), strokeWidth = 3f, cap = StrokeCap.Round)
        }

        // 2. Cast shadow under thoroughbred horse
        drawOval(
            color = Color(0x35000000),
            topLeft = Offset(cx - size * 0.25f, cy + size * 0.22f),
            size = Size(size * 0.50f, size * 0.12f)
        )

        // 3. Thoroughbred Chestnut Horse with muscle definition & mane
        val horsePath = Path().apply {
            moveTo(cx - size * 0.20f, cy + size * 0.25f)
            lineTo(cx - size * 0.20f, cy - size * 0.02f)
            lineTo(cx - size * 0.14f, cy - size * 0.26f) // pointed ear
            lineTo(cx - size * 0.06f, cy - size * 0.14f)
            lineTo(cx + size * 0.18f, cy - size * 0.04f) // muzzle
            lineTo(cx + size * 0.18f, cy + size * 0.06f)
            lineTo(cx + size * 0.06f, cy + size * 0.14f) // strong jaw
            lineTo(cx + size * 0.08f, cy + size * 0.25f)
            close()
        }
        drawPath(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFD97706), Color(0xFF92400E)),
                start = Offset(cx - size * 0.2f, cy - size * 0.2f),
                end = Offset(cx + size * 0.2f, cy + size * 0.2f)
            ),
            path = horsePath
        )

        // Dark flowing mane
        val manePath = Path().apply {
            moveTo(cx - size * 0.20f, cy - size * 0.02f)
            lineTo(cx - size * 0.27f, cy + size * 0.08f)
            lineTo(cx - size * 0.22f, cy + size * 0.16f)
            lineTo(cx - size * 0.17f, cy + size * 0.20f)
            close()
        }
        drawPath(manePath, color = Color(0xFF451A03))

        // Horse Eye with highlight
        drawCircle(Color(0xFF1E293B), radius = size * 0.035f, center = Offset(cx - size * 0.02f, cy - size * 0.04f))
        drawCircle(Color.White, radius = size * 0.012f, center = Offset(cx - size * 0.03f, cy - size * 0.05f))
    }

    private fun DrawScope.drawRealisticMountain(
        x: Float,
        y: Float,
        size: Float,
        hasSnow: Boolean,
        hasGold: Boolean,
        season: Season,
        animTick: Float = 0f
    ) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.54f

        // Mountain Cast Shadow
        drawOval(
            color = Color(0x3E000000),
            topLeft = Offset(cx - size * 0.44f, cy + size * 0.22f),
            size = Size(size * 0.88f, size * 0.16f)
        )

        // Jagged Rocky Ridges with Directional Lighting (Sun from top-left)
        val leftSlope = Path().apply {
            moveTo(cx, cy - size * 0.38f)
            lineTo(cx - size * 0.15f, cy - size * 0.10f)
            lineTo(cx - size * 0.25f, cy + size * 0.08f)
            lineTo(cx - size * 0.42f, cy + size * 0.28f)
            lineTo(cx, cy + size * 0.28f)
            close()
        }
        drawPath(
            brush = Brush.linearGradient(
                colors = listOf(COLOR_ROCK_LIGHT, Color(0xFF6B7280)),
                start = Offset(cx - size * 0.4f, cy - size * 0.3f),
                end = Offset(cx, cy + size * 0.28f)
            ),
            path = leftSlope
        )

        val rightSlope = Path().apply {
            moveTo(cx, cy - size * 0.38f)
            lineTo(cx + size * 0.12f, cy - size * 0.06f)
            lineTo(cx + size * 0.26f, cy + size * 0.10f)
            lineTo(cx + size * 0.42f, cy + size * 0.28f)
            lineTo(cx, cy + size * 0.28f)
            close()
        }
        drawPath(
            brush = Brush.linearGradient(
                colors = listOf(COLOR_ROCK_DARK, COLOR_ROCK_SHADOW),
                start = Offset(cx, cy - size * 0.38f),
                end = Offset(cx + size * 0.4f, cy + size * 0.28f)
            ),
            path = rightSlope
        )

        // Central Mountain Ridge Spine
        drawLine(Color(0xFF374151), Offset(cx, cy - size * 0.38f), Offset(cx - size * 0.05f, cy + size * 0.28f), strokeWidth = 2f)

        // Realistic Snow Cap with jagged crevasse & blue-shadowed edges
        if (hasSnow || season == Season.WINTER) {
            val snowCap = Path().apply {
                moveTo(cx, cy - size * 0.38f)
                lineTo(cx - size * 0.20f, cy - size * 0.12f)
                lineTo(cx - size * 0.08f, cy - size * 0.06f)
                lineTo(cx + size * 0.04f, cy - size * 0.14f)
                lineTo(cx + size * 0.18f, cy - size * 0.10f)
                close()
            }
            drawPath(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFE0F2FE)),
                    startY = cy - size * 0.38f,
                    endY = cy - size * 0.06f
                ),
                path = snowCap
            )
        }

        // Realistic Sparkling Gemstone / Gold Ore Fissure
        if (hasGold) {
            val gemX = cx + size * 0.24f
            val gemY = cy - size * 0.08f
            val gemR = size * 0.13f

            // Golden Glow Aura
            val pulse = (sin(animTick * 3f) * 0.3f + 0.7f)
            drawCircle(
                color = Color(0xFFFBBF24).copy(alpha = 0.4f * pulse),
                radius = gemR * 1.8f,
                center = Offset(gemX, gemY)
            )

            // Multi-faceted Gem Diamond
            val diamond = Path().apply {
                moveTo(gemX, gemY - gemR)
                lineTo(gemX + gemR, gemY)
                lineTo(gemX, gemY + gemR)
                lineTo(gemX - gemR, gemY)
                close()
            }
            drawPath(diamond, brush = Brush.linearGradient(listOf(Color(0xFFFEF08A), Color(0xFFD97706))))
            drawPath(diamond, color = Color(0xFF78350F), style = Stroke(width = 1.5f))

            // Specular Glint Cross
            val glintLen = gemR * 0.7f * pulse
            drawLine(Color.White, Offset(gemX - glintLen, gemY), Offset(gemX + glintLen, gemY), strokeWidth = 2f)
            drawLine(Color.White, Offset(gemX, gemY - glintLen), Offset(gemX, gemY + glintLen), strokeWidth = 2f)
        }
    }

    private fun DrawScope.drawRealisticSpiceGrove(x: Float, y: Float, size: Float, season: Season) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.54f

        // Tropical Palm Fronds
        drawCircle(Color(0xFF854D0E), radius = size * 0.06f, center = Offset(cx, cy + size * 0.15f))
        for (angle in listOf(-45f, 0f, 45f, -90f, 90f)) {
            val rad = Math.toRadians(angle.toDouble())
            val px = cx + (cos(rad) * size * 0.26f).toFloat()
            val py = cy - size * 0.1f + (sin(rad) * size * 0.16f).toFloat()
            drawLine(Color(0xFF15803D), Offset(cx, cy - size * 0.1f), Offset(px, py), strokeWidth = 3f, cap = StrokeCap.Round)
        }

        // Burlap spice sacks filled with crimson cinnamon and saffron
        drawOval(Color(0xFF9A3412), topLeft = Offset(cx - size * 0.18f, cy + size * 0.05f), size = Size(size * 0.18f, size * 0.18f))
        drawOval(Color(0xFFD97706), topLeft = Offset(cx + size * 0.02f, cy + size * 0.08f), size = Size(size * 0.16f, size * 0.16f))
    }

    private fun DrawScope.drawRealisticAncientRuins(x: Float, y: Float, size: Float, tick: Float) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.48f

        // Ambient mystical glow
        val pulse = (sin(tick * 2.2f) * 0.4f + 0.6f)
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.35f * pulse),
            radius = size * 0.32f,
            center = Offset(cx, cy)
        )

        // Ancient Carved Marble Pedestal & Colonnade
        // Base plinth
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(cx - size * 0.28f, cy + size * 0.12f),
            size = Size(size * 0.56f, size * 0.10f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Weathered Columns
        drawRect(Color(0xFFE2E8F0), topLeft = Offset(cx - size * 0.22f, cy - size * 0.18f), size = Size(size * 0.09f, size * 0.30f))
        drawRect(Color(0xFF94A3B8), topLeft = Offset(cx - size * 0.13f, cy - size * 0.18f), size = Size(size * 0.02f, size * 0.30f)) // column shade

        drawRect(Color(0xFFE2E8F0), topLeft = Offset(cx + size * 0.13f, cy - size * 0.18f), size = Size(size * 0.09f, size * 0.30f))
        drawRect(Color(0xFF94A3B8), topLeft = Offset(cx + size * 0.22f, cy - size * 0.18f), size = Size(size * 0.02f, size * 0.30f))

        // Architrave lintel
        drawRect(Color(0xFFF1F5F9), topLeft = Offset(cx - size * 0.26f, cy - size * 0.26f), size = Size(size * 0.52f, size * 0.08f))

        // Glowing Ancient Arcane Sigil
        drawCircle(Color(0xFF38BDF8), radius = size * 0.06f, center = Offset(cx, cy - size * 0.04f))
        drawCircle(Color.White, radius = size * 0.03f, center = Offset(cx, cy - size * 0.04f))
    }

    private fun DrawScope.drawRealisticBuilding(
        building: Building,
        x: Float,
        y: Float,
        size: Float,
        season: Season
    ) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.55f

        when (building.type) {
            BuildingType.CAPITAL_CASTLE -> {
                // High-fidelity Imperial Fortress Castle Keep with realistic ashlar stonework & battlements
                val castleW = size * 0.74f
                val castleH = size * 0.58f

                // Deep ground drop shadow
                drawOval(
                    color = Color(0x45000000),
                    topLeft = Offset(cx - castleW * 0.55f, cy + castleH * 0.30f),
                    size = Size(castleW * 1.1f, size * 0.18f)
                )

                // Main Castle Curtain Wall (Chiseled Stone)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF475569)),
                        startY = cy - castleH / 2,
                        endY = cy + castleH / 2
                    ),
                    topLeft = Offset(cx - castleW / 2, cy - castleH / 2),
                    size = Size(castleW, castleH),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Crenellated Battlements across wall top
                for (b in -3..3) {
                    val bx = cx + b * (size * 0.09f) - size * 0.035f
                    drawRect(Color(0xFF334155), topLeft = Offset(bx, cy - castleH / 2 - size * 0.06f), size = Size(size * 0.05f, size * 0.06f))
                }

                // Left & Right Corner Fortified Round Towers
                val towerW = size * 0.22f
                val towerH = size * 0.42f
                // Left Tower
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF94A3B8), Color(0xFF475569))),
                    topLeft = Offset(cx - castleW / 2 - size * 0.04f, cy - castleH / 2 - size * 0.14f),
                    size = Size(towerW, towerH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Right Tower
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF64748B), Color(0xFF334155))),
                    topLeft = Offset(cx + castleW / 2 - towerW + size * 0.04f, cy - castleH / 2 - size * 0.14f),
                    size = Size(towerW, towerH),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Conical Slate-Blue Roofs on Towers
                val leftRoof = Path().apply {
                    val lx = cx - castleW / 2 - size * 0.04f + towerW / 2
                    moveTo(lx, cy - castleH / 2 - size * 0.32f)
                    lineTo(lx - towerW / 2 - 2f, cy - castleH / 2 - size * 0.14f)
                    lineTo(lx + towerW / 2 + 2f, cy - castleH / 2 - size * 0.14f)
                    close()
                }
                drawPath(leftRoof, color = Color(0xFF1E3A8A)) // Imperial Blue Slate Roof

                val rightRoof = Path().apply {
                    val rx = cx + castleW / 2 + size * 0.04f - towerW / 2
                    moveTo(rx, cy - castleH / 2 - size * 0.32f)
                    lineTo(rx - towerW / 2 - 2f, cy - castleH / 2 - size * 0.14f)
                    lineTo(rx + towerW / 2 + 2f, cy - castleH / 2 - size * 0.14f)
                    close()
                }
                drawPath(rightRoof, color = Color(0xFF1E3A8A))

                // High Central Citadel Keep
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF475569))),
                    topLeft = Offset(cx - size * 0.16f, cy - castleH / 2 - size * 0.22f),
                    size = Size(size * 0.32f, size * 0.26f)
                )

                // Heavy Iron Portcullis Gate
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(cx - size * 0.11f, cy + castleH / 2 - size * 0.26f),
                    size = Size(size * 0.22f, size * 0.26f),
                    cornerRadius = CornerRadius(size * 0.10f, size * 0.10f)
                )
                // Portcullis iron bars
                drawLine(Color(0xFF94A3B8), Offset(cx - size * 0.05f, cy + castleH / 2 - size * 0.24f), Offset(cx - size * 0.05f, cy + castleH / 2), strokeWidth = 1.5f)
                drawLine(Color(0xFF94A3B8), Offset(cx + size * 0.05f, cy + castleH / 2 - size * 0.24f), Offset(cx + size * 0.05f, cy + castleH / 2), strokeWidth = 1.5f)

                // Majestic Imperial Banner (Fluttering Royal Purple Standard with Gold Lion crest)
                drawLine(Color(0xFF1E293B), Offset(cx, cy - castleH / 2 - size * 0.22f), Offset(cx, cy - castleH / 2 - size * 0.44f), strokeWidth = 2.5f)
                val flagPath = Path().apply {
                    moveTo(cx, cy - castleH / 2 - size * 0.44f)
                    lineTo(cx + size * 0.22f, cy - castleH / 2 - size * 0.38f)
                    lineTo(cx, cy - castleH / 2 - size * 0.32f)
                    close()
                }
                drawPath(flagPath, color = Color(0xFF7E22CE))
                drawCircle(Color(0xFFFBBF24), radius = size * 0.025f, center = Offset(cx + size * 0.09f, cy - castleH / 2 - size * 0.38f))
            }
            BuildingType.TOWN -> {
                // Half-timbered medieval dwellings with chimneys & cobbled square
                val townW = size * 0.58f
                val townH = size * 0.40f

                drawOval(Color(0x35000000), topLeft = Offset(cx - townW / 2, cy + townH * 0.3f), size = Size(townW, size * 0.12f))

                // Stone & timber house
                drawRoundRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset(cx - townW / 2, cy - townH / 2),
                    size = Size(townW, townH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Terracotta tiled roof
                val roof = Path().apply {
                    moveTo(cx, cy - townH / 2 - size * 0.22f)
                    lineTo(cx - townW / 2 - size * 0.04f, cy - townH / 2)
                    lineTo(cx + townW / 2 + size * 0.04f, cy - townH / 2)
                    close()
                }
                drawPath(roof, color = Color(0xFFC2410C))

                // Timber cross-beams
                drawLine(Color(0xFF78350F), Offset(cx - townW / 2, cy - townH / 2), Offset(cx + townW / 2, cy + townH / 2), strokeWidth = 1.5f)
                drawLine(Color(0xFF78350F), Offset(cx + townW / 2, cy - townH / 2), Offset(cx - townW / 2, cy + townH / 2), strokeWidth = 1.5f)

                // Stone chimney with rising smoke
                drawRect(Color(0xFF475569), topLeft = Offset(cx + size * 0.14f, cy - townH / 2 - size * 0.18f), size = Size(size * 0.08f, size * 0.14f))
                drawCircle(Color(0x40FFFFFF), radius = size * 0.04f, center = Offset(cx + size * 0.18f, cy - townH / 2 - size * 0.22f))
            }
            BuildingType.FARMSTEAD -> {
                // Weathered timber barn & stone grain silo
                drawRoundRect(Color(0xFF991B1B), topLeft = Offset(cx - size * 0.22f, cy - size * 0.12f), size = Size(size * 0.34f, size * 0.28f), cornerRadius = CornerRadius(2f, 2f))
                // White barn cross
                drawLine(Color.White, Offset(cx - size * 0.22f, cy - size * 0.12f), Offset(cx + size * 0.12f, cy + size * 0.16f), strokeWidth = 1.5f)
                // Silo with conical roof
                drawRoundRect(Color(0xFF94A3B8), topLeft = Offset(cx + size * 0.14f, cy - size * 0.20f), size = Size(size * 0.14f, size * 0.36f), cornerRadius = CornerRadius(2f, 2f))
            }
            BuildingType.LUMBER_MILL -> {
                // Waterwheel timber sawmill with stacked cut logs
                drawRoundRect(Color(0xFF78350F), topLeft = Offset(cx - size * 0.22f, cy - size * 0.12f), size = Size(size * 0.32f, size * 0.26f), cornerRadius = CornerRadius(2f, 2f))
                // Waterwheel
                drawCircle(Color(0xFF451A03), radius = size * 0.14f, center = Offset(cx + size * 0.18f, cy + size * 0.04f), style = Stroke(width = 3f))
                // Cut logs
                drawOval(Color(0xFFB45309), topLeft = Offset(cx - size * 0.24f, cy + size * 0.16f), size = Size(size * 0.18f, size * 0.08f))
            }
            BuildingType.QUARRY -> {
                // Tiered stone quarry pit with wooden crane
                drawRect(Color(0xFF475569), topLeft = Offset(cx - size * 0.24f, cy - size * 0.14f), size = Size(size * 0.48f, size * 0.32f))
                drawRect(Color(0xFF1E293B), topLeft = Offset(cx - size * 0.16f, cy - size * 0.06f), size = Size(size * 0.32f, size * 0.20f))
                // Stone blocks
                drawRect(Color(0xFFE2E8F0), topLeft = Offset(cx + size * 0.08f, cy + size * 0.16f), size = Size(size * 0.12f, size * 0.10f))
            }
            BuildingType.WATCHTOWER -> {
                // Tall fortress stone watchtower
                drawRoundRect(Color(0xFF64748B), topLeft = Offset(cx - size * 0.10f, cy - size * 0.34f), size = Size(size * 0.20f, size * 0.54f), cornerRadius = CornerRadius(2f, 2f))
                // Wooden lookout hoarding
                drawRect(Color(0xFF78350F), topLeft = Offset(cx - size * 0.14f, cy - size * 0.38f), size = Size(size * 0.28f, size * 0.12f))
                // Conical roof
                val roof = Path().apply {
                    moveTo(cx, cy - size * 0.50f)
                    lineTo(cx - size * 0.15f, cy - size * 0.38f)
                    lineTo(cx + size * 0.15f, cy - size * 0.38f)
                    close()
                }
                drawPath(roof, color = Color(0xFFC2410C))
            }
            BuildingType.MARKET -> {
                // Striped colorful merchant awnings & wooden barrels
                val awningW = size * 0.48f
                drawRoundRect(Color(0xFFFEF08A), topLeft = Offset(cx - awningW / 2, cy - size * 0.12f), size = Size(awningW, size * 0.24f), cornerRadius = CornerRadius(2f, 2f))
                // Red stripes
                for (s in -2..2) {
                    val sx = cx + s * (size * 0.09f)
                    drawRect(Color(0xFFDC2626), topLeft = Offset(sx, cy - size * 0.12f), size = Size(size * 0.045f, size * 0.24f))
                }
            }
            BuildingType.ACADEMY -> {
                // Classical domed academy with colonnade & armillary sphere
                drawRoundRect(Color(0xFFF1F5F9), topLeft = Offset(cx - size * 0.22f, cy - size * 0.10f), size = Size(size * 0.44f, size * 0.28f), cornerRadius = CornerRadius(3f, 3f))
                // Copper dome
                val dome = Path().apply {
                    moveTo(cx - size * 0.18f, cy - size * 0.10f)
                    quadraticTo(cx, cy - size * 0.36f, cx + size * 0.18f, cy - size * 0.10f)
                    close()
                }
                drawPath(dome, color = Color(0xFF0D9488)) // Oxidized green-blue copper dome
            }
        }
    }

    private fun DrawScope.drawRealisticUnitBadge(
        unit: GameUnit,
        x: Float,
        y: Float,
        size: Float,
        textMeasurer: TextMeasurer
    ) {
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f
        val badgeRadius = size * 0.32f

        // Cast deep drop shadow under unit medallion
        drawCircle(
            color = Color(0x55000000),
            radius = badgeRadius + 2f,
            center = Offset(cx + 2.5f, cy + 3.5f)
        )

        // Outer Metallic Gold / Bronze Embossed Rim
        val factionColor = Color(unit.factionId.colorHex)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFDE047), Color(0xFF92400E)),
                start = Offset(cx - badgeRadius, cy - badgeRadius),
                end = Offset(cx + badgeRadius, cy + badgeRadius)
            ),
            radius = badgeRadius + 2.5f,
            center = Offset(cx, cy)
        )

        // Inner Faction Colored Ring
        drawCircle(
            color = factionColor,
            radius = badgeRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 3.5f)
        )

        // Rich military sage disk (matching screenshot aesthetics with metallic gradient)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF9FB6A1), Color(0xFF6B8A6E)),
                center = Offset(cx - badgeRadius * 0.3f, cy - badgeRadius * 0.3f),
                radius = badgeRadius
            ),
            radius = badgeRadius - 2f,
            center = Offset(cx, cy)
        )

        // Detailed Military Heraldic Silhouette
        when (unit.type) {
            UnitType.SCOUT -> {
                // Brass Explorer's Compass Rose
                val compR = badgeRadius * 0.55f
                drawLine(Color(0xFF0F172A), Offset(cx, cy - compR), Offset(cx, cy + compR), strokeWidth = 2.5f)
                drawLine(Color(0xFF0F172A), Offset(cx - compR, cy), Offset(cx + compR, cy), strokeWidth = 2.5f)
                drawCircle(Color(0xFFDC2626), radius = 3.5f, center = Offset(cx, cy - compR * 0.7f))
            }
            UnitType.ARCHER -> {
                // Curved Recurve Bow & Steel-Tipped Arrow
                val bowPath = Path().apply {
                    moveTo(cx - badgeRadius * 0.45f, cy - badgeRadius * 0.45f)
                    quadraticTo(cx + badgeRadius * 0.15f, cy, cx - badgeRadius * 0.45f, cy + badgeRadius * 0.45f)
                }
                drawPath(bowPath, color = Color(0xFF0F172A), style = Stroke(width = 3f))
                drawLine(Color(0xFF991B1B), Offset(cx - badgeRadius * 0.35f, cy), Offset(cx + badgeRadius * 0.45f, cy), strokeWidth = 2.2f)
            }
            UnitType.KNIGHT -> {
                // Noble Warhorse & Visored Helm
                drawCircle(Color(0xFF0F172A), radius = badgeRadius * 0.38f, center = Offset(cx, cy))
                drawLine(Color(0xFFFBBF24), Offset(cx - badgeRadius * 0.2f, cy - badgeRadius * 0.25f), Offset(cx + badgeRadius * 0.25f, cy - badgeRadius * 0.25f), strokeWidth = 2.5f)
            }
            UnitType.CATAPULT -> {
                // Heavy Wooden Catapult Arm
                drawCircle(Color(0xFF0F172A), radius = badgeRadius * 0.38f, center = Offset(cx, cy), style = Stroke(width = 3.5f))
                drawLine(Color(0xFF0F172A), Offset(cx - badgeRadius * 0.3f, cy + badgeRadius * 0.2f), Offset(cx + badgeRadius * 0.3f, cy - badgeRadius * 0.3f), strokeWidth = 3f)
            }
            else -> {
                // Disciplined Legion Squad Formations (matching screenshot squad icons!)
                for (r in -1..1) {
                    for (c in -1..1) {
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 2.8f,
                            center = Offset(cx + c * 7f, cy - 4f + r * 7f)
                        )
                    }
                }
            }
        }

        // Glassmorphic / Enameled Troop Headcount Badge (e.g. 25, 20, 11)
        val countX = cx + badgeRadius * 0.75f
        val countY = cy - badgeRadius * 0.75f
        val countRadius = size * 0.17f

        // Shadow under count badge
        drawCircle(Color(0x55000000), radius = countRadius + 1f, center = Offset(countX + 1.5f, countY + 2f))
        drawCircle(Color(0xFFF8FAFC), radius = countRadius, center = Offset(countX, countY))
        drawCircle(factionColor, radius = countRadius, center = Offset(countX, countY), style = Stroke(width = 2f))

        val countText = unit.troops.toString()
        val textLayout = textMeasurer.measure(
            text = countText,
            style = TextStyle(
                fontSize = (size * 0.19f).sp,
                fontWeight = FontWeight.ExtraBold,
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

    private fun DrawScope.drawRealisticTerritoryBorder(factionId: FactionId, x: Float, y: Float, size: Float) {
        val color = Color(factionId.colorHex)

        // Elegant semi-translucent inner territory aura tint
        drawRect(
            color = color.copy(alpha = 0.08f),
            topLeft = Offset(x, y),
            size = Size(size, size)
        )

        // Polished Glowing Border Frame
        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(size, size),
            style = Stroke(width = 2.8f)
        )

        // Heraldic Corner Shield Badge
        val shieldX = x + size * 0.08f
        val shieldY = y + size * 0.08f
        val shieldW = size * 0.19f
        val shieldH = size * 0.23f

        val shieldPath = Path().apply {
            moveTo(shieldX, shieldY)
            lineTo(shieldX + shieldW, shieldY)
            lineTo(shieldX + shieldW, shieldY + shieldH * 0.6f)
            quadraticTo(shieldX + shieldW * 0.5f, shieldY + shieldH, shieldX + shieldW * 0.5f, shieldY + shieldH)
            quadraticTo(shieldX, shieldY + shieldH * 0.6f, shieldX, shieldY + shieldH * 0.6f)
            close()
        }
        drawPath(shieldPath, color = color.copy(alpha = 0.35f), style = Fill)
        drawPath(shieldPath, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.2f))
    }

    private fun DrawScope.drawRealisticFogOfWar(x: Float, y: Float, size: Float) {
        // Deep antique cartographic shroud with compass rose details
        drawRect(
            color = COLOR_FOG_PAPER,
            topLeft = Offset(x, y),
            size = Size(size, size)
        )

        // Fine cartographic stipples & compass points
        val cx = x + size * 0.5f
        val cy = y + size * 0.5f
        drawCircle(COLOR_FOG_DETAILS, radius = size * 0.12f, center = Offset(cx, cy), style = Stroke(width = 1.2f))
        drawLine(COLOR_FOG_DETAILS, Offset(cx - size * 0.18f, cy), Offset(cx + size * 0.18f, cy), strokeWidth = 1f)
        drawLine(COLOR_FOG_DETAILS, Offset(cx, cy - size * 0.18f), Offset(cx, cy + size * 0.18f), strokeWidth = 1f)
    }
}
