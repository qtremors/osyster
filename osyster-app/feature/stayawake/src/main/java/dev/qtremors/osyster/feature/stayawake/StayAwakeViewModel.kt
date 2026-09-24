package dev.qtremors.osyster.feature.stayawake

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.system.domain.SecureSettingsAccess
import dev.qtremors.osyster.core.system.domain.StayAwakeController
import dev.qtremors.osyster.core.system.domain.StayAwakeDurationsMinutes
import dev.qtremors.osyster.core.system.domain.StayAwakeTimer
import dev.qtremors.osyster.core.system.domain.WhileChargingState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class StayAwakeViewModel @Inject constructor(
    private val controller: StayAwakeController,
    secureSettings: SecureSettingsAccess
) : ViewModel() {
    val timer: StateFlow<StayAwakeTimer> = controller.timer
    val adbCommand: String = secureSettings.adbCommand

    private val _whileCharging = MutableStateFlow(controller.whileCharging())
    val whileCharging: StateFlow<WhileChargingState> = _whileCharging.asStateFlow()

    /** The duration selected in the picker; starts at the tile's first step. */
    private val _selectedMinutes = MutableStateFlow(StayAwakeDurationsMinutes.first())
    val selectedMinutes: StateFlow<Int?> = _selectedMinutes.asStateFlow()

    fun select(minutes: Int?) {
        _selectedMinutes.value = minutes
    }

    fun start() = controller.start(_selectedMinutes.value)
    fun stop() = controller.stop()

    /** Re-reads the system setting; it can change in Developer options or after an ADB grant. */
    fun refresh() {
        _whileCharging.value = controller.whileCharging()
    }

    fun setWhileCharging(enabled: Boolean) {
        controller.setWhileCharging(enabled)
        refresh()
    }
}
