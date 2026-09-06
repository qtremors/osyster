package dev.qtremors.osyster.ui.viewmodel

import dev.qtremors.osyster.monitor.AppStopperCounts
import dev.qtremors.osyster.monitor.MemoryState
import dev.qtremors.osyster.monitor.TelemetryResult
import org.junit.Assert.assertEquals
import org.junit.Test

class BentoUiStateTest {

    @Test
    fun defaultState_initializesWithSafePlaceholders() {
        val state = BentoUiState()
        assertEquals(TelemetryResult.Available(0f), state.cpuState.overallUsage)
        assertEquals(0f, state.cpuState.overallUsageOrZero, 0.001f)
        assertEquals(0L, state.memoryState.ramTotalKb)
        assertEquals(0, state.processesCount)
        assertEquals(0, state.batteryState.levelPercentage)
    }

    @Test
    fun ramUsedPercent_computesAccurately() {
        val mem = MemoryState(
            ramTotalKb = 10000000L,
            ramUsedKb = 3000000L,
            ramAvailableKb = 7000000L,
            ramFreeKb = 5000000L,
            ramCachedKb = 1000000L,
            ramBuffersKb = 1000000L,
            swapTotalKb = 0L,
            swapUsedKb = 0L,
            swapFreeKb = 0L
        )
        val state = BentoUiState(memoryState = mem)

        assertEquals(30.0f, state.ramUsedPercent, 0.01f)
    }

    @Test
    fun appStopperCounts_retainsValues() {
        val counts = AppStopperCounts(installedCount = 5, uninstalledCount = 2, totalCount = 7)
        val state = BentoUiState(appStopperCounts = counts, processesCount = 42)

        assertEquals(5, state.appStopperCounts.installedCount)
        assertEquals(2, state.appStopperCounts.uninstalledCount)
        assertEquals(7, state.appStopperCounts.totalCount)
        assertEquals(42, state.processesCount)
    }
}
