package com.example.drift

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drift.data.assignment.Assignment
import com.example.drift.data.assignment.AssignmentRepository
import com.example.drift.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HomeShape = RoundedCornerShape(14.dp)
private val PreviewWeeklyScreenTimeMinutes = listOf(30, 90, 150, 210, 270, 330, 360)

@Composable
fun DashboardScreen(
    userName: String = "",
    appBudgets: Map<String, Int> = mapOf(
        "Instagram" to 45,
        "YouTube" to 40,
        "Chrome" to 60
    ),
    focusStreakStats: FocusStreakStats = FocusStreakStats(0, 0, 0, emptyList()),
    onFocusClick: () -> Unit = {},
    onFocusScoreClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {},
    onUsageHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var upcomingAssignments by remember { mutableStateOf(emptyList<Assignment>()) }
    var weeklyUsageHistory by remember { mutableStateOf(emptyList<DailyUsageHistory>()) }
    var todayAppUsage by remember { mutableStateOf(emptyList<AppUsageEntry>()) }
    var todayUsage by remember {
        mutableStateOf(DailyUsageHistory(LocalDate.now(), emptyList()))
    }
    var selectedUsageDate by remember { mutableStateOf(LocalDate.now()) }
    var displayedUsage by remember {
        mutableStateOf(DailyUsageHistory(LocalDate.now(), emptyList()))
    }

    LaunchedEffect(Unit) {
        AssignmentRepository.loadAssignments().onSuccess { assignments ->
            upcomingAssignments = assignments.filterNot(Assignment::isCompleted).take(3)
        }
        if (UsageHistoryRepository.hasUsageAccess(context)) {
            val realHistory = withContext(Dispatchers.IO) {
                UsageHistoryRepository.loadLastSevenDays(context)
            }
            if (realHistory.isNotEmpty()) {
                weeklyUsageHistory = realHistory
                todayUsage = realHistory.last()
                todayAppUsage = todayUsage.apps
                displayedUsage = todayUsage
            }
        }
    }

    LaunchedEffect(selectedUsageDate) {
        if (selectedUsageDate == LocalDate.now()) {
            displayedUsage = todayUsage
        } else if (UsageHistoryRepository.hasUsageAccess(context)) {
            displayedUsage = DailyUsageHistory(
                selectedUsageDate,
                emptyList(),
                availability = UsageDataAvailability.Partial
            )
            withContext(Dispatchers.IO) {
                UsageHistoryRepository.loadDay(context, selectedUsageDate)
            }?.let { displayedUsage = it }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DriftBottomNavigation(DriftDestination.Home, {}, onBudgetClick, onFocusClick, onTasksClick, onInsightsClick)
        }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(22.dp))
            HomeTopBar(onProfileClick, onSettingsClick)
            Spacer(Modifier.height(25.dp))
            Text(
                text = if (userName.isBlank()) "Good morning" else "Good morning, $userName",
                style = MaterialTheme.typography.headlineSmall
            )
            Text("Here’s what deserves your attention today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            val budgetRatio = calculateBudgetUsageRatio(todayAppUsage, appBudgets)
            val focusScore = calculateFocusScore(
                screenTimeMinutes = todayUsage.totalMinutes,
                unlockCount = todayUsage.unlockCount,
                budgetUsageRatio = budgetRatio,
                focusedMinutes = focusStreakStats.todayMinutes,
                currentStreak = focusStreakStats.currentStreak,
                lateNightMinutes = todayUsage.lateNightMinutes
            ).total
            SignalCards(
                onFocusScoreClick,
                onInsightsClick,
                onUsageHistoryClick,
                weeklyUsageHistory.lastOrNull()?.totalMinutes ?: 0,
                todayUsage.unlockCount,
                focusScore
            )
            Spacer(Modifier.height(25.dp))
            UsageDaySelector(
                date = selectedUsageDate,
                canGoForward = selectedUsageDate < LocalDate.now(),
                onPrevious = { selectedUsageDate = selectedUsageDate.minusDays(1) },
                onNext = { if (selectedUsageDate < LocalDate.now()) selectedUsageDate = selectedUsageDate.plusDays(1) }
            )
            Spacer(Modifier.height(9.dp))
            HourlyUsageChart(displayedUsage)
            Spacer(Modifier.height(25.dp))
            SectionTitle("MOST USED", "View all apps", onUsageHistoryClick)
            Spacer(Modifier.height(9.dp))
            UsageBreakdown(displayedUsage.apps, appBudgets)
            Spacer(Modifier.height(25.dp))
            SectionTitle("COMING UP", "View tasks", onTasksClick)
            Spacer(Modifier.height(9.dp))
            UpcomingTasks(upcomingAssignments, onTasksClick)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HomeTopBar(onProfile: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(
            text = "Drift",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onProfile) {
                ProfileIcon()
            }
            IconButton(onClick = onSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Open settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_profile),
        contentDescription = "Open profile",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun SignalCards(
    onFocus: () -> Unit,
    onInsights: () -> Unit,
    onUsageHistory: () -> Unit,
    todayScreenTimeMinutes: Int,
    unlockCount: Int,
    focusScoreValue: Int
) {
    val screenTime = screenTimePalette(todayScreenTimeMinutes)
    val unlocks = unlockPalette(unlockCount)
    val focusScore = focusScorePalette(focusScoreValue)
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        SignalCard(
            "Screen time",
            formatGraphDuration(todayScreenTimeMinutes),
            screenTimeStatusLabel(todayScreenTimeMinutes),
            screenTime.background,
            screenTime.foreground,
            Modifier.weight(1f),
            onUsageHistory
        )
        SignalCard("Unlocks", unlockCount.toString(), metricStatusLabel(unlockStatus(unlockCount)), unlocks.background, unlocks.foreground, Modifier.weight(1f), onInsights)
        SignalCard("Focus score", focusScoreValue.toString(), metricStatusLabel(focusScoreStatus(focusScoreValue)), focusScore.background, focusScore.foreground, Modifier.weight(1f), onFocus)
    }
}

@Composable
private fun SignalCard(label: String, value: String, note: String, background: Color, ink: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(104.dp).clickable(onClick = onClick).background(background, HomeShape).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = ink, maxLines = 1)
            Text(
                "↗",
                style = MaterialTheme.typography.labelLarge,
                color = ink
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = ink)
        Spacer(Modifier.height(3.dp))
        Text(note, style = MaterialTheme.typography.labelSmall, color = ink)
    }
}

@Composable
private fun UsageDaySelector(
    date: LocalDate,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("HOURLY SCREEN TIME", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Text("‹", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                when (date) {
                    LocalDate.now() -> "Today"
                    LocalDate.now().minusDays(1) -> "Yesterday"
                    else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
            IconButton(onClick = onNext, enabled = canGoForward, modifier = Modifier.size(32.dp)) {
                Text("›", style = MaterialTheme.typography.titleLarge,
                    color = if (canGoForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ScreenTimeChart(history: List<DailyUsageHistory>) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = history.lastOrNull()
    val todayMinutes = today?.totalMinutes ?: 0
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, HomeShape).border(1.dp, MaterialTheme.colorScheme.outline, HomeShape).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column {
                Text(if (today == null) "Loading…" else formatGraphDuration(todayMinutes), style = MaterialTheme.typography.titleLarge)
                Text("today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(screenTimeStatusLabel(todayMinutes), style = MaterialTheme.typography.labelMedium, color = riskAccentColor(screenTimePalette(todayMinutes)))
        }
        Spacer(Modifier.height(18.dp))
        val maxMinutes = history.filter { it.availability != UsageDataAvailability.Unavailable }
            .maxOfOrNull { it.totalMinutes }?.coerceAtLeast(1) ?: 1
        Row(Modifier.fillMaxWidth().height(138.dp), Arrangement.spacedBy(9.dp), Alignment.Bottom) {
            history.forEachIndexed { index, day ->
                val value = day.totalMinutes
                val palette = screenTimePalette(value)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (day.availability == UsageDataAvailability.Unavailable) "—" else formatGraphDuration(value),
                        style = MaterialTheme.typography.labelSmall,
                        color = riskAccentColor(palette),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth()
                        .height(if (day.availability == UsageDataAvailability.Unavailable) 4.dp else (18f + value / maxMinutes.toFloat() * 68f).dp)
                        .clip(RoundedCornerShape(6.dp, 6.dp, 3.dp, 3.dp))
                        .background(if (day.availability == UsageDataAvailability.Unavailable) MaterialTheme.colorScheme.outlineVariant else palette.background))
                    Spacer(Modifier.height(6.dp))
                    Text(days[index], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ScreenTimeLegend()
    }
}

@Composable
private fun HourlyUsageChart(day: DailyUsageHistory) {
    val hours = day.hourlyMinutes.takeIf { it.size == 24 } ?: List(24) { 0 }
    val maximum = hours.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, HomeShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, HomeShape).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column {
                Text(formatGraphDuration(day.totalMinutes), style = MaterialTheme.typography.titleLarge)
                Text(
                    when (day.date) {
                        LocalDate.now() -> "today"
                        LocalDate.now().minusDays(1) -> "yesterday"
                        else -> day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(screenTimeStatusLabel(day.totalMinutes), style = MaterialTheme.typography.labelMedium,
                color = riskAccentColor(screenTimePalette(day.totalMinutes)))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().height(92.dp), Arrangement.spacedBy(3.dp), Alignment.Bottom) {
            hours.forEach { minutes ->
                Box(
                    Modifier.weight(1f)
                        .height(if (minutes == 0) 3.dp else (8f + minutes / maximum.toFloat() * 76f).dp)
                        .clip(RoundedCornerShape(4.dp, 4.dp, 2.dp, 2.dp))
                        .background(if (minutes == 0) MaterialTheme.colorScheme.outlineVariant else screenTimePalette(day.totalMinutes).background)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            listOf("12am", "6am", "12pm", "6pm", if (day.date == LocalDate.now()) "Now" else "11pm").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Hourly mobile screen usage", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatGraphDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours == 0 -> "${remainingMinutes}m"
        remainingMinutes == 0 -> "${hours}h"
        else -> "${hours}h${remainingMinutes}m"
    }
}

private fun metricStatusLabel(status: MetricStatus): String = when (status) {
    MetricStatus.Positive -> "Good"
    MetricStatus.Moderate -> "Moderate"
    MetricStatus.Attention -> "High"
}

private fun screenTimeStatusLabel(minutes: Int): String = when (screenTimeBand(minutes)) {
    ScreenTimeBand.Low1, ScreenTimeBand.Low2, ScreenTimeBand.Low3 -> "Low"
    ScreenTimeBand.Medium1 -> "Moderate"
    ScreenTimeBand.Medium2, ScreenTimeBand.Medium3 -> "Elevated"
    ScreenTimeBand.High -> "High"
}

@Composable
private fun ScreenTimeLegend() {
    val items = listOf(
        Triple("●", "Low <3h", riskAccentColor(screenTimePalette(179))),
        Triple("◆", "Moderate <4.5h", riskAccentColor(screenTimePalette(180))),
        Triple("■", "Elevated <6h", riskAccentColor(screenTimePalette(271))),
        Triple("▲", "High 6h+", riskAccentColor(screenTimePalette(360)))
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                rowItems.forEach { (marker, label, color) ->
            Text(
                "$marker $label",
                style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier.weight(1f)
            )
        }
            }
        }
    }
}

@Composable
private fun UsageBreakdown(
    appUsage: List<AppUsageEntry>,
    appBudgets: Map<String, Int>
) {
    val apps = appUsage.take(5)
    val whitelistedApps = setOf("WhatsApp", "Phone", "YouTube Music", "Spotify")
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, HomeShape).border(1.dp, MaterialTheme.colorScheme.outline, HomeShape).padding(16.dp)) {
        if (apps.isEmpty()) {
            Text(
                "No app usage recorded today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val longestUsage = apps.maxOfOrNull(AppUsageEntry::foregroundMillis)?.coerceAtLeast(1L) ?: 1L
        apps.forEachIndexed { index, app ->
            val limit = appBudgets.entries
                .firstOrNull { it.key.equals(app.appName, ignoreCase = true) }
                ?.value
            val whitelisted = whitelistedApps.any { it.equals(app.appName, ignoreCase = true) }
            val usedMinutes = app.foregroundMinutes
            val palette = if (limit != null && usedMinutes >= limit) {
                budgetUsagePalette(usedMinutes, limit)
            } else {
                screenTimePalette(usedMinutes)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    when {
                        whitelisted -> "${formatAppUsageDuration(app.foregroundMillis)} · Whitelisted"
                        limit != null -> "${formatAppUsageDuration(app.foregroundMillis)} / ${limit}m"
                        else -> formatAppUsageDuration(app.foregroundMillis)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = riskAccentColor(palette)
                )
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                    progress = {
                        if (whitelisted) {
                            (app.foregroundMillis / longestUsage.toFloat()).coerceIn(0f, 1f)
                        } else if (limit != null) {
                            (app.foregroundMillis / (limit * 60_000f)).coerceIn(0f, 1f)
                        } else {
                            (app.foregroundMillis / longestUsage.toFloat()).coerceIn(0f, 1f)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = palette.background,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {}
                )
            if (!whitelisted && limit != null && app.foregroundMillis >= limit * 60_000L) {
                    val overMillis = app.foregroundMillis - limit * 60_000L
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (overMillis < 60_000L) "Limit reached"
                        else "${formatAppUsageDuration(overMillis)} over",
                        style = MaterialTheme.typography.labelSmall,
                        color = riskAccentColor(palette),
                        modifier = Modifier.align(Alignment.End)
                    )
            }
            if (index < apps.lastIndex) Spacer(Modifier.height(14.dp))
        }
    }
}

private fun formatAppUsageDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> if (minutes > 0L) "${hours}h ${minutes}m" else "${hours}h"
        minutes > 0L -> if (minutes < 5L && seconds > 0L) "${minutes}m ${seconds}s" else "${minutes}m"
        else -> "${seconds}s"
    }
}

@Composable
private fun UpcomingTasks(assignments: List<Assignment>, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, HomeShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, HomeShape)
            .clickable(onClick = onClick)
    ) {
        if (assignments.isEmpty()) {
            Text(
                "No upcoming tasks. Tap to add one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            assignments.forEachIndexed { index, assignment ->
                TaskSummaryRow(
                    assignment.title,
                    assignment.dashboardDeadlineLabel(),
                    assignment.priority.uppercase()
                )
                if (index < assignments.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun Assignment.dashboardDeadlineLabel(): String {
    val date = LocalDate.parse(deadlineDate)
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    val dateLabel = when (days) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> if (days < 0) {
            "${-days} days overdue"
        } else {
            date.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
            )
        }
    }
    val timeLabel = deadlineTime?.let {
        LocalTime.parse(it).format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
        )
    }
    return timeLabel?.let { "$dateLabel · $it" } ?: dateLabel
}

@Composable
private fun PreviewUpcomingTasks(onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, HomeShape).border(1.dp, MaterialTheme.colorScheme.outline, HomeShape).clickable(onClick = onClick)) {
        TaskSummaryRow("AI report", "Tomorrow · 11:59 PM", "HIGH")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TaskSummaryRow("UI design", "In 3 days", "MEDIUM")
    }
}

@Composable
private fun TaskSummaryRow(title: String, deadline: String, priority: String) {
    val palette = riskPalette(priority)
    Row(Modifier.fillMaxWidth().padding(15.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(deadline, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(priority, style = MaterialTheme.typography.labelSmall, color = palette.foreground,
            modifier = Modifier.background(palette.background, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Preview(name = "Screen time palette", showBackground = true)
@Composable
private fun ScreenTimeChartPreview() {
    DriftTheme {
        Box(Modifier.padding(16.dp)) {
            ScreenTimeChart(PreviewWeeklyScreenTimeMinutes.mapIndexed { index, minutes ->
                DailyUsageHistory(LocalDate.now().minusDays((6 - index).toLong()), listOf(AppUsageEntry("preview", "Preview", minutes * 60_000L)))
            })
        }
    }
}

@Preview(name = "Screen time palette · dark", showBackground = true)
@Composable
private fun ScreenTimeChartDarkPreview() {
    DriftTheme(darkTheme = true) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            ScreenTimeChart(PreviewWeeklyScreenTimeMinutes.mapIndexed { index, minutes ->
                DailyUsageHistory(LocalDate.now().minusDays((6 - index).toLong()), listOf(AppUsageEntry("preview", "Preview", minutes * 60_000L)))
            })
        }
    }
}
