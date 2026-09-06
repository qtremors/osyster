package dev.qtremors.osyster.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.AppNetworkUsage
import dev.qtremors.osyster.monitor.NetworkInterfaceFilter
import dev.qtremors.osyster.monitor.NetworkInterval
import dev.qtremors.osyster.monitor.NetworkMonitor
import dev.qtremors.osyster.monitor.NetworkUsageSummary
import dev.qtremors.osyster.monitor.RealtimeSpeed
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class NetworkUiState(
    val hasPermission: Boolean = false,
    val hasPhonePermission: Boolean = false,
    val selectedInterval: NetworkInterval = NetworkInterval.DAY,
    val selectedFilter: NetworkInterfaceFilter = NetworkInterfaceFilter.ALL,
    val targetDateMillis: Long = System.currentTimeMillis(),
    val summary: NetworkUsageSummary = NetworkMonitor.emptySummary(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedBucketIndex: Int? = null,
    val selectedAppDetails: AppNetworkUsage? = null,
    val realtimeSpeed: RealtimeSpeed = RealtimeSpeed(0L, 0L)
) {
    val filteredApps: List<AppNetworkUsage>
        get() = if (searchQuery.isBlank()) {
            summary.apps
        } else {
            summary.apps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

    val dateLabel: String
        get() {
            val cal = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
            return when (selectedInterval) {
                NetworkInterval.DAY -> {
                    val nowCal = Calendar.getInstance()
                    if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
                    ) {
                        "Today, " + SimpleDateFormat("d MMM", Locale.getDefault()).format(cal.time)
                    } else {
                        SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(cal.time)
                    }
                }
                NetworkInterval.WEEK -> {
                    val endCal = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
                    val startCal = Calendar.getInstance().apply {
                        timeInMillis = targetDateMillis
                        add(Calendar.DAY_OF_YEAR, -6)
                    }
                    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                    "${fmt.format(startCal.time)} - ${fmt.format(endCal.time)}"
                }
                NetworkInterval.MONTH -> {
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                }
            }
        }

    val isCurrentPeriod: Boolean
        get() {
            val now = System.currentTimeMillis()
            val calNow = Calendar.getInstance().apply { timeInMillis = now }
            val calTarget = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
            return when (selectedInterval) {
                NetworkInterval.DAY -> calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                    calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
                NetworkInterval.WEEK -> calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                    calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
                NetworkInterval.MONTH -> calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                    calNow.get(Calendar.MONTH) == calTarget.get(Calendar.MONTH)
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val initialInterval = runCatching {
        NetworkInterval.valueOf(savedStateHandle[KEY_INTERVAL] ?: NetworkInterval.DAY.name)
    }.getOrDefault(NetworkInterval.DAY)

    private val initialFilter = runCatching {
        NetworkInterfaceFilter.valueOf(savedStateHandle[KEY_FILTER] ?: NetworkInterfaceFilter.ALL.name)
    }.getOrDefault(NetworkInterfaceFilter.ALL)

    private val initialTargetDate: Long = savedStateHandle[KEY_TARGET_DATE] ?: System.currentTimeMillis()
    private val initialSearch: String = savedStateHandle[KEY_SEARCH_QUERY] ?: ""

    private val _uiState = MutableStateFlow(
        NetworkUiState(
            hasPermission = NetworkMonitor.hasUsageAccess(application),
            hasPhonePermission = NetworkMonitor.hasPhonePermission(application),
            selectedInterval = initialInterval,
            selectedFilter = initialFilter,
            targetDateMillis = initialTargetDate,
            searchQuery = initialSearch
        )
    )
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    private var followsCurrentPeriod = _uiState.value.isCurrentPeriod
    private val usageRequest = LatestRequest(viewModelScope)
    private val preferencesManager = OsysterPreferencesManager.getInstance(application)

    init {
        startRealtimeStream()
        viewModelScope.launchWhileSubscribed(_uiState) {
            try {
                updateCurrentDate()
                checkPermissions()
                loadUsage()
                while (true) {
                    delay(30_000L)
                    updateCurrentDate()
                    checkPermissions()
                    if (_uiState.value.isCurrentPeriod && !_uiState.value.isLoading) loadUsage(clearSummary = false)
                }
            } finally {
                usageRequest.cancel()
            }
        }
    }

    private fun startRealtimeStream() {
        viewModelScope.launchWhileSubscribed(_uiState) {
            preferencesManager.state.map { it.diagnosticsInterval.millis }.distinctUntilChanged()
                .flatMapLatest { NetworkMonitor.streamRealtimeSpeed(it) }.collectLatest { speed ->
                _uiState.update { it.copy(realtimeSpeed = speed) }
            }
        }
    }

    fun checkPermissions() {
        val app = getApplication<Application>()
        val hasUsage = NetworkMonitor.hasUsageAccess(app)
        val hasPhone = NetworkMonitor.hasPhonePermission(app)
        val changed = hasUsage != _uiState.value.hasPermission || hasPhone != _uiState.value.hasPhonePermission
        _uiState.update {
            it.copy(hasPermission = hasUsage, hasPhonePermission = hasPhone)
        }
        if (!hasUsage) {
            usageRequest.cancel()
            _uiState.update { it.copy(summary = NetworkMonitor.emptySummary(), isLoading = false, selectedBucketIndex = null, selectedAppDetails = null) }
        }
        if (changed && hasUsage) {
            loadUsage()
        }
    }

    fun onPhonePermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPhonePermission = granted) }
        loadUsage()
    }

    fun setInterval(interval: NetworkInterval) {
        savedStateHandle[KEY_INTERVAL] = interval.name
        _uiState.update { it.copy(selectedInterval = interval) }
        loadUsage()
    }

    fun setFilter(filter: NetworkInterfaceFilter) {
        savedStateHandle[KEY_FILTER] = filter.name
        _uiState.update { it.copy(selectedFilter = filter) }
        loadUsage()
    }

    fun setTargetDateMillis(millis: Long) {
        savedStateHandle[KEY_TARGET_DATE] = millis
        _uiState.update { it.copy(targetDateMillis = millis) }
        followsCurrentPeriod = _uiState.value.isCurrentPeriod
        loadUsage()
    }

    private fun updateCurrentDate() {
        if (followsCurrentPeriod) {
            val now = System.currentTimeMillis()
            savedStateHandle[KEY_TARGET_DATE] = now
            _uiState.update { it.copy(targetDateMillis = now) }
        }
    }

    fun navigatePreviousDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = _uiState.value.targetDateMillis }
        when (_uiState.value.selectedInterval) {
            NetworkInterval.DAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            NetworkInterval.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
            NetworkInterval.MONTH -> cal.add(Calendar.MONTH, -1)
        }
        setTargetDateMillis(cal.timeInMillis)
    }

    fun navigateNextDate() {
        if (_uiState.value.isCurrentPeriod) return
        val cal = Calendar.getInstance().apply { timeInMillis = _uiState.value.targetDateMillis }
        when (_uiState.value.selectedInterval) {
            NetworkInterval.DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            NetworkInterval.WEEK -> cal.add(Calendar.DAY_OF_YEAR, 7)
            NetworkInterval.MONTH -> cal.add(Calendar.MONTH, 1)
        }
        val target = cal.timeInMillis.coerceAtMost(System.currentTimeMillis())
        setTargetDateMillis(target)
    }

    fun setSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active) }
    }

    fun selectBucket(index: Int?) {
        _uiState.update { it.copy(selectedBucketIndex = index) }
    }

    fun selectAppDetails(app: AppNetworkUsage?) {
        _uiState.update { it.copy(selectedAppDetails = app) }
    }

    fun loadUsage(clearSummary: Boolean = true) {
        val state = _uiState.value
        if (!state.hasPermission) {
            usageRequest.cancel()
            _uiState.update { it.copy(summary = NetworkMonitor.emptySummary(), isLoading = false) }
            return
        }
        _uiState.update {
            it.copy(isLoading = true, selectedBucketIndex = null, selectedAppDetails = null,
                summary = if (clearSummary) NetworkMonitor.emptySummary() else it.summary)
        }
        usageRequest.submit(load = {
            NetworkMonitor.queryNetworkUsage(getApplication(), state.selectedInterval,
                state.selectedFilter, state.targetDateMillis)
        }, publish = { result ->
            _uiState.update { it.copy(summary = result, isLoading = false) }
        })
    }

    fun openUsageAccessSettings() {
        NetworkMonitor.openUsageAccessSettings(getApplication())
    }

    fun openAppDetailsSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { getApplication<Application>().startActivity(intent) }
        selectAppDetails(null)
    }

    companion object {
        const val KEY_INTERVAL = "network_interval"
        const val KEY_FILTER = "network_filter"
        const val KEY_TARGET_DATE = "network_target_date"
        const val KEY_SEARCH_QUERY = "network_search_query"
    }
}
