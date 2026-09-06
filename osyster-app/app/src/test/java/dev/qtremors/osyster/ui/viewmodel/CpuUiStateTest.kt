package dev.qtremors.osyster.ui.viewmodel

import dev.qtremors.osyster.monitor.CpuCoreState
import dev.qtremors.osyster.monitor.CpuState
import dev.qtremors.osyster.monitor.TelemetryResult
import dev.qtremors.osyster.settings.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CpuUiStateTest {

    @Test
    fun defaultState_hasEmptyHistory() {
        val state = CpuUiState()
        assertTrue(state.cpuHistory.isEmpty())
        assertEquals(TelemetryResult.Available(0f), state.cpuState.overallUsage)
        assertTrue(state.cpuState.coreStates.isEmpty())
        assertEquals(TemperatureUnit.CELSIUS, state.temperatureUnit)
    }

    @Test
    fun historyBuffer_capsAt25Points() {
        var history = emptyList<Float>()
        for (i in 1..40) {
            history = (history + i.toFloat()).takeLast(25)
        }

        assertEquals(25, history.size)
        assertEquals(16f, history.first(), 0.01f)
        assertEquals(40f, history.last(), 0.01f)
    }

    @Test
    fun customCpuState_retainsValues() {
        val cores = listOf(
            CpuCoreState(id = 0, usagePercentage = 40f, currentFreqKhz = 1800000L, maxFreqKhz = 2400000L),
            CpuCoreState(id = 1, usagePercentage = 50f, currentFreqKhz = 2200000L, maxFreqKhz = 2400000L)
        )
        val cpuState = CpuState(
            overallUsage = TelemetryResult.Available(45.5f),
            coreStates = cores,
            cpuTempCelsius = TelemetryResult.Available(38.0f),
            cpuModel = "Octa-core",
            cpuArchitecture = "aarch64"
        )
        val state = CpuUiState(
            cpuState = cpuState,
            cpuHistory = listOf(10f, 20f, 30f),
            temperatureUnit = TemperatureUnit.FAHRENHEIT
        )

        assertEquals(TelemetryResult.Available(45.5f), state.cpuState.overallUsage)
        assertEquals(45.5f, state.cpuState.overallUsageOrZero, 0.01f)
        assertEquals(3, state.cpuHistory.size)
        assertEquals(2, state.cpuState.coreStates.size)
        assertEquals(TelemetryResult.Available(38.0f), state.cpuState.cpuTempCelsius)
        assertEquals(TemperatureUnit.FAHRENHEIT, state.temperatureUnit)
    }

    @Test
    fun restrictedCpuState_identifiesRestrictions() {
        val cpuState = CpuState(
            overallUsage = TelemetryResult.Restricted,
            coreStates = emptyList(),
            cpuTempCelsius = TelemetryResult.Restricted,
            cpuModel = "Cortex-A55",
            cpuArchitecture = "aarch64"
        )
        val state = CpuUiState(cpuState = cpuState)

        assertTrue(state.cpuState.isUsageRestricted)
        assertTrue(state.cpuState.isTempRestricted)
        assertEquals(0f, state.cpuState.overallUsageOrZero, 0.001f)
    }
}
