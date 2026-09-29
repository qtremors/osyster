package dev.qtremors.osyster.feature.datausage

import dev.qtremors.osyster.core.navigation.AppUsageRoute
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.DataUsageRoute

/** Where Data usage wants to go next. The app shell maps these to system screens. */
sealed interface DataUsageDestination {
    data object UsageAccessSettings : DataUsageDestination
    data class AppInfo(val packageName: String) : DataUsageDestination
    /** One app's own usage page; Screen usage opens the same page. */
    data class AppUsage(val packageName: String) : DataUsageDestination
}

fun NavGraphBuilder.registerDataUsageRoute(onNavigate: (DataUsageDestination) -> Unit) {
    composable<DataUsageRoute> {
        val viewModel: DataUsageViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val live by viewModel.liveRate.collectAsStateWithLifecycle()
        // Coming back from Settings after granting access should load data without a restart.
        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        DataUsageScreen(
            state = state,
            live = live,
            actions = DataUsageActions(
                onPeriod = viewModel::setPeriod,
                onJump = viewModel::jumpTo,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onQuery = viewModel::setQuery,
                onFilter = viewModel::setFilter,
                onToggleShowAll = viewModel::toggleShowAll,
                onGrantAccess = { onNavigate(DataUsageDestination.UsageAccessSettings) },
                onOpenApp = { onNavigate(DataUsageDestination.AppInfo(it)) },
                onOpenUsage = { onNavigate(DataUsageDestination.AppUsage(it)) }
            )
        )
    }
    composable<AppUsageRoute> {
        val viewModel: AppUsageViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LifecycleResumeEffect(Unit) {
            viewModel.load()
            onPauseOrDispose { }
        }
        AppUsageScreen(
            packageName = viewModel.packageName,
            screenTime = viewModel.screenTime,
            state = state,
            onPeriod = viewModel::setPeriod,
            onAppInfo = { onNavigate(DataUsageDestination.AppInfo(viewModel.packageName)) }
        )
    }
}
