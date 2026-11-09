package dev.qtremors.osyster.feature.home

import dev.qtremors.osyster.feature.home.search.SearchTarget
import dev.qtremors.osyster.feature.home.search.SearchViewModel
import dev.qtremors.osyster.feature.home.search.SettingsPlace
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.HomeRoute
import kotlinx.coroutines.launch

/** Where Home wants to go next. The app shell maps these to routes or a share sheet. */
sealed interface HomeDestination {
    data class DevicePage(val pageId: String) : HomeDestination
    data class Tool(val toolId: String) : HomeDestination
    data class ShareReport(val text: String) : HomeDestination
    data object Settings : HomeDestination
    data object About : HomeDestination
    data object Privacy : HomeDestination
    data object Licenses : HomeDestination
    data object UsageAccess : HomeDestination
}

fun NavGraphBuilder.registerHomeRoute(onNavigate: (HomeDestination) -> Unit) {
    composable<HomeRoute> {
        val viewModel: HomeViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        var sharing by remember { mutableStateOf(false) }
        val searchViewModel: SearchViewModel = hiltViewModel()
        val query by searchViewModel.query.collectAsStateWithLifecycle()
        val facts by searchViewModel.facts.collectAsStateWithLifecycle()
        HomeScreen(
            state = state,
            sharing = sharing,
            search = HomeSearch(query, facts, searchViewModel::setQuery) { target -> onNavigate(target.toDestination()) },
            actions = HomeActions(
                onOpenPage = { page -> onNavigate(HomeDestination.DevicePage(page.id)) },
                onOpenTool = { tool -> onNavigate(HomeDestination.Tool(tool.id)) },
                onTogglePin = viewModel::togglePin,
                onMove = viewModel::move,
                onHide = viewModel::hide,
                onShow = viewModel::show,
                onGrantUsage = { onNavigate(HomeDestination.UsageAccess) },
                edit = HomeEditActions(
                    onReorderPinned = viewModel::reorderPinned,
                    onReorderGroup = viewModel::reorderGroup,
                    onVisible = viewModel::setVisible,
                    onReorderTiles = viewModel::reorderTiles,
                    onTileVisible = viewModel::setTileVisible
                ),
                onShareReport = {
                    if (!sharing) {
                        sharing = true
                        scope.launch {
                            try {
                                onNavigate(HomeDestination.ShareReport(viewModel.buildReport()))
                            } finally {
                                sharing = false
                            }
                        }
                    }
                }
            )
        )
    }
}

private fun SearchTarget.toDestination(): HomeDestination = when (this) {
    is SearchTarget.Page -> HomeDestination.DevicePage(pageId)
    is SearchTarget.Tool -> HomeDestination.Tool(toolId)
    is SearchTarget.Setting -> when (place) {
        SettingsPlace.SETTINGS -> HomeDestination.Settings
        SettingsPlace.ABOUT -> HomeDestination.About
        SettingsPlace.PRIVACY -> HomeDestination.Privacy
        SettingsPlace.LICENSES -> HomeDestination.Licenses
    }
}
