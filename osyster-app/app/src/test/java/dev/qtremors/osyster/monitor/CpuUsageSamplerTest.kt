package dev.qtremors.osyster.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CpuUsageSamplerTest {
    @Test
    fun interleavedCollectorsKeepIndependentBaselines() {
        val first = CpuUsageSampler()
        val second = CpuUsageSampler()
        assertEquals(0f, first.sample(CpuTimeSnapshot(null, 100, 100)), 0.001f)
        assertEquals(0f, second.sample(CpuTimeSnapshot(null, 150, 150)), 0.001f)
        assertEquals(75f, first.sample(CpuTimeSnapshot(null, 250, 150)), 0.001f)
        assertEquals(100f, second.sample(CpuTimeSnapshot(null, 250, 150)), 0.001f)
    }

    @Test
    fun counterResetAndIndependentCoresDoNotInventLoad() {
        val sampler = CpuUsageSampler()
        sampler.sample(CpuTimeSnapshot(null, 100, 100))
        assertEquals(0f, sampler.sample(CpuTimeSnapshot(null, 10, 10)), 0.001f)
        assertEquals(0f, sampler.sample(CpuTimeSnapshot(0, 100, 100)), 0.001f)
        assertEquals(25f, sampler.sample(CpuTimeSnapshot(0, 125, 175)), 0.001f)
    }

    @Test
    fun rssUsesProvidedKernelPageSize() {
        assertEquals(32L, SystemMonitor.parseRssFromStatm("100 2 0", 16384L))
        assertEquals(8L, SystemMonitor.parseRssFromStatm("100 2 0", 4096L))
    }

    @Test
    fun unrelatedThermalSensorsAreNotCpuTemperatures() {
        assertTrue(SystemMonitor.isCpuThermalZone("CPU-THERM"))
        assertTrue(SystemMonitor.isCpuThermalZone("soc_thermal"))
        assertFalse(SystemMonitor.isCpuThermalZone("battery"))
        assertFalse(SystemMonitor.isCpuThermalZone("skin"))
        assertFalse(SystemMonitor.isCpuThermalZone("tsens_tz_sensor0"))
        assertFalse(SystemMonitor.isCpuThermalZone(""))
    }
}
