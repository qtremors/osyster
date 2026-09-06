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
import kotlinx.coroutines.launch
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

    init {
        refreshProcesses(initialLoad = true)
        observePreferences()
        startPolling()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.state
                .map { it.showKernelThreads }
                .distinctUntilChanged()
                .collectLatest { showThreads ->
                    _uiState.update { it.copy(showKernelThreads = showThreads) }
                    refreshProcesses()
                }
        }
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
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = !initialLoad,
                    isLoading = initialLoad && it.rawProcesses.isEmpty()
                )
            }
            val showThreads = preferencesManager.state.value.showKernelThreads
            val list = withContext(Dispatchers.IO) {
                SystemMonitor.getActiveProcesses(
                    context = getApplication(),
                    showKernelThreads = showThreads
                )
            }
            _uiState.update {
                it.copy(
                    rawProcesses = list,
                    isRefreshing = false,
                    isLoading = false
                )
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(5000L)
                if (!_uiState.value.isRefreshing) {
                    val showThreads = preferencesManager.state.value.showKernelThreads
                    val list = withContext(Dispatchers.IO) {
                        SystemMonitor.getActiveProcesses(
                            context = getApplication(),
                            showKernelThreads = showThreads
                        )
                    }
                    _uiState.update { it.copy(rawProcesses = list) }
                }
            }
        }
    }

    fun killBackgroundProcesses(packageName: String) {
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
