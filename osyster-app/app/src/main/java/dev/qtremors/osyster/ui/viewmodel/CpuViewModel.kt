package dev.qtremors.osyster.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.CpuState
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.monitor.TelemetryResult
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.settings.TemperatureUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpuUiState(
    val cpuState: CpuState = CpuState(),
    val cpuHistory: List<Float> = emptyList(),
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
)

@OptIn(ExperimentalCoroutinesApi::class)
class CpuViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle? = null
) : AndroidViewModel(application) {

    private val preferencesManager = OsysterPreferencesManager.getInstance(application)
    private val _uiState = MutableStateFlow(CpuUiState())
    val uiState: StateFlow<CpuUiState> = _uiState.asStateFlow()

    init {
        startStreaming()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.state
                .map { it.temperatureUnit }
                .distinctUntilChanged()
                .collectLatest { unit ->
                    _uiState.update { it.copy(temperatureUnit = unit) }
                }
        }
    }

    private fun startStreaming() {
        viewModelScope.launch {
            preferencesManager.state
                .map { it.diagnosticsInterval.millis }
                .distinctUntilChanged()
                .flatMapLatest { intervalMs ->
                    SystemMonitor.streamCpu(intervalMs)
                }
                .collectLatest { state ->
                    _uiState.update { current ->
                        val newHistory = when (val usage = state.overallUsage) {
                            is TelemetryResult.Available -> (current.cpuHistory + usage.value).takeLast(25)
                            is TelemetryResult.Restricted -> current.cpuHistory
                        }
                        current.copy(
                            cpuState = state,
                            cpuHistory = newHistory
                        )
                    }
                }
        }
    }
}
