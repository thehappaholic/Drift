package com.example.drift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ScreenTimeColorScheme(
    val low1: Color,
    val low2: Color,
    val low3: Color,
    val onLow: Color,
    val onLowStrong: Color,
    val medium1: Color,
    val medium2: Color,
    val medium3: Color,
    val onMedium: Color,
    val onMediumStrong: Color,
    val high: Color,
    val onHigh: Color
) {
    // General positive-state container used outside time-band charts.
    val low: Color get() = low1
}

private val LightScreenTimeColors = ScreenTimeColorScheme(
    ScreenTimeLow1, ScreenTimeLow2, ScreenTimeLow3, OnScreenTimeLow, OnScreenTimeLowStrong,
    ScreenTimeMedium1, ScreenTimeMedium2, ScreenTimeMedium3, OnScreenTimeMedium, OnScreenTimeMediumStrong,
    ScreenTimeHigh, OnScreenTimeHigh
)

private val DarkScreenTimeColors = ScreenTimeColorScheme(
    ScreenTimeLow1Dark, ScreenTimeLow2Dark, ScreenTimeLow3Dark, OnScreenTimeLowDark, OnScreenTimeLowStrongDark,
    ScreenTimeMedium1Dark, ScreenTimeMedium2Dark, ScreenTimeMedium3Dark, OnScreenTimeMediumDark, OnScreenTimeMediumStrongDark,
    ScreenTimeHighDark, OnScreenTimeHighDark
)

val LocalScreenTimeColors = staticCompositionLocalOf { LightScreenTimeColors }

private val DriftColors = lightColorScheme(
    primary = DriftPrimaryLight,
    onPrimary = Surface,
    primaryContainer = DriftPrimaryContainer,
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
    CompositionLocalProvider(
        LocalScreenTimeColors provides if (darkTheme) DarkScreenTimeColors else LightScreenTimeColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DriftDarkColors else DriftColors,
            typography = Typography,
            content = content
        )
    }
}
