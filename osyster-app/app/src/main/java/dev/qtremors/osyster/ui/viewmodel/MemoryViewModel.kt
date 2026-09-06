package dev.qtremors.osyster.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.MemoryState
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.settings.OsysterPreferencesManager
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

data class MemoryUiState(
    val memoryState: MemoryState = MemoryState(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L),
    val ramUsedPercent: Float = if (memoryState.ramTotalKb > 0) {
        (memoryState.ramUsedKb.toFloat() / memoryState.ramTotalKb.toFloat()) * 100f
    } else 0f,
    val swapUsedPercent: Float = if (memoryState.swapTotalKb > 0) {
        (memoryState.swapUsedKb.toFloat() / memoryState.swapTotalKb.toFloat()) * 100f
    } else 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle? = null
) : AndroidViewModel(application) {

    private val preferencesManager = OsysterPreferencesManager.getInstance(application)
    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        startStreaming()
    }

    private fun startStreaming() {
        viewModelScope.launch {
            preferencesManager.state
                .map { it.diagnosticsInterval.millis }
                .distinctUntilChanged()
                .flatMapLatest { intervalMs ->
                    SystemMonitor.streamMemory(intervalMs)
                }
                .collectLatest { state ->
                    val ramPercent = if (state.ramTotalKb > 0) {
                        (state.ramUsedKb.toFloat() / state.ramTotalKb.toFloat()) * 100f
                    } else 0f
                    val swapPercent = if (state.swapTotalKb > 0) {
                        (state.swapUsedKb.toFloat() / state.swapTotalKb.toFloat()) * 100f
                    } else 0f
                    _uiState.update {
                        it.copy(
                            memoryState = state,
                            ramUsedPercent = ramPercent,
                            swapUsedPercent = swapPercent
                        )
                    }
                }
        }
    }
}
