package com.happaholic.drift

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTimeBandTest {
    @Test
    fun `screen time boundaries map to the intended bands`() {
        val cases = mapOf(
            0 to ScreenTimeBand.Low1,
            59 to ScreenTimeBand.Low1,
            60 to ScreenTimeBand.Low2,
            119 to ScreenTimeBand.Low2,
            120 to ScreenTimeBand.Low3,
            179 to ScreenTimeBand.Low3,
            180 to ScreenTimeBand.Medium1,
            270 to ScreenTimeBand.Medium1,
            271 to ScreenTimeBand.Medium2,
            359 to ScreenTimeBand.Medium2,
            360 to ScreenTimeBand.High
        )

        cases.forEach { (minutes, expected) ->
            assertEquals("$minutes minutes", expected, screenTimeBand(minutes))
        }
    }

    @Test
    fun `unlock thresholds use positive moderate and attention states`() {
        assertEquals(MetricStatus.Positive, unlockStatus(0))
        assertEquals(MetricStatus.Positive, unlockStatus(39))
        assertEquals(MetricStatus.Moderate, unlockStatus(40))
        assertEquals(MetricStatus.Moderate, unlockStatus(79))
        assertEquals(MetricStatus.Attention, unlockStatus(80))
    }

    @Test
    fun `focus score thresholds reward higher scores`() {
        assertEquals(MetricStatus.Attention, focusScoreStatus(59))
        assertEquals(MetricStatus.Moderate, focusScoreStatus(60))
        assertEquals(MetricStatus.Moderate, focusScoreStatus(79))
        assertEquals(MetricStatus.Positive, focusScoreStatus(80))
        assertEquals(MetricStatus.Positive, focusScoreStatus(100))
    }

    @Test
    fun `focus score labels describe score health rather than risk severity`() {
        assertEquals("Room to grow", focusScoreLabel(53))
        assertEquals("Moderate", focusScoreLabel(60))
        assertEquals("Good", focusScoreLabel(80))
    }

    @Test
    fun `budget usage warns before and at the allocated limit`() {
        assertEquals(MetricStatus.Positive, budgetUsageStatus(33, 45))
        assertEquals(MetricStatus.Moderate, budgetUsageStatus(34, 45))
        assertEquals(MetricStatus.Moderate, budgetUsageStatus(44, 45))
        assertEquals(MetricStatus.Attention, budgetUsageStatus(45, 45))
        assertEquals(MetricStatus.Attention, budgetUsageStatus(58, 45))
    }
}
