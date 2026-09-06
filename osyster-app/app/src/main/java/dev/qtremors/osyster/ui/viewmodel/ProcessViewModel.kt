package dev.qtremors.osyster.ui.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.AppStopperMonitor
import dev.qtremors.osyster.monitor.ProcessInfo
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class ProcessUiState(
    val rawProcesses: List<ProcessInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val selectedProcess: ProcessInfo? = null,
    val processGuidanceTarget: ProcessInfo? = null,
    val showKernelThreads: Boolean = false
) {
    val filteredProcesses: List<ProcessInfo>
        get() = if (searchQuery.isBlank()) {
            rawProcesses
        } else {
            rawProcesses.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.pid.toString() == searchQuery
            }
        }
}

class ProcessViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val preferencesManager = OsysterPreferencesManager.getInstance(application)
    private val initialQuery: String = savedStateHandle[KEY_SEARCH_QUERY] ?: ""
    private val _uiState = MutableStateFlow(ProcessUiState(searchQuery = initialQuery))
    val uiState: StateFlow<ProcessUiState> = _uiState.asStateFlow()

    private val processRequest = LatestRequest(viewModelScope)

    init {
        startPolling()
    }

    fun setSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectProcess(process: ProcessInfo?) {
        _uiState.update { it.copy(selectedProcess = process) }
    }

    fun setGuidanceTarget(process: ProcessInfo?) {
        _uiState.update { it.copy(processGuidanceTarget = process) }
    }

    fun refreshProcesses(initialLoad: Boolean = false) {
        _uiState.update { it.copy(isRefreshing = !initialLoad, isLoading = it.rawProcesses.isEmpty()) }
        val showThreads = preferencesManager.state.value.showKernelThreads
        processRequest.submit(load = {
            withContext(Dispatchers.IO) {
                SystemMonitor.getActiveProcesses(getApplication(), showKernelThreads = showThreads)
            }
        }, publish = { list ->
            _uiState.update { it.copy(rawProcesses = list, isRefreshing = false, isLoading = false) }
        })
    }

    private fun startPolling() {
        viewModelScope.launchWhileSubscribed(_uiState) {
            try {
                preferencesManager.state.map { Pair(it.showKernelThreads, it.diagnosticsInterval.millis) }
                    .distinctUntilChanged().collectLatest { (showThreads, interval) ->
                        _uiState.update { it.copy(showKernelThreads = showThreads) }
                        refreshProcesses(initialLoad = true)
                        while (true) {
                            delay(interval.coerceAtLeast(2000L))
                            if (!_uiState.value.isRefreshing && !_uiState.value.isLoading) refreshProcesses(initialLoad = true)
                        }
                    }
            } finally {
                processRequest.cancel()
            }
        }
    }

    fun killBackgroundProcesses(packageName: String) {
        if (android.os.Build.VERSION.SDK_INT >= 34) return
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.killBackgroundProcesses(packageName)
        selectProcess(null)
        refreshProcesses()
    }

    fun openAppInfo(packageName: String) {
        AppStopperMonitor.openAppInfo(getApplication(), packageName)
        selectProcess(null)
        setGuidanceTarget(null)
    }

    companion object {
        const val KEY_SEARCH_QUERY = "process_search_query"
    }
}
