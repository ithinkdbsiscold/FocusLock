package com.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AccountingClockTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test fun midnightUsesNewLocalAccountingDate() {
        val start = AccountingClock.dayStartMillis(LocalDate.of(2026, 11, 18), 0, zone)
        assertEquals(LocalDate.of(2026, 11, 18), AccountingClock.accountingDate(start, 0, zone))
    }

    @Test fun customResetKeepsPreviousDateBeforeReset() {
        val atOneAm = AccountingClock.dayStartMillis(LocalDate.of(2026, 11, 18), 60, zone)
        assertEquals(LocalDate.of(2026, 11, 17), AccountingClock.accountingDate(atOneAm - 1, 60, zone))
        assertEquals(LocalDate.of(2026, 11, 18), AccountingClock.accountingDate(atOneAm, 60, zone))
    }

    @Test fun nextResetCrossesToFollowingAccountingDay() {
        val noon = AccountingClock.dayStartMillis(LocalDate.of(2026, 11, 18), 0, zone) + 12 * 60 * 60 * 1000L
        val expected = AccountingClock.dayStartMillis(LocalDate.of(2026, 11, 19), 0, zone)
        assertEquals(expected, AccountingClock.nextResetMillis(noon, 0, zone))
    }

    @Test fun timezoneProducesZoneLocalBoundary() {
        val utc = ZoneId.of("UTC")
        val india = ZoneId.of("Asia/Kolkata")
        val date = LocalDate.of(2026, 11, 18)
        assertEquals(-19_800_000L, AccountingClock.dayStartMillis(date, 0, india) - AccountingClock.dayStartMillis(date, 0, utc))
    }
}
