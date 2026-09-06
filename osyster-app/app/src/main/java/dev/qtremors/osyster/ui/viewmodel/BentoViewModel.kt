package dev.qtremors.osyster.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.AppStopperCounts
import dev.qtremors.osyster.monitor.AppStopperMonitor
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.CpuState
import dev.qtremors.osyster.monitor.MemoryState
import dev.qtremors.osyster.monitor.NetworkInterfaceFilter
import dev.qtremors.osyster.monitor.NetworkInterval
import dev.qtremors.osyster.monitor.NetworkMonitor
import dev.qtremors.osyster.monitor.RealtimeSpeed
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BentoUiState(
    val cpuState: CpuState = CpuState(),
    val memoryState: MemoryState = MemoryState(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L),
    val batteryState: BatteryState = BatteryState(0, 0f, "", "", 0, ""),
    val processesCount: Int = 0,
    val realtimeSpeed: RealtimeSpeed = RealtimeSpeed(0L, 0L),
    val activeNetworkType: NetworkInterfaceFilter = NetworkInterfaceFilter.ALL,
    val todayNetworkTotal: String = "",
    val todayNetworkLabel: String = "Today",
    val appStopperCounts: AppStopperCounts = AppStopperCounts(0, 0, 0)
) {
    val ramUsedPercent: Float
        get() = if (memoryState.ramTotalKb > 0) {
            (memoryState.ramUsedKb.toFloat() / memoryState.ramTotalKb.toFloat()) * 100f
        } else 0f
}

@OptIn(ExperimentalCoroutinesApi::class)
class BentoViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle? = null
) : AndroidViewModel(application) {

    private val preferencesManager = OsysterPreferencesManager.getInstance(application)
    private val _uiState = MutableStateFlow(BentoUiState())
    val uiState: StateFlow<BentoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val battery = SystemMonitor.getBatteryState(app)
            val netType = NetworkMonitor.getActiveNetworkType(app)
            _uiState.update { it.copy(batteryState = battery, activeNetworkType = netType) }
        }
        startTelemetryStreams()
        refreshNetworkSummary()
        startProcessCountPolling()
    }

    fun refreshAppStopperCounts(managedPackages: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val counts = AppStopperMonitor.getManagedAppCounts(getApplication(), managedPackages)
            _uiState.update { it.copy(appStopperCounts = counts) }
        }
    }

    private fun startTelemetryStreams() {
        viewModelScope.launch {
            preferencesManager.state
                .map { it.diagnosticsInterval.millis }
                .distinctUntilChanged()
                .flatMapLatest { intervalMs ->
                    SystemMonitor.streamCpu(intervalMs)
                }
                .collectLatest { cpu ->
                    _uiState.update { it.copy(cpuState = cpu) }
                }
        }
        viewModelScope.launch {
            preferencesManager.state
                .map { it.diagnosticsInterval.millis }
                .distinctUntilChanged()
                .flatMapLatest { intervalMs ->
                    SystemMonitor.streamMemory(intervalMs)
                }
                .collectLatest { mem ->
                    _uiState.update { it.copy(memoryState = mem) }
                }
        }
        viewModelScope.launch {
            preferencesManager.state
                .map { it.diagnosticsInterval.millis }
                .distinctUntilChanged()
                .flatMapLatest { intervalMs ->
                    NetworkMonitor.streamRealtimeSpeed(intervalMs)
                }
                .collectLatest { speed ->
                    _uiState.update { it.copy(realtimeSpeed = speed) }
                }
        }
        viewModelScope.launch {
            SystemMonitor.streamBattery(getApplication(), 3000L).collectLatest { bat ->
                _uiState.update { it.copy(batteryState = bat) }
            }
        }
    }

    fun refreshNetworkSummary() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val currentType = NetworkMonitor.getActiveNetworkType(context)
            if (NetworkMonitor.hasUsageAccess(context)) {
                val summary = NetworkMonitor.queryNetworkUsage(
                    context = context,
                    interval = NetworkInterval.DAY,
                    filter = NetworkInterfaceFilter.ALL,
                    targetDateMillis = System.currentTimeMillis()
                )
                val totalStr = when (currentType) {
                    NetworkInterfaceFilter.MOBILE -> NetworkMonitor.formatBytes(summary.mobileBytes)
                    NetworkInterfaceFilter.WIFI -> NetworkMonitor.formatBytes(summary.wifiBytes)
                    NetworkInterfaceFilter.ALL -> NetworkMonitor.formatBytes(summary.totalBytes)
                }
                val labelStr = when (currentType) {
                    NetworkInterfaceFilter.MOBILE -> "Mobile • Today"
                    NetworkInterfaceFilter.WIFI -> "Wi-Fi • Today"
                    NetworkInterfaceFilter.ALL -> "Today"
                }
                _uiState.update {
                    it.copy(
                        activeNetworkType = currentType,
                        todayNetworkTotal = totalStr,
                        todayNetworkLabel = labelStr
                    )
                }
            } else {
                _uiState.update {
                    it.copy(activeNetworkType = currentType)
                }
            }
        }
    }

    private fun startProcessCountPolling() {
        viewModelScope.launch {
            preferencesManager.state
                .map { it.diagnosticsInterval.millis }
                .distinctUntilChanged()
                .flatMapLatest { intervalMs ->
                    val pollDelay = (intervalMs * 2).coerceIn(2000L, 10000L)
                    flow {
                        while (true) {
                            val count = withContext(Dispatchers.IO) {
                                SystemMonitor.getActiveProcesses(getApplication()).size
                            }
                            emit(count)
                            delay(pollDelay)
                        }
                    }
                }
                .collectLatest { count ->
                    _uiState.update { it.copy(processesCount = count) }
                }
        }
    }
}
