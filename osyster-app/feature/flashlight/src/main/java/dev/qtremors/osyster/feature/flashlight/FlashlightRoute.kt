package dev.qtremors.osyster.feature.flashlight

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.navigation.FlashlightRoute
import dev.qtremors.osyster.core.system.domain.TorchController
import dev.qtremors.osyster.core.system.domain.TorchState
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class FlashlightViewModel @Inject constructor(private val torch: TorchController) : ViewModel() {
    val state: StateFlow<TorchState> = torch.state
    fun toggle() = torch.setOn(!torch.state.value.on)
    fun setLevel(level: Int) = torch.setLevel(level)
}

fun NavGraphBuilder.registerFlashlightRoute() {
    composable<FlashlightRoute> {
        val viewModel: FlashlightViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        FlashlightScreen(state = state, onToggle = viewModel::toggle, onLevel = viewModel::setLevel)
    }
}
