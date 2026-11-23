package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AmbientTrackProfile
import com.example.model.AudioUiState
import com.example.ui.theme.*
import kotlin.math.roundToInt
import kotlin.math.sin

private fun profileAccentColor(profile: AmbientTrackProfile): Color = when (profile) {
    AmbientTrackProfile.VOID_MENU -> SanityCyan
    AmbientTrackProfile.FLESH_CATACOMBS -> ToxicGreen
    AmbientTrackProfile.STEEL_FOUNDRY -> ForgeAmber
    AmbientTrackProfile.CRUCIBLE_ABYSS -> EldritchViolet
}

@Composable
fun TrackIndicatorPill(
    audioState: AudioUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierColor = profileAccentColor(audioState.activeProfile)
    val isSurging = audioState.isCombatSurgeActive && !audioState.isMuted
    val borderColor = if (isSurging) BloodCrimson else tierColor.copy(alpha = 0.7f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("hud_track_indicator")
            .clip(CutCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        VoidBlack.copy(alpha = 0.94f),
                        (if (isSurging) BloodCrimson else tierColor).copy(alpha = 0.16f),
                        SurfaceIron.copy(alpha = 0.94f)
                    )
                )
            )
            .border(1.5.dp, borderColor, CutCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AmbientWaveBars(
                color = if (isSurging) BloodCrimson else tierColor,
                isActive = !audioState.isMuted && audioState.musicVolume > 0.01f,
                combatIntensity = audioState.combatIntensity
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (audioState.isMuted) "🔇 ${audioState.activeProfile.title}" else "🎵 ${audioState.activeProfile.title}",
                        color = if (audioState.isMuted) AshGrey else BoneIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = audioState.activeProfile.subtitle,
                    color = tierColor.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSurging) {
                Box(
                    modifier = Modifier
                        .background(BloodCrimson.copy(alpha = 0.25f), CutCornerShape(3.dp))
                        .border(1.dp, BloodCrimson, CutCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⚡ БІЙ",
                        color = BloodCrimson,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(SurfaceVariantIron, CutCornerShape(3.dp))
                    .border(1.dp, BorderMetal, CutCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🎛️ ${(audioState.musicVolume * 100).roundToInt()}%",
                    color = BoneIvory,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun AmbientWaveBars(
    color: Color,
    isActive: Boolean,
    combatIntensity: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_wave_bars")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (combatIntensity >= 0.35f) 1100 else 2200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(
        modifier = modifier
            .width(20.dp)
            .height(18.dp)
    ) {
        val barCount = 4
        val barWidth = (size.width / (barCount * 2f - 1f)).coerceAtLeast(2f)
        val gap = barWidth
        for (i in 0 until barCount) {
            val fraction = if (!isActive) {
                0.15f
            } else {
                val base = 0.32f + 0.25f * combatIntensity
                val wave = 0.5f + 0.5f * sin(phase + i * 1.25f)
                (base + wave * (0.38f + 0.25f * combatIntensity)).coerceIn(0.18f, 1f)
            }
            val barH = size.height * fraction
            val left = i * (barWidth + gap)
            drawRect(
                color = if (isActive) color else AshGrey.copy(alpha = 0.45f),
                topLeft = Offset(left, size.height - barH),
                size = Size(barWidth, barH)
            )
        }
    }
}

@Composable
fun AudioMixerPanel(
    audioState: AudioUiState,
    onToggleMute: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showTrackHeader: Boolean = true,
    onClose: (() -> Unit)? = null
) {
    val tierColor = profileAccentColor(audioState.activeProfile)

    PixelCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = tierColor,
        backgroundColor = SurfaceIron
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎛️ АУДІО-Синтез (16-BIT)",
                    color = tierColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (showTrackHeader) {
                    Text(
                        text = "${audioState.activeProfile.title} • ${audioState.activeProfile.subtitle}",
                        color = AshGrey,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RetroButton(
                    text = if (audioState.isMuted) "🔇 ВИМК" else "🔊 УВІМК",
                    onClick = onToggleMute,
                    color = if (audioState.isMuted) BloodCrimson else SurfaceVariantIron,
                    textColor = BoneIvory,
                    testTag = "btn_mute_toggle",
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
                if (onClose != null) {
                    RetroButton(
                        text = "✕",
                        onClick = onClose,
                        color = SurfaceVariantIron,
                        textColor = BoneIvory,
                        testTag = "btn_close_audio_mixer",
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        VolumeSliderRow(
            label = "МУЗИКА (ЕМБІЄНТ)",
            value = audioState.musicVolume,
            accentColor = tierColor,
            enabled = !audioState.isMuted,
            testTag = "slider_music_volume",
            onValueChange = onMusicVolumeChange
        )

        Spacer(modifier = Modifier.height(6.dp))

        VolumeSliderRow(
            label = "ЕФЕКТИ (SFX)",
            value = audioState.sfxVolume,
            accentColor = SanityCyan,
            enabled = !audioState.isMuted,
            testTag = "slider_sfx_volume",
            onValueChange = onSfxVolumeChange
        )

        if (showTrackHeader) {
            Spacer(modifier = Modifier.height(6.dp))
            val tensionPct = (audioState.combatIntensity * 100).roundToInt()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VoidBlack.copy(alpha = 0.7f), CutCornerShape(3.dp))
                    .border(1.dp, BorderMetal, CutCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "БОЙОВИЙ ШАР:",
                    color = AshGrey,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (audioState.isCombatSurgeActive) "⚡ АКТИВНИЙ ($tensionPct%)" else "СПОКІЙ ($tensionPct%)",
                    color = if (audioState.isCombatSurgeActive) BloodCrimson else ToxicGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun VolumeSliderRow(
    label: String,
    value: Float,
    accentColor: Color,
    enabled: Boolean,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (enabled) BoneIvory else AshGrey,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${(value * 100).roundToInt()}%",
                color = if (enabled) accentColor else AshGrey,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = VoidBlack,
                disabledThumbColor = AshGrey,
                disabledActiveTrackColor = AshGrey.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .testTag(testTag)
        )
    }
}

@Composable
fun AudioMixerOverlay(
    visible: Boolean,
    audioState: AudioUiState,
    onToggleMute: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.96f, animationSpec = tween(180)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.96f, animationSpec = tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.68f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(top = 48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                AudioMixerPanel(
                    audioState = audioState,
                    onToggleMute = onToggleMute,
                    onMusicVolumeChange = onMusicVolumeChange,
                    onSfxVolumeChange = onSfxVolumeChange,
                    showTrackHeader = true,
                    onClose = onDismiss
                )
            }
        }
    }
}
