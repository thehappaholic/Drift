package com.example.drift

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

internal class UsageLedgerStore(context: Context) {
    private val preferences = context.getSharedPreferences("mobile_usage_ledger", Context.MODE_PRIVATE)

    val trackingStartDate: LocalDate
        get() {
            val currentZone = ZoneId.systemDefault().id
            val existing = preferences.getString(KEY_START_DATE, null)
            val storedZone = preferences.getString(KEY_ZONE, null)
            if (existing != null && storedZone == currentZone) return LocalDate.parse(existing)
            return LocalDate.now().also {
                preferences.edit()
                    .putString(KEY_START_DATE, it.toString())
                    .putString(KEY_ZONE, currentZone)
                    .apply()
            }
        }

    val needsInitialBackfill: Boolean
        get() = !preferences.getBoolean(KEY_INITIAL_BACKFILL_COMPLETE, false)

    fun markInitialBackfillComplete() {
        preferences.edit().putBoolean(KEY_INITIAL_BACKFILL_COMPLETE, true).apply()
    }

    fun read(date: LocalDate): DailyUsageHistory? {
        val encoded = preferences.getString(dayKey(date), null) ?: return null
        return runCatching {
            val root = JSONObject(encoded)
            val appsJson = root.getJSONArray("apps")
            val apps = buildList {
                for (index in 0 until appsJson.length()) {
                    val app = appsJson.getJSONObject(index)
                    val hourly = app.optJSONArray("hourly")?.let { values ->
                        (0 until values.length()).map(values::optLong)
                    }.orEmpty()
                    add(AppUsageEntry(app.getString("package"), app.getString("name"), app.getLong("millis"), hourly))
                }
            }
            DailyUsageHistory(
                date = date,
                apps = apps,
                unlockCount = root.optInt("unlocks"),
                lateNightMinutes = root.optInt("lateNight"),
                hourlyMinutes = root.optJSONArray("hourly")?.let { hourly ->
                    (0 until hourly.length()).map(hourly::optInt)
                }.orEmpty(),
                intentionalFocusMinutes = root.optInt("intentionalFocus"),
                intentionalFocusHourlyMinutes = root.optJSONArray("focusHourly")?.let { hourly ->
                    (0 until hourly.length()).map(hourly::optInt)
                }.orEmpty(),
                availability = if (root.optBoolean("finalized")) UsageDataAvailability.Collected
                    else UsageDataAvailability.Partial
            )
        }.getOrNull()
    }

    fun write(day: DailyUsageHistory, finalized: Boolean) {
        val apps = JSONArray()
        day.apps.forEach { entry ->
            apps.put(JSONObject().put("package", entry.packageName).put("name", entry.appName)
                .put("millis", entry.foregroundMillis).put("hourly", JSONArray(entry.hourlyMillis)))
        }
        val root = JSONObject()
            .put("apps", apps)
            .put("unlocks", day.unlockCount)
            .put("lateNight", day.lateNightMinutes)
            .put("hourly", JSONArray(day.hourlyMinutes))
            .put("intentionalFocus", day.intentionalFocusMinutes)
            .put("focusHourly", JSONArray(day.intentionalFocusHourlyMinutes))
            .put("finalized", finalized)
            .put("updatedAt", System.currentTimeMillis())
        preferences.edit().putString(dayKey(day.date), root.toString()).apply()
    }

    private fun dayKey(date: LocalDate) = "day_$date"

    companion object {
        private const val KEY_START_DATE = "tracking_start_date"
        private const val KEY_ZONE = "tracking_zone"
        private const val KEY_INITIAL_BACKFILL_COMPLETE = "initial_recent_backfill_complete_v1"
    }
}
