package com.focuslock.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object AccountingClock {
    fun accountingDate(
        nowMillis: Long = System.currentTimeMillis(),
        resetMinutes: Int = 0,
        zone: ZoneId = ZoneId.systemDefault()
    ): LocalDate = Instant.ofEpochMilli(nowMillis)
        .atZone(zone)
        .minusMinutes(resetMinutes.coerceIn(0, 1439).toLong())
        .toLocalDate()

    fun dayStartMillis(date: LocalDate, resetMinutes: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).plusMinutes(resetMinutes.coerceIn(0, 1439).toLong()).toInstant().toEpochMilli()

    fun nextResetMillis(nowMillis: Long, resetMinutes: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = accountingDate(nowMillis, resetMinutes, zone)
        return dayStartMillis(date.plusDays(1), resetMinutes, zone)
    }
}
