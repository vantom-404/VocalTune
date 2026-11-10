package com.intervigil.micdroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = StudioCardElevated,
    onPrimaryContainer = ElectricViolet,
    secondary = ElectricViolet,
    onSecondary = Color.Black,
    tertiary = NeonCyan,
    background = StudioDarkBackground,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary
)

@Composable
fun VocalTuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
