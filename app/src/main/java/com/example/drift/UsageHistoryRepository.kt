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
    val foregroundMillis: Long
) {
    val foregroundMinutes: Int get() = (foregroundMillis / 60_000L).toInt()
}

data class DailyUsageHistory(
    val date: LocalDate,
    val apps: List<AppUsageEntry>,
    val unlockCount: Int = 0,
    val lateNightMinutes: Int = 0,
    val hourlyMinutes: List<Int> = emptyList(),
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

    fun loadRecentDays(
        context: Context,
        dayCount: Int,
        today: LocalDate = LocalDate.now()
    ): List<DailyUsageHistory> {
        if (!hasUsageAccess(context)) return emptyList()
        refreshLedger(context, today)
        val ledger = UsageLedgerStore(context)
        val trackingStart = ledger.trackingStartDate
        return ((dayCount - 1).toLong() downTo 0L).map { offset ->
            val date = today.minusDays(offset)
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

            val durations = calculateForegroundDurations(timeline, start, end)
            val hourlyMinutes = (0 until 24).map { hour ->
                val hourStart = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
                val hourEnd = minOf(hourStart + 60 * 60 * 1000L, end)
                if (hourEnd <= hourStart) 0 else {
                    (calculateForegroundDurations(timeline, hourStart, hourEnd).values.sum() / 60_000L).toInt()
                }
            }
            val morningEnd = date.atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
            val eveningStart = date.atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
            val lateNightMillis =
                calculateForegroundDurations(timeline, start, minOf(morningEnd, end)).values.sum() +
                    if (end > eveningStart) {
                        calculateForegroundDurations(timeline, eveningStart, end).values.sum()
                    } else 0L
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
                    AppUsageEntry(packageName, label, millis)
                }
                .sortedByDescending(AppUsageEntry::foregroundMillis)
                .toList()
        return DailyUsageHistory(
            date = date,
            apps = apps,
            unlockCount = unlockCount,
            lateNightMinutes = (lateNightMillis / 60_000L).toInt(),
            hourlyMinutes = hourlyMinutes,
            availability = if (date == LocalDate.now()) UsageDataAvailability.Partial else UsageDataAvailability.Collected
        )
    }
}
