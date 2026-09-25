package dev.qtremors.osyster.feature.home

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
    data object Search : HomeDestination
}

fun NavGraphBuilder.registerHomeRoute(onNavigate: (HomeDestination) -> Unit) {
    composable<HomeRoute> {
        val viewModel: HomeViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        var sharing by remember { mutableStateOf(false) }
        HomeScreen(
            state = state,
            sharing = sharing,
            actions = HomeActions(
                onOpenPage = { page -> onNavigate(HomeDestination.DevicePage(page.id)) },
                onOpenTool = { tool -> onNavigate(HomeDestination.Tool(tool.id)) },
                onTogglePin = viewModel::togglePin,
                onMove = viewModel::move,
                onHide = viewModel::hide,
                onShow = viewModel::show,
                onSearch = { onNavigate(HomeDestination.Search) },
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
