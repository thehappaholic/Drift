package com.happaholic.drift

import kotlin.math.roundToInt

data class FocusScoreBreakdown(
    val screenTime: Int,
    val unlocks: Int,
    val budgetAdherence: Int,
    val focusHabit: Int,
    val lateNight: Int
) {
    val total: Int get() = screenTime + unlocks + budgetAdherence + focusHabit + lateNight
}

fun calculateFocusScore(
    screenTimeMinutes: Int,
    unlockCount: Int,
    budgetUsageRatio: Float,
    focusedMinutes: Int,
    currentStreak: Int,
    lateNightMinutes: Int
): FocusScoreBreakdown {
    val screenTime = when {
        screenTimeMinutes <= 180 -> 30f
        screenTimeMinutes <= 360 -> 30f - (screenTimeMinutes - 180) / 180f * 20f
        else -> (10f - (screenTimeMinutes - 360) / 240f * 10f).coerceAtLeast(0f)
    }
    val unlocks = when {
        unlockCount <= 20 -> 20f
        unlockCount <= 80 -> 20f - (unlockCount - 20) / 60f * 15f
        else -> (5f - (unlockCount - 80) / 70f * 5f).coerceAtLeast(0f)
    }
    val budget = when {
        budgetUsageRatio <= .75f -> 20f
        budgetUsageRatio <= 1f -> 20f - (budgetUsageRatio - .75f) / .25f * 8f
        else -> (12f - (budgetUsageRatio - 1f) * 12f).coerceAtLeast(0f)
    }
    val focus =
        (focusedMinutes / DAILY_FOCUS_GOAL_MINUTES.toFloat()).coerceIn(0f, 1f) * 15f +
            (currentStreak / 7f).coerceIn(0f, 1f) * 5f
    val lateNight = (10f - lateNightMinutes / 120f * 10f).coerceAtLeast(0f)
    return FocusScoreBreakdown(
        screenTime.roundToInt(),
        unlocks.roundToInt(),
        budget.roundToInt(),
        focus.roundToInt(),
        lateNight.roundToInt()
    )
}

fun calculateBudgetUsageRatio(
    apps: List<AppUsageEntry>,
    appBudgets: Map<String, Int>
): Float {
    if (appBudgets.isEmpty()) return 0f
    return appBudgets.map { (name, limit) ->
        val usedMillis = apps.firstOrNull { it.appName.equals(name, ignoreCase = true) }
            ?.foregroundMillis ?: 0L
        if (limit <= 0) 0f else usedMillis / (limit * 60_000f)
    }.average().toFloat()
}
