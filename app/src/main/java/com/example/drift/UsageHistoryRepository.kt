package com.example.drift

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val foregroundMinutes: Int
)

data class DailyUsageHistory(
    val date: LocalDate,
    val apps: List<AppUsageEntry>
) {
    val totalMinutes: Int get() = apps.sumOf(AppUsageEntry::foregroundMinutes)
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
        if (!hasUsageAccess(context)) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val zone = ZoneId.systemDefault()

        return (6L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                .coerceAtMost(System.currentTimeMillis())
            val apps = if (end <= start) {
                emptyList()
            } else {
                manager.queryAndAggregateUsageStats(start, end)
                    .values
                    .asSequence()
                    .filter { it.packageName != context.packageName && it.totalTimeInForeground >= 60_000L }
                    .map { stats ->
                        val label = runCatching {
                            packageManager.getApplicationInfo(stats.packageName, 0)
                                .loadLabel(packageManager)
                                .toString()
                        }.getOrDefault(stats.packageName)
                        AppUsageEntry(
                            packageName = stats.packageName,
                            appName = label,
                            foregroundMinutes = (stats.totalTimeInForeground / 60_000L).toInt()
                        )
                    }
                    .sortedByDescending(AppUsageEntry::foregroundMinutes)
                    .toList()
            }
            DailyUsageHistory(date, apps)
        }
    }
}
