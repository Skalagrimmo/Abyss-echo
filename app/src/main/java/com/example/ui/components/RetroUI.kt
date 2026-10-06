package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ComboStage
import com.example.model.TimePhase
import com.example.ui.theme.*

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderMetal,
    backgroundColor: Color = SurfaceIron,
    borderWidth: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .border(borderWidth, borderColor, CutCornerShape(4.dp))
            .background(backgroundColor, CutCornerShape(4.dp))
            .padding(10.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun RetroStatBar(
    label: String,
    current: Int,
    max: Int,
    barColor: Color,
    modifier: Modifier = Modifier,
    icon: String = ""
) {
    val progress = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$icon $label".trim(),
                color = BoneIvory,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$current / $max",
                color = AshGrey,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(VoidBlack, RoundedCornerShape(2.dp))
                .border(1.dp, BorderMetal, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "retro_btn",
    enabled: Boolean = true,
    color: Color = ForgeAmber,
    textColor: Color = VoidBlack
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .testTag(testTag)
            .clip(CutCornerShape(4.dp))
            .background(if (enabled) color else AshGrey.copy(alpha = 0.3f))
            .border(2.dp, if (enabled) BoneIvory.copy(alpha = 0.4f) else Color.Transparent, CutCornerShape(4.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else AshGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ComboDisplay(
    comboStage: ComboStage,
    comboHits: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(VoidBlack.copy(alpha = 0.8f), CutCornerShape(4.dp))
            .border(1.dp, if (comboHits > 0) ForgeAmber else BorderMetal, CutCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "КОМБО:",
            color = AshGrey,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        for (i in 1..3) {
            val isActive = comboHits >= i
            val isFinal = i == 3 && comboStage == ComboStage.HEAVY_PRIMED
            val boxColor = when {
                isFinal -> BloodCrimson
                isActive -> ForgeAmber
                else -> VoidBlack
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(boxColor, CutCornerShape(2.dp))
                    .border(1.dp, if (isActive) BoneIvory else BorderMetal, CutCornerShape(2.dp))
            )
        }
        if (comboStage == ComboStage.HEAVY_PRIMED) {
            Text(
                text = "⚡ РОЗКОЛ ГОТОВИЙ!",
                color = ForgeAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TimePhaseBanner(
    phase: TimePhase,
    turnCount: Int,
    modifier: Modifier = Modifier
) {
    val phaseColor = when (phase) {
        TimePhase.TWILIGHT -> SanityCyan
        TimePhase.DEEPENING_GLOOM -> EldritchViolet
        TimePhase.HOUR_OF_WHISPERS -> BloodCrimson
        TimePhase.AWAKENING_OF_GOD -> SlagEmber
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        VoidBlack.copy(alpha = 0.9f),
                        phaseColor.copy(alpha = 0.25f),
                        VoidBlack.copy(alpha = 0.9f)
                    )
                ),
                CutCornerShape(4.dp)
            )
            .border(1.dp, phaseColor.copy(alpha = 0.6f), CutCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(phaseColor, CutCornerShape(1.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = phase.title,
                    color = phaseColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = phase.modifierDesc,
                color = BoneIvory.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = "ХІД $turnCount",
            color = AshGrey,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
