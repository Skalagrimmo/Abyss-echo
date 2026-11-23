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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun DungeonCanvas(
    tiles: Array<Array<Tile>>?,
    player: Player,
    enemies: List<Enemy>,
    props: List<TacticalProp> = emptyList(),
    validTargetCells: Set<GridPos> = emptySet(),
    selectedSkill: CombatSkill? = null,
    targetEnemyId: String?,
    floatingFxList: List<FloatingFx>,
    activeAttackFx: List<AttackVisualFx> = emptyList(),
    activeExplosions: List<ExplosionVisualFx> = emptyList(),
    activeMutationSurges: List<MutationSurgeFx> = emptyList(),
    activeEnemyAnims: List<EnemyAnimFx> = emptyList(),
    activeBanner: MilestoneBanner?,
    onTileTap: (GridPos) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tiles == null) return

    val context = LocalContext.current
    val enemySpriteBank = remember(context) { EnemySpriteCache.getOrLoad(context) }

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

    // Animation frame ticker ONLY when floating numbers or active FX are on screen
    var animFrame by remember { mutableLongStateOf(0L) }
    val hasActiveFx = floatingFxList.isNotEmpty() ||
            activeAttackFx.isNotEmpty() ||
            activeExplosions.isNotEmpty() ||
            activeMutationSurges.isNotEmpty() ||
            activeEnemyAnims.isNotEmpty()

    LaunchedEffect(hasActiveFx) {
        if (hasActiveFx) {
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
                    val isStructuralWall = tile.type == TileType.VOID ||
                        tile.type == TileType.WALL ||
                        tile.type == TileType.IRON_WALL
                    if (!isStructuralWall) {
                        draw16BitFloor(tile, drawX, drawY, tileSize, pulseAlpha)
                    } else {
                        draw16BitWall(tile, drawX, drawY, tileSize, torchFlicker)
                    }

                    // Downward wall shadow if north tile is a front-facing wall
                    if (!isStructuralWall && y > 0 && tiles[x][y - 1].wallFrontFace) {
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

            // 1.5 Draw Tactical Grid Overlay (Movement & Attack Distance Ranges)
            if (player.currentAp > 0) {
                // Tactical Movement Cells (adjacent walkable cells)
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val mx = player.pos.x + dx
                        val my = player.pos.y + dy
                        if (mx in minCol..maxCol && my in minRow..maxRow) {
                            val mTile = tiles[mx][my]
                            val isBlocked = props.any { !it.isDead && it.pos.x == mx && it.pos.y == my && it.type.blocksMovement }
                            if (mTile.isWalkable && !isBlocked && mTile.isVisible) {
                                val mDrawX = camOffsetX + mx * tileSize
                                val mDrawY = camOffsetY + my * tileSize
                                drawRect(
                                    color = SanityCyan.copy(alpha = 0.08f),
                                    topLeft = Offset(mDrawX, mDrawY),
                                    size = Size(tileSize, tileSize)
                                )
                                drawTacticalCellCorners(mDrawX, mDrawY, tileSize, SanityCyan.copy(alpha = 0.35f * pulseAlpha))
                            }
                        }
                    }
                }

                // Tactical Attack Target Cells (calculated by CombatEngine)
                for (targetPos in validTargetCells) {
                    if (targetPos.x in minCol..maxCol && targetPos.y in minRow..maxRow) {
                        val tTile = tiles[targetPos.x][targetPos.y]
                        if (tTile.isVisible) {
                            val tDrawX = camOffsetX + targetPos.x * tileSize
                            val tDrawY = camOffsetY + targetPos.y * tileSize

                            // Translucent range wash
                            val isAoE = selectedSkill?.isAreaEffect == true
                            val rangeColor = if (isAoE) EldritchViolet else ForgeAmber

                            drawRect(
                                color = rangeColor.copy(alpha = 0.16f),
                                topLeft = Offset(tDrawX, tDrawY),
                                size = Size(tileSize, tileSize)
                            )
                            drawTacticalCellCorners(tDrawX, tDrawY, tileSize, rangeColor.copy(alpha = 0.65f * pulseAlpha))

                            // If cell has enemy or barrel, draw tactical target lock reticle
                            val hasEnemy = enemies.any { !it.isDead && it.pos == targetPos }
                            val targetProp = props.find { !it.isDead && it.pos == targetPos }
                            if (hasEnemy || (targetProp != null && targetProp.type.isExplosive)) {
                                val reticleColor = if (hasEnemy) BloodCrimson else ForgeAmber
                                drawCircle(
                                    color = reticleColor.copy(alpha = pulseAlpha),
                                    radius = tileSize * 0.42f,
                                    center = Offset(tDrawX + tileSize / 2f, tDrawY + tileSize / 2f),
                                    style = Stroke(width = 2f)
                                )
                                drawLine(
                                    color = reticleColor.copy(alpha = pulseAlpha),
                                    start = Offset(tDrawX + tileSize * 0.15f, tDrawY + tileSize / 2f),
                                    end = Offset(tDrawX + tileSize * 0.85f, tDrawY + tileSize / 2f),
                                    strokeWidth = 1.5f
                                )
                                drawLine(
                                    color = reticleColor.copy(alpha = pulseAlpha),
                                    start = Offset(tDrawX + tileSize / 2f, tDrawY + tileSize * 0.15f),
                                    end = Offset(tDrawX + tileSize / 2f, tDrawY + tileSize * 0.85f),
                                    strokeWidth = 1.5f
                                )
                            }
                        }
                    }
                }
            }

            // 1.8 Draw Tactical Props & Obstacles (Barrels, Crates, Traps)
            for (prop in props) {
                if (prop.isDead) continue
                if (prop.pos.x !in minCol..maxCol || prop.pos.y !in minRow..maxRow) continue
                val propTile = tiles[prop.pos.x][prop.pos.y]
                if (!propTile.isVisible) continue

                val propDrawX = camOffsetX + prop.pos.x * tileSize
                val propDrawY = camOffsetY + prop.pos.y * tileSize

                draw16BitProp(prop, propDrawX, propDrawY, tileSize, pulseAlpha)
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
                val enemyAnim = activeEnemyAnims.lastOrNull { it.enemyId == enemy.id }

                draw16BitEnemy(
                    enemy = enemy,
                    x = drawX,
                    y = drawY,
                    tileSize = tileSize,
                    isSelected = isSelected,
                    pulseAlpha = pulseAlpha,
                    playerPos = player.pos,
                    enemyAnim = enemyAnim,
                    spriteFrames = enemySpriteBank[enemy.archetype],
                    now = now
                )
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

            // 3.1 Draw Mutation Surge Bio-electric Auras and Floating Glyphs
            for (surge in activeMutationSurges) {
                drawRetroMutationSurgeFx(surge, camOffsetX, camOffsetY, tileSize, now, textPaint)
            }

            // 3.2 Draw 16-Bit Attack Transition Animations (Slashes, Whips, Projectiles, Blasts)
            for (attackFx in activeAttackFx) {
                drawRetroAttackFx(attackFx, camOffsetX, camOffsetY, tileSize, now)
            }

            // 3.3 Draw 16-Bit Barrel & Hazard Explosions with Debris Particles
            for (exp in activeExplosions) {
                drawRetroExplosionFx(exp, camOffsetX, camOffsetY, tileSize, now)
            }

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

private data class BiomeTilePalette(
    val wallTop: Color,
    val ironTop: Color,
    val wallFace: Color,
    val ironFace: Color,
    val wallBevel: Color,
    val mortar: Color,
    val rivet: Color,
    val pipePrimary: Color,
    val pipeHighlight: Color,
    val tendrilPrimary: Color,
    val tendrilHighlight: Color,
    val floorColors: List<Color>,
    val floorSeam: Color,
    val conduitPrimary: Color,
    val conduitHighlight: Color
)

private fun paletteForBiome(biome: TilesetBiome): BiomeTilePalette = when (biome) {
    TilesetBiome.FLESH -> BiomeTilePalette(
        wallTop = Color(0xFF2D1C31),
        ironTop = Color(0xFF39233D),
        wallFace = Color(0xFF1C1020),
        ironFace = Color(0xFF25162B),
        wallBevel = Color(0xFF693B5E),
        mortar = Color(0xFF120914),
        rivet = Color(0xFF9E6B8A),
        pipePrimary = Color(0xFF7F1D1D),
        pipeHighlight = Color(0xFFDC2626),
        tendrilPrimary = Color(0xFF881337),
        tendrilHighlight = Color(0xFFE11D48),
        floorColors = listOf(
            Color(0xFF130D17),
            Color(0xFF18101E),
            Color(0xFF1C1222),
            Color(0xFF150E1A)
        ),
        floorSeam = Color(0xFF0A060E),
        conduitPrimary = Color(0xFF7F1D1D),
        conduitHighlight = Color(0xFFEF4444)
    )
    TilesetBiome.STEEL -> BiomeTilePalette(
        wallTop = Color(0xFF2A3342),
        ironTop = Color(0xFF334155),
        wallFace = Color(0xFF18202B),
        ironFace = Color(0xFF1E293B),
        wallBevel = Color(0xFF64748B),
        mortar = Color(0xFF0F141C),
        rivet = Color(0xFF94A3B8),
        pipePrimary = Color(0xFF92400E),
        pipeHighlight = Color(0xFFF59E0B),
        tendrilPrimary = Color(0xFF475569),
        tendrilHighlight = Color(0xFF94A3B8),
        floorColors = listOf(
            Color(0xFF11161F),
            Color(0xFF151C27),
            Color(0xFF18202D),
            Color(0xFF131924)
        ),
        floorSeam = Color(0xFF090D14),
        conduitPrimary = Color(0xFF92400E),
        conduitHighlight = Color(0xFFFBBF24)
    )
    TilesetBiome.CRUCIBLE -> BiomeTilePalette(
        wallTop = Color(0xFF2C1D1A),
        ironTop = Color(0xFF3A241E),
        wallFace = Color(0xFF1A100D),
        ironFace = Color(0xFF241612),
        wallBevel = Color(0xFF7C4432),
        mortar = Color(0xFF100907),
        rivet = Color(0xFFA8A29E),
        pipePrimary = Color(0xFF7C2D12),
        pipeHighlight = Color(0xFFF97316),
        tendrilPrimary = Color(0xFF9A3412),
        tendrilHighlight = Color(0xFFFB923C),
        floorColors = listOf(
            Color(0xFF140D0B),
            Color(0xFF19100E),
            Color(0xFF1E1310),
            Color(0xFF160E0C)
        ),
        floorSeam = Color(0xFF0A0605),
        conduitPrimary = Color(0xFFC2410C),
        conduitHighlight = Color(0xFFFBBF24)
    )
}

private fun DrawScope.draw16BitWall(
    tile: Tile,
    x: Float,
    y: Float,
    tileSize: Float,
    torchFlicker: Float
) {
    val palette = paletteForBiome(tile.biome)
    val isIron = tile.type == TileType.IRON_WALL
    val wallTopColor = if (isIron) palette.ironTop else palette.wallTop
    val wallFaceColor = if (isIron) palette.ironFace else palette.wallFace

    val hasNorth = (tile.wallMask and 1) != 0
    val hasEast = (tile.wallMask and 2) != 0
    val hasSouth = (tile.wallMask and 4) != 0
    val hasWest = (tile.wallMask and 8) != 0

    // Isolated 16-bit Biomechanical Pillar (wallMask == 0)
    if (tile.isPillar) {
        val floorColor = palette.floorColors[tile.floorVariation.coerceIn(0, 3)]
        drawRect(color = floorColor, topLeft = Offset(x, y), size = Size(tileSize, tileSize))

        val pad = tileSize * 0.14f
        val colW = tileSize - pad * 2f
        val topH = tileSize * 0.34f
        val bodyH = tileSize - pad * 2f - topH

        // Drop shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(x + pad * 0.6f, y + tileSize - pad * 1.4f),
            size = Size(tileSize - pad * 1.2f, pad * 1.2f)
        )
        // Column cap
        drawRect(
            color = wallTopColor,
            topLeft = Offset(x + pad, y + pad),
            size = Size(colW, topH)
        )
        drawRect(
            color = palette.wallBevel,
            topLeft = Offset(x + pad, y + pad),
            size = Size(colW, 2.5f)
        )
        // Column shaft
        drawRect(
            color = wallFaceColor,
            topLeft = Offset(x + pad, y + pad + topH),
            size = Size(colW, bodyH)
        )

        when (tile.biome) {
            TilesetBiome.FLESH -> {
                // Ribbed spinal core
                for (i in 1..2) {
                    val ribY = y + pad + topH + bodyH * (i / 3f)
                    drawLine(
                        color = BoneIvory.copy(alpha = 0.55f),
                        start = Offset(x + pad + 2f, ribY),
                        end = Offset(x + tileSize - pad - 2f, ribY),
                        strokeWidth = 2.5f
                    )
                }
                drawCircle(
                    color = palette.tendrilHighlight,
                    radius = tileSize * 0.08f,
                    center = Offset(x + tileSize * 0.5f, y + pad + topH * 0.5f)
                )
            }
            TilesetBiome.STEEL -> {
                // Hydraulic piston flange + rivets
                drawRect(
                    color = palette.pipePrimary,
                    topLeft = Offset(x + pad - 2f, y + pad + topH + bodyH * 0.35f),
                    size = Size(colW + 4f, tileSize * 0.14f)
                )
                drawCircle(palette.rivet, 2f, Offset(x + pad + 4f, y + pad + 4f))
                drawCircle(palette.rivet, 2f, Offset(x + tileSize - pad - 4f, y + pad + 4f))
            }
            TilesetBiome.CRUCIBLE -> {
                // Slag brazier vent
                drawRect(
                    color = palette.pipeHighlight.copy(alpha = 0.85f * torchFlicker),
                    topLeft = Offset(x + tileSize * 0.38f, y + pad + topH + bodyH * 0.25f),
                    size = Size(tileSize * 0.24f, bodyH * 0.5f)
                )
            }
        }
        drawRect(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(x + pad, y + pad),
            size = Size(colW, tileSize - pad * 2f),
            style = Stroke(width = 1.5f)
        )
        return
    }

    if (tile.wallFrontFace || !hasSouth) {
        val topH = tileSize * 0.38f
        val faceH = tileSize * 0.62f

        // Top Roof Parapet
        drawRect(
            color = wallTopColor,
            topLeft = Offset(x, y),
            size = Size(tileSize, topH)
        )
        // Exposed parapet bevel edges (4-bit autotiled)
        if (!hasNorth) {
            drawLine(
                color = palette.wallBevel,
                start = Offset(x, y + 1f),
                end = Offset(x + tileSize, y + 1f),
                strokeWidth = 2f
            )
        }
        if (!hasWest) {
            drawLine(
                color = palette.wallBevel.copy(alpha = 0.75f),
                start = Offset(x + 1f, y),
                end = Offset(x + 1f, y + tileSize),
                strokeWidth = 2f
            )
        }
        if (!hasEast) {
            drawLine(
                color = Color.Black.copy(alpha = 0.8f),
                start = Offset(x + tileSize - 1f, y),
                end = Offset(x + tileSize - 1f, y + tileSize),
                strokeWidth = 2f
            )
        }

        // Parapet overhang shadow line
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
            drawCircle(palette.rivet, 2f, Offset(x + 4f, y + topH + 4f))
            drawCircle(palette.rivet, 2f, Offset(x + tileSize - 4f, y + topH + 4f))
            drawCircle(palette.rivet, 2f, Offset(x + 4f, y + tileSize - 4f))
            drawCircle(palette.rivet, 2f, Offset(x + tileSize - 4f, y + tileSize - 4f))
            drawLine(
                color = palette.mortar,
                start = Offset(x + tileSize * 0.5f, y + topH),
                end = Offset(x + tileSize * 0.5f, y + tileSize),
                strokeWidth = 1.5f
            )
        } else {
            val midFaceY = y + topH + faceH * 0.5f
            drawLine(
                color = palette.mortar,
                start = Offset(x, midFaceY),
                end = Offset(x + tileSize, midFaceY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = palette.mortar,
                start = Offset(x + tileSize * 0.5f, y + topH),
                end = Offset(x + tileSize * 0.5f, midFaceY),
                strokeWidth = 1.5f
            )
        }

        // Biomechanical horizontal wall pipe
        if (tile.hasWallPipe) {
            val pipeY = y + topH + faceH * 0.42f
            val pipeH = tileSize * 0.16f
            drawRect(
                color = palette.pipePrimary,
                topLeft = Offset(x, pipeY),
                size = Size(tileSize, pipeH)
            )
            drawLine(
                color = palette.pipeHighlight,
                start = Offset(x, pipeY + 1.5f),
                end = Offset(x + tileSize, pipeY + 1.5f),
                strokeWidth = 1.5f
            )
            // Bolted collar flanges
            drawRect(
                color = palette.rivet,
                topLeft = Offset(x + tileSize * 0.15f, pipeY - 1.5f),
                size = Size(tileSize * 0.1f, pipeH + 3f)
            )
            drawRect(
                color = palette.rivet,
                topLeft = Offset(x + tileSize * 0.75f, pipeY - 1.5f),
                size = Size(tileSize * 0.1f, pipeH + 3f)
            )
        }

        // Organic hanging tendons / veins on facade
        if (tile.hasWallTendril) {
            val offsets = listOf(0.24f, 0.52f, 0.78f)
            val lengths = listOf(0.45f, 0.72f, 0.38f)
            for (i in offsets.indices) {
                val tx = x + tileSize * offsets[i]
                val endY = y + topH + faceH * lengths[i]
                drawLine(
                    color = palette.tendrilPrimary,
                    start = Offset(tx, y + topH),
                    end = Offset(tx, endY),
                    strokeWidth = 3f
                )
                drawLine(
                    color = palette.tendrilHighlight,
                    start = Offset(tx - 0.5f, y + topH + 1f),
                    end = Offset(tx - 0.5f, endY - 2f),
                    strokeWidth = 1.2f
                )
            }
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
                color = ForgeAmber.copy(alpha = 0.25f * torchFlicker),
                radius = tileSize * 0.22f,
                center = Offset(sconceX, sconceY - 2f)
            )
            drawCircle(
                color = ForgeAmber,
                radius = 3.5f * torchFlicker,
                center = Offset(sconceX, sconceY - 2f)
            )
        }
    } else {
        // Interior / ceiling wall mass with 4-bit exposed border bevels
        drawRect(
            color = wallTopColor,
            topLeft = Offset(x, y),
            size = Size(tileSize, tileSize)
        )

        if (!hasNorth) {
            drawLine(
                color = palette.wallBevel,
                start = Offset(x, y + 1f),
                end = Offset(x + tileSize, y + 1f),
                strokeWidth = 2f
            )
        }
        if (!hasWest) {
            drawLine(
                color = palette.wallBevel.copy(alpha = 0.8f),
                start = Offset(x + 1f, y),
                end = Offset(x + 1f, y + tileSize),
                strokeWidth = 2f
            )
        }
        if (!hasEast) {
            drawLine(
                color = Color.Black.copy(alpha = 0.75f),
                start = Offset(x + tileSize - 1f, y),
                end = Offset(x + tileSize - 1f, y + tileSize),
                strokeWidth = 2f
            )
        }

        if (isIron) {
            drawCircle(palette.rivet.copy(alpha = 0.55f), 1.6f, Offset(x + tileSize * 0.22f, y + tileSize * 0.22f))
            drawCircle(palette.rivet.copy(alpha = 0.55f), 1.6f, Offset(x + tileSize * 0.78f, y + tileSize * 0.78f))
        } else {
            // Subtle biomechanical vein/seam across ceiling block
            drawLine(
                color = palette.mortar.copy(alpha = 0.7f),
                start = Offset(x + tileSize * 0.2f, y + tileSize * 0.5f),
                end = Offset(x + tileSize * 0.8f, y + tileSize * 0.5f),
                strokeWidth = 1.2f
            )
        }
    }
}

private fun DrawScope.draw16BitFloor(
    tile: Tile,
    x: Float,
    y: Float,
    tileSize: Float,
    pulseAlpha: Float
) {
    val palette = paletteForBiome(tile.biome)
    val floorColor = palette.floorColors[tile.floorVariation.coerceIn(0, 3)]

    drawRect(
        color = floorColor,
        topLeft = Offset(x, y),
        size = Size(tileSize, tileSize)
    )

    // Subtle mortar seam
    drawLine(
        color = palette.floorSeam,
        start = Offset(x, y),
        end = Offset(x + tileSize, y),
        strokeWidth = 1f
    )
    drawLine(
        color = palette.floorSeam,
        start = Offset(x, y),
        end = Offset(x, y + tileSize),
        strokeWidth = 1f
    )

    // Biomechanical floor decals & 4-bit connected conduits
    val cx = x + tileSize * 0.5f
    val cy = y + tileSize * 0.5f
    when (tile.decal) {
        BiomechDecal.RIVET_PLATE -> {
            val inset = tileSize * 0.12f
            drawRect(
                color = palette.mortar.copy(alpha = 0.75f),
                topLeft = Offset(x + inset, y + inset),
                size = Size(tileSize - inset * 2f, tileSize - inset * 2f),
                style = Stroke(width = 1.2f)
            )
            drawCircle(palette.rivet.copy(alpha = 0.65f), 1.6f, Offset(x + inset + 3f, y + inset + 3f))
            drawCircle(palette.rivet.copy(alpha = 0.65f), 1.6f, Offset(x + tileSize - inset - 3f, y + inset + 3f))
            drawCircle(palette.rivet.copy(alpha = 0.65f), 1.6f, Offset(x + inset + 3f, y + tileSize - inset - 3f))
            drawCircle(palette.rivet.copy(alpha = 0.65f), 1.6f, Offset(x + tileSize - inset - 3f, y + tileSize - inset - 3f))
        }
        BiomechDecal.BONE_RIBS -> {
            for (i in 1..3) {
                val ry = y + tileSize * (i / 4f)
                drawLine(
                    color = BoneIvory.copy(alpha = 0.32f),
                    start = Offset(x + tileSize * 0.15f, ry),
                    end = Offset(x + tileSize * 0.85f, ry),
                    strokeWidth = 2.5f
                )
            }
        }
        BiomechDecal.FLESH_VEINS,
        BiomechDecal.COPPER_PIPE,
        BiomechDecal.RUNIC_SEAM -> {
            val cMask = tile.conduitMask
            val isPulsing = tile.decal != BiomechDecal.COPPER_PIPE
            val bodyColor = palette.conduitPrimary
            val coreColor = if (isPulsing) palette.conduitHighlight.copy(alpha = 0.78f * pulseAlpha) else palette.conduitHighlight
            val lineW = tileSize * 0.16f
            val coreW = tileSize * 0.06f

            fun drawArm(endX: Float, endY: Float) {
                drawLine(bodyColor, Offset(cx, cy), Offset(endX, endY), strokeWidth = lineW)
                drawLine(coreColor, Offset(cx, cy), Offset(endX, endY), strokeWidth = coreW)
            }

            if ((cMask and 1) != 0) drawArm(cx, y)
            if ((cMask and 2) != 0) drawArm(x + tileSize, cy)
            if ((cMask and 4) != 0) drawArm(cx, y + tileSize)
            if ((cMask and 8) != 0) drawArm(x, cy)

            // Center joint / biomechanical node
            drawCircle(
                color = bodyColor,
                radius = tileSize * 0.14f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = coreColor,
                radius = tileSize * 0.07f,
                center = Offset(cx, cy)
            )
        }
        BiomechDecal.NONE -> {}
    }

    // Interactive pools and structures
    when (tile.type) {
        TileType.RUST_GRATE -> {
            for (i in 1..3) {
                val gx = x + tileSize * (i / 4f)
                drawLine(
                    color = palette.rivet.copy(alpha = 0.55f),
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
                center = Offset(cx, cy)
            )
        }
        TileType.OIL_SLICK -> {
            drawCircle(
                color = Color(0xFF581C87).copy(alpha = 0.5f),
                radius = tileSize * 0.38f,
                center = Offset(cx, cy)
            )
        }
        TileType.TOXIC_MIRE -> {
            drawCircle(
                color = ToxicGreen.copy(alpha = 0.45f * pulseAlpha),
                radius = tileSize * 0.36f,
                center = Offset(cx, cy)
            )
        }
        TileType.EMBER_HEARTH -> {
            drawCircle(
                color = SlagEmber.copy(alpha = 0.6f * pulseAlpha),
                radius = tileSize * 0.4f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = ForgeAmber,
                radius = tileSize * 0.18f,
                center = Offset(cx, cy)
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
                center = Offset(cx, cy),
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
                center = Offset(cx, cy),
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
    pulseAlpha: Float,
    playerPos: GridPos,
    enemyAnim: EnemyAnimFx?,
    spriteFrames: EnemySpriteFrames?,
    now: Long
) {
    val baseCenterX = x + tileSize / 2f
    val baseCenterY = y + tileSize / 2f

    if (isSelected) {
        drawCircle(
            color = ForgeAmber.copy(alpha = pulseAlpha),
            radius = tileSize * 0.48f,
            center = Offset(baseCenterX, baseCenterY),
            style = Stroke(width = 2.5f)
        )
    }

    // Resolve animation phase, lunge/recoil offset, and white damage flash
    var offsetX = 0f
    var offsetY = 0f
    var flashWhite = false
    var activePhase = EnemyAnimPhase.IDLE

    if (enemyAnim != null) {
        val ageMs = (now - enemyAnim.startTime).coerceAtLeast(0L)
        if (ageMs <= enemyAnim.durationMs) {
            val progress = ageMs.toFloat() / enemyAnim.durationMs.toFloat()
            activePhase = enemyAnim.phase

            val len = hypot(enemyAnim.dirX.toFloat(), enemyAnim.dirY.toFloat()).coerceAtLeast(1f)
            val normX = enemyAnim.dirX / len
            val normY = enemyAnim.dirY / len

            when (enemyAnim.phase) {
                EnemyAnimPhase.ATTACK -> {
                    val lunge = sin(progress * PI.toFloat()) * (tileSize * 0.36f)
                    offsetX = normX * lunge
                    offsetY = normY * lunge
                }
                EnemyAnimPhase.HURT -> {
                    val recoil = sin(progress * PI.toFloat()) * (tileSize * 0.2f)
                    val shake = sin(progress * 32f) * 3.5f * (1f - progress)
                    offsetX = normX * recoil + shake
                    offsetY = normY * recoil
                    flashWhite = progress < 0.42f
                }
                EnemyAnimPhase.IDLE -> {}
            }
        }
    }

    val centerX = baseCenterX + offsetX
    val centerY = baseCenterY + offsetY

    // Contact ground shadow under monster
    drawOval(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(centerX - tileSize * 0.34f, baseCenterY + tileSize * 0.24f),
        size = Size(tileSize * 0.68f, tileSize * 0.2f)
    )

    if (spriteFrames != null) {
        val frameBitmap = when (activePhase) {
            EnemyAnimPhase.IDLE -> spriteFrames.idle
            EnemyAnimPhase.ATTACK -> spriteFrames.attack
            EnemyAnimPhase.HURT -> spriteFrames.hurt
        }
        val spriteScale = if (enemy.archetype == EnemyArchetype.CRUCIBLE_TITAN) 1.24f else 1.04f
        val drawDim = (tileSize * spriteScale).roundToInt().coerceAtLeast(1)
        val drawLeft = (centerX - drawDim / 2f).roundToInt()
        val drawTop = (centerY - drawDim / 2f - tileSize * 0.06f).roundToInt()

        val faceLeft = playerPos.x < enemy.pos.x
        val colorFilter = if (flashWhite) {
            ColorFilter.tint(Color.White, BlendMode.SrcAtop)
        } else {
            null
        }

        withTransform({
            if (faceLeft) {
                scale(
                    scaleX = -1f,
                    scaleY = 1f,
                    pivot = Offset(centerX, centerY)
                )
            }
        }) {
            drawImage(
                image = frameBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(frameBitmap.width, frameBitmap.height),
                dstOffset = IntOffset(drawLeft, drawTop),
                dstSize = IntSize(drawDim, drawDim),
                alpha = 1f,
                colorFilter = colorFilter,
                filterQuality = FilterQuality.None
            )
        }
    } else {
        val baseColor = if (flashWhite) Color.White else Color(enemy.archetype.colorHex)
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
    }

    val hpRatio = (enemy.currentHp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)
    val barW = tileSize * 0.78f
    val barH = 3.5f
    val barX = baseCenterX - barW / 2f
    val barY = y - 5f

    drawRect(color = Color.Black, topLeft = Offset(barX, barY), size = Size(barW, barH))
    drawRect(color = BloodCrimson, topLeft = Offset(barX, barY), size = Size(barW * hpRatio, barH))
}

private fun DrawScope.drawTacticalCellCorners(
    x: Float,
    y: Float,
    tileSize: Float,
    cornerColor: Color
) {
    val len = tileSize * 0.22f
    val stroke = 1.5f

    // Top-Left corner
    drawLine(cornerColor, Offset(x, y), Offset(x + len, y), stroke)
    drawLine(cornerColor, Offset(x, y), Offset(x, y + len), stroke)

    // Top-Right corner
    drawLine(cornerColor, Offset(x + tileSize, y), Offset(x + tileSize - len, y), stroke)
    drawLine(cornerColor, Offset(x + tileSize, y), Offset(x + tileSize, y + len), stroke)

    // Bottom-Left corner
    drawLine(cornerColor, Offset(x, y + tileSize), Offset(x + len, y + tileSize), stroke)
    drawLine(cornerColor, Offset(x, y + tileSize), Offset(x, y + tileSize - len), stroke)

    // Bottom-Right corner
    drawLine(cornerColor, Offset(x + tileSize, y + tileSize), Offset(x + tileSize - len, y + tileSize), stroke)
    drawLine(cornerColor, Offset(x + tileSize, y + tileSize), Offset(x + tileSize, y + tileSize - len), stroke)
}

private fun DrawScope.draw16BitProp(
    prop: TacticalProp,
    x: Float,
    y: Float,
    tileSize: Float,
    pulseAlpha: Float
) {
    val centerX = x + tileSize / 2f
    val centerY = y + tileSize / 2f

    when (prop.type) {
        TacticalPropType.EXPLOSIVE_BARREL -> {
            // Metallic Rust Red Barrel Body
            val bW = tileSize * 0.62f
            val bH = tileSize * 0.76f
            val bX = centerX - bW / 2f
            val bY = centerY - bH / 2f

            drawRect(
                color = Color(0xFF991B1B), // Dark crimson red metal
                topLeft = Offset(bX, bY),
                size = Size(bW, bH)
            )
            // Metal rim rings top and bottom
            drawRect(
                color = Color(0xFF450A0A),
                topLeft = Offset(bX, bY),
                size = Size(bW, tileSize * 0.12f)
            )
            drawRect(
                color = Color(0xFF450A0A),
                topLeft = Offset(bX, bY + bH - tileSize * 0.12f),
                size = Size(bW, tileSize * 0.12f)
            )
            // Yellow & Black hazard band
            drawRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(bX, centerY - tileSize * 0.12f),
                size = Size(bW, tileSize * 0.24f)
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(centerX - tileSize * 0.08f, centerY - tileSize * 0.12f),
                size = Size(tileSize * 0.16f, tileSize * 0.24f)
            )
            // Top ember glow
            drawCircle(
                color = Color(0xFFF97316).copy(alpha = pulseAlpha),
                radius = tileSize * 0.1f,
                center = Offset(centerX, bY + tileSize * 0.06f)
            )
        }
        TacticalPropType.BARRICADE_CRATE -> {
            // Reinforced Iron-Bound Crate
            val cW = tileSize * 0.72f
            val cH = tileSize * 0.72f
            val cX = centerX - cW / 2f
            val cY = centerY - cH / 2f

            // Wood plank base
            drawRect(
                color = Color(0xFF78350F),
                topLeft = Offset(cX, cY),
                size = Size(cW, cH)
            )
            // Iron outer border
            drawRect(
                color = Color(0xFF334155),
                topLeft = Offset(cX, cY),
                size = Size(cW, cH),
                style = Stroke(width = 3f)
            )
            // Diagonal iron brace
            drawLine(
                color = Color(0xFF475569),
                start = Offset(cX, cY),
                end = Offset(cX + cW, cY + cH),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFF475569),
                start = Offset(cX + cW, cY),
                end = Offset(cX, cY + cH),
                strokeWidth = 2.5f
            )
            // Rivets
            drawCircle(Color(0xFFCBD5E1), 2.5f, Offset(cX + 4f, cY + 4f))
            drawCircle(Color(0xFFCBD5E1), 2.5f, Offset(cX + cW - 4f, cY + 4f))
            drawCircle(Color(0xFFCBD5E1), 2.5f, Offset(cX + 4f, cY + cH - 4f))
            drawCircle(Color(0xFFCBD5E1), 2.5f, Offset(cX + cW - 4f, cY + cH - 4f))
        }
        TacticalPropType.STEAM_PRESSURE_VENT -> {
            // Floor / Wall Grate
            val gW = tileSize * 0.6f
            val gH = tileSize * 0.6f
            val gX = centerX - gW / 2f
            val gY = centerY - gH / 2f

            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(gX, gY),
                size = Size(gW, gH)
            )
            // Grate slits
            for (i in 1..3) {
                val slitY = gY + i * (gH / 4f)
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(gX + 3f, slitY),
                    end = Offset(gX + gW - 3f, slitY),
                    strokeWidth = 2f
                )
            }
            // Pulsing steam cloud plume
            drawCircle(
                color = Color(0xFFE2E8F0).copy(alpha = 0.35f * pulseAlpha),
                radius = tileSize * 0.32f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = SanityCyan.copy(alpha = 0.2f * pulseAlpha),
                radius = tileSize * 0.22f,
                center = Offset(centerX, centerY - tileSize * 0.1f)
            )
        }
        TacticalPropType.SPIKE_PLATE -> {
            // Iron Trap Plate
            val sW = tileSize * 0.68f
            val sH = tileSize * 0.68f
            val sX = centerX - sW / 2f
            val sY = centerY - sH / 2f

            drawRect(
                color = Color(0xFF334155),
                topLeft = Offset(sX, sY),
                size = Size(sW, sH)
            )
            // Spike points in 2x2 grid
            val offsets = listOf(
                Offset(centerX - tileSize * 0.16f, centerY - tileSize * 0.16f),
                Offset(centerX + tileSize * 0.16f, centerY - tileSize * 0.16f),
                Offset(centerX - tileSize * 0.16f, centerY + tileSize * 0.16f),
                Offset(centerX + tileSize * 0.16f, centerY + tileSize * 0.16f)
            )
            for (sp in offsets) {
                drawCircle(Color(0xFF94A3B8), tileSize * 0.08f, sp)
                drawCircle(Color(0xFFF1F5F9), tileSize * 0.04f, sp)
            }
        }
    }

    // HP Bar for destructible props (Barrels & Crates)
    if (prop.currentHp < prop.type.maxHp && prop.type.maxHp > 1) {
        val hpRatio = (prop.currentHp.toFloat() / prop.type.maxHp.toFloat()).coerceIn(0f, 1f)
        val barW = tileSize * 0.6f
        val barH = 3f
        val barX = centerX - barW / 2f
        val barY = y - 3f

        drawRect(color = Color.Black, topLeft = Offset(barX, barY), size = Size(barW, barH))
        drawRect(
            color = if (prop.type.isExplosive) ForgeAmber else Color(0xFF64748B),
            topLeft = Offset(barX, barY),
            size = Size(barW * hpRatio, barH)
        )
    }
}

// ---------------- 16-BIT RETRO COMBAT ANIMATION HELPERS ----------------

private fun DrawScope.drawRetroMutationSurgeFx(
    fx: MutationSurgeFx,
    camOffsetX: Float,
    camOffsetY: Float,
    tileSize: Float,
    now: Long,
    textPaint: Paint
) {
    val ageMs = (now - fx.startTime).coerceAtLeast(0)
    if (ageMs > fx.durationMs) return

    val progress = ageMs.toFloat() / fx.durationMs.toFloat()
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val centerX = camOffsetX + fx.playerPos.x * tileSize + tileSize / 2f
    val centerY = camOffsetY + fx.playerPos.y * tileSize + tileSize / 2f

    val baseColor = Color(fx.colorHex)

    // 1. Pulsating Bio-Electric Outer Runic Halo
    val haloRadius = tileSize * (0.6f + 0.15f * sin(progress * 12f))
    drawCircle(
        color = baseColor.copy(alpha = alpha * 0.35f),
        radius = haloRadius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 3f)
    )

    // 2. Rotating Runic Tick Markers (8-pointed bio-matrix)
    val rotationOffset = (progress * 360f) * (PI.toFloat() / 180f)
    for (i in 0 until 8) {
        val angle = rotationOffset + i * (PI.toFloat() / 4f)
        val outerX = centerX + cos(angle) * (haloRadius + 4f)
        val outerY = centerY + sin(angle) * (haloRadius + 4f)
        val innerX = centerX + cos(angle) * (haloRadius - 3f)
        val innerY = centerY + sin(angle) * (haloRadius - 3f)
        drawLine(
            color = if (i % 2 == 0) Color.White.copy(alpha = alpha * 0.9f) else baseColor.copy(alpha = alpha * 0.7f),
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 2f
        )
    }

    // 3. Bio-electric Jagged Arc Sparks
    val arcCount = 4
    for (a in 0 until arcCount) {
        val arcPhase = (progress * 18f + a * 2.2f)
        val arcAngle = arcPhase % (2f * PI.toFloat())
        val sparkDist = tileSize * 0.45f * (0.7f + 0.3f * sin(arcPhase))
        val spX = centerX + cos(arcAngle) * sparkDist
        val spY = centerY + sin(arcAngle) * sparkDist
        drawRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(spX - 2f, spY - 2f),
            size = Size(4f, 4f)
        )
        drawLine(
            color = baseColor.copy(alpha = alpha * 0.8f),
            start = Offset(centerX, centerY),
            end = Offset(spX, spY),
            strokeWidth = 1.5f
        )
    }

    // 4. Rising Runic Glyph with Text Shadow
    val riseY = centerY - tileSize * 0.35f - (progress * tileSize * 0.75f)
    textPaint.textSize = tileSize * 0.45f
    textPaint.color = Color.Black.copy(alpha = alpha * 0.85f).toArgb()
    drawContext.canvas.nativeCanvas.drawText(
        fx.runeGlyph,
        centerX + 2f,
        riseY + 2f,
        textPaint
    )
    textPaint.color = baseColor.copy(alpha = alpha).toArgb()
    drawContext.canvas.nativeCanvas.drawText(
        fx.runeGlyph,
        centerX,
        riseY,
        textPaint
    )
}

private fun DrawScope.drawRetroAttackFx(
    fx: AttackVisualFx,
    camOffsetX: Float,
    camOffsetY: Float,
    tileSize: Float,
    now: Long
) {
    val ageMs = (now - fx.startTime).coerceAtLeast(0)
    if (ageMs > fx.durationMs) return

    val progress = ageMs.toFloat() / fx.durationMs.toFloat()
    val alpha = (1f - progress).coerceIn(0f, 1f)

    val fromX = camOffsetX + fx.from.x * tileSize + tileSize / 2f
    val fromY = camOffsetY + fx.from.y * tileSize + tileSize / 2f
    val toX = camOffsetX + fx.to.x * tileSize + tileSize / 2f
    val toY = camOffsetY + fx.to.y * tileSize + tileSize / 2f

    val baseColor = Color(fx.colorHex)

    when (fx.distanceType) {
        AttackDistanceType.MELEE -> {
            // Crescent Slash Arc across target cell
            val dx = toX - fromX
            val dy = toY - fromY
            val angle = atan2(dy, dx)
            val arcRadius = tileSize * 0.55f

            // Slash arc sweeps dynamically
            val sweepStart = angle - (PI.toFloat() * 0.35f) + (progress * 0.4f)
            val sweepEnd = angle + (PI.toFloat() * 0.35f)

            // Draw curved slash trail using segmented lines for pixel sharpness
            val segments = 8
            var prevX = toX + cos(sweepStart) * arcRadius
            var prevY = toY + sin(sweepStart) * arcRadius
            for (s in 1..segments) {
                val t = s.toFloat() / segments.toFloat()
                val currAngle = sweepStart + (sweepEnd - sweepStart) * t
                val curRadius = arcRadius * (1f - 0.15f * sin(t * PI.toFloat()))
                val nextX = toX + cos(currAngle) * curRadius
                val nextY = toY + sin(currAngle) * curRadius

                // Outer slash trail
                drawLine(
                    color = baseColor.copy(alpha = alpha * 0.9f),
                    start = Offset(prevX, prevY),
                    end = Offset(nextX, nextY),
                    strokeWidth = 4.5f * (1f - progress * 0.6f)
                )
                // White razor core
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(prevX, prevY),
                    end = Offset(nextX, nextY),
                    strokeWidth = 2f
                )
                prevX = nextX
                prevY = nextY
            }

            // Flying slash sparks
            for (i in 1..3) {
                val sparkT = (progress * 1.6f + i * 0.25f) % 1f
                val spAngle = angle + (i - 2) * 0.35f
                val spDist = arcRadius * (0.8f + sparkT * 0.6f)
                drawRect(
                    color = Color.White.copy(alpha = alpha),
                    topLeft = Offset(toX + cos(spAngle) * spDist - 1.5f, toY + sin(spAngle) * spDist - 1.5f),
                    size = Size(3f, 3f)
                )
            }
        }
        AttackDistanceType.REACH -> {
            // Segmented Kinetic Whip / Bone Lance Thrust
            val segments = 6
            val thrustProgress = (sin(progress * PI.toFloat())).coerceIn(0f, 1f)
            val reachX = fromX + (toX - fromX) * (0.4f + 0.6f * thrustProgress)
            val reachY = fromY + (toY - fromY) * (0.4f + 0.6f * thrustProgress)

            var prevX = fromX
            var prevY = fromY
            for (i in 1..segments) {
                val t = i.toFloat() / segments.toFloat()
                // Wavy zigzag for whip tension
                val waveOffset = sin(t * PI.toFloat() * 3f + progress * 8f) * (tileSize * 0.12f * (1f - t))
                val perpAngle = atan2(reachY - fromY, reachX - fromX) + (PI.toFloat() / 2f)
                val currX = fromX + (reachX - fromX) * t + cos(perpAngle) * waveOffset
                val currY = fromY + (reachY - fromY) * t + sin(perpAngle) * waveOffset

                drawLine(
                    color = baseColor.copy(alpha = alpha),
                    start = Offset(prevX, prevY),
                    end = Offset(currX, currY),
                    strokeWidth = 3f
                )
                // Node diamond
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = 2.5f,
                    center = Offset(currX, currY)
                )
                prevX = currX
                prevY = currY
            }

            // Impact flash at spear tip
            if (thrustProgress > 0.7f) {
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.9f),
                    radius = tileSize * 0.22f * thrustProgress,
                    center = Offset(toX, toY),
                    style = Stroke(width = 2.5f)
                )
            }
        }
        AttackDistanceType.RANGED -> {
            // High-velocity Tracer with Spark Tail
            val flightProgress = progress.coerceIn(0f, 1f)
            val currX = fromX + (toX - fromX) * flightProgress
            val currY = fromY + (toY - fromY) * flightProgress

            val tailProgress = (flightProgress - 0.28f).coerceAtLeast(0f)
            val tailX = fromX + (toX - fromX) * tailProgress
            val tailY = fromY + (toY - fromY) * tailProgress

            // Tracer beam
            drawLine(
                color = baseColor.copy(alpha = alpha * 0.8f),
                start = Offset(tailX, tailY),
                end = Offset(currX, currY),
                strokeWidth = 4f
            )
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(tailX, tailY),
                end = Offset(currX, currY),
                strokeWidth = 2f
            )

            // Projectile Head
            drawCircle(
                color = ForgeAmber,
                radius = 4.5f,
                center = Offset(currX, currY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = Offset(currX, currY)
            )

            // Trailing pixel sparks
            for (p in 1..4) {
                val sparkBack = (flightProgress - p * 0.06f).coerceAtLeast(0f)
                val spX = fromX + (toX - fromX) * sparkBack + ((p % 2 * 2 - 1) * 3f)
                val spY = fromY + (toY - fromY) * sparkBack + ((p % 3 - 1) * 3f)
                drawRect(
                    color = ForgeAmber.copy(alpha = alpha * 0.7f),
                    topLeft = Offset(spX - 1.5f, spY - 1.5f),
                    size = Size(3f, 3f)
                )
            }
        }
        AttackDistanceType.AREA -> {
            // Expanding Pixel Shockwave Cluster
            val maxRadius = tileSize * 1.4f
            val currRadius = maxRadius * progress

            // Shockwave outer ring
            drawCircle(
                color = baseColor.copy(alpha = alpha * 0.85f),
                radius = currRadius,
                center = Offset(toX, toY),
                style = Stroke(width = 3.5f * (1f - progress * 0.5f))
            )
            // Shockwave inner bright pulse
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.9f),
                radius = currRadius * 0.82f,
                center = Offset(toX, toY),
                style = Stroke(width = 1.5f)
            )

            // 8 Cardinal/Diagonal Pixelated Bursts
            for (i in 0 until 8) {
                val burstAngle = i * (PI.toFloat() / 4f)
                val burstX = toX + cos(burstAngle) * currRadius
                val burstY = toY + sin(burstAngle) * currRadius
                drawRect(
                    color = Color.White.copy(alpha = alpha),
                    topLeft = Offset(burstX - 2.5f, burstY - 2.5f),
                    size = Size(5f, 5f)
                )
            }
        }
    }
}

private fun DrawScope.drawRetroExplosionFx(
    fx: ExplosionVisualFx,
    camOffsetX: Float,
    camOffsetY: Float,
    tileSize: Float,
    now: Long
) {
    val ageMs = (now - fx.startTime).coerceAtLeast(0)
    if (ageMs > fx.durationMs) return

    val progress = ageMs.toFloat() / fx.durationMs.toFloat()
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val centerX = camOffsetX + fx.epicenter.x * tileSize + tileSize / 2f
    val centerY = camOffsetY + fx.epicenter.y * tileSize + tileSize / 2f

    val maxBlastRadius = tileSize * fx.radiusTiles
    // Ease-out explosion expansion
    val currentBlastRadius = maxBlastRadius * (1f - (1f - progress) * (1f - progress))

    // 1. Expanding Dark Smoke Cloud Layer
    val smokeRadius = currentBlastRadius * 1.15f
    val smokeAlpha = (alpha * 0.5f).coerceIn(0f, 1f)
    for (s in 0 until 6) {
        val sAngle = s * (PI.toFloat() / 3f) + (progress * 0.5f)
        val sDist = smokeRadius * 0.65f
        val sx = centerX + cos(sAngle) * sDist
        val sy = centerY + sin(sAngle) * sDist
        drawCircle(
            color = Color(0xFF334155).copy(alpha = smokeAlpha),
            radius = smokeRadius * 0.45f,
            center = Offset(sx, sy)
        )
    }

    // 2. Outer Fireball Rim (Blood Crimson)
    drawCircle(
        color = BloodCrimson.copy(alpha = alpha * 0.85f),
        radius = currentBlastRadius,
        center = Offset(centerX, centerY)
    )

    // 3. Mid Fire Layer (Forge Amber)
    drawCircle(
        color = ForgeAmber.copy(alpha = alpha * 0.95f),
        radius = currentBlastRadius * 0.72f,
        center = Offset(centerX, centerY)
    )

    // 4. White-Hot Detonation Core
    drawCircle(
        color = Color.White.copy(alpha = (1f - progress * 1.5f).coerceIn(0f, 1f)),
        radius = currentBlastRadius * 0.4f,
        center = Offset(centerX, centerY)
    )

    // 5. Pixelated Flying Debris Particles (Wood, Shrapnel, Sparks)
    for (d in fx.debris) {
        val debrisTime = progress
        val dx = centerX + d.vx * debrisTime
        // Gravity accelerates debris downwards
        val dy = centerY + d.vy * debrisTime + 0.5f * 95f * (debrisTime * debrisTime)

        drawRect(
            color = Color(d.colorHex).copy(alpha = alpha),
            topLeft = Offset(dx - d.pixelSize / 2f, dy - d.pixelSize / 2f),
            size = Size(d.pixelSize, d.pixelSize)
        )
    }
}


