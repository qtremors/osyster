package dev.qtremors.osyster.feature.appmanager

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.apps.domain.AppDetails
import dev.qtremors.osyster.core.apps.domain.AppSort
import dev.qtremors.osyster.core.apps.domain.AppSummary
import dev.qtremors.osyster.core.apps.domain.InstalledApps
import dev.qtremors.osyster.core.navigation.AppDetailsRoute
import dev.qtremors.osyster.core.navigation.AppManagerRoute
import dev.qtremors.osyster.core.navigation.AppPermissionsRoute
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which apps a list shows. */
enum class AppFilter { USER, SYSTEM, ALL }

/** The installed apps, shared by the app list and the permission explorer. */
@HiltViewModel
class AppManagerViewModel @Inject constructor(private val installed: InstalledApps) : ViewModel() {
    /** Null while the first load runs. */
    private val _apps = MutableStateFlow<List<AppSummary>?>(null)
    val apps: StateFlow<List<AppSummary>?> = _apps.asStateFlow()

    private val _filter = MutableStateFlow(AppFilter.USER)
    val filter: StateFlow<AppFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(AppSort.NAME)
    val sort: StateFlow<AppSort> = _sort.asStateFlow()

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    init {
        viewModelScope.launch { _apps.value = installed.apps() }
    }

    /** Reads again, for coming back after an app was installed, updated, or removed. */
    fun refresh() {
        viewModelScope.launch { _apps.value = installed.apps(fresh = true) }
    }

    fun filter(filter: AppFilter) {
        _filter.value = filter
    }

    fun sort(sort: AppSort) {
        _sort.value = sort
    }

    fun search(text: String) {
        _search.value = text
    }
}

/** One app's page. */
@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val installed: InstalledApps
) : ViewModel() {
    val packageName: String = savedState.toRoute<AppDetailsRoute>().packageName

    /** Null while loading; [Missing] when the app is gone. */
    private val _details = MutableStateFlow<DetailsState>(DetailsState.Loading)
    val details: StateFlow<DetailsState> = _details.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _details.value = installed.details(packageName)?.let(DetailsState::Loaded) ?: DetailsState.Missing
        }
    }
}

sealed interface DetailsState {
    data object Loading : DetailsState
    data object Missing : DetailsState
    data class Loaded(val details: AppDetails) : DetailsState
}

sealed interface AppManagerDestination {
    data object Permissions : AppManagerDestination
    data class Details(val packageName: String) : AppManagerDestination
    data class Usage(val packageName: String) : AppManagerDestination
    data class Open(val packageName: String) : AppManagerDestination
    data class AppInfo(val packageName: String) : AppManagerDestination
    data class Store(val packageName: String) : AppManagerDestination
}

fun NavGraphBuilder.registerAppManagerRoutes(onNavigate: (AppManagerDestination) -> Unit) {
    composable<AppManagerRoute> { AppListScreen(viewModel = hiltViewModel(), onNavigate = onNavigate) }
    composable<AppPermissionsRoute> { PermissionsScreen(viewModel = hiltViewModel(), onNavigate = onNavigate) }
    composable<AppDetailsRoute> { AppDetailsScreen(viewModel = hiltViewModel(), onNavigate = onNavigate) }
}
