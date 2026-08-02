package com.example.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.drift.ui.theme.LocalScreenTimeColors
import com.example.drift.ui.theme.DriftLilac
import com.example.drift.ui.theme.Ink
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FocusHubShape = RoundedCornerShape(16.dp)

@Composable
fun FocusScoreScreen(
    stats: FocusStreakStats,
    appBudgets: Map<String, Int> = emptyMap(),
    onBack: () -> Unit,
    onBudgetClick: () -> Unit = {},
    onFocusTimerClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val positiveColors = LocalScreenTimeColors.current
    var selectedDate by remember(stats.recentDays) { mutableStateOf(LocalDate.now()) }
    var patternMonth by remember { mutableStateOf(YearMonth.now()) }
    val selectedFocusDay = stats.recentDays.firstOrNull { it.date == selectedDate }
        ?: FocusDay(selectedDate, 0)
    val dailyProgress =
        (selectedFocusDay.focusedMinutes / DAILY_FOCUS_GOAL_MINUTES.toFloat()).coerceIn(0f, 1f)
    val goalReached = selectedFocusDay.goalReached
    var factorsExpanded by remember { mutableStateOf(false) }
    var score by remember {
        mutableStateOf(
            calculateFocusScore(0, 0, 0f, stats.todayMinutes, stats.currentStreak, 0)
        )
    }
    var scoreAvailable by remember { mutableStateOf(false) }
    var usageHistory by remember { mutableStateOf<List<DailyUsageHistory>>(emptyList()) }
    LaunchedEffect(stats, appBudgets) {
        if (UsageHistoryRepository.hasUsageAccess(context)) {
            usageHistory = withContext(Dispatchers.IO) {
                UsageHistoryRepository.loadRecentDays(context, 365)
            }
        }
    }
    LaunchedEffect(selectedDate, usageHistory, stats, appBudgets) {
        val selectedUsage = usageHistory.firstOrNull { it.date == selectedDate }
        scoreAvailable = selectedUsage != null && selectedUsage.availability != UsageDataAvailability.Unavailable
        score = if (selectedUsage != null && selectedUsage.availability != UsageDataAvailability.Unavailable) {
            calculateFocusScore(
                selectedUsage.totalMinutes,
                selectedUsage.unlockCount,
                calculateBudgetUsageRatio(selectedUsage.apps, appBudgets),
                selectedFocusDay.focusedMinutes,
                focusStreakEndingOn(stats.recentDays, selectedDate),
                selectedUsage.lateNightMinutes
            )
        } else calculateFocusScore(0, 0, 0f, selectedFocusDay.focusedMinutes, 0, 0)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DriftBottomNavigation(
                DriftDestination.Focus,
                onBack,
                onBudgetClick,
                onFocusTimerClick,
                onTasksClick,
                onInsightsClick
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DriftBackButton(onClick = onBack)
                Column(Modifier.padding(start = 18.dp)) {
                    Text(
                        "Focus",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Your focus health at a glance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                FocusMetricCircle(
                    label = "FOCUS SCORE",
                    value = if (scoreAvailable) score.total.toString() else "—",
                    supporting = "/100",
                    progress = if (scoreAvailable) score.total / 100f else 0f,
                    progressColor = focusScorePalette(score.total).foreground,
                    modifier = Modifier.weight(1f)
                )
                FocusMetricCircle(
                    label = "CURRENT STREAK",
                    value = stats.currentStreak.toString(),
                    supporting = if (stats.currentStreak == 1) "day" else "days",
                    progress = (stats.currentStreak / 7f).coerceIn(0f, 1f),
                    progressColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, FocusHubShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, FocusHubShape)
                    .padding(15.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text(
                            if (selectedDate == LocalDate.now()) "Today's focus goal"
                            else selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (goalReached) "Goal complete — your streak is safe"
                            else if (selectedDate == LocalDate.now()) "${DAILY_FOCUS_GOAL_MINUTES - selectedFocusDay.focusedMinutes} minutes remaining"
                            else "${selectedFocusDay.focusedMinutes} focused minutes recorded",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (goalReached) positiveColors.onLow
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${selectedFocusDay.focusedMinutes}/$DAILY_FOCUS_GOAL_MINUTES min",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (goalReached) positiveColors.onLow
                        else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { dailyProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (goalReached) positiveColors.onLow else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {}
                )

                Spacer(Modifier.height(14.dp))
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    reverseLayout = true
                ) {
                    items(stats.recentDays.asReversed(), key = { it.date }) { day ->
                        val isSelected = day.date == selectedDate
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .size(31.dp)
                                    .clickable { selectedDate = day.date }
                                    .background(
                                        when {
                                            day.goalReached -> positiveColors.low
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        CircleShape
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day.goalReached) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_completed_check),
                                        contentDescription = "Focus goal completed",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        day.date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, FocusHubShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, FocusHubShape)
                    .clickable { factorsExpanded = !factorsExpanded }
                    .padding(horizontal = 15.dp, vertical = 13.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("What affects your score", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (factorsExpanded) "5 score signals shown below"
                            else "5 real signals shape this score",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .background(DriftLilac, CircleShape)
                            .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            if (factorsExpanded) "View less" else "View more",
                            style = MaterialTheme.typography.labelMedium,
                            color = Ink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_double_chevron_down),
                            contentDescription = if (factorsExpanded) "Collapse score factors" else "Expand score factors",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp).rotate(if (factorsExpanded) 180f else 0f)
                        )
                    }
                }

                if (factorsExpanded) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("Screen-time balance", componentLabel(score.screenTime, 30))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("Unlock frequency", componentLabel(score.unlocks, 20))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("App budget adherence", componentLabel(score.budgetAdherence, 20))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("Focus habit", componentLabel(score.focusHabit, 20))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("Late-night usage", componentLabel(score.lateNight, 10))
                }
            }

            Spacer(Modifier.height(14.dp))
            FocusPatternCalendar(
                focusDays = stats.recentDays,
                usageHistory = usageHistory,
                appBudgets = appBudgets,
                month = patternMonth,
                onPreviousMonth = { patternMonth = patternMonth.minusMonths(1) },
                onNextMonth = {
                    if (patternMonth < YearMonth.now()) patternMonth = patternMonth.plusMonths(1)
                },
                selectedDate = selectedDate,
                onSelectDate = { selectedDate = it }
            )

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                CompactFocusStat("Longest", "${stats.longestStreak} days", Modifier.weight(1f))
                CompactFocusStat(
                    "This week",
                    "${stats.recentDays.takeLast(7).sumOf(FocusDay::focusedMinutes)} min",
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onFocusTimerClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (goalReached) "Start another focus session" else "Start 40-minute focus",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun focusStreakEndingOn(days: List<FocusDay>, date: LocalDate): Int {
    val byDate = days.associateBy(FocusDay::date)
    var cursor = date
    var streak = 0
    while (byDate[cursor]?.goalReached == true) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

@Composable
private fun FocusPatternCalendar(
    focusDays: List<FocusDay>,
    usageHistory: List<DailyUsageHistory>,
    appBudgets: Map<String, Int>,
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val usageByDate = usageHistory.associateBy(DailyUsageHistory::date)
    val focusByDate = focusDays.associateBy(FocusDay::date)
    val firstDay = month.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value % 7
    val calendarCells: List<LocalDate?> = buildList {
        repeat(leadingBlanks) { add(null) }
        (1..month.lengthOfMonth()).forEach { add(month.atDay(it)) }
        while (size % 7 != 0) add(null)
    }
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, FocusHubShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, FocusHubShape)
            .padding(15.dp)
    ) {
        Text("Focus pattern", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
                Text("‹", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onNextMonth, enabled = month < YearMonth.now(), modifier = Modifier.size(32.dp)) {
                Text("›", style = MaterialTheme.typography.titleLarge,
                    color = if (month < YearMonth.now()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(7.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(7.dp))
        calendarCells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(7.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                        return@forEach
                    }
                    val day = focusByDate[date] ?: FocusDay(date, 0)
                    val usage = usageByDate[date]
                    val isFuture = date.isAfter(LocalDate.now())
                    val hasData = usage != null && usage.availability != UsageDataAvailability.Unavailable
                    val dayScore = if (hasData) calculateFocusScore(
                        usage!!.totalMinutes,
                        usage.unlockCount,
                        calculateBudgetUsageRatio(usage.apps, appBudgets),
                        day.focusedMinutes,
                        focusStreakEndingOn(focusDays, date),
                        usage.lateNightMinutes
                    ).total else null
                    val palette: RiskPalette? = if (dayScore != null) focusScorePalette(dayScore) else null
                    val accessibilityStatus = when {
                        isFuture -> "future date"
                        dayScore == null -> "not collected"
                        focusScoreStatus(dayScore) == MetricStatus.Positive -> "strong focus"
                        focusScoreStatus(dayScore) == MetricStatus.Moderate -> "moderate focus"
                        else -> "needs attention"
                    }
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .background(palette?.background ?: MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9.dp))
                            .then(if (date == selectedDate) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)) else Modifier)
                            .clickable(enabled = !isFuture) { onSelectDate(date) }
                            .semantics {
                                contentDescription = "${date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))}, $accessibilityStatus"
                            },
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }
            Spacer(Modifier.height(7.dp))
        }
        Text("Green strong  •  Yellow moderate  •  Red needs attention  •  Grey not collected",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun componentLabel(points: Int, maximum: Int): String = when {
    points >= maximum * .75f -> "Good"
    points >= maximum * .4f -> "Moderate"
    else -> "High"
}

@Composable
private fun FocusMetricCircle(
    label: String,
    value: String,
    supporting: String,
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, FocusHubShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, FocusHubShape)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 8.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleLarge)
                Text(supporting, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompactFocusStat(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ScoreFactor(label: String, value: String) {
    val palette = riskPalette(value)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = palette.foreground,
            modifier = Modifier
                .background(palette.background, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
