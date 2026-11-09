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
    data class Launch(val packageName: String) : AppStopperDestination
    /** The app's store listing; for uninstalled apps this is how to get them back. */
    data class Store(val packageName: String) : AppStopperDestination
}

fun NavGraphBuilder.registerAppStopperRoute(onNavigate: (AppStopperDestination) -> Unit) {
    composable<AppStopperRoute> {
        val viewModel: AppStopperViewModel = hiltViewModel()
        val apps by viewModel.apps.collectAsStateWithLifecycle()
        val picker by viewModel.picker.collectAsStateWithLifecycle()
        val oneTap by viewModel.oneTap.collectAsStateWithLifecycle()
        val columns by viewModel.columns.collectAsStateWithLifecycle()
        // Without Shizuku or root, force stop happens in App info; re-read the flags on return.
        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        AppStopperScreen(
            ui = AppStopperUi(apps = apps, picker = picker, privileged = oneTap, columns = columns),
            actions = AppStopperActions(
                onStop = { pkg -> viewModel.stop(pkg) { onNavigate(AppStopperDestination.AppInfo(pkg)) } },
                onStopAll = viewModel::stopAll,
                onLaunch = { onNavigate(AppStopperDestination.Launch(it)) },
                onAppInfo = { onNavigate(AppStopperDestination.AppInfo(it)) },
                onStore = { onNavigate(AppStopperDestination.Store(it)) },
                onRemove = viewModel::remove,
                onFreeze = viewModel::setFrozen,
                onColumns = viewModel::setColumns,
                onOpenPicker = { viewModel.loadPicker() },
                onPickerSystem = { viewModel.loadPicker(includeSystem = it) },
                onAdd = viewModel::add
            )
        )
    }
}
