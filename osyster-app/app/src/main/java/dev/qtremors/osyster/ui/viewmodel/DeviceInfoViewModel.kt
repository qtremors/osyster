package dev.qtremors.osyster.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.settings.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceInfoUiState(
    val batteryState: BatteryState = BatteryState(0, 0f, "", "", 0, ""),
    val manufacturer: String = Build.MANUFACTURER ?: "unknown",
    val model: String = Build.MODEL ?: "unknown",
    val board: String = Build.BOARD ?: "unknown",
    val hardware: String = Build.HARDWARE ?: "unknown",
    val supportedAbis: String = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "unknown",
    val androidVersion: String = Build.VERSION.RELEASE ?: "unknown",
    val apiLevel: String = Build.VERSION.SDK_INT.toString(),
    val securityPatch: String = Build.VERSION.SECURITY_PATCH ?: "unknown",
    val bootloader: String = Build.BOOTLOADER ?: "unknown",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
)

class DeviceInfoViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle? = null
) : AndroidViewModel(application) {

    private val preferencesManager = OsysterPreferencesManager.getInstance(application)
    private val _uiState = MutableStateFlow(DeviceInfoUiState())
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()

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
            SystemMonitor.streamBattery(getApplication(), 3000L).collectLatest { state ->
                _uiState.update { it.copy(batteryState = state) }
            }
        }
    }
}
