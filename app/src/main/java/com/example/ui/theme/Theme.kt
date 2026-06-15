package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CosmicBlack,
    secondary = ElectricMagenta,
    onSecondary = TextPrimary,
    tertiary = ElectricPurple,
    onTertiary = TextPrimary,
    background = CosmicBlack,
    onBackground = TextPrimary,
    surface = DeepSpace,
    onSurface = TextPrimary,
    surfaceVariant = GlassWhite,
    onSurfaceVariant = TextPrimary,
    outline = GlassBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for cohesive high-tech sci-fi aesthetic
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
