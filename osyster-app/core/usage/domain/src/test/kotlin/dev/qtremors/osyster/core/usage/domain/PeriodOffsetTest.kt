package dev.qtremors.osyster.core.usage.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodOffsetTest {
    private val today = LocalDate.of(2026, 9, 15)

    @Test
    fun daysBackForToday() {
        assertEquals(0, offsetFor(DataPeriod.TODAY, today, today))
        assertEquals(3, offsetFor(DataPeriod.TODAY, today, LocalDate.of(2026, 9, 12)))
    }

    @Test
    fun wholeWeeksBackForSevenDays() {
        assertEquals(0, offsetFor(DataPeriod.WEEK, today, LocalDate.of(2026, 9, 9)))
        assertEquals(1, offsetFor(DataPeriod.WEEK, today, LocalDate.of(2026, 9, 8)))
        assertEquals(1, offsetFor(DataPeriod.WEEK, today, LocalDate.of(2026, 9, 2)))
    }

    @Test
    fun calendarMonthsBackForThisMonth() {
        assertEquals(0, offsetFor(DataPeriod.MONTH, today, LocalDate.of(2026, 9, 1)))
        assertEquals(1, offsetFor(DataPeriod.MONTH, today, LocalDate.of(2026, 8, 31)))
        assertEquals(8, offsetFor(DataPeriod.MONTH, today, LocalDate.of(2026, 1, 20)))
    }

    @Test
    fun futureAndAncientDatesAreClamped() {
        assertEquals(0, offsetFor(DataPeriod.TODAY, today, today.plusDays(5)))
        assertEquals(maxOffset(DataPeriod.MONTH), offsetFor(DataPeriod.MONTH, today, LocalDate.of(2020, 1, 1)))
    }

    @Test
    fun everydayChoiceSitsInTheMiddle() {
        assertEquals(DataPeriod.TODAY, PeriodButtons[1])
        assertEquals(NetworkFilter.ALL, NetworkButtons[1])
    }
}
