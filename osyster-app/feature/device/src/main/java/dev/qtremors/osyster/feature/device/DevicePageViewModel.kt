package dev.qtremors.osyster.feature.device

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.DevicePageSource
import dev.qtremors.osyster.core.device.domain.InfoSection
import dev.qtremors.osyster.core.navigation.DevicePageRoute
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DevicePageState {
    data object Loading : DevicePageState
    data object NotBuilt : DevicePageState
    data object UnknownPage : DevicePageState
    data class Ready(val sections: List<InfoSection>) : DevicePageState
}

@HiltViewModel
class DevicePageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    source: DevicePageSource
) : ViewModel() {
    val page: DevicePage? = DevicePage.fromId(savedStateHandle.toRoute<DevicePageRoute>().pageId)

    /**
     * Live pages keep sampling only while the screen collects; a short grace period survives
     * rotation without restarting the GPU probe or the CPU load sampler.
     */
    val state: StateFlow<DevicePageState> = when {
        page == null -> flowOf(DevicePageState.UnknownPage)
        !source.isBuilt(page) -> flowOf(DevicePageState.NotBuilt)
        else -> source.sections(page).map { DevicePageState.Ready(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DevicePageState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 3_000L
    }
}
