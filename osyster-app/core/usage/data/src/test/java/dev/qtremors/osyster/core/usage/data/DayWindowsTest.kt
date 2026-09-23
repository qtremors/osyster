package dev.qtremors.osyster.core.usage.data

import dev.qtremors.osyster.core.usage.domain.DataPeriod
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class DayWindowsTest {
    private val zone: ZoneId = ZoneOffset.ofHoursMinutes(5, 30)
    private fun at(y: Int, m: Int, d: Int, h: Int = 0) =
        LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun startOfDayIsLocalMidnight() {
        assertEquals(at(2026, 9, 22), DayWindows.startOfDay(at(2026, 9, 22, 21), zone))
    }

    @Test
    fun periodsCoverTheRightDays() {
        val now = at(2026, 9, 22, 21)
        assertEquals(listOf(at(2026, 9, 22)), DayWindows.daysOf(DataPeriod.TODAY, now, zone))
        val week = DayWindows.daysOf(DataPeriod.WEEK, now, zone)
        assertEquals(7, week.size)
        assertEquals(at(2026, 9, 16), week.first())
        val month = DayWindows.daysOf(DataPeriod.MONTH, now, zone)
        assertEquals(22, month.size)
        assertEquals(at(2026, 9, 1), month.first())
    }

    @Test
    fun lastDaysEndWithToday() {
        val days = DayWindows.lastDays(3, at(2026, 10, 1, 9), zone)
        assertEquals(listOf(at(2026, 9, 29), at(2026, 9, 30), at(2026, 10, 1)), days)
    }
}
