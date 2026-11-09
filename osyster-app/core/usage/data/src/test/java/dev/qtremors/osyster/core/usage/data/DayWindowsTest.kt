package dev.qtremors.osyster.core.usage.data

import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.maxOffset
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayWindowsTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = ZonedDateTime.of(2026, 9, 15, 18, 30, 0, 0, zone).toInstant().toEpochMilli()

    private fun day(millis: Long): LocalDate = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    @Test
    fun todayIsOneDay() {
        val days = DayWindows.daysOf(DataPeriod.TODAY, now, zone = zone)
        assertEquals(listOf(LocalDate.of(2026, 9, 15)), days.map(::day))
    }

    @Test
    fun yesterdayIsOneStepBack() {
        val days = DayWindows.daysOf(DataPeriod.TODAY, now, offset = 1, zone = zone)
        assertEquals(listOf(LocalDate.of(2026, 9, 14)), days.map(::day))
    }

    @Test
    fun weeksStepBackSevenDaysAtATime() {
        val current = DayWindows.daysOf(DataPeriod.WEEK, now, zone = zone).map(::day)
        val previous = DayWindows.daysOf(DataPeriod.WEEK, now, offset = 1, zone = zone).map(::day)
        assertEquals(7, current.size)
        assertEquals(LocalDate.of(2026, 9, 15), current.last())
        assertEquals(LocalDate.of(2026, 9, 8), previous.last())
        assertEquals(current.first().minusDays(1), previous.last())
    }

    @Test
    fun pastMonthsAreWholeCalendarMonths() {
        val august = DayWindows.daysOf(DataPeriod.MONTH, now, offset = 1, zone = zone).map(::day)
        assertEquals(LocalDate.of(2026, 8, 1), august.first())
        assertEquals(LocalDate.of(2026, 8, 31), august.last())
        val current = DayWindows.daysOf(DataPeriod.MONTH, now, zone = zone).map(::day)
        assertEquals(LocalDate.of(2026, 9, 15), current.last())
    }

    @Test
    fun aNormalDayHasTwentyFourHours() {
        val start = DayWindows.startOfDay(now, zone)
        val hours = DayWindows.hoursOf(start, zone)
        assertEquals(24, hours.size)
        assertEquals(start, hours.first())
        assertEquals(3_600_000L, hours[1] - hours[0])
    }

    @Test
    fun clockChangeDaysHaveTheirRealHourCount() {
        val berlin = ZoneId.of("Europe/Berlin")
        val springForward = LocalDate.of(2026, 3, 29).atStartOfDay(berlin).toInstant().toEpochMilli()
        assertEquals(23, DayWindows.hoursOf(springForward, berlin).size)
    }

    @Test
    fun offsetsAreBounded() {
        assertTrue(maxOffset(DataPeriod.TODAY) > maxOffset(DataPeriod.MONTH))
    }
}
