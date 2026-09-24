package dev.qtremors.osyster.feature.activitylauncher

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.ActivityLauncherRoute

fun NavGraphBuilder.registerActivityLauncherRoute() {
    composable<ActivityLauncherRoute> {
        val viewModel: ActivityLauncherViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val message by viewModel.message.collectAsStateWithLifecycle()
        ActivityLauncherScreen(
            state = state,
            message = message,
            onSelect = viewModel::select,
            onLaunch = viewModel::launch,
            onPin = viewModel::pin,
            onMessageShown = viewModel::messageShown
        )
    }
}
