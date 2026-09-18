package dev.qtremors.osyster.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScreenUsageHelperTest {

    @Test
    fun formatDuration_zeroOrNegative_returnsZeroMinutes() {
        assertEquals("0m", ScreenUsageHelper.formatDuration(0L))
        assertEquals("0m", ScreenUsageHelper.formatDuration(-500L))
    }

    @Test
    fun formatDuration_secondsOnly_formatsCorrectly() {
        assertEquals("45s", ScreenUsageHelper.formatDuration(45000L))
        assertEquals("1s", ScreenUsageHelper.formatDuration(1000L))
    }

    @Test
    fun formatDuration_minutesOnly_formatsCorrectly() {
        assertEquals("1m", ScreenUsageHelper.formatDuration(60000L))
        assertEquals("15m", ScreenUsageHelper.formatDuration(15 * 60000L))
        assertEquals("59m", ScreenUsageHelper.formatDuration(59 * 60000L))
    }

    @Test
    fun formatDuration_hoursAndMinutes_formatsCorrectly() {
        assertEquals("1h", ScreenUsageHelper.formatDuration(3600000L))
        assertEquals("1h 15m", ScreenUsageHelper.formatDuration(3600000L + 15 * 60000L))
        assertEquals("2h 30m", ScreenUsageHelper.formatDuration(2 * 3600000L + 30 * 60000L))
    }

    @Test
    fun dayBoundaries_alignWithMidnight() {
        val now = System.currentTimeMillis()
        val dayStart = ScreenUsageHelper.getDayStartTime(now)
        val dayEnd = ScreenUsageHelper.getDayEndTime(dayStart)

        val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))

        assertEquals(dayStart + 86400000L, dayEnd)
    }

    @Test
    fun usageResult_initializesWithSafeDefaults() {
        val result = ScreenUsageHelper.UsageResult()
        assertEquals(0L, result.totalGlobalUsage)
        assertTrue(result.appUsageMap.isEmpty())
        assertTrue(result.hourlyUsageMap.isEmpty())
        assertTrue(result.sessionCounts.isEmpty())
    }

    @Test
    fun dailyUsageSummary_preservesFields() {
        val summary = ScreenUsageHelper.DailyUsageSummary(
            dateMillis = 1700000000000L,
            totalTimeMillis = 7200000L,
            isToday = true,
            appUsageMap = mapOf("com.example.app" to 7200000L)
        )
        assertEquals(1700000000000L, summary.dateMillis)
        assertEquals(7200000L, summary.totalTimeMillis)
        assertTrue(summary.isToday)
        assertEquals(1, summary.appUsageMap.size)
        assertEquals(7200000L, summary.appUsageMap["com.example.app"])
    }

    @Test
    fun formatDuration_largeValues_formatsCorrectly() {
        assertEquals("25h 10m", ScreenUsageHelper.formatDuration(25 * 3600000L + 10 * 60000L))
    }

    @Test
    fun clearCache_executesSafely() {
        ScreenUsageHelper.clearCache()
    }
}
