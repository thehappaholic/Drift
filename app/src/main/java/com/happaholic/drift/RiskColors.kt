package com.happaholic.drift

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.happaholic.drift.ui.theme.LocalScreenTimeColors

data class RiskPalette(val background: Color, val foreground: Color)

@Composable
fun riskAccentColor(palette: RiskPalette): Color =
    if (MaterialTheme.colorScheme.background.luminance() < .5f) palette.background else palette.foreground

enum class MetricStatus {
    Positive,
    Moderate,
    Attention
}

fun unlockStatus(unlockCount: Int): MetricStatus = when {
    unlockCount < 40 -> MetricStatus.Positive
    unlockCount < 80 -> MetricStatus.Moderate
    else -> MetricStatus.Attention
}

fun focusScoreStatus(score: Int): MetricStatus = when {
    score >= 80 -> MetricStatus.Positive
    score >= 60 -> MetricStatus.Moderate
    else -> MetricStatus.Attention
}

fun focusScoreLabel(score: Int): String = when (focusScoreStatus(score)) {
    MetricStatus.Positive -> "Good"
    MetricStatus.Moderate -> "Moderate"
    MetricStatus.Attention -> "Room to grow"
}

fun budgetUsageStatus(usedMinutes: Int, limitMinutes: Int): MetricStatus = when {
    limitMinutes <= 0 -> MetricStatus.Attention
    usedMinutes * 4 < limitMinutes * 3 -> MetricStatus.Positive
    usedMinutes < limitMinutes -> MetricStatus.Moderate
    else -> MetricStatus.Attention
}

enum class ScreenTimeBand {
    Low1,
    Low2,
    Low3,
    Medium1,
    Medium2,
    Medium3,
    High
}

fun screenTimeBand(totalMinutes: Int): ScreenTimeBand = when {
    totalMinutes < 60 -> ScreenTimeBand.Low1
    totalMinutes < 120 -> ScreenTimeBand.Low2
    totalMinutes < 180 -> ScreenTimeBand.Low3
    totalMinutes <= 270 -> ScreenTimeBand.Medium1
    totalMinutes < 360 -> ScreenTimeBand.Medium2
    else -> ScreenTimeBand.High
}

@Composable
fun screenTimePalette(totalMinutes: Int): RiskPalette {
    val colors = LocalScreenTimeColors.current
    return when (screenTimeBand(totalMinutes)) {
        ScreenTimeBand.Low1 -> RiskPalette(colors.low1, colors.onLowStrong)
        ScreenTimeBand.Low2 -> RiskPalette(colors.low2, colors.onLow)
        ScreenTimeBand.Low3 -> RiskPalette(colors.low3, colors.onLow)
        ScreenTimeBand.Medium1 -> RiskPalette(colors.medium1, colors.onMedium)
        ScreenTimeBand.Medium2 -> RiskPalette(colors.medium2, colors.onMediumStrong)
        ScreenTimeBand.Medium3 -> RiskPalette(colors.medium3, colors.onMediumStrong)
        ScreenTimeBand.High -> RiskPalette(colors.high, colors.onHigh)
    }
}

@Composable
fun metricStatusPalette(status: MetricStatus): RiskPalette {
    val colors = LocalScreenTimeColors.current
    return when (status) {
        MetricStatus.Positive -> RiskPalette(colors.low1, colors.onLowStrong)
        MetricStatus.Moderate -> RiskPalette(colors.medium1, colors.onMedium)
        MetricStatus.Attention -> RiskPalette(colors.high, colors.onHigh)
    }
}

@Composable
fun unlockPalette(unlockCount: Int): RiskPalette =
    metricStatusPalette(unlockStatus(unlockCount))

@Composable
fun focusScorePalette(score: Int): RiskPalette =
    metricStatusPalette(focusScoreStatus(score))

@Composable
fun budgetUsagePalette(usedMinutes: Int, limitMinutes: Int): RiskPalette =
    metricStatusPalette(budgetUsageStatus(usedMinutes, limitMinutes))

@Composable
fun riskPalette(level: String): RiskPalette {
    val colors = LocalScreenTimeColors.current
    return when (level.lowercase()) {
        "low", "good" -> RiskPalette(colors.low1, colors.onLowStrong)
        "medium", "moderate" -> RiskPalette(colors.medium1, colors.onMedium)
        "elevated" -> RiskPalette(colors.medium2, colors.onMediumStrong)
        "medium 1" -> RiskPalette(colors.medium1, colors.onMedium)
        "medium 2" -> RiskPalette(colors.medium2, colors.onMedium)
        "medium 3" -> RiskPalette(colors.medium3, colors.onMediumStrong)
        "high" -> RiskPalette(colors.high, colors.onHigh)
        else -> RiskPalette(Color(0xFFE8E7E2), Color(0xFF55544F))
    }
}
