package com.example.ui.screen

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun DungeonCanvas(
    tiles: Array<Array<Tile>>?,
    player: Player,
    enemies: List<Enemy>,
    targetEnemyId: String?,
    onTileTap: (GridPos) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tiles == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val gridW = tiles.size
    val gridH = tiles[0].size

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
                .pointerInput(tiles) {
                    detectTapGestures { tapOffset ->
                        val viewW = size.width
                        val viewH = size.height
                        val tileSize = min(viewW / 11f, viewH / 11f)

                        // Camera centers on player
                        val camOffsetX = (viewW / 2f) - (player.pos.x * tileSize + tileSize / 2f)
                        val camOffsetY = (viewH / 2f) - (player.pos.y * tileSize + tileSize / 2f)

                        val cellX = ((tapOffset.x - camOffsetX) / tileSize).toInt()
                        val cellY = ((tapOffset.y - camOffsetY) / tileSize).toInt()

                        if (cellX in 0 until gridW && cellY in 0 until gridH) {
                            onTileTap(GridPos(cellX, cellY))
                        }
                    }
                }
        ) {
            val viewW = size.width
            val viewH = size.height
            val tileSize = min(viewW / 11f, viewH / 11f)

            // Centering camera on player
            val camOffsetX = (viewW / 2f) - (player.pos.x * tileSize + tileSize / 2f)
            val camOffsetY = (viewH / 2f) - (player.pos.y * tileSize + tileSize / 2f)

            // Draw visible viewport cells
            for (x in 0 until gridW) {
                for (y in 0 until gridH) {
                    val tile = tiles[x][y]
                    val drawX = camOffsetX + x * tileSize
                    val drawY = camOffsetY + y * tileSize

                    // Skip off-screen
                    if (drawX + tileSize < 0 || drawX > viewW || drawY + tileSize < 0 || drawY > viewH) continue

                    if (!tile.isDiscovered) {
                        // Unexplored void
                        drawRect(
                            color = VoidBlack,
                            topLeft = Offset(drawX, drawY),
                            size = Size(tileSize, tileSize)
                        )
                        continue
                    }

                    // Base tile render
                    val baseColor = when (tile.type) {
                        TileType.VOID -> VoidBlack
                        TileType.WALL -> Color(0xFF1E1B26)
                        TileType.IRON_WALL -> Color(0xFF282736)
                        TileType.FLOOR -> Color(0xFF13121C)
                        TileType.RUST_GRATE -> Color(0xFF2B241E)
                        TileType.WATER_PUDDLE -> Color(0xFF0F2B48)
                        TileType.OIL_SLICK -> Color(0xFF2E1A47)
                        TileType.TOXIC_MIRE -> Color(0xFF143820)
                        TileType.EMBER_HEARTH -> Color(0xFF4A2010)
                        TileType.CHTHONIC_ALTAR -> Color(0xFF3B1854)
                        TileType.SMELTER_FORGE -> Color(0xFF5A2A0C)
                        TileType.RELIC_CHEST -> Color(0xFF4A3E1A)
                        TileType.ELEVATOR_EXIT -> Color(0xFF164E63)
                    }

                    drawRect(
                        color = baseColor,
                        topLeft = Offset(drawX, drawY),
                        size = Size(tileSize, tileSize)
                    )

                    // Subtle grid border
                    drawRect(
                        color = Color.Black.copy(alpha = 0.4f),
                        topLeft = Offset(drawX, drawY),
                        size = Size(tileSize, tileSize),
                        style = Stroke(width = 1f)
                    )

                    // Environmental decorations
                    when (tile.type) {
                        TileType.RUST_GRATE -> {
                            // Draw grating lines
                            drawLine(
                                color = BorderMetal,
                                start = Offset(drawX + tileSize * 0.25f, drawY),
                                end = Offset(drawX + tileSize * 0.25f, drawY + tileSize),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = BorderMetal,
                                start = Offset(drawX + tileSize * 0.75f, drawY),
                                end = Offset(drawX + tileSize * 0.75f, drawY + tileSize),
                                strokeWidth = 1.5f
                            )
                        }
                        TileType.WATER_PUDDLE -> {
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                                radius = tileSize * 0.35f,
                                center = Offset(drawX + tileSize / 2f, drawY + tileSize / 2f)
                            )
                        }
                        TileType.OIL_SLICK -> {
                            drawCircle(
                                color = Color(0xFFA855F7).copy(alpha = 0.4f),
                                radius = tileSize * 0.38f,
                                center = Offset(drawX + tileSize / 2f, drawY + tileSize / 2f)
                            )
                        }
                        TileType.TOXIC_MIRE -> {
                            drawCircle(
                                color = ToxicGreen.copy(alpha = 0.45f * pulseAlpha),
                                radius = tileSize * 0.35f,
                                center = Offset(drawX + tileSize / 2f, drawY + tileSize / 2f)
                            )
                        }
                        TileType.EMBER_HEARTH -> {
                            drawCircle(
                                color = SlagEmber.copy(alpha = 0.6f * pulseAlpha),
                                radius = tileSize * 0.4f,
                                center = Offset(drawX + tileSize / 2f, drawY + tileSize / 2f)
                            )
                        }
                        TileType.CHTHONIC_ALTAR -> {
                            // Mystic purple altar glyph
                            drawRect(
                                color = EldritchViolet,
                                topLeft = Offset(drawX + tileSize * 0.2f, drawY + tileSize * 0.2f),
                                size = Size(tileSize * 0.6f, tileSize * 0.6f)
                            )
                        }
                        TileType.SMELTER_FORGE -> {
                            // Heavy iron forge
                            drawRect(
                                color = ForgeAmber,
                                topLeft = Offset(drawX + tileSize * 0.2f, drawY + tileSize * 0.2f),
                                size = Size(tileSize * 0.6f, tileSize * 0.6f)
                            )
                        }
                        TileType.RELIC_CHEST -> {
                            // Chest icon
                            drawRect(
                                color = Color(0xFFEAB308),
                                topLeft = Offset(drawX + tileSize * 0.25f, drawY + tileSize * 0.3f),
                                size = Size(tileSize * 0.5f, tileSize * 0.4f)
                            )
                        }
                        TileType.ELEVATOR_EXIT -> {
                            // Pulsing exit hatch
                            drawCircle(
                                color = SanityCyan.copy(alpha = 0.7f * pulseAlpha),
                                radius = tileSize * 0.4f,
                                center = Offset(drawX + tileSize / 2f, drawY + tileSize / 2f),
                                style = Stroke(width = 3f)
                            )
                        }
                        else -> {}
                    }

                    // Blood splatter if marked
                    if (tile.hasBloodSplatter) {
                        drawCircle(
                            color = BloodCrimson.copy(alpha = 0.6f),
                            radius = tileSize * 0.22f,
                            center = Offset(drawX + tileSize * 0.35f, drawY + tileSize * 0.6f)
                        )
                    }

                    // Loot indicator
                    if (tile.scrapLoot > 0 || tile.biomassLoot > 0) {
                        drawCircle(
                            color = ForgeAmber,
                            radius = tileSize * 0.15f,
                            center = Offset(drawX + tileSize * 0.7f, drawY + tileSize * 0.3f)
                        )
                    }

                    // Fog of war overlay
                    if (!tile.isVisible) {
                        // In memory but not currently lit
                        drawRect(
                            color = VoidBlack.copy(alpha = 0.65f),
                            topLeft = Offset(drawX, drawY),
                            size = Size(tileSize, tileSize)
                        )
                    } else {
                        // Torchlight gradient tint
                        val distToPlayer = player.pos.manhattanDistance(tile.pos)
                        val lightAlpha = (distToPlayer.toFloat() / (player.fovRadius + 1)).coerceIn(0f, 0.45f)
                        drawRect(
                            color = VoidBlack.copy(alpha = lightAlpha),
                            topLeft = Offset(drawX, drawY),
                            size = Size(tileSize, tileSize)
                        )
                    }
                }
            }

            // Draw Enemies
            for (enemy in enemies) {
                if (enemy.isDead) continue
                val enemyTile = tiles[enemy.pos.x][enemy.pos.y]
                if (!enemyTile.isVisible) continue

                val drawX = camOffsetX + enemy.pos.x * tileSize
                val drawY = camOffsetY + enemy.pos.y * tileSize

                val isSelected = enemy.id == targetEnemyId

                // Selection ring
                if (isSelected) {
                    drawCircle(
                        color = ForgeAmber.copy(alpha = pulseAlpha),
                        radius = tileSize * 0.48f,
                        center = Offset(drawX + tileSize / 2f, drawY + tileSize / 2f),
                        style = Stroke(width = 2.5f)
                    )
                }

                // Enemy Body representation
                val enemyColor = Color(enemy.archetype.colorHex)
                drawCircle(
                    color = enemyColor,
                    radius = tileSize * 0.38f,
                    center = Offset(drawX + tileSize / 2f, drawY + tileSize / 2f)
                )

                // Inner core/eye
                drawCircle(
                    color = Color.Black,
                    radius = tileSize * 0.16f,
                    center = Offset(drawX + tileSize / 2f, drawY + tileSize / 2f)
                )

                // HP Bar above enemy
                val hpRatio = (enemy.currentHp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)
                val barW = tileSize * 0.8f
                val barH = 4f
                val barX = drawX + (tileSize - barW) / 2f
                val barY = drawY - 6f

                drawRect(
                    color = Color.Black,
                    topLeft = Offset(barX, barY),
                    size = Size(barW, barH)
                )
                drawRect(
                    color = BloodCrimson,
                    topLeft = Offset(barX, barY),
                    size = Size(barW * hpRatio, barH)
                )

                // Intent Icon marker
                val intentColor = when (enemy.intent.type) {
                    EnemyIntentType.ATTACK_MELEE -> BloodCrimson
                    EnemyIntentType.ATTACK_RANGED -> ForgeAmber
                    EnemyIntentType.CAST_CURSE -> EldritchViolet
                    EnemyIntentType.APPROACH -> AshGrey
                    EnemyIntentType.DEFEND -> SanityCyan
                }
                drawCircle(
                    color = intentColor,
                    radius = 3.5f,
                    center = Offset(drawX + tileSize * 0.85f, drawY + 4f)
                )
            }

            // Draw Player
            val pX = camOffsetX + player.pos.x * tileSize
            val pY = camOffsetY + player.pos.y * tileSize

            // Player aura / torch circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ForgeAmber.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(pX + tileSize / 2f, pY + tileSize / 2f),
                    radius = tileSize * 1.5f
                ),
                radius = tileSize * 1.5f,
                center = Offset(pX + tileSize / 2f, pY + tileSize / 2f)
            )

            // Player 16-bit Avatar
            // Base armor body
            drawCircle(
                color = BoneIvory,
                radius = tileSize * 0.42f,
                center = Offset(pX + tileSize / 2f, pY + tileSize / 2f)
            )
            drawCircle(
                color = SurfaceVariantIron,
                radius = tileSize * 0.35f,
                center = Offset(pX + tileSize / 2f, pY + tileSize / 2f)
            )

            // Glowing visor (Forge amber or Eldritch violet depending on mutations)
            val visorColor = if (player.activeMutations.any { it.category == MutationCategory.CHTHONIC_FLESH }) {
                EldritchViolet
            } else {
                ForgeAmber
            }
            drawRect(
                color = visorColor,
                topLeft = Offset(pX + tileSize * 0.32f, pY + tileSize * 0.35f),
                size = Size(tileSize * 0.36f, tileSize * 0.16f)
            )

            // Biomechanical mutation spikes if present
            if (player.activeMutations.isNotEmpty()) {
                drawCircle(
                    color = EldritchViolet,
                    radius = 3.5f,
                    center = Offset(pX + tileSize * 0.18f, pY + tileSize * 0.25f)
                )
                drawCircle(
                    color = EldritchViolet,
                    radius = 3.5f,
                    center = Offset(pX + tileSize * 0.82f, pY + tileSize * 0.25f)
                )
            }
        }
    }
}
