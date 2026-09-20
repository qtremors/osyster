package dev.qtremors.osyster.feature.home

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.HomeRoute

/** Where Home wants to go next. The app shell maps these to routes. */
sealed interface HomeDestination {
    data class DevicePage(val pageId: String) : HomeDestination
}

fun NavGraphBuilder.registerHomeRoute(onNavigate: (HomeDestination) -> Unit) {
    composable<HomeRoute> {
        val viewModel: HomeViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        HomeScreen(
            state = state,
            onOpenPage = { page -> onNavigate(HomeDestination.DevicePage(page.id)) }
        )
    }
}
