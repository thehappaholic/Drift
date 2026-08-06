package com.happaholic.drift

import android.content.SharedPreferences
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val DAILY_FOCUS_GOAL_MINUTES = 40

data class FocusDay(
    val date: LocalDate,
    val focusedMinutes: Int
) {
    val goalReached: Boolean get() = focusedMinutes >= DAILY_FOCUS_GOAL_MINUTES
}

data class FocusStreakStats(
    val todayMinutes: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val recentDays: List<FocusDay>
)

fun focusStreakEndingOn(days: List<FocusDay>, date: LocalDate): Int {
    val byDate = days.associateBy(FocusDay::date)
    var cursor = date
    var streak = 0
    while (byDate[cursor]?.goalReached == true) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

class FocusStreakStore(private val preferences: SharedPreferences) {
    fun minutesFor(date: LocalDate): Int = readHistory()[date] ?: 0

    fun recordCompletedFocus(focusedSeconds: Int, today: LocalDate = LocalDate.now()): FocusStreakStats {
        val minutes = (focusedSeconds / 60).coerceAtLeast(0)
        if (minutes > 0) {
            val history = readHistory().toMutableMap()
            history[today] = (history[today] ?: 0) + minutes
            writeHistory(history)
        }
        return load(today)
    }

    fun load(today: LocalDate = LocalDate.now()): FocusStreakStats {
        val history = readHistory()
        val current = calculateCurrentStreak(history, today)
        val longest = maxOf(preferences.getInt(KEY_LONGEST_STREAK, 0), calculateLongestStreak(history))
        if (longest != preferences.getInt(KEY_LONGEST_STREAK, 0)) {
            preferences.edit().putInt(KEY_LONGEST_STREAK, longest).apply()
        }
        return FocusStreakStats(
            todayMinutes = history[today] ?: 0,
            currentStreak = current,
            longestStreak = longest,
            recentDays = (364L downTo 0L).map { offset ->
                val date = today.minusDays(offset)
                FocusDay(date, history[date] ?: 0)
            }
        )
    }

    private fun readHistory(): Map<LocalDate, Int> =
        preferences.getString(KEY_HISTORY, "")
            .orEmpty()
            .split(",")
            .mapNotNull { entry ->
                val separator = entry.lastIndexOf(':')
                if (separator <= 0) return@mapNotNull null
                val date = runCatching { LocalDate.parse(entry.substring(0, separator)) }.getOrNull()
                val minutes = entry.substring(separator + 1).toIntOrNull()
                if (date == null || minutes == null) null else date to minutes
            }
            .toMap()

    private fun writeHistory(history: Map<LocalDate, Int>) {
        val cutoff = LocalDate.now().minusDays(365)
        val encoded = history
            .filterKeys { !it.isBefore(cutoff) }
            .toSortedMap()
            .entries
            .joinToString(",") { (date, minutes) -> "$date:$minutes" }
        preferences.edit().putString(KEY_HISTORY, encoded).apply()
    }

    private fun calculateCurrentStreak(history: Map<LocalDate, Int>, today: LocalDate): Int {
        var date = if ((history[today] ?: 0) >= DAILY_FOCUS_GOAL_MINUTES) {
            today
        } else {
            today.minusDays(1)
        }
        var streak = 0
        while ((history[date] ?: 0) >= DAILY_FOCUS_GOAL_MINUTES) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    private fun calculateLongestStreak(history: Map<LocalDate, Int>): Int {
        val completed = history.filterValues { it >= DAILY_FOCUS_GOAL_MINUTES }.keys.sorted()
        var longest = 0
        var running = 0
        var previous: LocalDate? = null
        completed.forEach { date ->
            running = if (previous != null && ChronoUnit.DAYS.between(previous, date) == 1L) {
                running + 1
            } else {
                1
            }
            longest = maxOf(longest, running)
            previous = date
        }
        return longest
    }

    private companion object {
        const val KEY_HISTORY = "focus_minutes_by_date"
        const val KEY_LONGEST_STREAK = "longest_focus_streak"
    }
}
