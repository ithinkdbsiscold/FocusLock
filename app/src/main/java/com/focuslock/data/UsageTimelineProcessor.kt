package com.focuslock.data

import android.app.usage.UsageEvents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Robust timeline processor for Android UsageEvents.
 *
 * Implements a strict chronological state machine to accurately extract app foreground
 * sessions, avoiding double-counting, accounting for screen off / keyguard lock, and
 * cleanly splitting sessions across midnight and hourly boundaries.
 */
object UsageTimelineProcessor {

    // Event type constants matching Android android.app.usage.UsageEvents.Event
    const val EVENT_ACTIVITY_RESUMED = 1
    const val EVENT_ACTIVITY_PAUSED = 2
    const val EVENT_SCREEN_INTERACTIVE = 15
    const val EVENT_SCREEN_NON_INTERACTIVE = 16
    const val EVENT_KEYGUARD_SHOWN = 17
    const val EVENT_KEYGUARD_HIDDEN = 18

    val EXCLUDED_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.motorola.launcher3",
        "com.motorola.launcher",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller"
    )

    data class RawEvent(
        val packageName: String,
        val eventType: Int,
        val timeStamp: Long
    )

    data class Session(
        val packageName: String,
        val startMillis: Long,
        val endMillis: Long
    ) {
        val duration: Long get() = (endMillis - startMillis).coerceAtLeast(0L)
    }

    /**
     * Chronologically processes raw usage events into non-overlapping foreground sessions.
     */
    fun processRawEvents(
        events: List<RawEvent>,
        queryStart: Long,
        queryEnd: Long
    ): List<Session> {
        val sorted = events
            .filter { it.timeStamp in queryStart..queryEnd }
            .sortedBy { it.timeStamp }

        val sessions = mutableListOf<Session>()

        var currentPackage: String? = null
        var sessionStartTime: Long = 0L
        var isScreenInteractive = true
        var isKeyguardLocked = false

        fun closeCurrentSession(closeTime: Long) {
            val pkg = currentPackage
            if (pkg != null && isScreenInteractive && !isKeyguardLocked && sessionStartTime > 0L) {
                val clampedStart = maxOf(sessionStartTime, queryStart)
                val clampedEnd = minOf(closeTime, queryEnd)
                if (clampedEnd > clampedStart && pkg !in EXCLUDED_PACKAGES) {
                    sessions.add(Session(pkg, clampedStart, clampedEnd))
                }
            }
            sessionStartTime = 0L
        }

        for (event in sorted) {
            when (event.eventType) {
                EVENT_ACTIVITY_RESUMED -> {
                    val pkg = event.packageName
                    if (currentPackage == pkg) {
                        // Activity transition within same package (e.g. Tab switch or sub-activity).
                        // Do not close or restart; keep continuous session going.
                        if (sessionStartTime == 0L && isScreenInteractive && !isKeyguardLocked) {
                            sessionStartTime = event.timeStamp
                        }
                    } else {
                        // Switched to a new package
                        closeCurrentSession(event.timeStamp)
                        currentPackage = pkg
                        if (isScreenInteractive && !isKeyguardLocked) {
                            sessionStartTime = event.timeStamp
                        }
                    }
                }

                EVENT_ACTIVITY_PAUSED -> {
                    val pkg = event.packageName
                    if (currentPackage == pkg) {
                        closeCurrentSession(event.timeStamp)
                        currentPackage = null
                    }
                }

                EVENT_SCREEN_NON_INTERACTIVE -> {
                    closeCurrentSession(event.timeStamp)
                    isScreenInteractive = false
                }

                EVENT_SCREEN_INTERACTIVE -> {
                    isScreenInteractive = true
                    if (currentPackage != null && !isKeyguardLocked) {
                        sessionStartTime = event.timeStamp
                    }
                }

                EVENT_KEYGUARD_SHOWN -> {
                    closeCurrentSession(event.timeStamp)
                    isKeyguardLocked = true
                }

                EVENT_KEYGUARD_HIDDEN -> {
                    isKeyguardLocked = false
                    if (currentPackage != null && isScreenInteractive) {
                        sessionStartTime = event.timeStamp
                    }
                }
            }
        }

        // Close any ongoing session at queryEnd (e.g. app actively open right now)
        closeCurrentSession(queryEnd)

        return sessions
    }

    /**
     * Reads native Android UsageEvents and converts them to RawEvents.
     */
    fun extractRawEvents(usageEvents: UsageEvents?): List<RawEvent> {
        if (usageEvents == null) return emptyList()
        val list = mutableListOf<RawEvent>()
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val pkg = event.packageName ?: ""
            list.add(RawEvent(pkg, event.eventType, event.timeStamp))
        }
        return list
    }

    /**
     * Splits a session across midnight boundaries if it spans multiple days.
     */
    fun splitSessionByDay(
        session: Session,
        zone: ZoneId,
        resetMinutes: Int = 0
    ): Map<LocalDate, Long> {
        val results = mutableMapOf<LocalDate, Long>()
        var currentStart = session.startMillis
        val totalEnd = session.endMillis

        while (currentStart < totalEnd) {
            val date = AccountingClock.accountingDate(currentStart, resetMinutes, zone)
            val nextDayStart = AccountingClock.dayStartMillis(date.plusDays(1), resetMinutes, zone)
            val chunkEnd = minOf(totalEnd, nextDayStart)
            val chunkDuration = (chunkEnd - currentStart).coerceAtLeast(0L)
            if (chunkDuration > 0) {
                results[date] = (results[date] ?: 0L) + chunkDuration
            }
            currentStart = chunkEnd
        }
        return results
    }

    /**
     * Distributes a day's sessions into 24 hourly buckets (0..23).
     * If a session spans across an hour boundary (e.g. 1:45 PM to 2:15 PM),
     * exactly 15m is allocated to 1 PM and 15m to 2 PM.
     */
    fun distributeToHourlyBuckets(
        sessions: List<Session>,
        dayStartMillis: Long,
        dayEndMillis: Long,
        zone: ZoneId
    ): LongArray {
        val buckets = LongArray(24) { 0L }

        for (session in sessions) {
            val clampedStart = maxOf(session.startMillis, dayStartMillis)
            val clampedEnd = minOf(session.endMillis, dayEndMillis)
            if (clampedEnd <= clampedStart) continue

            var cursor = clampedStart
            while (cursor < clampedEnd) {
                val zdt = Instant.ofEpochMilli(cursor).atZone(zone)
                val hour = zdt.hour.coerceIn(0, 23)
                val nextHourZdt = zdt.withMinute(0).withSecond(0).withNano(0).plusHours(1)
                val nextHourMillis = nextHourZdt.toInstant().toEpochMilli()
                val chunkEnd = minOf(clampedEnd, nextHourMillis)
                val chunkDuration = (chunkEnd - cursor).coerceAtLeast(0L)

                buckets[hour] += chunkDuration
                cursor = chunkEnd
            }
        }
        return buckets
    }

    /**
     * Calculates unique foreground time per package (preventing any duplicate counting).
     */
    fun aggregatePackageTotals(sessions: List<Session>): Map<String, Long> {
        val byPackage = sessions.groupBy { it.packageName }
        val totals = mutableMapOf<String, Long>()

        for ((pkg, list) in byPackage) {
            val intervals = list.map { UsageCalculator.Interval(it.startMillis, it.endMillis) }
            val uniqueDuration = UsageCalculator.totalUniqueDuration(intervals, 0L, Long.MAX_VALUE)
            if (uniqueDuration > 0) {
                totals[pkg] = uniqueDuration
            }
        }
        return totals
    }
}
