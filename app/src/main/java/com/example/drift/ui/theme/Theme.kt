package com.example.drift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DriftColors = lightColorScheme(
    primary = DriftPrimaryLight,
    onPrimary = Surface,
    primaryContainer = Color(0xFFE9E2FF),
    onPrimaryContainer = Ink,
    secondary = DriftLilac,
    onSecondary = Surface,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = Muted,
    outline = Border,
    outlineVariant = Border,
    tertiary = DriftWarmYellow,
    error = Color(0xFFBA1A1A)
)

private val DriftDarkColors = darkColorScheme(
    primary = DriftPrimaryDark,
    onPrimary = Color(0xFF131314),
    primaryContainer = Color(0xFF3A3158),
    onPrimaryContainer = Color(0xFFF1ECFF),
    secondary = DriftLilac,
    onSecondary = Color(0xFF131314),
    tertiary = DriftWarmYellow,
    background = Color(0xFF111116),
    onBackground = Color(0xFFF8F6FA),
    surface = Color(0xFF1B1A22),
    onSurface = Color(0xFFF8F6FA),
    surfaceVariant = Color(0xFF282631),
    onSurfaceVariant = Color(0xFFC9C3D1),
    outline = Color(0xFF4B4756),
    outlineVariant = Color(0xFF35323F),
    error = Color(0xFFF0B8B2)
)

@Composable
fun DriftTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DriftDarkColors else DriftColors,
        typography = Typography,
        content = content
    )
}
