package com.example.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.drift.ui.theme.LocalScreenTimeColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val FocusHubShape = RoundedCornerShape(16.dp)

@Composable
fun FocusScoreScreen(
    stats: FocusStreakStats,
    onBack: () -> Unit,
    onBudgetClick: () -> Unit = {},
    onFocusTimerClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {}
) {
    val positiveColors = LocalScreenTimeColors.current
    val dailyProgress =
        (stats.todayMinutes / DAILY_FOCUS_GOAL_MINUTES.toFloat()).coerceIn(0f, 1f)
    val goalReached = stats.todayMinutes >= DAILY_FOCUS_GOAL_MINUTES
    var factorsExpanded by remember { mutableStateOf(false) }
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
                    value = "78",
                    supporting = "/100",
                    progress = .78f,
                    modifier = Modifier.weight(1f)
                )
                FocusMetricCircle(
                    label = "CURRENT STREAK",
                    value = stats.currentStreak.toString(),
                    supporting = if (stats.currentStreak == 1) "day" else "days",
                    progress = (stats.currentStreak / 7f).coerceIn(0f, 1f),
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
                        Text("Today's focus goal", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (goalReached) "Goal complete — your streak is safe"
                            else "${DAILY_FOCUS_GOAL_MINUTES - stats.todayMinutes} minutes remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (goalReached) positiveColors.onLow
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${stats.todayMinutes}/$DAILY_FOCUS_GOAL_MINUTES min",
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
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    stats.recentDays.forEach { day ->
                        val isToday = day.date == LocalDate.now()
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
                                    .background(
                                        when {
                                            day.goalReached -> positiveColors.low
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        CircleShape
                                    )
                                    .then(
                                        if (isToday) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (day.goalReached) "✓" else day.date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.goalReached) positiveColors.onLowStrong
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                            if (factorsExpanded) "Tap to show less"
                            else "4 signals · 2 need attention",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (factorsExpanded) "↑" else "Tap to know more  ↗",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (factorsExpanded) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("Late-night usage", "High")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("Unlock frequency", "Moderate")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("App budget adherence", "Good")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScoreFactor("Deadline pressure", "High")
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                CompactFocusStat("Longest", "${stats.longestStreak} days", Modifier.weight(1f))
                CompactFocusStat(
                    "This week",
                    "${stats.recentDays.sumOf(FocusDay::focusedMinutes)} min",
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

@Composable
private fun FocusMetricCircle(
    label: String,
    value: String,
    supporting: String,
    progress: Float,
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
                color = MaterialTheme.colorScheme.primary,
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
