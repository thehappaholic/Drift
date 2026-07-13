package com.example.drift

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DriftTask(
    val title: String,
    val course: String,
    val deadline: String,
    val priority: String,
    val isCompleted: Boolean = false
)

@Composable
fun TasksScreen(
    onHomeClick: () -> Unit,
    onBudgetClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf("Upcoming") }
    var showAddTask by remember { mutableStateOf(false) }
    var showCalendarOverview by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newCourse by remember { mutableStateOf("") }
    var newDeadlineDate by remember { mutableStateOf("") }
    var newDeadlineTime by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("Medium") }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    val tasks = remember {
        mutableStateListOf(
            DriftTask("AI Report", "ML", "Tomorrow, 11:59 PM", "High"),
            DriftTask("UI Design", "HCI", "3 days left", "Medium"),
            DriftTask("Testing Log", "SE", "Completed yesterday", "Low", isCompleted = true),
            DriftTask("Research Paper", "DS", "5 days left", "Medium")
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DriftBottomNavigation(DriftDestination.Tasks, onHomeClick, onBudgetClick, onFocusClick, {}, onInsightsClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "<",
                    fontSize = 28.sp,
                    modifier = Modifier.clickable { onHomeClick() }
                )

                Text(
                    text = "Tasks",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "+",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.clickable {
                        showAddTask = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                listOf("Upcoming", "Completed", "All").forEach { tab ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedTab == tab) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(7.dp)
                            )
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedTab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val visibleTasks = when (selectedTab) {
                "Upcoming" -> tasks.filterNot { it.isCompleted }
                "Completed" -> tasks.filter { it.isCompleted }
                else -> tasks
            }

            visibleTasks.forEach { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = {
                        val index = tasks.indexOf(task)
                        if (index >= 0) tasks[index] = task.copy(isCompleted = !task.isCompleted)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (visibleTasks.isEmpty()) {
                Text(
                    if (selectedTab == "Completed") "Completed tasks will appear here." else "Nothing due right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)
                )
            }

            DeadlineCrunchCard(
                onViewCalendar = { showCalendarOverview = true }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
        if (showAddTask) {
            AlertDialog(
                onDismissRequest = {
                    showAddTask = false
                },
                title = {
                    Text(
                        text = "Add Task",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Task title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newCourse,
                            onValueChange = { newCourse = it },
                            label = { Text("Course") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Deadline date *", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(7.dp))
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(year, month, day)
                                        newDeadlineDate = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
                                            .format(calendar.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).apply {
                                    datePicker.minDate = System.currentTimeMillis() - 1000
                                }.show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (newDeadlineDate.isBlank()) "Select date" else newDeadlineDate)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Deadline time (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(7.dp))
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        newDeadlineTime = SimpleDateFormat("h:mm a", Locale.getDefault())
                                            .format(calendar.time)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    DateFormat.is24HourFormat(context)
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (newDeadlineTime.isBlank()) "Select time" else newDeadlineTime)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Priority",
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Low", "Medium", "High").forEach { priority ->
                                val palette = riskPalette(priority)
                                val selected = newPriority == priority
                                Text(
                                    text = priority,
                                    modifier = Modifier
                                        .clickable {
                                            newPriority = priority
                                        }
                                        .background(
                                            color = palette.background,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .border(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) palette.foreground else Color.Transparent,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 7.dp
                                        ),
                                    color = palette.foreground,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank() && newDeadlineDate.isNotBlank()) {
                                tasks.add(
                                    DriftTask(
                                        title = newTitle,
                                        course = newCourse.ifBlank {
                                            "General"
                                        },
                                        deadline = listOf(newDeadlineDate, newDeadlineTime)
                                            .filter { it.isNotBlank() }.joinToString(" at "),
                                        priority = newPriority
                                    )
                                )

                                newTitle = ""
                                newCourse = ""
                                newDeadlineDate = ""
                                newDeadlineTime = ""
                                newPriority = "Medium"
                                showAddTask = false
                            }
                        },
                        enabled = newTitle.isNotBlank() && newDeadlineDate.isNotBlank()
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddTask = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCalendarOverview) {
            TaskCalendarDialog(
                tasks = tasks,
                onDismiss = { showCalendarOverview = false }
            )
        }
}

@Composable
private fun TaskCard(task: DriftTask, onToggleComplete: () -> Unit) {
    val palette = riskPalette(task.priority)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.course,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = task.priority,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(
                        palette.background,
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        1.dp,
                        palette.background,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                color = palette.foreground,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = task.deadline,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(12.dp))
        Text(
            if (task.isCompleted) "Move back to upcoming" else "Mark as completed",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onToggleComplete).padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun DeadlineCrunchCard(onViewCalendar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .padding(17.dp)
    ) {
        Text(
            text = "Deadline Crunch",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "2 deadlines are close together.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "View calendar  →",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onViewCalendar).padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun TaskCalendarDialog(
    tasks: List<DriftTask>,
    onDismiss: () -> Unit
) {
    val today = remember { Calendar.getInstance() }
    val year = today.get(Calendar.YEAR)
    val month = today.get(Calendar.MONTH)
    val firstDay = remember {
        Calendar.getInstance().apply { set(year, month, 1) }
    }
    val leadingBlanks = firstDay.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dueDays = tasks.associateWith { taskDueDay(it.deadline, today) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Task calendar", fontWeight = FontWeight.SemiBold)
                Text(
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(firstDay.time),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF777770),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val cells = leadingBlanks + daysInMonth
                repeat((cells + 6) / 7) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val day = week * 7 + column - leadingBlanks + 1
                            val taskForDay = dueDays.entries.firstOrNull { it.value == day }?.key
                            Box(
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day in 1..daysInMonth) {
                                    val palette = taskForDay?.let { riskPalette(it.priority) }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                when {
                                                    palette != null -> palette.background
                                                    day == today.get(Calendar.DAY_OF_MONTH) -> Color(0xFFE8E7E2)
                                                    else -> Color.Transparent
                                                },
                                                RoundedCornerShape(9.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            day.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (taskForDay != null) FontWeight.SemiBold else FontWeight.Normal,
                                            color = palette?.foreground ?: MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Upcoming deadlines", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                tasks.forEach { task ->
                    val palette = riskPalette(task.priority)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).background(palette.background, RoundedCornerShape(3.dp)))
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(task.deadline, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

private fun taskDueDay(deadline: String, today: Calendar): Int? {
    val currentDay = today.get(Calendar.DAY_OF_MONTH)
    return when {
        deadline.startsWith("Tomorrow", ignoreCase = true) -> currentDay + 1
        Regex("(\\d+) days? left", RegexOption.IGNORE_CASE).find(deadline) != null -> {
            currentDay + (Regex("(\\d+) days? left", RegexOption.IGNORE_CASE)
                .find(deadline)?.groupValues?.get(1)?.toIntOrNull() ?: 0)
        }
        Regex("(\\d+) week left", RegexOption.IGNORE_CASE).containsMatchIn(deadline) -> currentDay + 7
        else -> runCatching {
            val datePart = deadline.substringBefore(" at ")
            val date = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).parse(datePart)
            Calendar.getInstance().apply { time = date!! }.get(Calendar.DAY_OF_MONTH)
        }.getOrNull()
    }
}

