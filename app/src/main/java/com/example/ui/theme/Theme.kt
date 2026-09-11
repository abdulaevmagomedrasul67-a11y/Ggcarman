package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TacticalCyberColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkCanvas,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = NeonCyanGlow,
    secondary = TerminalGreen,
    onSecondary = DarkCanvas,
    secondaryContainer = DarkSurfaceHighlight,
    onSecondaryContainer = TerminalGreen,
    tertiary = PurpleAccent,
    onTertiary = DarkCanvas,
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkSurfaceHighlight,
    error = CriticalRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TacticalCyberColorScheme,
        typography = Typography,
        content = content
    )
}

