package com.example.ui.screen

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DungeonCanvas(
    tiles: Array<Array<Tile>>?,
    player: Player,
    enemies: List<Enemy>,
    targetEnemyId: String?,
    floatingFxList: List<FloatingFx>,
    activeBanner: MilestoneBanner?,
    onTileTap: (GridPos) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tiles == null) return

    val gridW = tiles.size
    val gridH = tiles[0].size

    // Pre-allocated reusable Paint - 0 allocations per frame
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
    }

    // Keep updated callbacks for stable pointerInput
    val currentOnTileTap by rememberUpdatedState(onTileTap)
    val currentPlayerPos by rememberUpdatedState(player.pos)

    // Animation frame ticker ONLY when floating numbers or banners are on screen
    var animFrame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(floatingFxList.isNotEmpty()) {
        if (floatingFxList.isNotEmpty()) {
            while (true) {
                delay(16)
                animFrame = System.currentTimeMillis()
            }
        }
    }

    Box(
        modifier = modifier
            .clip(CutCornerShape(6.dp))
            .border(2.dp, BorderMetal, CutCornerShape(6.dp))
            .background(VoidBlack)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dungeon_canvas")
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val viewW = size.width
                        val viewH = size.height
                        val tileSize = min(viewW / 11f, viewH / 11f)

                        val camOffsetX = (viewW / 2f) - (currentPlayerPos.x * tileSize + tileSize / 2f)
                        val camOffsetY = (viewH / 2f) - (currentPlayerPos.y * tileSize + tileSize / 2f)

                        val cellX = ((tapOffset.x - camOffsetX) / tileSize).toInt()
                        val cellY = ((tapOffset.y - camOffsetY) / tileSize).toInt()

                        if (cellX in 0 until gridW && cellY in 0 until gridH) {
                            currentOnTileTap(GridPos(cellX, cellY))
                        }
                    }
                }
        ) {
            val viewW = size.width
            val viewH = size.height
            val tileSize = min(viewW / 11f, viewH / 11f)

            val camOffsetX = (viewW / 2f) - (player.pos.x * tileSize + tileSize / 2f)
            val camOffsetY = (viewH / 2f) - (player.pos.y * tileSize + tileSize / 2f)

            val now = if (animFrame > 0L) animFrame else System.currentTimeMillis()

            // Dynamic sine pulse without triggering Compose recomposition
            val pulsePhase = ((now % 1400) / 1400.0) * (2.0 * Math.PI)
            val pulseAlpha = (0.75f + 0.25f * sin(pulsePhase).toFloat())

            val torchPhase = ((now % 300) / 300.0) * (2.0 * Math.PI)
            val torchFlicker = (0.92f + 0.16f * sin(torchPhase).toFloat())

            // --- VIEWPORT CULLING (Only render tiles visible on screen) ---
            val minCol = ((-camOffsetX) / tileSize).toInt().coerceIn(0, gridW - 1)
            val maxCol = (((viewW - camOffsetX) / tileSize).toInt() + 1).coerceIn(0, gridW - 1)
            val minRow = ((-camOffsetY) / tileSize).toInt().coerceIn(0, gridH - 1)
            val maxRow = (((viewH - camOffsetY) / tileSize).toInt() + 1).coerceIn(0, gridH - 1)

            // 1. Draw 16-Bit Autotiled Floor & Wall Tiles
            for (x in minCol..maxCol) {
                for (y in minRow..maxRow) {
                    val tile = tiles[x][y]
                    val drawX = camOffsetX + x * tileSize
                    val drawY = camOffsetY + y * tileSize

                    if (!tile.isDiscovered) {
                        drawRect(
                            color = VoidBlack,
                            topLeft = Offset(drawX, drawY),
                            size = Size(tileSize, tileSize)
                        )
                        continue
                    }

                    // Render tile with 16-bit perspective
                    if (tile.isWalkable) {
                        draw16BitFloor(tile, drawX, drawY, tileSize, pulseAlpha)
                    } else {
                        draw16BitWall(tile, drawX, drawY, tileSize, torchFlicker)
                    }

                    // Downward wall shadow if north tile is a front-facing wall
                    if (tile.isWalkable && y > 0 && tiles[x][y - 1].wallFrontFace) {
                        drawRect(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(drawX, drawY),
                            size = Size(tileSize, tileSize * 0.28f)
                        )
                    }

                    // Blood splatter
                    if (tile.hasBloodSplatter && tile.isWalkable) {
                        drawCircle(
                            color = BloodCrimson.copy(alpha = 0.7f),
                            radius = tileSize * 0.22f,
                            center = Offset(drawX + tileSize * 0.4f, drawY + tileSize * 0.55f)
                        )
                    }

                    // Loot sparkler
                    if (tile.scrapLoot > 0 || tile.biomassLoot > 0) {
                        drawCircle(
                            color = ForgeAmber,
                            radius = tileSize * 0.15f,
                            center = Offset(drawX + tileSize * 0.7f, drawY + tileSize * 0.35f)
                        )
                    }

                    // Fog of War & Torch Radiance
                    if (!tile.isVisible) {
                        drawRect(
                            color = VoidBlack.copy(alpha = 0.72f),
                            topLeft = Offset(drawX, drawY),
                            size = Size(tileSize, tileSize)
                        )
                    } else {
                        val dist = player.pos.manhattanDistance(tile.pos)
                        val lightFalloff = (dist.toFloat() / (player.fovRadius + 1)).coerceIn(0f, 0.45f)
                        drawRect(
                            color = VoidBlack.copy(alpha = lightFalloff),
                            topLeft = Offset(drawX, drawY),
                            size = Size(tileSize, tileSize)
                        )
                    }
                }
            }

            // 2. Draw Enemies
            for (enemy in enemies) {
                if (enemy.isDead) continue
                if (enemy.pos.x !in minCol..maxCol || enemy.pos.y !in minRow..maxRow) continue
                val enemyTile = tiles[enemy.pos.x][enemy.pos.y]
                if (!enemyTile.isVisible) continue

                val drawX = camOffsetX + enemy.pos.x * tileSize
                val drawY = camOffsetY + enemy.pos.y * tileSize
                val isSelected = enemy.id == targetEnemyId

                draw16BitEnemy(enemy, drawX, drawY, tileSize, isSelected, pulseAlpha)
            }

            // 3. Draw Player 16-Bit Avatar & Torch Flame Aura
            val pX = camOffsetX + player.pos.x * tileSize
            val pY = camOffsetY + player.pos.y * tileSize

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ForgeAmber.copy(alpha = 0.28f * torchFlicker), Color.Transparent),
                    center = Offset(pX + tileSize / 2f, pY + tileSize / 2f),
                    radius = tileSize * 1.5f
                ),
                radius = tileSize * 1.5f,
                center = Offset(pX + tileSize / 2f, pY + tileSize / 2f)
            )

            draw16BitPlayer(player, pX, pY, tileSize)

            // 4. Draw Floating Combat Numbers (Damage / Heal / Loot)
            for (fx in floatingFxList) {
                val ageMs = (now - fx.startTime).coerceAtLeast(0)
                if (ageMs > 1500) continue

                val progress = ageMs / 1500f
                val alpha = (1f - progress).coerceIn(0f, 1f)
                val floatOffsetY = -progress * tileSize * 0.85f

                val targetX = camOffsetX + fx.gridX * tileSize + tileSize / 2f
                val targetY = camOffsetY + fx.gridY * tileSize + tileSize * 0.2f + floatOffsetY

                val textSize = if (fx.isCrit) tileSize * 0.42f else tileSize * 0.34f
                textPaint.textSize = textSize

                // Shadow
                textPaint.color = Color.Black.copy(alpha = alpha * 0.8f).toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    fx.text,
                    targetX + 2f,
                    targetY + 2f,
                    textPaint
                )

                // Foreground
                val baseColor = Color(fx.colorHex)
                textPaint.color = baseColor.copy(alpha = alpha).toArgb()
                textPaint.isFakeBoldText = fx.isCrit
                drawContext.canvas.nativeCanvas.drawText(
                    fx.text,
                    targetX,
                    targetY,
                    textPaint
                )
            }
        }

        // 5. Sleek Milestone Banner (Auto-fading, non-intrusive)
        if (activeBanner != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .clip(CutCornerShape(4.dp))
                    .background(VoidBlack.copy(alpha = 0.88f))
                    .border(1.5.dp, Color(activeBanner.colorHex), CutCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${activeBanner.icon} ",
                        fontSize = 12.sp
                    )
                    Text(
                        text = activeBanner.text,
                        color = Color(activeBanner.colorHex),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ---------------- 16-BIT TILE RENDERING HELPERS ----------------

private fun DrawScope.draw16BitWall(
    tile: Tile,
    x: Float,
    y: Float,
    tileSize: Float,
    torchFlicker: Float
) {
    val isIron = tile.type == TileType.IRON_WALL
    val wallTopColor = if (isIron) Color(0xFF333145) else Color(0xFF282436)
    val wallFaceColor = if (isIron) Color(0xFF1E1C2B) else Color(0xFF171522)
    val mortarColor = Color(0xFF0F0E17)
    val rivetColor = Color(0xFF6B678A)

    if (tile.wallFrontFace) {
        val topH = tileSize * 0.38f
        val faceH = tileSize * 0.62f

        // Top Roof Parapet
        drawRect(
            color = wallTopColor,
            topLeft = Offset(x, y),
            size = Size(tileSize, topH)
        )
        // Specular rim
        drawLine(
            color = Color(0xFF4C4763),
            start = Offset(x, y + 1f),
            end = Offset(x + tileSize, y + 1f),
            strokeWidth = 2f
        )
        // Parapet edge shadow
        drawLine(
            color = Color.Black,
            start = Offset(x, y + topH),
            end = Offset(x + tileSize, y + topH),
            strokeWidth = 2f
        )

        // Vertical Wall Facade
        drawRect(
            color = wallFaceColor,
            topLeft = Offset(x, y + topH),
            size = Size(tileSize, faceH)
        )

        if (isIron) {
            drawCircle(rivetColor, 2f, Offset(x + 4f, y + topH + 4f))
            drawCircle(rivetColor, 2f, Offset(x + tileSize - 4f, y + topH + 4f))
            drawCircle(rivetColor, 2f, Offset(x + 4f, y + tileSize - 4f))
            drawCircle(rivetColor, 2f, Offset(x + tileSize - 4f, y + tileSize - 4f))
        } else {
            val midFaceY = y + topH + faceH * 0.5f
            drawLine(
                color = mortarColor,
                start = Offset(x, midFaceY),
                end = Offset(x + tileSize, midFaceY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = mortarColor,
                start = Offset(x + tileSize * 0.5f, y + topH),
                end = Offset(x + tileSize * 0.5f, midFaceY),
                strokeWidth = 1.5f
            )
        }

        // Wall Torch
        if (tile.hasWallTorch) {
            val sconceX = x + tileSize * 0.5f
            val sconceY = y + topH + 6f
            drawRect(
                color = Color(0xFF4A455A),
                topLeft = Offset(sconceX - 3f, sconceY),
                size = Size(6f, 8f)
            )
            drawCircle(
                color = ForgeAmber,
                radius = 3.5f * torchFlicker,
                center = Offset(sconceX, sconceY - 2f)
            )
        }
    } else {
        drawRect(
            color = wallTopColor,
            topLeft = Offset(x, y),
            size = Size(tileSize, tileSize)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(x, y),
            size = Size(tileSize, tileSize),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.draw16BitFloor(
    tile: Tile,
    x: Float,
    y: Float,
    tileSize: Float,
    pulseAlpha: Float
) {
    val floorColor = when (tile.floorVariation) {
        1 -> Color(0xFF111019)
        2 -> Color(0xFF161424)
        3 -> Color(0xFF181622)
        else -> Color(0xFF13121C)
    }

    drawRect(
        color = floorColor,
        topLeft = Offset(x, y),
        size = Size(tileSize, tileSize)
    )

    // Subtle mortar seam
    drawLine(
        color = Color(0xFF0C0B12),
        start = Offset(x, y),
        end = Offset(x + tileSize, y),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0xFF0C0B12),
        start = Offset(x, y),
        end = Offset(x, y + tileSize),
        strokeWidth = 1f
    )

    // Interactive pools and structures
    when (tile.type) {
        TileType.RUST_GRATE -> {
            for (i in 1..3) {
                val gx = x + tileSize * (i / 4f)
                drawLine(
                    color = Color(0xFF382F26),
                    start = Offset(gx, y),
                    end = Offset(gx, y + tileSize),
                    strokeWidth = 2f
                )
            }
        }
        TileType.WATER_PUDDLE -> {
            drawCircle(
                color = Color(0xFF1E3A8A).copy(alpha = 0.45f),
                radius = tileSize * 0.36f,
                center = Offset(x + tileSize / 2f, y + tileSize / 2f)
            )
        }
        TileType.OIL_SLICK -> {
            drawCircle(
                color = Color(0xFF581C87).copy(alpha = 0.5f),
                radius = tileSize * 0.38f,
                center = Offset(x + tileSize / 2f, y + tileSize / 2f)
            )
        }
        TileType.TOXIC_MIRE -> {
            drawCircle(
                color = ToxicGreen.copy(alpha = 0.45f * pulseAlpha),
                radius = tileSize * 0.36f,
                center = Offset(x + tileSize / 2f, y + tileSize / 2f)
            )
        }
        TileType.EMBER_HEARTH -> {
            drawCircle(
                color = SlagEmber.copy(alpha = 0.6f * pulseAlpha),
                radius = tileSize * 0.4f,
                center = Offset(x + tileSize / 2f, y + tileSize / 2f)
            )
            drawCircle(
                color = ForgeAmber,
                radius = tileSize * 0.18f,
                center = Offset(x + tileSize / 2f, y + tileSize / 2f)
            )
        }
        TileType.CHTHONIC_ALTAR -> {
            drawRect(
                color = Color(0xFF2E1045),
                topLeft = Offset(x + tileSize * 0.15f, y + tileSize * 0.15f),
                size = Size(tileSize * 0.7f, tileSize * 0.7f)
            )
            drawCircle(
                color = EldritchViolet,
                radius = tileSize * 0.25f,
                center = Offset(x + tileSize / 2f, y + tileSize / 2f),
                style = Stroke(width = 2.5f)
            )
        }
        TileType.SMELTER_FORGE -> {
            drawRect(
                color = Color(0xFF451A03),
                topLeft = Offset(x + tileSize * 0.15f, y + tileSize * 0.15f),
                size = Size(tileSize * 0.7f, tileSize * 0.7f)
            )
            drawRect(
                color = ForgeAmber,
                topLeft = Offset(x + tileSize * 0.3f, y + tileSize * 0.3f),
                size = Size(tileSize * 0.4f, tileSize * 0.4f)
            )
        }
        TileType.RELIC_CHEST -> {
            drawRect(
                color = Color(0xFF78350F),
                topLeft = Offset(x + tileSize * 0.22f, y + tileSize * 0.28f),
                size = Size(tileSize * 0.56f, tileSize * 0.44f)
            )
            drawRect(
                color = Color(0xFFFBBF24),
                topLeft = Offset(x + tileSize * 0.44f, y + tileSize * 0.42f),
                size = Size(tileSize * 0.12f, tileSize * 0.16f)
            )
        }
        TileType.ELEVATOR_EXIT -> {
            drawCircle(
                color = SanityCyan.copy(alpha = 0.8f * pulseAlpha),
                radius = tileSize * 0.42f,
                center = Offset(x + tileSize / 2f, y + tileSize / 2f),
                style = Stroke(width = 3f)
            )
        }
        else -> {}
    }
}

// ---------------- 16-BIT ENTITY SPRITES ----------------

private fun DrawScope.draw16BitPlayer(
    player: Player,
    x: Float,
    y: Float,
    tileSize: Float
) {
    val centerX = x + tileSize / 2f
    val centerY = y + tileSize / 2f

    drawRect(
        color = BoneIvory,
        topLeft = Offset(centerX - tileSize * 0.28f, centerY - tileSize * 0.3f),
        size = Size(tileSize * 0.56f, tileSize * 0.6f)
    )
    drawRect(
        color = SurfaceVariantIron,
        topLeft = Offset(centerX - tileSize * 0.22f, centerY - tileSize * 0.24f),
        size = Size(tileSize * 0.44f, tileSize * 0.48f)
    )

    val visorColor = if (player.activeMutations.any { it.category == MutationCategory.CHTHONIC_FLESH }) {
        EldritchViolet
    } else {
        ForgeAmber
    }
    drawRect(
        color = visorColor,
        topLeft = Offset(centerX - tileSize * 0.16f, centerY - tileSize * 0.18f),
        size = Size(tileSize * 0.32f, tileSize * 0.1f)
    )

    if (player.activeMutations.isNotEmpty()) {
        drawCircle(
            color = EldritchViolet,
            radius = tileSize * 0.08f,
            center = Offset(centerX - tileSize * 0.3f, centerY - tileSize * 0.1f)
        )
        drawCircle(
            color = ForgeAmber,
            radius = tileSize * 0.08f,
            center = Offset(centerX + tileSize * 0.3f, centerY - tileSize * 0.1f)
        )
    }
}

private fun DrawScope.draw16BitEnemy(
    enemy: Enemy,
    x: Float,
    y: Float,
    tileSize: Float,
    isSelected: Boolean,
    pulseAlpha: Float
) {
    val centerX = x + tileSize / 2f
    val centerY = y + tileSize / 2f

    if (isSelected) {
        drawCircle(
            color = ForgeAmber.copy(alpha = pulseAlpha),
            radius = tileSize * 0.48f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.5f)
        )
    }

    val baseColor = Color(enemy.archetype.colorHex)

    when (enemy.archetype) {
        EnemyArchetype.CRAWLING_SPIDER -> {
            drawCircle(baseColor, tileSize * 0.28f, Offset(centerX, centerY))
            drawCircle(Color.Black, tileSize * 0.12f, Offset(centerX, centerY))
            drawLine(baseColor, Offset(centerX - tileSize * 0.35f, centerY - tileSize * 0.25f), Offset(centerX, centerY), 2f)
            drawLine(baseColor, Offset(centerX + tileSize * 0.35f, centerY - tileSize * 0.25f), Offset(centerX, centerY), 2f)
            drawLine(baseColor, Offset(centerX - tileSize * 0.35f, centerY + tileSize * 0.25f), Offset(centerX, centerY), 2f)
            drawLine(baseColor, Offset(centerX + tileSize * 0.35f, centerY + tileSize * 0.25f), Offset(centerX, centerY), 2f)
        }
        EnemyArchetype.CRUCIBLE_TITAN -> {
            drawRect(
                color = baseColor,
                topLeft = Offset(centerX - tileSize * 0.4f, centerY - tileSize * 0.4f),
                size = Size(tileSize * 0.8f, tileSize * 0.8f)
            )
            drawRect(
                color = ForgeAmber,
                topLeft = Offset(centerX - tileSize * 0.2f, centerY - tileSize * 0.2f),
                size = Size(tileSize * 0.4f, tileSize * 0.4f)
            )
        }
        else -> {
            drawCircle(baseColor, tileSize * 0.34f, Offset(centerX, centerY))
            drawCircle(Color.Black, tileSize * 0.14f, Offset(centerX, centerY))
        }
    }

    val hpRatio = (enemy.currentHp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)
    val barW = tileSize * 0.76f
    val barH = 3.5f
    val barX = centerX - barW / 2f
    val barY = y - 4f

    drawRect(color = Color.Black, topLeft = Offset(barX, barY), size = Size(barW, barH))
    drawRect(color = BloodCrimson, topLeft = Offset(barX, barY), size = Size(barW * hpRatio, barH))
}
