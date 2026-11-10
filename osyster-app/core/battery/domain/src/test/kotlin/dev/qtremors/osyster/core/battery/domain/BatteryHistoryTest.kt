package dev.qtremors.osyster.core.battery.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryHistoryTest {
    private fun s(minute: Int, level: Int, charging: Boolean = false, current: Int? = null, temp: Float? = 30f) =
        BatterySample(minute * 60_000L, level, charging, if (charging) PlugType.AC else PlugType.NONE, temp, 4000, current)

    @Test
    fun `charge sessions start and end at the readings around them`() {
        val samples = listOf(
            s(0, 40), s(30, 35),
            s(60, 50, true, 1500, 36f), s(90, 80, true, 1300, 38f),
            s(120, 95), s(150, 92),
            s(180, 93, true, 900)
        )
        val sessions = chargeSessions(samples)
        assertEquals(2, sessions.size)
        val first = sessions[0]
        assertEquals(30 * 60_000L, first.start)
        assertEquals(120 * 60_000L, first.end)
        assertEquals(35, first.fromLevel)
        assertEquals(95, first.toLevel)
        assertEquals(1400, first.averageCurrentMa)
        assertEquals(38f, first.maxTemperatureC)
        assertTrue(!first.ongoing)
        assertTrue(sessions[1].ongoing)
        assertEquals(92, sessions[1].fromLevel)
    }

    @Test
    fun `drain counts only time on battery`() {
        assertNull(drainPerHour(listOf(s(0, 80), s(20, 79))))
        val samples = listOf(s(0, 80), s(60, 76), s(90, 90, true), s(120, 95), s(180, 91))
        // 4% over the first hour and 4% over the last: 8% in 2 hours.
        assertEquals(4.0, drainPerHour(samples)!!, 0.001)
    }

    @Test
    fun `currents come out in milliamps with the charging sign`() {
        assertEquals(1500, normalizeCurrent(-1_500_000, charging = true))
        assertEquals(-420, normalizeCurrent(420_000, charging = false))
        assertEquals(-420, normalizeCurrent(-420, charging = false))
        assertNull(normalizeCurrent(0, charging = true))
        assertNull(normalizeCurrent(Int.MIN_VALUE, charging = false))
    }
}
