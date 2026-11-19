package com.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UsageTimelineProcessorTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun singleAppSessionCalculatesExactDuration() {
        val start = 1_000_000L
        val end = 1_300_000L // 300,000 ms = 5 minutes
        val events = listOf(
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_RESUMED, start),
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_PAUSED, end)
        )

        val sessions = UsageTimelineProcessor.processRawEvents(events, start - 1000, end + 1000)
        assertEquals(1, sessions.size)
        assertEquals("com.android.chrome", sessions[0].packageName)
        assertEquals(300_000L, sessions[0].duration)
    }

    @Test
    fun appSwitchingClosesPreviousSession() {
        val t0 = 1_000_000L
        val t1 = 1_300_000L // +5 min
        val t2 = 1_480_000L // +3 min
        val events = listOf(
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_RESUMED, t0),
            UsageTimelineProcessor.RawEvent("com.google.android.youtube", UsageTimelineProcessor.EVENT_ACTIVITY_RESUMED, t1),
            UsageTimelineProcessor.RawEvent("com.google.android.youtube", UsageTimelineProcessor.EVENT_ACTIVITY_PAUSED, t2)
        )

        val sessions = UsageTimelineProcessor.processRawEvents(events, t0 - 1000, t2 + 1000)
        assertEquals(2, sessions.size)
        assertEquals("com.android.chrome", sessions[0].packageName)
        assertEquals(300_000L, sessions[0].duration) // 5 minutes

        assertEquals("com.google.android.youtube", sessions[1].packageName)
        assertEquals(180_000L, sessions[1].duration) // 3 minutes
    }

    @Test
    fun screenOffStopsSessionAndScreenOnResumes() {
        val t0 = 1_000_000L
        val screenOff = 1_180_000L // +3 min of active usage
        val screenOn = 1_600_000L  // screen off for 7 min
        val pause = 1_720_000L     // +2 min of active usage after screen on

        val events = listOf(
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_RESUMED, t0),
            UsageTimelineProcessor.RawEvent("android", UsageTimelineProcessor.EVENT_SCREEN_NON_INTERACTIVE, screenOff),
            UsageTimelineProcessor.RawEvent("android", UsageTimelineProcessor.EVENT_SCREEN_INTERACTIVE, screenOn),
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_PAUSED, pause)
        )

        val sessions = UsageTimelineProcessor.processRawEvents(events, t0 - 1000, pause + 1000)
        assertEquals(2, sessions.size)
        assertEquals(180_000L, sessions[0].duration) // 3 min
        assertEquals(120_000L, sessions[1].duration) // 2 min

        val totals = UsageTimelineProcessor.aggregatePackageTotals(sessions)
        assertEquals(300_000L, totals["com.android.chrome"]) // 5 min total (7 min screen-off ignored!)
    }

    @Test
    fun keyguardLockPausesSession() {
        val t0 = 1_000_000L
        val lock = 1_120_000L // +2 min
        val unlock = 1_400_000L
        val pause = 1_520_000L // +2 min

        val events = listOf(
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_RESUMED, t0),
            UsageTimelineProcessor.RawEvent("android", UsageTimelineProcessor.EVENT_KEYGUARD_SHOWN, lock),
            UsageTimelineProcessor.RawEvent("android", UsageTimelineProcessor.EVENT_KEYGUARD_HIDDEN, unlock),
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_PAUSED, pause)
        )

        val sessions = UsageTimelineProcessor.processRawEvents(events, t0 - 1000, pause + 1000)
        val totals = UsageTimelineProcessor.aggregatePackageTotals(sessions)
        assertEquals(240_000L, totals["com.android.chrome"]) // 4 min total
    }

    @Test
    fun multipleActivitiesInSamePackageDoNotDoubleCount() {
        val t0 = 1_000_000L
        val t1 = 1_060_000L // +1 min: tab switch / second activity inside Chrome
        val t2 = 1_300_000L // +4 min: exit Chrome

        val events = listOf(
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_RESUMED, t0),
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_RESUMED, t1),
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_PAUSED, t2)
        )

        val sessions = UsageTimelineProcessor.processRawEvents(events, t0 - 1000, t2 + 1000)
        val totals = UsageTimelineProcessor.aggregatePackageTotals(sessions)
        assertEquals(300_000L, totals["com.android.chrome"]) // 5 min continuous, NOT 9 min!
    }

    @Test
    fun ongoingSessionAtQueryEndIsCalculated() {
        val t0 = 1_000_000L
        val queryEnd = 1_600_000L // ongoing for 10 minutes, no pause event yet
        val events = listOf(
            UsageTimelineProcessor.RawEvent("com.android.chrome", UsageTimelineProcessor.EVENT_ACTIVITY_RESUMED, t0)
        )

        val sessions = UsageTimelineProcessor.processRawEvents(events, t0 - 1000, queryEnd)
        assertEquals(1, sessions.size)
        assertEquals(600_000L, sessions[0].duration) // 10 minutes accounted for
    }

    @Test
    fun hourlyDistributionSplitsAcrossHourBoundary() {
        // 13:45 to 14:15 UTC on 2026-11-19
        val date = LocalDate.of(2026, 11, 19)
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val start13_45 = date.atTime(13, 45).atZone(zone).toInstant().toEpochMilli()
        val end14_15 = date.atTime(14, 15).atZone(zone).toInstant().toEpochMilli()

        val sessions = listOf(
            UsageTimelineProcessor.Session("com.android.chrome", start13_45, end14_15)
        )

        val hourlyBuckets = UsageTimelineProcessor.distributeToHourlyBuckets(sessions, dayStart, dayEnd, zone)
        assertEquals(15 * 60_000L, hourlyBuckets[13]) // 15 mins in hour 13
        assertEquals(15 * 60_000L, hourlyBuckets[14]) // 15 mins in hour 14
        assertEquals(0L, hourlyBuckets[12])
        assertEquals(0L, hourlyBuckets[15])
        assertEquals(30 * 60_000L, hourlyBuckets.sum()) // sum = 30 mins
    }

    @Test
    fun midnightBoundarySplitsSessionAcrossDays() {
        // 23:50 to 00:20 across midnight (30 mins total)
        val date1 = LocalDate.of(2026, 11, 19)
        val date2 = LocalDate.of(2026, 11, 20)

        val start = date1.atTime(23, 50).atZone(zone).toInstant().toEpochMilli()
        val end = date2.atTime(0, 20).atZone(zone).toInstant().toEpochMilli()

        val session = UsageTimelineProcessor.Session("com.android.chrome", start, end)
        val daySplits = UsageTimelineProcessor.splitSessionByDay(session, zone, resetMinutes = 0)

        assertEquals(10 * 60_000L, daySplits[date1]) // 10 mins on Day 1
        assertEquals(20 * 60_000L, daySplits[date2]) // 20 mins on Day 2
        assertEquals(30 * 60_000L, daySplits.values.sum())
    }
}
