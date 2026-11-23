package com.example.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Mutation
import com.example.model.MutationCategory
import com.example.model.MutationSlot
import com.example.model.SlotIntegrationPulseFx
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AnimatedAnatomicalSlotCard(
    slot: MutationSlot,
    activeMutation: Mutation?,
    isInspected: Boolean,
    pulseFx: SlotIntegrationPulseFx?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategory = if (pulseFx?.slot == slot) pulseFx.category else activeMutation?.category
    val (branchPrimary, branchAccent) = when (activeCategory) {
        MutationCategory.CHTHONIC_FLESH -> ToxicGreen to BloodCrimson
        MutationCategory.IRON_CYBERNETICS -> ForgeAmber to SanityCyan
        MutationCategory.OCCULT_SIGIL -> EldritchViolet to BloodCrimson
        null -> BorderMetal to BorderMetal
    }

    // Soft continuous breathing phase for occupied slots
    val infiniteTransition = rememberInfiniteTransition(label = "slot_breath_${slot.name}")
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_alpha_${slot.name}"
    )

    // One-shot surge animation triggered whenever this slot integrates/upgrades an organ
    val surgeProgress = remember { Animatable(0f) }
    val activePulseId = if (pulseFx?.slot == slot) pulseFx.id else 0L
    LaunchedEffect(activePulseId) {
        if (activePulseId != 0L && pulseFx != null) {
            surgeProgress.snapTo(0.01f)
            surgeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = pulseFx.durationMs.toInt().coerceAtLeast(350),
                    easing = LinearOutSlowInEasing
                )
            )
            surgeProgress.snapTo(0f)
        }
    }

    val isBursting = surgeProgress.value > 0.005f && surgeProgress.value < 0.995f
    val borderColor = when {
        isBursting -> Color.White.copy(alpha = 0.85f)
        isInspected -> BoneIvory
        activeMutation != null -> branchPrimary.copy(alpha = 0.55f + 0.45f * breath)
        else -> BorderMetal
    }

    Box(
        modifier = modifier
            .heightIn(min = 58.dp)
            .testTag("slot_${slot.name.lowercase()}")
            .clip(CutCornerShape(4.dp))
            .background(if (activeMutation != null) SurfaceVariantIron else AbyssalDark)
            .border(
                width = if (isBursting || isInspected) 2.dp else 1.2.dp,
                color = borderColor,
                shape = CutCornerShape(4.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 16-Bit Procedural Branch Motif + Breathing Glow + Surge Sparks
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            if (activeMutation != null && activeCategory != null) {
                // Soft background breath wash
                drawRect(
                    color = branchPrimary.copy(alpha = 0.07f + 0.11f * breath),
                    size = size
                )

                // Branch-distinct internal motif
                when (activeCategory) {
                    MutationCategory.CHTHONIC_FLESH -> {
                        val veinAlpha = 0.25f + 0.25f * breath
                        // Organic branching tendrils from opposite corners
                        drawLine(
                            color = branchPrimary.copy(alpha = veinAlpha),
                            start = Offset(2f, h - 2f),
                            end = Offset(cx - 4f, cy + 2f),
                            strokeWidth = 1.6f
                        )
                        drawLine(
                            color = branchPrimary.copy(alpha = veinAlpha),
                            start = Offset(cx - 4f, cy + 2f),
                            end = Offset(w * 0.35f, h * 0.22f),
                            strokeWidth = 1.2f
                        )
                        drawLine(
                            color = branchAccent.copy(alpha = veinAlpha),
                            start = Offset(w - 2f, 2f),
                            end = Offset(cx + 5f, cy - 3f),
                            strokeWidth = 1.5f
                        )
                        // Pulsing spore nodes
                        drawCircle(
                            color = branchPrimary.copy(alpha = 0.45f * breath),
                            radius = 2.2f + breath,
                            center = Offset(w * 0.22f, h * 0.72f)
                        )
                        drawCircle(
                            color = branchAccent.copy(alpha = 0.45f * breath),
                            radius = 2f + breath * 0.8f,
                            center = Offset(w * 0.78f, h * 0.24f)
                        )
                    }
                    MutationCategory.IRON_CYBERNETICS -> {
                        val metalAlpha = 0.32f + 0.28f * breath
                        // Corner rivets
                        val rOff = 5f
                        drawRect(branchPrimary.copy(alpha = metalAlpha), Offset(rOff, rOff), Size(2.5f, 2.5f))
                        drawRect(branchPrimary.copy(alpha = metalAlpha), Offset(w - rOff - 2.5f, rOff), Size(2.5f, 2.5f))
                        drawRect(branchPrimary.copy(alpha = metalAlpha), Offset(rOff, h - rOff - 2.5f), Size(2.5f, 2.5f))
                        drawRect(branchPrimary.copy(alpha = metalAlpha), Offset(w - rOff - 2.5f, h - rOff - 2.5f), Size(2.5f, 2.5f))
                        // Mechanical corner brackets & gear ticks
                        drawLine(branchAccent.copy(alpha = metalAlpha * 0.75f), Offset(0f, cy), Offset(4f, cy), 1.5f)
                        drawLine(branchAccent.copy(alpha = metalAlpha * 0.75f), Offset(w - 4f, cy), Offset(w, cy), 1.5f)
                        drawLine(branchPrimary.copy(alpha = metalAlpha * 0.65f), Offset(cx - 8f, 3f), Offset(cx + 8f, 3f), 1.5f)
                    }
                    MutationCategory.OCCULT_SIGIL -> {
                        val sigilAlpha = 0.28f + 0.32f * breath
                        val rad = min(w, h) * 0.34f
                        drawCircle(
                            color = branchPrimary.copy(alpha = sigilAlpha),
                            radius = rad,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.2f)
                        )
                        val diamond = Path().apply {
                            moveTo(cx, cy - rad * 1.15f)
                            lineTo(cx + rad * 1.15f, cy)
                            lineTo(cx, cy + rad * 1.15f)
                            lineTo(cx - rad * 1.15f, cy)
                            close()
                        }
                        drawPath(
                            path = diamond,
                            color = branchAccent.copy(alpha = sigilAlpha * 0.85f),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // Integration Surge Burst FX
            if (isBursting) {
                val p = surgeProgress.value
                val fade = (1f - p).coerceIn(0f, 1f)
                val isUpgrade = pulseFx?.slot == slot && pulseFx.isUpgrade

                // Initial core flash
                if (p < 0.32f) {
                    val flashAlpha = ((0.32f - p) / 0.32f) * 0.55f
                    drawRect(
                        color = Color.White.copy(alpha = flashAlpha),
                        size = size
                    )
                }

                // Expanding shockwave rectangle
                val inset = (1f - p) * min(w, h) * 0.28f
                drawRect(
                    color = branchPrimary.copy(alpha = fade * 0.95f),
                    topLeft = Offset(inset, inset),
                    size = Size((w - inset * 2f).coerceAtLeast(2f), (h - inset * 2f).coerceAtLeast(2f)),
                    style = Stroke(width = if (isUpgrade) 3.5f else 2.5f)
                )

                if (isUpgrade) {
                    drawRect(
                        color = SanityCyan.copy(alpha = fade * 0.8f),
                        topLeft = Offset(inset * 0.5f, inset * 0.5f),
                        size = Size((w - inset).coerceAtLeast(2f), (h - inset).coerceAtLeast(2f)),
                        style = Stroke(width = 1.8f)
                    )
                }

                // 8 Pixel sparks flying radially in branch color
                val maxSparkDist = min(w, h) * 0.62f
                for (i in 0 until 8) {
                    val angle = i * (PI.toFloat() / 4f) + (p * 0.4f)
                    val dist = maxSparkDist * (0.25f + p * 0.85f)
                    val sx = cx + cos(angle) * dist
                    val sy = cy + sin(angle) * dist
                    val sparkColor = when {
                        i % 3 == 0 -> Color.White
                        i % 2 == 0 -> branchPrimary
                        else -> branchAccent
                    }
                    drawRect(
                        color = sparkColor.copy(alpha = fade),
                        topLeft = Offset(sx - 2f, sy - 2f),
                        size = Size(4f, 4f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slotLabelShort(slot),
                color = if (activeMutation != null) BoneIvory.copy(alpha = 0.75f) else AshGrey,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = activeMutation?.name?.take(7) ?: "Вільно",
                color = if (activeMutation != null) BoneIvory else Color.DarkGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (activeMutation != null) {
                Text(
                    text = "Ранг ${activeMutation.tierRoman()}",
                    color = if (isBursting) Color.White else branchPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun slotLabelShort(slot: MutationSlot): String = when (slot) {
    MutationSlot.HEAD -> "ГОЛОВА"
    MutationSlot.ARMS -> "РУКИ"
    MutationSlot.TORSO -> "ТОРС"
    MutationSlot.LEGS -> "НОГИ"
    MutationSlot.CORE -> "ЯДРО"
}
