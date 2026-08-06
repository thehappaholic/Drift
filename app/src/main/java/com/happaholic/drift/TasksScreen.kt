package com.happaholic.drift

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happaholic.drift.data.assignment.Assignment
import com.happaholic.drift.data.assignment.AssignmentDraft
import com.happaholic.drift.data.assignment.AssignmentRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

@Composable
fun TasksScreen(
    managementAreas: Set<String> = emptySet(),
    onHomeClick: () -> Unit,
    onBudgetClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf("Upcoming") }
    var assignments by remember { mutableStateOf(emptyList<Assignment>()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editorAssignment by remember { mutableStateOf<Assignment?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Assignment?>(null) }
    var showCalendarOverview by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadAssignments() {
        scope.launch {
            loading = true
            AssignmentRepository.loadAssignments()
                .onSuccess { assignments = it }
                .onFailure { errorMessage = assignmentError(it) }
            loading = false
            AssignmentRepository.refreshAssignments()
                .onSuccess { assignments = it }
        }
    }

    LaunchedEffect(Unit) {
        AssignmentRepository.loadAssignments()
            .onSuccess { assignments = it }
            .onFailure { errorMessage = assignmentError(it) }
        loading = false
        AssignmentRepository.refreshAssignments()
            .onSuccess { assignments = it }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DriftBottomNavigation(
                DriftDestination.Tasks,
                onHomeClick,
                onBudgetClick,
                onFocusClick,
                {},
                onInsightsClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    DriftBackButton(onClick = onHomeClick)
                }

                Text(
                    text = "To-do",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterEnd)
                        .clickable {
                        editorAssignment = null
                        showEditor = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            AssignmentTabs(selectedTab) { selectedTab = it }
            Spacer(Modifier.height(18.dp))

            if (errorMessage != null) {
                ErrorCard(
                    message = errorMessage.orEmpty(),
                    onRetry = {
                        errorMessage = null
                        loadAssignments()
                    }
                )
                Spacer(Modifier.height(14.dp))
            }

            when {
                loading -> {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                    }
                }

                else -> {
                    val visible = when (selectedTab) {
                        "Upcoming" -> assignments.filterNot(Assignment::isCompleted)
                        "Completed" -> assignments.filter(Assignment::isCompleted)
                        else -> assignments
                    }

                    DeadlineSummaryCard(
                        assignments = assignments.filterNot(Assignment::isCompleted),
                        onViewCalendar = { showCalendarOverview = true }
                    )
                    Spacer(Modifier.height(18.dp))

                    visible.forEach { assignment ->
                        AssignmentCard(
                            assignment = assignment,
                            onToggleComplete = {
                                scope.launch {
                                    val newValue = !assignment.isCompleted
                                    AssignmentRepository.setCompleted(assignment.id, newValue)
                                        .onSuccess {
                                            assignments = assignments
                                                .map {
                                                    if (it.id == assignment.id) {
                                                        it.copy(isCompleted = newValue)
                                                    } else {
                                                        it
                                                    }
                                                }
                                                .sortedAssignments()
                                        }
                                        .onFailure { errorMessage = assignmentError(it) }
                                }
                            },
                            onEdit = {
                                editorAssignment = assignment
                                showEditor = true
                            },
                            onDelete = { deleteCandidate = assignment }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    if (visible.isEmpty()) {
                        EmptyAssignments(selectedTab) {
                            editorAssignment = null
                            showEditor = true
                        }
                    }

                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showEditor) {
        AssignmentEditorDialog(
            assignment = editorAssignment,
            saving = saving,
            onDismiss = { if (!saving) showEditor = false },
            onSave = { draft ->
                scope.launch {
                    saving = true
                    AssignmentRepository.saveAssignment(
                        draft = draft,
                        existingId = editorAssignment?.id,
                        isCompleted = editorAssignment?.isCompleted ?: false
                    )
                        .onSuccess { saved ->
                            assignments = (assignments.filterNot { it.id == saved.id } + saved)
                                .sortedAssignments()
                            showEditor = false
                            editorAssignment = null
                        }
                        .onFailure { errorMessage = assignmentError(it) }
                    saving = false
                }
            }
        )
    }

    deleteCandidate?.let { assignment ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete task?") },
            text = { Text("“${assignment.title}” will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                        scope.launch {
                            AssignmentRepository.deleteAssignment(assignment.id)
                                .onSuccess {
                                    assignments = assignments.filterNot { it.id == assignment.id }
                                }
                                .onFailure { errorMessage = assignmentError(it) }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            }
        )
    }

    if (showCalendarOverview) {
        AssignmentCalendarDialog(
            assignments = assignments.filterNot(Assignment::isCompleted),
            onDismiss = { showCalendarOverview = false }
        )
    }
}

@Composable
private fun AssignmentTabs(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Upcoming", "Completed", "All").forEach { tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(tab) }
                    .background(
                        if (selected == tab) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (selected == tab) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected == tab) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    assignment: Assignment,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = riskPalette(assignment.priority)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(assignment.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (assignment.course.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        assignment.course,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                assignment.priority,
                fontSize = 11.sp,
                color = palette.foreground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(palette.background, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            assignment.deadlineLabel(),
            fontSize = 14.sp,
            color = if (assignment.isOverdue()) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (assignment.isCompleted) "Move to upcoming" else "Mark completed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onToggleComplete).padding(vertical = 5.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Edit",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable(onClick = onEdit).padding(vertical = 5.dp)
                )
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable(onClick = onDelete).padding(vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyAssignments(selectedTab: String, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            when (selectedTab) {
                "Completed" -> "No tasks completed yet"
                "All" -> "No tasks yet."
                else -> "Nothing due right now."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (selectedTab != "Completed") {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAdd) { Text("Add your first task") }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun AssignmentEditorDialog(
    assignment: Assignment?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (AssignmentDraft) -> Unit
) {
    var title by remember(assignment?.id) { mutableStateOf(assignment?.title.orEmpty()) }
    val existingArea = assignment?.course.orEmpty()
    val areaOptions = remember(existingArea) {
        if (existingArea.isNotBlank() && existingArea !in DriftManagementAreas) {
            DriftManagementAreas + existingArea
        } else {
            DriftManagementAreas
        }
    }
    var selectedArea by remember(assignment?.id, areaOptions) {
        mutableStateOf(
            existingArea.takeIf { it in areaOptions }
                ?: if (existingArea.isNotBlank()) "Other" else areaOptions.first()
        )
    }
    var otherArea by remember(assignment?.id) {
        mutableStateOf(existingArea.takeUnless { it in DriftManagementAreas }.orEmpty())
    }
    var areaMenuExpanded by remember { mutableStateOf(false) }
    var deadlineDate by remember(assignment?.id) {
        mutableStateOf(assignment?.deadlineDate.orEmpty())
    }
    var deadlineTime by remember(assignment?.id) {
        mutableStateOf(assignment?.deadlineTime.orEmpty())
    }
    var priority by remember(assignment?.id) {
        mutableStateOf(assignment?.priority ?: "Medium")
    }
    val context = LocalContext.current
    val selectedCalendar = remember(assignment?.id) {
        Calendar.getInstance().apply {
            assignment?.deadlineDate
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let { date -> set(date.year, date.monthValue - 1, date.dayOfMonth) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (assignment == null) "Add Your To-do" else "Edit task",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(200) },
                    label = { Text("Task to be done *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Area", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { areaMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape
                    ) {
                        Text(selectedArea.ifBlank { "Select an area (optional)" })
                    }
                    DropdownMenu(
                        expanded = areaMenuExpanded,
                        onDismissRequest = { areaMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(.82f)
                    ) {
                        areaOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedArea = option
                                    if (option != "Other") otherArea = ""
                                    areaMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                if (selectedArea == "Other") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otherArea,
                        onValueChange = { otherArea = it.take(80) },
                        label = { Text("Specify area") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Deadline date *", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                selectedCalendar.set(year, month, day)
                                deadlineDate = LocalDate.of(year, month + 1, day).toString()
                            },
                            selectedCalendar.get(Calendar.YEAR),
                            selectedCalendar.get(Calendar.MONTH),
                            selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            if (assignment == null) {
                                datePicker.minDate = System.currentTimeMillis() - 1_000
                            }
                        }.show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text(
                        deadlineDate.takeIf(String::isNotBlank)
                            ?.let(::formatDate)
                            ?: "Select date"
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Deadline time (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        val initialTime = deadlineTime
                            .takeIf(String::isNotBlank)
                            ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                            ?: LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                deadlineTime = LocalTime.of(hour, minute).toString()
                            },
                            initialTime.hour,
                            initialTime.minute,
                            DateFormat.is24HourFormat(context)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text(
                        deadlineTime.takeIf(String::isNotBlank)
                            ?.let(::formatTime)
                            ?: "Select time"
                    )
                }
                if (deadlineTime.isNotBlank()) {
                    TextButton(onClick = { deadlineTime = "" }) { Text("Remove time") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Priority", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { option ->
                        val palette = riskPalette(option)
                        val selected = priority == option
                        Text(
                            option,
                            color = palette.foreground,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { priority = option }
                                .background(palette.background, RoundedCornerShape(20.dp))
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) palette.foreground else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        AssignmentDraft(
                            title = title,
                            course = if (selectedArea == "Other") otherArea.trim() else selectedArea,
                            deadlineDate = deadlineDate,
                            deadlineTime = deadlineTime.takeIf(String::isNotBlank),
                            priority = priority
                        )
                    )
                },
                enabled = title.isNotBlank() &&
                    deadlineDate.isNotBlank() &&
                    (selectedArea != "Other" || otherArea.isNotBlank()) &&
                    !saving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (assignment == null) "Add" else "Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeadlineSummaryCard(
    assignments: List<Assignment>,
    onViewCalendar: () -> Unit
) {
    val closeDeadlines = assignments.count {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(it.deadlineDate))
        days in 0..7
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(17.dp)
    ) {
        Text("Deadline overview", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            when (closeDeadlines) {
                0 -> "No deadlines in the next 7 days."
                1 -> "1 deadline is coming up this week."
                else -> "$closeDeadlines deadlines are coming up this week."
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "View calendar →",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onViewCalendar).padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun AssignmentCalendarDialog(
    assignments: List<Assignment>,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    var displayedMonth by remember { mutableStateOf(YearMonth.from(today)) }
    val firstDay = displayedMonth.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value % 7
    val daysInMonth = displayedMonth.lengthOfMonth()
    val currentMonthAssignments = assignments.filter {
        runCatching { LocalDate.parse(it.deadlineDate) }.getOrNull()?.let { date ->
            YearMonth.from(date) == displayedMonth
        } == true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("To-do calendar", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { displayedMonth = displayedMonth.minusMonths(1) }
                    ) {
                        Text("‹", fontSize = 24.sp)
                    }
                    Text(
                        displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = { displayedMonth = displayedMonth.plusMonths(1) }
                    ) {
                        Text("›", fontSize = 24.sp)
                    }
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                        Text(
                            label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val cellCount = leadingBlanks + daysInMonth
                repeat((cellCount + 6) / 7) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val day = week * 7 + column - leadingBlanks + 1
                            val dayAssignments = currentMonthAssignments.filter {
                                LocalDate.parse(it.deadlineDate).dayOfMonth == day
                            }
                            Box(
                                Modifier.weight(1f).height(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day in 1..daysInMonth) {
                                    val palette = dayAssignments.firstOrNull()
                                        ?.let { riskPalette(it.priority) }
                                    Box(
                                        Modifier
                                            .size(32.dp)
                                            .background(
                                                palette?.background
                                                    ?: if (
                                                        displayedMonth == YearMonth.from(today) &&
                                                        day == today.dayOfMonth
                                                    ) {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                RoundedCornerShape(9.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            day.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (dayAssignments.isNotEmpty()) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            },
                                            color = palette?.foreground
                                                ?: MaterialTheme.colorScheme.onSurface
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
                if (assignments.isEmpty()) {
                    Text(
                        "No upcoming tasks.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    assignments.sortedAssignments().take(8).forEach { assignment ->
                        val palette = riskPalette(assignment.priority)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(palette.background, RoundedCornerShape(3.dp))
                            )
                            Spacer(Modifier.size(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    assignment.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    assignment.deadlineLabel(),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

private fun List<Assignment>.sortedAssignments(): List<Assignment> =
    sortedWith(
        compareBy<Assignment> { it.isCompleted }
            .thenBy { it.deadlineDate }
            .thenBy { it.deadlineTime ?: "23:59" }
    )

private fun Assignment.deadlineLabel(): String {
    val date = LocalDate.parse(deadlineDate)
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    val dateText = when (days) {
        -1L -> "Yesterday"
        0L -> "Today"
        1L -> "Tomorrow"
        else -> if (days < -1) {
            "${-days} days overdue"
        } else {
            formatDate(deadlineDate)
        }
    }
    return deadlineTime?.let { "$dateText · ${formatTime(it)}" } ?: dateText
}

private fun Assignment.isOverdue(): Boolean {
    if (isCompleted) return false
    val date = LocalDate.parse(deadlineDate)
    if (date.isBefore(LocalDate.now())) return true
    return date == LocalDate.now() &&
        deadlineTime?.let { LocalTime.parse(it).isBefore(LocalTime.now()) } == true
}

private fun formatDate(value: String): String =
    LocalDate.parse(value).format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    )

private fun formatTime(value: String): String =
    LocalTime.parse(value).format(
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    )

private fun assignmentError(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        "assignments" in message && ("schema cache" in message || "PGRST205" in message) ->
            "Tasks are temporarily unavailable. Please try again shortly."
        "session" in message.lowercase() ->
            "Your session expired. Please log in again."
        "network" in message.lowercase() || "unable to resolve" in message.lowercase() ->
            "We couldn’t reach Drift. Check your connection and try again."
        else -> "We couldn’t save your tasks. Please try again."
    }
}
