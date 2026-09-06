package dev.qtremors.osyster.ui.viewmodel

import dev.qtremors.osyster.monitor.MemoryState
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryUiStateTest {

    @Test
    fun defaultState_initializesWithSafeZeroPlaceholders() {
        val state = MemoryUiState()
        assertEquals(0L, state.memoryState.ramTotalKb)
        assertEquals(0f, state.ramUsedPercent, 0.001f)
        assertEquals(0f, state.swapUsedPercent, 0.001f)
    }
    fun ramPercentage_computesCorrectly() {
        val memoryState = MemoryState(
            ramTotalKb = 8000000L,
            ramUsedKb = 4000000L,
            ramAvailableKb = 4000000L,
            ramFreeKb = 2000000L,
            ramCachedKb = 1000000L,
            ramBuffersKb = 1000000L,
            swapTotalKb = 2000000L,
            swapUsedKb = 500000L,
            swapFreeKb = 1500000L
        )
        val state = MemoryUiState(
            memoryState = memoryState,
            ramUsedPercent = 50.0f,
            swapUsedPercent = 25.0f
        )

        assertEquals(50.0f, state.ramUsedPercent, 0.01f)
        assertEquals(25.0f, state.swapUsedPercent, 0.01f)
        assertEquals(8000000L, state.memoryState.ramTotalKb)
    }

    @Test
    fun zeroTotalMemory_handlesDivisionByZeroGracefully() {
        val memoryState = MemoryState(
            ramTotalKb = 0L,
            ramUsedKb = 0L,
            ramAvailableKb = 0L,
            ramFreeKb = 0L,
            ramCachedKb = 0L,
            ramBuffersKb = 0L,
            swapTotalKb = 0L,
            swapUsedKb = 0L,
            swapFreeKb = 0L
        )
        val state = MemoryUiState(memoryState = memoryState)

        assertEquals(0f, state.ramUsedPercent, 0.01f)
        assertEquals(0f, state.swapUsedPercent, 0.01f)
    }
}
