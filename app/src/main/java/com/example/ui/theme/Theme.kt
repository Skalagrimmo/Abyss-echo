package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ForgeAmber,
    onPrimary = VoidBlack,
    primaryContainer = SlagEmber,
    onPrimaryContainer = BoneIvory,
    secondary = EldritchViolet,
    onSecondary = VoidBlack,
    secondaryContainer = SurfaceVariantIron,
    onSecondaryContainer = BoneIvory,
    tertiary = BloodCrimson,
    onTertiary = BoneIvory,
    background = VoidBlack,
    onBackground = BoneIvory,
    surface = AbyssalDark,
    onSurface = BoneIvory,
    surfaceVariant = SurfaceIron,
    onSurfaceVariant = AshGrey,
    outline = BorderMetal
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
