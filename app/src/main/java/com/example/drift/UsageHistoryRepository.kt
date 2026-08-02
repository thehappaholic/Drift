package com.example.drift

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val foregroundMillis: Long,
    val hourlyMillis: List<Long> = emptyList()
) {
    val foregroundMinutes: Int get() = (foregroundMillis / 60_000L).toInt()
}

data class DailyUsageHistory(
    val date: LocalDate,
    val apps: List<AppUsageEntry>,
    val unlockCount: Int = 0,
    val lateNightMinutes: Int = 0,
    val hourlyMinutes: List<Int> = emptyList(),
    val intentionalFocusMinutes: Int = 0,
    val intentionalFocusHourlyMinutes: List<Int> = emptyList(),
    val availability: UsageDataAvailability = UsageDataAvailability.Collected
) {
    val totalMinutes: Int
        get() = (apps.sumOf(AppUsageEntry::foregroundMillis) / 60_000L).toInt()
}

enum class UsageDataAvailability { Collected, Partial, Unavailable }

fun recentOnboardingDates(today: LocalDate, completedDays: Int = 4): List<LocalDate> =
    (1..completedDays).map { today.minusDays(it.toLong()) }

enum class UsageTimelineEventType {
    Resumed,
    Paused,
    ScreenOff
}

data class UsageTimelineEvent(
    val timestamp: Long,
    val packageName: String?,
    val type: UsageTimelineEventType
)

/**
 * Reconstructs one foreground timeline instead of summing UsageStats buckets,
 * which can overlap or extend beyond the requested calendar day on some OEMs.
 */
fun calculateForegroundDurations(
    events: List<UsageTimelineEvent>,
    rangeStart: Long,
    rangeEnd: Long
): Map<String, Long> {
    if (rangeEnd <= rangeStart) return emptyMap()
    val durations = mutableMapOf<String, Long>()
    var activePackage: String? = null
    var activeSince = rangeStart

    fun closeActive(at: Long) {
        val packageName = activePackage ?: return
        val clippedStart = maxOf(activeSince, rangeStart)
        val clippedEnd = minOf(at, rangeEnd)
        if (clippedEnd > clippedStart) {
            durations[packageName] =
                (durations[packageName] ?: 0L) + (clippedEnd - clippedStart)
        }
        activePackage = null
    }

    events.sortedBy(UsageTimelineEvent::timestamp).forEach { event ->
        if (event.timestamp > rangeEnd) return@forEach
        when (event.type) {
            UsageTimelineEventType.Resumed -> {
                val packageName = event.packageName ?: return@forEach
                if (activePackage != packageName) {
                    closeActive(event.timestamp)
                    activePackage = packageName
                    activeSince = event.timestamp
                }
            }
            UsageTimelineEventType.Paused -> {
                if (activePackage == event.packageName) closeActive(event.timestamp)
            }
            UsageTimelineEventType.ScreenOff -> closeActive(event.timestamp)
        }
    }
    closeActive(rangeEnd)
    return durations
}

object UsageHistoryRepository {
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun loadLastSevenDays(context: Context, today: LocalDate = LocalDate.now()): List<DailyUsageHistory> {
        return loadRecentDays(context, 7, today)
    }

    fun loadSevenDaysEnding(context: Context, endDate: LocalDate): List<DailyUsageHistory> {
        if (!hasUsageAccess(context) || endDate.isAfter(LocalDate.now())) return emptyList()
        refreshLedger(context, LocalDate.now())
        return readDaysEnding(context, 7, endDate)
    }

    fun loadRecentDays(
        context: Context,
        dayCount: Int,
        today: LocalDate = LocalDate.now()
    ): List<DailyUsageHistory> {
        if (!hasUsageAccess(context)) return emptyList()
        refreshLedger(context, today)
        return readDaysEnding(context, dayCount, today)
    }

    private fun readDaysEnding(
        context: Context,
        dayCount: Int,
        endDate: LocalDate
    ): List<DailyUsageHistory> {
        val ledger = UsageLedgerStore(context)
        val trackingStart = ledger.trackingStartDate
        return ((dayCount - 1).toLong() downTo 0L).map { offset ->
            val date = endDate.minusDays(offset)
            ledger.read(date) ?: DailyUsageHistory(
                date = date,
                apps = emptyList(),
                availability = if (date < trackingStart) UsageDataAvailability.Unavailable
                    else UsageDataAvailability.Partial
            )
        }
    }

    fun loadDay(context: Context, date: LocalDate): DailyUsageHistory? {
        if (!hasUsageAccess(context) || date.isAfter(LocalDate.now())) return null
        val day = queryDay(context, date)
        UsageLedgerStore(context).write(day, finalized = date < LocalDate.now())
        return day
    }

    fun refreshLedger(context: Context, today: LocalDate = LocalDate.now()) {
        if (!hasUsageAccess(context)) return
        val ledger = UsageLedgerStore(context)
        val trackingStart = ledger.trackingStartDate
        if (ledger.needsInitialBackfill) {
            // Samsung retained events were validated for the most recent four completed
            // days on this device. Older dates remain unavailable rather than estimated.
            recentOnboardingDates(today).forEach { date ->
                ledger.write(queryDay(context, date), finalized = true)
            }
            ledger.markInitialBackfillComplete()
        }
        val dates = buildList {
            add(today)
            val yesterday = today.minusDays(1)
            if (yesterday >= trackingStart) add(yesterday)
        }
        dates.forEach { date ->
            ledger.write(queryDay(context, date), finalized = date < today)
        }
    }

    private fun queryDay(context: Context, date: LocalDate): DailyUsageHistory {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val zone = ZoneId.systemDefault()
        val homePackage = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        val excludedPackages = setOfNotNull("android", "com.android.systemui", homePackage)

        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            .coerceAtMost(System.currentTimeMillis())
        if (end <= start) return DailyUsageHistory(date, emptyList(), availability = UsageDataAvailability.Partial)

            // The lookback seeds the active app when a session crosses midnight.
        val rawEvents = manager.queryEvents(start - 24 * 60 * 60 * 1000L, end)
            val event = UsageEvents.Event()
            val timeline = mutableListOf<UsageTimelineEvent>()
            var unlockCount = 0
            while (rawEvents.hasNextEvent()) {
                rawEvents.getNextEvent(event)
                if (
                    event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN &&
                    event.timeStamp in start until end
                ) {
                    unlockCount++
                }
                val type = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UsageTimelineEventType.Resumed
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> UsageTimelineEventType.Paused
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN,
                    UsageEvents.Event.DEVICE_SHUTDOWN -> UsageTimelineEventType.ScreenOff
                    else -> null
                }
                if (type != null) {
                    timeline += UsageTimelineEvent(event.timeStamp, event.packageName, type)
                }
            }

            val verifiedFocusIntervals = FocusSessionIntervalStore(context).intervalsFor(date, zone)
            val durations = calculateAdjustedForegroundDurations(
                timeline, start, end, context.packageName, verifiedFocusIntervals
            ).toMutableMap()
            val hourlyDurations = (0 until 24).map { hour ->
                val hourStart = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
                val hourEnd = minOf(hourStart + 60 * 60 * 1000L, end)
                if (hourEnd <= hourStart) emptyMap() else {
                    calculateAdjustedForegroundDurations(
                        timeline, hourStart, hourEnd, context.packageName, verifiedFocusIntervals
                    )
                }.toMutableMap()
            }
            val intentionalFocusHourlyMillis = (0 until 24).map { hour ->
                val hourStart = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
                val hourEnd = minOf(hourStart + 60 * 60 * 1000L, end)
                verifiedFocusIntervals.sumOf { interval ->
                    (minOf(interval.endMillis, hourEnd) - maxOf(interval.startMillis, hourStart))
                        .coerceAtLeast(0L)
                }
            }.toMutableList()
            val intervalFocusMillis = verifiedFocusIntervals.sumOf { interval ->
                (minOf(interval.endMillis, end) - maxOf(interval.startMillis, start)).coerceAtLeast(0L)
            }
            val completedFocusMillis = FocusStreakStore(
                context.getSharedPreferences("focus_streak", Context.MODE_PRIVATE)
            ).minutesFor(date) * 60_000L
            var missingLegacyFocusMillis = (completedFocusMillis - intervalFocusMillis).coerceAtLeast(0L)
            hourlyDurations.indices
                .sortedByDescending { hourlyDurations[it][context.packageName] ?: 0L }
                .forEach { hour ->
                    if (missingLegacyFocusMillis <= 0L) return@forEach
                    val driftMillis = hourlyDurations[hour][context.packageName] ?: 0L
                    val matched = minOf(driftMillis, missingLegacyFocusMillis)
                    if (matched > 0L) {
                        val remaining = driftMillis - matched
                        if (remaining > 0L) hourlyDurations[hour][context.packageName] = remaining
                        else hourlyDurations[hour].remove(context.packageName)
                        val dailyRemaining = (durations[context.packageName] ?: 0L) - matched
                        if (dailyRemaining > 0L) durations[context.packageName] = dailyRemaining
                        else durations.remove(context.packageName)
                        intentionalFocusHourlyMillis[hour] += matched
                        missingLegacyFocusMillis -= matched
                    }
                }
            val hourlyMinutes = hourlyDurations.map { durationsByApp ->
                (durationsByApp.values.sum() / 60_000L).toInt()
            }
            val intentionalFocusHourlyMinutes = intentionalFocusHourlyMillis.map { (it / 60_000L).toInt() }
            val lateNightMillis = hourlyDurations.filterIndexed { hour, _ -> hour < 6 || hour >= 22 }
                .sumOf { it.values.sum() }
            val intentionalFocusMillis = maxOf(intervalFocusMillis, completedFocusMillis)
            val apps = durations
                .asSequence()
                .filter { (packageName, millis) ->
                    packageName !in excludedPackages && millis >= 60_000L
                }
                .map { (packageName, millis) ->
                    val label = runCatching {
                        packageManager.getApplicationInfo(packageName, 0)
                            .loadLabel(packageManager)
                            .toString()
                    }.getOrDefault(packageName)
                    AppUsageEntry(
                        packageName,
                        label,
                        millis,
                        hourlyDurations.map { it[packageName] ?: 0L }
                    )
                }
                .sortedByDescending(AppUsageEntry::foregroundMillis)
                .toList()
        return DailyUsageHistory(
            date = date,
            apps = apps,
            unlockCount = unlockCount,
            lateNightMinutes = (lateNightMillis / 60_000L).toInt(),
            hourlyMinutes = hourlyMinutes,
            intentionalFocusMinutes = (intentionalFocusMillis / 60_000L).toInt(),
            intentionalFocusHourlyMinutes = intentionalFocusHourlyMinutes,
            availability = if (date == LocalDate.now()) UsageDataAvailability.Partial else UsageDataAvailability.Collected
        )
    }
}
