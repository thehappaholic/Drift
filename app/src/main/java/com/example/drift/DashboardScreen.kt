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
private val SampleWeeklyScreenTimeMinutes = listOf(30, 90, 150, 210, 270, 330, 360)

@Composable
fun DashboardScreen(
    userName: String = "",
    appBudgets: Map<String, Int> = mapOf(
        "Instagram" to 45,
        "YouTube" to 40,
        "Chrome" to 60
    ),
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
    var weeklyScreenTimeMinutes by remember {
        mutableStateOf(SampleWeeklyScreenTimeMinutes)
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
                weeklyScreenTimeMinutes = realHistory.map(DailyUsageHistory::totalMinutes)
            }
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
            SignalCards(
                onFocusScoreClick,
                onInsightsClick,
                onUsageHistoryClick,
                weeklyScreenTimeMinutes.lastOrNull() ?: 0
            )
            Spacer(Modifier.height(25.dp))
            SectionTitle("SCREEN TIME", "View history", onUsageHistoryClick)
            Spacer(Modifier.height(9.dp))
            ScreenTimeChart(weeklyScreenTimeMinutes)
            Spacer(Modifier.height(25.dp))
            SectionTitle("MOST USED", "View all apps", onUsageHistoryClick)
            Spacer(Modifier.height(9.dp))
            UsageBreakdown(appBudgets)
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
    todayScreenTimeMinutes: Int
) {
    val screenTime = screenTimePalette(todayScreenTimeMinutes)
    val unlocks = unlockPalette(46)
    val focusScore = focusScorePalette(78)
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
        SignalCard("Unlocks", "46", "Moderate · −8", unlocks.background, unlocks.foreground, Modifier.weight(1f), onInsights)
        SignalCard("Focus score", "78", "Moderate · +5", focusScore.background, focusScore.foreground, Modifier.weight(1f), onFocus)
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
private fun ScreenTimeChart(minutes: List<Int>) {
    // Sample week intentionally covers every semantic screen-time threshold.
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val todayMinutes = minutes.lastOrNull() ?: 0
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, HomeShape).border(1.dp, MaterialTheme.colorScheme.outline, HomeShape).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column {
                Text(formatGraphDuration(todayMinutes), style = MaterialTheme.typography.titleLarge)
                Text("today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(screenTimeStatusLabel(todayMinutes), style = MaterialTheme.typography.labelMedium, color = screenTimePalette(todayMinutes).foreground)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().height(138.dp), Arrangement.spacedBy(9.dp), Alignment.Bottom) {
            minutes.forEachIndexed { index, value ->
                val palette = screenTimePalette(value)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatGraphDuration(value),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.foreground,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().height((value / 4.5f).dp).clip(RoundedCornerShape(6.dp, 6.dp, 3.dp, 3.dp))
                            .background(palette.background)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(days[index], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ScreenTimeLegend()
    }
}

private fun formatGraphDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (remainingMinutes == 0) "${hours}h" else "${hours}h${remainingMinutes}"
}

private fun screenTimeStatusLabel(minutes: Int): String = when (screenTimeBand(minutes)) {
    ScreenTimeBand.Low1, ScreenTimeBand.Low2, ScreenTimeBand.Low3 -> "Low"
    ScreenTimeBand.Medium1, ScreenTimeBand.Medium2, ScreenTimeBand.Medium3 -> "Medium"
    ScreenTimeBand.High -> "High"
}

@Composable
private fun ScreenTimeLegend() {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        listOf(
            Triple("●", "Low <3h", screenTimePalette(179).foreground),
            Triple("◆", "Medium 3–5h59", screenTimePalette(240).foreground),
            Triple("▲", "High 6h+", screenTimePalette(360).foreground)
        ).forEach { (marker, label, color) ->
            Text(
                "$marker $label",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun UsageBreakdown(appBudgets: Map<String, Int>) {
    val apps = listOf("Instagram" to 58, "YouTube" to 42, "Chrome" to 27, "WhatsApp" to 18)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, HomeShape).border(1.dp, MaterialTheme.colorScheme.outline, HomeShape).padding(16.dp)) {
        apps.forEachIndexed { index, (app, minutes) ->
            val limit = appBudgets[app]
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(app, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (limit == null) "Whitelisted" else "$minutes / ${limit}m",
                    style = MaterialTheme.typography.labelLarge,
                    color = limit?.let { budgetUsagePalette(minutes, it).foreground }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (limit != null) {
                val palette = budgetUsagePalette(minutes, limit)
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    progress = { (minutes / limit.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = palette.foreground,
                    trackColor = palette.background,
                    drawStopIndicator = {}
                )
                if (minutes >= limit) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (minutes == limit) "Limit reached" else "${minutes - limit} min over",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.foreground,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            if (index < apps.lastIndex) Spacer(Modifier.height(14.dp))
        }
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
                "No upcoming assignments. Tap to add one.",
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
            ScreenTimeChart(SampleWeeklyScreenTimeMinutes)
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
            ScreenTimeChart(SampleWeeklyScreenTimeMinutes)
        }
    }
}
