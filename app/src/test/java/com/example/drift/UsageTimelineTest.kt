package com.example.drift

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UsageTimelineTest {
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

    private fun event(time: Long, packageName: String?, type: UsageTimelineEventType) =
        UsageTimelineEvent(time, packageName, type)
}
