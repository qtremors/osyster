package dev.qtremors.osyster.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.AppStopperMonitor
import dev.qtremors.osyster.monitor.InstalledAppItem
import dev.qtremors.osyster.monitor.ManagedAppInfo
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppStopperUiState(
    val managedApps: List<ManagedAppInfo> = emptyList(),
    val installedApps: List<InstalledAppItem> = emptyList(),
    val isLoading: Boolean = true,
    val isInstalledLoading: Boolean = false,
    val searchQuery: String = "",
    val includeSystemApps: Boolean = false,
    val selectedAppForOptions: ManagedAppInfo? = null,
    val showAddSheet: Boolean = false,
    val showGridMenu: Boolean = false
) {
    val filteredApps: List<ManagedAppInfo>
        get() = if (searchQuery.isBlank()) {
            managedApps
        } else {
            managedApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

    val uninstalledCount: Int
        get() = managedApps.count { it.isUninstalled }

    val stoppedCount: Int
        get() = managedApps.count { it.isStopped && !it.isUninstalled }

    val activeCount: Int
        get() = managedApps.count { !it.isStopped && !it.isUninstalled }
}

class AppStopperViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val initialSearch: String = savedStateHandle[KEY_SEARCH_QUERY] ?: ""
    private val _uiState = MutableStateFlow(AppStopperUiState(searchQuery = initialSearch))
    val uiState: StateFlow<AppStopperUiState> = _uiState.asStateFlow()

    fun setSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedAppForOptions(app: ManagedAppInfo?) {
        _uiState.update { it.copy(selectedAppForOptions = app) }
    }

    fun setShowAddSheet(show: Boolean) {
        _uiState.update { it.copy(showAddSheet = show) }
    }

    fun setShowGridMenu(show: Boolean) {
        _uiState.update { it.copy(showGridMenu = show) }
    }

    fun loadManagedApps(packages: Set<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.managedApps.isEmpty()) }
            val list = withContext(Dispatchers.IO) {
                AppStopperMonitor.loadManagedApps(getApplication(), packages)
            }
            _uiState.update {
                it.copy(
                    managedApps = list,
                    isLoading = false
                )
            }
        }
    }

    fun loadInstalledApps(alreadyManaged: Set<String>, includeSystem: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalledLoading = true, includeSystemApps = includeSystem) }
            val list = withContext(Dispatchers.IO) {
                AppStopperMonitor.loadAllInstalledApps(
                    context = getApplication(),
                    excludePackages = alreadyManaged,
                    includeSystemApps = includeSystem
                )
            }
            _uiState.update {
                it.copy(
                    installedApps = list,
                    isInstalledLoading = false
                )
            }
        }
    }

    fun openAppInfo(packageName: String) {
        AppStopperMonitor.openAppInfo(getApplication(), packageName)
        setSelectedAppForOptions(null)
    }

    fun launchApp(packageName: String) {
        AppStopperMonitor.launchApp(getApplication(), packageName)
        setSelectedAppForOptions(null)
    }

    fun openInPlayStore(packageName: String) {
        AppStopperMonitor.openInPlayStore(getApplication(), packageName)
        setSelectedAppForOptions(null)
    }

    fun removeManagedApp(packageName: String, manager: OsysterPreferencesManager) {
        setSelectedAppForOptions(null)
        manager.removeManagedStopPackage(packageName)
        AppStopperMonitor.removeAppLabel(getApplication(), packageName)
        _uiState.update { current ->
            current.copy(managedApps = current.managedApps.filter { it.packageName != packageName })
        }
    }

    fun addManagedPackages(packages: Set<String>, manager: OsysterPreferencesManager) {
        val current = manager.state.value.managedStopPackages
        manager.setManagedStopPackages(current + packages)
        setShowAddSheet(false)
    }

    companion object {
        const val KEY_SEARCH_QUERY = "app_stopper_search_query"
    }
}
