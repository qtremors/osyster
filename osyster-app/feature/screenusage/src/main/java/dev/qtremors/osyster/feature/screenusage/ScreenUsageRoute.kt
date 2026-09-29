package dev.qtremors.osyster.feature.screenusage

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.ScreenUsageRoute

sealed interface ScreenUsageDestination {
    data object UsageAccessSettings : ScreenUsageDestination
    data class AppInfo(val packageName: String) : ScreenUsageDestination
    data class AppUsage(val packageName: String) : ScreenUsageDestination
}

fun NavGraphBuilder.registerScreenUsageRoute(onNavigate: (ScreenUsageDestination) -> Unit) {
    composable<ScreenUsageRoute> {
        val viewModel: ScreenUsageViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        ScreenUsageScreen(
            state = state,
            onSelectDay = viewModel::select,
            onToggleShowAll = viewModel::toggleShowAll,
            onGrantAccess = { onNavigate(ScreenUsageDestination.UsageAccessSettings) },
            onOpenApp = { onNavigate(ScreenUsageDestination.AppUsage(it)) }
        )
    }
}
