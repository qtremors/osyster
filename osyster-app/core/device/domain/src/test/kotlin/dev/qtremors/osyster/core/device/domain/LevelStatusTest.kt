package dev.qtremors.osyster.core.device.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelStatusTest {
    @Test
    fun fullnessThresholds() {
        assertEquals(LevelStatus.GOOD, fullnessStatus(0.5f))
        assertEquals(LevelStatus.WARNING, fullnessStatus(0.8f))
        assertEquals(LevelStatus.CRITICAL, fullnessStatus(0.95f))
    }

    @Test
    fun batteryLowIsBadUnlessCharging() {
        assertEquals(LevelStatus.GOOD, batteryStatus(0.8f, charging = false))
        assertEquals(LevelStatus.WARNING, batteryStatus(0.25f, charging = false))
        assertEquals(LevelStatus.CRITICAL, batteryStatus(0.1f, charging = false))
        assertEquals(LevelStatus.GOOD, batteryStatus(0.1f, charging = true))
    }

    @Test
    fun loadAndTemperature() {
        assertEquals(LevelStatus.GOOD, loadStatus(0.3f))
        assertEquals(LevelStatus.WARNING, loadStatus(0.7f))
        assertEquals(LevelStatus.CRITICAL, loadStatus(0.9f))
        assertEquals(LevelStatus.WARNING, batteryTemperatureStatus(42f))
        assertEquals(LevelStatus.CRITICAL, worst(LevelStatus.GOOD, batteryTemperatureStatus(46f)))
    }
}
