package com.focuslock.ui

import java.util.concurrent.TimeUnit

fun formatDuration(millis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis.coerceAtLeast(0))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

fun formatDurationDetailed(millis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis.coerceAtLeast(0))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} hr ${minutes} min"
        hours > 0 -> "${hours} hr"
        minutes > 0 -> "${minutes} min"
        else -> "< 1 min"
    }
}

fun formatClockMinutes(minutes: Int): String {
    val hour = (minutes.coerceIn(0, 1439) / 60)
    val min = minutes % 60
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when (hour % 12) { 0 -> 12; else -> hour % 12 }
    return "%d:%02d %s".format(displayHour, min, suffix)
}
