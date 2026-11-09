package dev.qtremors.osyster.feature.device

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.DevicePageRoute
import dev.qtremors.osyster.core.navigation.ScreenTesterRoute

/** Where a device page wants to go next. The app shell maps these to routes or system screens. */
sealed interface DeviceDestination {
    /** Android's own battery usage screen; apps have no honest per-app drain data of their own. */
    data object SystemBatteryUsage : DeviceDestination
    data object ScreenTester : DeviceDestination
    /** Osyster's App info page, for permissions the user declined with "Don't ask again". */
    data object AppPermissionSettings : DeviceDestination
}

fun NavGraphBuilder.registerDevicePageRoute(onNavigate: (DeviceDestination) -> Unit) {
    composable<DevicePageRoute> {
        val viewModel: DevicePageViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        DevicePageScreen(
            page = viewModel.page,
            state = state,
            readings = viewModel::sensorReadings,
            onNavigate = onNavigate
        )
    }
    composable<ScreenTesterRoute> {
        ScreenTesterScreen()
    }
}
