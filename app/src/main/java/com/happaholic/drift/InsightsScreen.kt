package com.happaholic.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happaholic.drift.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val InsightLilacInk = Color(0xFF624A78)
private val InsightCard = RoundedCornerShape(14.dp)

@Composable
fun InsightsScreen(
    focusStreakStats: FocusStreakStats,
    appBudgets: Map<String, Int> = emptyMap(),
    onHomeClick: () -> Unit,
    onBudgetClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf<List<DailyUsageHistory>>(emptyList()) }
    var unlockWindowEnd by remember { mutableStateOf(LocalDate.now()) }
    var unlockHistory by remember { mutableStateOf<List<DailyUsageHistory>>(emptyList()) }
    LaunchedEffect(Unit) {
        history = withContext(Dispatchers.IO) { UsageHistoryRepository.loadLastSevenDays(context) }
    }
    LaunchedEffect(unlockWindowEnd) {
        unlockHistory = withContext(Dispatchers.IO) {
            UsageHistoryRepository.loadSevenDaysEnding(context, unlockWindowEnd)
        }
    }
    val scoreDays = history.filter { it.availability != UsageDataAvailability.Unavailable }
    val scores = scoreDays.map { day ->
        val focusMinutes = focusStreakStats.recentDays.firstOrNull { it.date == day.date }?.focusedMinutes ?: 0
        calculateFocusScore(
            day.totalMinutes,
            day.unlockCount,
            calculateBudgetUsageRatio(day.apps, appBudgets),
            focusMinutes,
            if (day.date == LocalDate.now()) focusStreakStats.currentStreak else 0,
            day.lateNightMinutes
        ).total
    }
    val labels = scoreDays.map { it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { DriftBottomNavigation(DriftDestination.Insights, onHomeClick, onBudgetClick, onFocusClick, onTasksClick, {}) }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    DriftBackButton(onClick = onHomeClick)
                    Text("Insights", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 10.dp))
                }
                Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            Spacer(Modifier.height(24.dp))
            WeeklyUsageCard(history)
            Spacer(Modifier.height(26.dp))
            SectionHeading("WEEKLY ATTENTION USAGE", "Last 7 days")
            Spacer(Modifier.height(10.dp))
            WeeklyScreenTimeChart(history)
            Spacer(Modifier.height(26.dp))
            SectionHeading("UNLOCK PATTERN", "Last 7 days")
            Spacer(Modifier.height(10.dp))
            UnlockPatternChart(
                history = unlockHistory,
                canMoveForward = unlockWindowEnd < LocalDate.now(),
                onPrevious = { unlockWindowEnd = unlockWindowEnd.minusDays(7) },
                onNext = { unlockWindowEnd = unlockWindowEnd.plusDays(7).coerceAtMost(LocalDate.now()) }
            )
            Spacer(Modifier.height(26.dp))
            SectionHeading("FOCUS RHYTHM", "Daily score")
            Spacer(Modifier.height(10.dp))
            FocusChart(scores, labels)
            Spacer(Modifier.height(26.dp))
            SectionHeading("WHAT CHANGED", "Compared with yesterday")
            Spacer(Modifier.height(10.dp))
            ChangeCards(history)
            Spacer(Modifier.height(26.dp))
            SectionHeading("ATTENTION-USAGE RISK", "Last seven days")
            Spacer(Modifier.height(10.dp))
            RiskHistoryCard(history)
            Spacer(Modifier.height(26.dp))
            PatternCard(history.lastOrNull(), onHomeClick)
            Spacer(Modifier.height(28.dp))
        }
        if (scrollState.canScrollForward) {
            Row(
                Modifier.align(Alignment.BottomEnd).padding(14.dp)
                    .clickable { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } }
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f), RoundedCornerShape(50))
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("More", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("↓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        }
    }
}

@Composable
private fun UnlockPatternChart(
    history: List<DailyUsageHistory>,
    canMoveForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val available = history.filter { it.availability != UsageDataAvailability.Unavailable }
    val maximum = available.maxOfOrNull(DailyUsageHistory::unlockCount)?.coerceAtLeast(20) ?: 20
    val pointColors = history.map { day ->
        if (day.availability == UsageDataAvailability.Unavailable) DriftLilac
        else riskAccentColor(unlockPalette(day.unlockCount))
    }
    val lineColor = MaterialTheme.colorScheme.secondary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val today = history.lastOrNull()
    val yesterday = history.getOrNull(history.lastIndex - 1)

    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, InsightCard)
            .border(1.dp, MaterialTheme.colorScheme.outline, InsightCard)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Text("‹", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                unlockDateRange(history),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onNext, enabled = canMoveForward, modifier = Modifier.size(32.dp)) {
                Text(
                    "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (canMoveForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Canvas(Modifier.fillMaxWidth().height(158.dp)) {
            if (history.isEmpty()) return@Canvas
            val chartTop = 28.dp.toPx()
            val chartBottom = size.height - 30.dp.toPx()
            val chartHeight = chartBottom - chartTop
            val step = if (history.size > 1) size.width / (history.size - 1) else 0f
            val points = history.mapIndexed { index, day ->
                val x = if (history.size == 1) size.width / 2f else index * step
                val y = if (day.availability == UsageDataAvailability.Unavailable) chartBottom
                else chartBottom - (day.unlockCount / maximum.toFloat()) * chartHeight
                androidx.compose.ui.geometry.Offset(x, y)
            }

            drawLine(guideColor, androidx.compose.ui.geometry.Offset(0f, chartBottom), androidx.compose.ui.geometry.Offset(size.width, chartBottom), 1.dp.toPx())
            for (index in 0 until history.lastIndex) {
                val currentAvailable = history[index].availability != UsageDataAvailability.Unavailable
                val nextAvailable = history[index + 1].availability != UsageDataAvailability.Unavailable
                if (currentAvailable && nextAvailable) {
                    drawLine(lineColor, points[index], points[index + 1], 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }

            points.forEachIndexed { index, point ->
                val day = history[index]
                val isToday = day.date == LocalDate.now()
                if (isToday && day.availability != UsageDataAvailability.Unavailable) {
                    drawCircle(lineColor, 7.dp.toPx(), point)
                    drawCircle(surfaceColor, 5.dp.toPx(), point)
                }
                drawCircle(pointColors[index], if (isToday) 3.8.dp.toPx() else 4.5.dp.toPx(), point)
            }

            drawIntoCanvas { canvas ->
                val valuePaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.sp.toPx()
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                val dayPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.sp.toPx()
                    color = labelColor.toArgb()
                }
                points.forEachIndexed { index, point ->
                    val day = history[index]
                    valuePaint.color = pointColors[index].toArgb()
                    val value = if (day.availability == UsageDataAvailability.Unavailable) "—" else day.unlockCount.toString()
                    canvas.nativeCanvas.drawText(value, point.x, point.y - 10.dp.toPx(), valuePaint)
                    val dayLabel = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    canvas.nativeCanvas.drawText(dayLabel, point.x, size.height - 3.dp.toPx(), dayPaint)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            unlockComparisonText(today, yesterday),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "● Under 40   ● 40–79   ● 80+   ● Not collected",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun unlockComparisonText(today: DailyUsageHistory?, yesterday: DailyUsageHistory?): String {
    val isToday = today?.date == LocalDate.now()
    val selectedLabel = if (isToday) "today" else today?.date?.let { "on ${it.dayOfMonth} ${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}" } ?: ""
    if (today == null || today.availability == UsageDataAvailability.Unavailable) return "Unlock data is not available for this day"
    if (yesterday == null || yesterday.availability == UsageDataAvailability.Unavailable) return "${today.unlockCount} unlocks $selectedLabel · no comparison available"
    val difference = today.unlockCount - yesterday.unlockCount
    val comparison = when {
        difference < 0 -> "${abs(difference)} fewer than the previous day"
        difference > 0 -> "$difference more than the previous day"
        else -> "same as the previous day"
    }
    return "${today.unlockCount} unlocks $selectedLabel · $comparison"
}

private fun unlockDateRange(history: List<DailyUsageHistory>): String {
    val first = history.firstOrNull()?.date ?: return "Loading…"
    val last = history.lastOrNull()?.date ?: return "Loading…"
    val month = last.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return if (first.month == last.month) "${first.dayOfMonth}–${last.dayOfMonth} $month ${last.year}"
    else "${first.dayOfMonth} ${first.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}–${last.dayOfMonth} $month"
}

@Composable
private fun WeeklyScreenTimeChart(history: List<DailyUsageHistory>) {
    val maximum = history.filter { it.availability != UsageDataAvailability.Unavailable }
        .maxOfOrNull(DailyUsageHistory::totalMinutes)?.coerceAtLeast(1) ?: 1
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, InsightCard)
        .border(1.dp, MaterialTheme.colorScheme.outline, InsightCard).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().height(142.dp), Arrangement.spacedBy(8.dp), Alignment.Bottom) {
            history.forEach { day ->
                val unavailable = day.availability == UsageDataAvailability.Unavailable
                val palette = screenTimePalette(day.totalMinutes)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (unavailable) "—" else formatInsightDuration(day.totalMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unavailable) Muted else riskAccentColor(palette), maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth()
                        .height(if (unavailable) 4.dp else (18f + day.totalMinutes / maximum.toFloat() * 72f).dp)
                        .clip(RoundedCornerShape(6.dp, 6.dp, 3.dp, 3.dp))
                        .background(if (unavailable) MaterialTheme.colorScheme.outlineVariant else palette.background))
                    Spacer(Modifier.height(6.dp))
                    Text(day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("● Low <3h    ◆ Moderate <4.5h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("■ Elevated <6h    ▲ High 6h+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeeklyUsageCard(history: List<DailyUsageHistory>) {
    val collected = history.filter { it.availability != UsageDataAvailability.Unavailable }
    val total = collected.sumOf { it.totalMinutes }
    val average = if (collected.isEmpty()) 0 else total / collected.size
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, InsightCard)
        .border(1.dp, MaterialTheme.colorScheme.outline, InsightCard).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("THIS WEEK", style = MaterialTheme.typography.labelSmall, color = InsightLilacInk)
            Text("${formatInsightDuration(average)}/day", style = MaterialTheme.typography.labelMedium, color = InsightLilacInk,
                modifier = Modifier.background(Color(0xFFF4EDF9), RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(15.dp))
        Text(formatInsightDuration(total), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text("total attention usage", style = MaterialTheme.typography.bodyLarge, color = InsightLilacInk)
        Spacer(Modifier.height(12.dp))
        Text("Based on ${collected.size} mobile-only day${if (collected.size == 1) "" else "s"} collected by Drift.", style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun FocusChart(scores: List<Int>, days: List<String>) {
    val average = if (scores.isEmpty()) 0 else scores.average().toInt()
    val change = if (scores.size > 1) scores.last() - scores.first() else 0
    val palette = focusScorePalette(average)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, InsightCard)
        .border(1.dp, MaterialTheme.colorScheme.outline, InsightCard).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column {
                Text(average.toString(), style = MaterialTheme.typography.headlineSmall)
                Text("weekly average", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            Text("${signed(change)} points", style = MaterialTheme.typography.labelMedium, color = palette.foreground,
                modifier = Modifier.background(palette.background, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth().height(150.dp), Arrangement.spacedBy(8.dp), Alignment.Bottom) {
            scores.forEachIndexed { index, score ->
                val scorePalette = focusScorePalette(score)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(score.toString(), style = MaterialTheme.typography.labelSmall, color = scorePalette.foreground)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().height((30 + score * .75).dp)
                        .clip(RoundedCornerShape(7.dp, 7.dp, 3.dp, 3.dp)).background(scorePalette.background))
                    Spacer(Modifier.height(7.dp))
                    Text(days.getOrElse(index) { "" }, style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Uses attention time, unlocks, budgets, focus habit and late-night use.", style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun ChangeCards(history: List<DailyUsageHistory>) {
    val today = history.lastOrNull()
    val yesterday = history.getOrNull(history.lastIndex - 1)
    val screenDelta = (today?.totalMinutes ?: 0) - (yesterday?.totalMinutes ?: 0)
    val unlockDelta = (today?.unlockCount ?: 0) - (yesterday?.unlockCount ?: 0)
    val screenPalette = metricStatusPalette(if (screenDelta <= 0) MetricStatus.Positive else MetricStatus.Attention)
    val unlockPalette = metricStatusPalette(if (unlockDelta <= 0) MetricStatus.Positive else MetricStatus.Attention)
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
        ChangeCard("Attention use", "${signed(screenDelta)}m", screenPalette.background, screenPalette.foreground, Modifier.weight(1f))
        ChangeCard("Unlocks", signed(unlockDelta), unlockPalette.background, unlockPalette.foreground, Modifier.weight(1f))
    }
}

@Composable
private fun ChangeCard(title: String, value: String, background: Color, foreground: Color, modifier: Modifier) {
    Column(modifier.background(background, InsightCard).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = foreground)
        Spacer(Modifier.height(15.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text("vs yesterday", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun RiskHistoryCard(history: List<DailyUsageHistory>) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, InsightCard)
        .border(1.dp, MaterialTheme.colorScheme.outline, InsightCard).padding(16.dp)) {
        history.asReversed().forEachIndexed { index, item ->
            val unavailable = item.availability == UsageDataAvailability.Unavailable
            val palette = screenTimePalette(item.totalMinutes)
            val risk = when (screenTimeBand(item.totalMinutes)) {
                ScreenTimeBand.Low1, ScreenTimeBand.Low2, ScreenTimeBand.Low3 -> "Low"
                ScreenTimeBand.Medium1 -> "Moderate"
                ScreenTimeBand.Medium2, ScreenTimeBand.Medium3 -> "Elevated"
                ScreenTimeBand.High -> "High"
            }
            val day = when (index) {
                0 -> "Today"
                1 -> "Yesterday"
                else -> item.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(day, style = MaterialTheme.typography.bodyMedium)
                    Text(if (unavailable) "Not collected" else "${item.unlockCount} unlocks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (unavailable) "Unavailable" else "$risk · ${formatInsightDuration(item.totalMinutes)}", style = MaterialTheme.typography.labelMedium,
                    color = palette.foreground, modifier = Modifier.background(palette.background, RoundedCornerShape(7.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp))
            }
            if (index < history.lastIndex) HorizontalDivider(color = Border)
        }
    }
}

@Composable
private fun PatternCard(today: DailyUsageHistory?, onAdjustWindDown: () -> Unit) {
    val minutes = today?.lateNightMinutes ?: 0
    val palette = riskPalette(if (minutes == 0) "Low" else if (minutes <= 30) "Medium" else "High")
    Column(Modifier.fillMaxWidth().clickable(onClick = onAdjustWindDown).background(palette.background, InsightCard).padding(18.dp)) {
        Text("A pattern worth noticing", style = MaterialTheme.typography.labelSmall, color = palette.foreground)
        Spacer(Modifier.height(8.dp))
        Text("Late-night screen time", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(if (minutes == 0) "No screen time was detected between 10 PM and 6 AM today."
            else "$minutes minutes were detected between 10 PM and 6 AM today.",
            style = MaterialTheme.typography.bodyMedium, color = palette.foreground)
        Spacer(Modifier.height(15.dp))
        Text("Adjust wind-down  →", style = MaterialTheme.typography.labelLarge, color = palette.foreground)
    }
}

private fun formatInsightDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> "${remainder}m"
        remainder == 0 -> "${hours}h"
        else -> "${hours}h${remainder}m"
    }
}

private fun signed(value: Int) = "${if (value > 0) "+" else if (value < 0) "−" else ""}${abs(value)}"

@Composable
private fun SectionHeading(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Muted)
        Text(trailing, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}
