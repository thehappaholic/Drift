package com.example.drift

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class FocusSessionInterval(val startMillis: Long, val endMillis: Long)

class FocusSessionIntervalStore(context: Context) {
    private val preferences = context.getSharedPreferences("focus_session_intervals", Context.MODE_PRIVATE)

    fun start(nowMillis: Long = System.currentTimeMillis()) {
        if (!preferences.contains(KEY_ACTIVE_START)) {
            preferences.edit().putLong(KEY_ACTIVE_START, nowMillis).apply()
        }
    }

    fun stop(nowMillis: Long = System.currentTimeMillis()) {
        if (!preferences.contains(KEY_ACTIVE_START)) return
        val start = preferences.getLong(KEY_ACTIVE_START, nowMillis)
        val intervals = readAll().toMutableList()
        if (nowMillis > start) intervals += FocusSessionInterval(start, nowMillis)
        writeAll(intervals, nowMillis)
        preferences.edit().remove(KEY_ACTIVE_START).apply()
    }

    fun intervalsFor(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<FocusSessionInterval> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return readAll().filter { it.endMillis > start && it.startMillis < end }
    }

    fun finalizeCompletedSession(
        sessionStartMillis: Long,
        completedAtMillis: Long = System.currentTimeMillis(),
        focusedSeconds: Int
    ) {
        stop(completedAtMillis)
        val targetMillis = focusedSeconds.coerceAtLeast(0) * 1_000L
        if (targetMillis == 0L || completedAtMillis <= sessionStartMillis) return
        val all = readAll().toMutableList()
        val covered = mergeIntervals(all.filter {
            it.endMillis > sessionStartMillis && it.startMillis < completedAtMillis
        }.map {
            FocusSessionInterval(
                maxOf(it.startMillis, sessionStartMillis),
                minOf(it.endMillis, completedAtMillis)
            )
        })
        var missing = targetMillis - covered.sumOf { it.endMillis - it.startMillis }
        if (missing > 0L) {
            val gaps = buildList {
                var cursor = sessionStartMillis
                covered.forEach { interval ->
                    if (interval.startMillis > cursor) add(FocusSessionInterval(cursor, interval.startMillis))
                    cursor = maxOf(cursor, interval.endMillis)
                }
                if (cursor < completedAtMillis) add(FocusSessionInterval(cursor, completedAtMillis))
            }
            gaps.asReversed().forEach { gap ->
                if (missing <= 0L) return@forEach
                val amount = minOf(missing, gap.endMillis - gap.startMillis)
                all += FocusSessionInterval(gap.endMillis - amount, gap.endMillis)
                missing -= amount
            }
        }
        writeAll(all, completedAtMillis)
    }

    private fun readAll(): List<FocusSessionInterval> {
        val encoded = preferences.getString(KEY_INTERVALS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(encoded)
            (0 until array.length()).mapNotNull { index ->
                val item = array.getJSONObject(index)
                val start = item.optLong("start")
                val end = item.optLong("end")
                if (end > start) FocusSessionInterval(start, end) else null
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(intervals: List<FocusSessionInterval>, nowMillis: Long) {
        val cutoff = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate().minusDays(365)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val array = JSONArray()
        mergeIntervals(intervals.filter { it.endMillis >= cutoff }).forEach {
            array.put(JSONObject().put("start", it.startMillis).put("end", it.endMillis))
        }
        preferences.edit().putString(KEY_INTERVALS, array.toString()).apply()
    }

    private fun mergeIntervals(intervals: List<FocusSessionInterval>): List<FocusSessionInterval> {
        val sorted = intervals.sortedBy(FocusSessionInterval::startMillis)
        if (sorted.isEmpty()) return emptyList()
        val merged = mutableListOf(sorted.first())
        sorted.drop(1).forEach { next ->
            val last = merged.last()
            if (next.startMillis <= last.endMillis) {
                merged[merged.lastIndex] = FocusSessionInterval(last.startMillis, maxOf(last.endMillis, next.endMillis))
            } else merged += next
        }
        return merged
    }

    private companion object {
        const val KEY_ACTIVE_START = "active_start"
        const val KEY_INTERVALS = "verified_intervals"
    }
}

fun calculateAdjustedForegroundDurations(
    events: List<UsageTimelineEvent>,
    rangeStart: Long,
    rangeEnd: Long,
    driftPackageName: String,
    verifiedFocusIntervals: List<FocusSessionInterval>
): Map<String, Long> {
    val raw = calculateForegroundDurations(events, rangeStart, rangeEnd).toMutableMap()
    val excludedDriftMillis = verifiedFocusIntervals.sumOf { interval ->
        val start = maxOf(rangeStart, interval.startMillis)
        val end = minOf(rangeEnd, interval.endMillis)
        if (end <= start) 0L else {
            calculateForegroundDurations(events, start, end)[driftPackageName] ?: 0L
        }
    }.coerceAtMost(raw[driftPackageName] ?: 0L)
    if (excludedDriftMillis > 0L) {
        val remaining = (raw[driftPackageName] ?: 0L) - excludedDriftMillis
        if (remaining > 0L) raw[driftPackageName] = remaining else raw.remove(driftPackageName)
    }
    return raw
}
