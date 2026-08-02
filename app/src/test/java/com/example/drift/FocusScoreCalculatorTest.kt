package com.example.drift

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusScoreCalculatorTest {
    @Test
    fun `balanced productive day scores highly`() {
        val result = calculateFocusScore(150, 18, .6f, 40, 3, 5)
        assertTrue(result.total >= 85)
    }

    @Test
    fun `heavy fragmented late usage reduces score`() {
        val result = calculateFocusScore(540, 110, 1.5f, 0, 0, 120)
        assertTrue(result.total <= 20)
    }

    @Test
    fun `component weights total one hundred`() {
        val result = calculateFocusScore(0, 0, 0f, 40, 7, 0)
        assertEquals(100, result.total)
    }
}
