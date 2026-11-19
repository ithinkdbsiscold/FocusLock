package com.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageCalculatorTest {
    @Test fun clipsSessionAtDayStart() {
        assertEquals(300L, UsageCalculator.clippedDuration(UsageCalculator.Interval(700, 1_300), 1_000, 2_000))
    }

    @Test fun clipsSessionAtDayEnd() {
        assertEquals(200L, UsageCalculator.clippedDuration(UsageCalculator.Interval(1_800, 2_500), 1_000, 2_000))
    }

    @Test fun ignoresOutsideSession() {
        assertEquals(0L, UsageCalculator.clippedDuration(UsageCalculator.Interval(20, 50), 100, 200))
    }

    @Test fun mergesOverlappingSessionsToPreventDuplicateUsage() {
        val time = UsageCalculator.totalUniqueDuration(
            listOf(UsageCalculator.Interval(100, 500), UsageCalculator.Interval(400, 900)), 0, 1_000
        )
        assertEquals(800L, time)
    }

    @Test fun mergesNestedSessions() {
        val time = UsageCalculator.totalUniqueDuration(
            listOf(UsageCalculator.Interval(100, 1_000), UsageCalculator.Interval(250, 500)), 0, 1_200
        )
        assertEquals(900L, time)
    }

    @Test fun preservesSeparateSessions() {
        val time = UsageCalculator.totalUniqueDuration(
            listOf(UsageCalculator.Interval(100, 200), UsageCalculator.Interval(400, 700)), 0, 1_000
        )
        assertEquals(400L, time)
    }
}
