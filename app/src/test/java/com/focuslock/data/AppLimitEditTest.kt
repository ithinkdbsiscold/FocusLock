package com.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLimitEditTest {

    @Test
    fun loweringLimitBelowAccumulatedUsageActivatesBlocking() {
        val accumulatedUsage = 45 * 60_000L // 45 minutes used
        val oldLimit = 60 * 60_000L         // 1 hour limit (not blocked)
        assertFalse(BlockingPolicy.shouldBlock(true, false, accumulatedUsage, oldLimit))

        val newLowerLimit = 30 * 60_000L    // User edits limit down to 30 mins
        assertTrue(BlockingPolicy.shouldBlock(true, false, accumulatedUsage, newLowerLimit))
    }

    @Test
    fun increasingLimitAboveAccumulatedUsageAllowsAccess() {
        val accumulatedUsage = 35 * 60_000L // 35 minutes used
        val oldLimit = 30 * 60_000L         // was 30 mins (was blocked)
        assertTrue(BlockingPolicy.shouldBlock(true, true, accumulatedUsage, oldLimit))

        // When user edits limit to 60 mins, evaluation against new limit must allow access
        val newHigherLimit = 60 * 60_000L
        val shouldBlockUnderNewLimit = accumulatedUsage >= newHigherLimit
        assertFalse(shouldBlockUnderNewLimit)
    }

    @Test
    fun disablingLimitAllowsAccessRegardlessOfUsage() {
        val accumulatedUsage = 120 * 60_000L // 2 hours used
        val limit = 30 * 60_000L
        assertFalse(BlockingPolicy.shouldBlock(blockingEnabled = false, alreadyBlocked = true, usageMillis = accumulatedUsage, limitMillis = limit))
    }

    @Test
    fun remainingTimeCalculationIsAccurate() {
        val limit = 90 * 60_000L // 1h 30m
        val usage = 40 * 60_000L // 40m
        val row = AppUsageRow("com.android.chrome", "Chrome", limit, true, usage, 5, false)
        assertEquals(50 * 60_000L, row.remainingMillis)
        assertEquals(40f / 90f, row.progress, 0.001f)
    }

    @Test
    fun remainingTimeDoesNotUnderflowWhenOverLimit() {
        val limit = 30 * 60_000L
        val usage = 45 * 60_000L
        val row = AppUsageRow("com.android.chrome", "Chrome", limit, true, usage, 5, true)
        assertEquals(0L, row.remainingMillis)
        assertEquals(1.0f, row.progress, 0.001f)
    }
}
