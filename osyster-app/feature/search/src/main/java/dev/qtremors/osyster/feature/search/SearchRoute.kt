package dev.qtremors.osyster.feature.search

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.SearchRoute

/** Where a search result leads. The app shell maps these to routes. */
sealed interface SearchDestination {
    data class DevicePage(val pageId: String) : SearchDestination
    data class Tool(val toolId: String) : SearchDestination
    data object Settings : SearchDestination
    data object About : SearchDestination
    data object Privacy : SearchDestination
    data object Licenses : SearchDestination
}

fun NavGraphBuilder.registerSearchRoute(onNavigate: (SearchDestination) -> Unit) {
    composable<SearchRoute> {
        val viewModel: SearchViewModel = hiltViewModel()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val facts by viewModel.facts.collectAsStateWithLifecycle()
        SearchScreen(
            query = query,
            facts = facts,
            onQueryChange = viewModel::setQuery,
            onOpen = { target -> onNavigate(target.toDestination()) }
        )
    }
}

private fun SearchTarget.toDestination(): SearchDestination = when (this) {
    is SearchTarget.Page -> SearchDestination.DevicePage(pageId)
    is SearchTarget.Tool -> SearchDestination.Tool(toolId)
    is SearchTarget.Setting -> when (place) {
        SettingsPlace.SETTINGS -> SearchDestination.Settings
        SettingsPlace.ABOUT -> SearchDestination.About
        SettingsPlace.PRIVACY -> SearchDestination.Privacy
        SettingsPlace.LICENSES -> SearchDestination.Licenses
    }
}
