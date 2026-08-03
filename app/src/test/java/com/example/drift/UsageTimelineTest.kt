package com.example.drift

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UsageTimelineTest {
    @Test
    fun `attention usage reclassifies verified focus during device use`() {
        assertEquals(373L, calculateAttentionMillis(524L, 151L))
        assertEquals(0L, calculateAttentionMillis(40L, 60L))
    }

    @Test
    fun `daily history separates device time attention time and focus overlap`() {
        val history = DailyUsageHistory(
            date = LocalDate.of(2026, 8, 3),
            apps = listOf(
                AppUsageEntry(
                    "com.example.drift",
                    "Drift",
                    180 * 60_000L,
                    List(24) { hour -> if (hour == 10) 60 * 60_000L else 0L }
                ),
                AppUsageEntry("video", "Video", 140 * 60_000L)
            ),
            intentionalFocusHourlyMinutes = List(24) { hour -> if (hour == 10) 40 else 0 },
            attentionMillis = 140 * 60_000L
        )

        assertEquals(320, history.deviceScreenMinutes)
        assertEquals(180, history.driftForegroundMinutes)
        assertEquals(140, history.attentionMinutes)
        assertEquals(40, history.focusOverlapMinutes)
        assertEquals(140, history.totalMinutes)
    }

    @Test
    fun onboardingBackfillUsesOnlyFourRecentCompletedDays() {
        val today = LocalDate.of(2026, 8, 2)

        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 7, 30),
                LocalDate.of(2026, 7, 29)
            ),
            recentOnboardingDates(today)
        )
    }

    @Test
    fun stoppedActivityClosesOlderRetainedSession() {
        val durations = calculateForegroundDurations(
            events = listOf(
                UsageTimelineEvent(1_000, "video", UsageTimelineEventType.Resumed),
                UsageTimelineEvent(61_000, "video", UsageTimelineEventType.Paused),
                UsageTimelineEvent(12_000_000, null, UsageTimelineEventType.ScreenOff)
            ),
            rangeStart = 0,
            rangeEnd = 24_000_000
        )

        assertEquals(60_000L, durations["video"])
    }

    @Test
    fun `switching apps creates non-overlapping durations`() {
        val result = calculateForegroundDurations(
            listOf(
                event(0, "a", UsageTimelineEventType.Resumed),
                event(10, "b", UsageTimelineEventType.Resumed),
                event(25, "b", UsageTimelineEventType.Paused)
            ),
            rangeStart = 0,
            rangeEnd = 30
        )
        assertEquals(mapOf("a" to 10L, "b" to 15L), result)
    }

    @Test
    fun `session crossing midnight is clipped to requested day`() {
        val result = calculateForegroundDurations(
            listOf(
                event(80, "a", UsageTimelineEventType.Resumed),
                event(130, "a", UsageTimelineEventType.Paused)
            ),
            rangeStart = 100,
            rangeEnd = 200
        )
        assertEquals(30L, result["a"])
    }

    @Test
    fun `screen off closes the active app`() {
        val result = calculateForegroundDurations(
            listOf(
                event(10, "a", UsageTimelineEventType.Resumed),
                event(40, null, UsageTimelineEventType.ScreenOff)
            ),
            rangeStart = 0,
            rangeEnd = 100
        )
        assertEquals(30L, result["a"])
    }

    @Test
    fun `open session is closed at query end`() {
        val result = calculateForegroundDurations(
            listOf(event(25, "a", UsageTimelineEventType.Resumed)),
            rangeStart = 0,
            rangeEnd = 100
        )
        assertEquals(75L, result["a"])
    }

    @Test
    fun `duplicate resume does not restart the session`() {
        val result = calculateForegroundDurations(
            listOf(
                event(10, "a", UsageTimelineEventType.Resumed),
                event(20, "a", UsageTimelineEventType.Resumed),
                event(40, "a", UsageTimelineEventType.Paused)
            ),
            rangeStart = 0,
            rangeEnd = 100
        )
        assertEquals(30L, result["a"])
    }

    @Test
    fun `verified focus removes only overlapping Drift foreground time`() {
        val result = calculateAdjustedForegroundDurations(
            events = listOf(
                event(0, "com.example.drift", UsageTimelineEventType.Resumed),
                event(100, "com.example.drift", UsageTimelineEventType.Paused)
            ),
            rangeStart = 0,
            rangeEnd = 100,
            driftPackageName = "com.example.drift",
            verifiedFocusIntervals = listOf(FocusSessionInterval(20, 80))
        )

        assertEquals(40L, result["com.example.drift"])
    }

    @Test
    fun `verified focus never removes another app usage`() {
        val result = calculateAdjustedForegroundDurations(
            events = listOf(
                event(0, "video", UsageTimelineEventType.Resumed),
                event(100, "video", UsageTimelineEventType.Paused)
            ),
            rangeStart = 0,
            rangeEnd = 100,
            driftPackageName = "com.example.drift",
            verifiedFocusIntervals = listOf(FocusSessionInterval(20, 80))
        )

        assertEquals(100L, result["video"])
    }

    @Test
    fun `stopping an older activity does not close another activity in the same app`() {
        val result = calculateForegroundDurations(
            listOf(
                UsageTimelineEvent(0, "instagram", UsageTimelineEventType.Resumed, instanceId = 1),
                UsageTimelineEvent(10, "instagram", UsageTimelineEventType.Resumed, instanceId = 2),
                UsageTimelineEvent(12, "instagram", UsageTimelineEventType.Paused, instanceId = 1),
                UsageTimelineEvent(15, "instagram", UsageTimelineEventType.Paused, instanceId = 1),
                UsageTimelineEvent(100, "instagram", UsageTimelineEventType.Paused, instanceId = 2)
            ),
            rangeStart = 0,
            rangeEnd = 100
        )

        assertEquals(100L, result["instagram"])
    }

    private fun event(time: Long, packageName: String?, type: UsageTimelineEventType) =
        UsageTimelineEvent(time, packageName, type)
}
