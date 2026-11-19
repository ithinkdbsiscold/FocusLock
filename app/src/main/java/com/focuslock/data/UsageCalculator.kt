package com.focuslock.data

/** Pure interval arithmetic used when reconciling system UsageEvents. */
object UsageCalculator {
    data class Interval(val startedAtMillis: Long, val endedAtMillis: Long)

    fun clippedDuration(interval: Interval, rangeStart: Long, rangeEnd: Long): Long {
        val start = maxOf(interval.startedAtMillis, rangeStart)
        val end = minOf(interval.endedAtMillis, rangeEnd)
        return (end - start).coerceAtLeast(0)
    }

    /** Merges overlaps so repeated events cannot double-count foreground time. */
    fun totalUniqueDuration(intervals: List<Interval>, rangeStart: Long, rangeEnd: Long): Long {
        val clipped = intervals.mapNotNull { interval ->
            val start = maxOf(interval.startedAtMillis, rangeStart)
            val end = minOf(interval.endedAtMillis, rangeEnd)
            if (end > start) Interval(start, end) else null
        }.sortedBy { it.startedAtMillis }
        if (clipped.isEmpty()) return 0L

        var total = 0L
        var currentStart = clipped.first().startedAtMillis
        var currentEnd = clipped.first().endedAtMillis
        for (interval in clipped.drop(1)) {
            if (interval.startedAtMillis <= currentEnd) {
                currentEnd = maxOf(currentEnd, interval.endedAtMillis)
            } else {
                total += currentEnd - currentStart
                currentStart = interval.startedAtMillis
                currentEnd = interval.endedAtMillis
            }
        }
        return total + currentEnd - currentStart
    }
}
