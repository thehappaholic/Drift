package com.happaholic.drift

val DriftManagementAreas = listOf(
    "Coursework",
    "Exam",
    "Viva/Demo",
    "Competition",
    "Project",
    "Meeting",
    "Personal responsibility",
    "Online event",
    "Other",
)

fun normalizeManagementAreas(values: Iterable<String>): Set<String> = values.mapNotNull { value ->
    when (value.trim()) {
        "Studies", "Study", "Courses", "Course", "Assignments", "Assignment", "Coursework" -> "Coursework"
        "Exams", "Exam" -> "Exam"
        "Evaluations", "Evaluation", "Demonstrations", "Demonstration", "Viva", "Demo", "Viva/Demo" -> "Viva/Demo"
        "Competitions", "Competition" -> "Competition"
        "Work projects", "Work project", "Projects", "Project" -> "Project"
        "Meetings", "Meeting" -> "Meeting"
        "Personal responsibilities", "Personal responsibility" -> "Personal responsibility"
        "Events", "Event", "Online events", "Online event" -> "Online event"
        "Other" -> "Other"
        else -> null
    }
}.toSet()
