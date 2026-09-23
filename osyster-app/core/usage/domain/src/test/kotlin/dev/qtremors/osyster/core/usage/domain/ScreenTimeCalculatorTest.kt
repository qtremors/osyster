package dev.qtremors.osyster.core.usage.domain

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenTimeCalculatorTest {
    private val calculator = ScreenTimeCalculator(zone = ZoneOffset.UTC)
    private val hour = 3_600_000L
    private val minute = 60_000L

    private fun fg(pkg: String, t: Long) = UsageEvent(UsageEventKind.APP_FOREGROUND, pkg, t)
    private fun bg(pkg: String, t: Long) = UsageEvent(UsageEventKind.APP_BACKGROUND, pkg, t)
    private fun off(t: Long) = UsageEvent(UsageEventKind.SCREEN_OFF, null, t)
    private fun on(t: Long) = UsageEvent(UsageEventKind.SCREEN_ON, null, t)

    @Test
    fun foregroundUntilBackgroundCounts() {
        val result = calculator.calculate(sequenceOf(fg("a", 10 * minute), bg("a", 40 * minute)), 0, 24 * hour)
        assertEquals(30 * minute, result.perApp["a"])
        assertEquals(1, result.opens["a"])
        assertEquals(30 * minute, result.total)
    }

    @Test
    fun switchingAppsEndsThePreviousSegment() {
        val result = calculator.calculate(sequenceOf(fg("a", 0), fg("b", 5 * minute), bg("b", 8 * minute)), 0, hour)
        assertEquals(5 * minute, result.perApp["a"])
        assertEquals(3 * minute, result.perApp["b"])
    }

    @Test
    fun screenOffStopsTimeAndIgnoresAppsUntilScreenOn() {
        val events = sequenceOf(fg("a", 0), off(10 * minute), fg("b", 12 * minute), on(20 * minute), fg("b", 21 * minute), bg("b", 25 * minute))
        val result = calculator.calculate(events, 0, hour)
        assertEquals(10 * minute, result.perApp["a"])
        assertEquals(4 * minute, result.perApp["b"])
    }

    @Test
    fun segmentsAreClippedToTheWindow() {
        val result = calculator.calculate(sequenceOf(fg("a", -30 * minute), bg("a", 30 * minute)), 0, hour)
        assertEquals(30 * minute, result.perApp["a"])
    }

    @Test
    fun openSessionRunsToTheEndOfTheWindow() {
        val result = calculator.calculate(sequenceOf(fg("a", 50 * minute)), 0, hour)
        assertEquals(10 * minute, result.perApp["a"])
    }

    @Test
    fun timeIsSplitAcrossClockHours() {
        val result = calculator.calculate(sequenceOf(fg("a", 9 * hour + 50 * minute), bg("a", 10 * hour + 20 * minute)), 0, 24 * hour)
        assertEquals(10 * minute, result.hourly[9])
        assertEquals(20 * minute, result.hourly[10])
    }

    @Test
    fun blipsAreNoiseAndShortVisitsAreNotOpens() {
        val result = calculator.calculate(sequenceOf(fg("a", 0), bg("a", 200), fg("b", hour), bg("b", hour + 1_000)), 0, 2 * hour)
        assertNull(result.perApp["a"])
        assertEquals(1_000L, result.perApp["b"])
        assertNull(result.opens["b"])
    }
}
