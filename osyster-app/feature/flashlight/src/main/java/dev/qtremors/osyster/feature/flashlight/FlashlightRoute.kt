package dev.qtremors.osyster.feature.flashlight

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.navigation.FlashlightRoute
import dev.qtremors.osyster.core.system.domain.FlashlightPulse
import dev.qtremors.osyster.core.system.domain.TorchController
import dev.qtremors.osyster.core.system.domain.TorchState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class FlashlightViewModel @Inject constructor(
    private val torch: TorchController,
    private val pulse: FlashlightPulse
) : ViewModel() {
    val state: StateFlow<TorchState> = torch.state
    val pulseEnabled: StateFlow<Boolean> = pulse.enabled
    private val _hasAccess = MutableStateFlow(pulse.hasNotificationAccess())
    val hasNotificationAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    fun toggle() = torch.setOn(!torch.state.value.on)
    fun setLevel(level: Int) = torch.setLevel(level)
    fun setPulse(enabled: Boolean) = pulse.setEnabled(enabled)
    fun refresh() {
        _hasAccess.value = pulse.hasNotificationAccess()
    }
}

sealed interface FlashlightDestination {
    data object NotificationAccess : FlashlightDestination
}

fun NavGraphBuilder.registerFlashlightRoute(onNavigate: (FlashlightDestination) -> Unit) {
    composable<FlashlightRoute> {
        val viewModel: FlashlightViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val pulseEnabled by viewModel.pulseEnabled.collectAsStateWithLifecycle()
        val hasAccess by viewModel.hasNotificationAccess.collectAsStateWithLifecycle()
        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        FlashlightScreen(
            state = state,
            pulse = PulseUi(enabled = pulseEnabled, hasAccess = hasAccess),
            onToggle = viewModel::toggle,
            onLevel = viewModel::setLevel,
            onPulse = viewModel::setPulse,
            onGrantAccess = { onNavigate(FlashlightDestination.NotificationAccess) }
        )
    }
}
