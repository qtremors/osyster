package dev.qtremors.osyster.feature.appstopper

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.AppStopperRoute

sealed interface AppStopperDestination {
    /** Android's App info page, where Force stop lives for apps without privileged access. */
    data class AppInfo(val packageName: String) : AppStopperDestination
}

fun NavGraphBuilder.registerAppStopperRoute(onNavigate: (AppStopperDestination) -> Unit) {
    composable<AppStopperRoute> {
        val viewModel: AppStopperViewModel = hiltViewModel()
        val apps by viewModel.apps.collectAsStateWithLifecycle()
        val picker by viewModel.picker.collectAsStateWithLifecycle()
        // Force stop happens in Settings; re-read the stopped flags when the user comes back.
        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        AppStopperScreen(
            apps = apps,
            picker = picker,
            actions = AppStopperActions(
                onStop = { onNavigate(AppStopperDestination.AppInfo(it)) },
                onRemove = viewModel::remove,
                onOpenPicker = { viewModel.loadPicker() },
                onPickerSystem = { viewModel.loadPicker(includeSystem = it) },
                onAdd = viewModel::add
            )
        )
    }
}
