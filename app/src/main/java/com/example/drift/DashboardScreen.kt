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
import androidx.compose.ui.text.font.FontWeight
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

private val HomeShape = RoundedCornerShape(14.dp)

@Composable
fun DashboardScreen(
    userName: String = "",
    onFocusClick: () -> Unit = {},
    onFocusScoreClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var upcomingAssignments by remember { mutableStateOf(emptyList<Assignment>()) }

    LaunchedEffect(Unit) {
        AssignmentRepository.loadAssignments().onSuccess { assignments ->
            upcomingAssignments = assignments.filterNot(Assignment::isCompleted).take(3)
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
            SignalCards(onFocusScoreClick, onInsightsClick)
            Spacer(Modifier.height(25.dp))
            SectionTitle("SCREEN TIME", "View insights", onInsightsClick)
            Spacer(Modifier.height(9.dp))
            ScreenTimeChart()
            Spacer(Modifier.height(25.dp))
            SectionTitle("MOST USED", "2h 15m total", onInsightsClick)
            Spacer(Modifier.height(9.dp))
            UsageBreakdown()
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
        Text("Drift", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onProfile) {
                ProfileIcon()
            }
            IconButton(onClick = onSettings) {
                Text("⚙", fontSize = 23.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ProfileIcon() {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(23.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension * 0.19f,
            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.31f),
            style = Stroke(strokeWidth)
        )
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.47f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.50f),
            style = Stroke(strokeWidth)
        )
    }
}

@Composable
private fun SignalCards(onFocus: () -> Unit, onInsights: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        SignalCard("Screen time", "2h 15m", "15m over", Color(0xFFFFE7B8), Color(0xFF80510A), Modifier.weight(1f), onInsights)
        SignalCard("Unlocks", "46", "−8 today", Color(0xFFD5F1EA), Color(0xFF23675D), Modifier.weight(1f), onInsights)
        SignalCard("Focus score", "78", "+5 this week", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f), onFocus)
    }
}

@Composable
private fun SignalCard(label: String, value: String, note: String, background: Color, ink: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).background(background, HomeShape).padding(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ink, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = ink)
        Spacer(Modifier.height(3.dp))
        Text(note, style = MaterialTheme.typography.labelSmall, color = ink)
    }
}

@Composable
private fun ScreenTimeChart() {
    val minutes = listOf(168, 142, 155, 126, 135, 119, 135)
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, HomeShape).border(1.dp, MaterialTheme.colorScheme.outline, HomeShape).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column {
                Text("2h 15m", style = MaterialTheme.typography.titleLarge)
                Text("today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("↓ 33m from Monday", style = MaterialTheme.typography.labelMedium, color = Color(0xFF23675D))
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().height(112.dp), Arrangement.spacedBy(9.dp), Alignment.Bottom) {
            minutes.forEachIndexed { index, value ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().height((value / 2.1f).dp).clip(RoundedCornerShape(6.dp, 6.dp, 3.dp, 3.dp))
                            .background(if (index == minutes.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(days[index], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UsageBreakdown() {
    val apps = listOf("Instagram" to 58, "YouTube" to 42, "Chrome" to 27, "WhatsApp" to 18)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, HomeShape).border(1.dp, MaterialTheme.colorScheme.outline, HomeShape).padding(16.dp)) {
        apps.forEachIndexed { index, (app, minutes) ->
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(app, style = MaterialTheme.typography.bodyMedium)
                Text("${minutes}m", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { minutes / 60f },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
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
