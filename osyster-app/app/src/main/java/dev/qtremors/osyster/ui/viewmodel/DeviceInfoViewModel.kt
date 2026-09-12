package dev.qtremors.osyster.ui.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.CameraInfo
import dev.qtremors.osyster.monitor.ConnectivitySpecs
import dev.qtremors.osyster.monitor.CpuClusterInfo
import dev.qtremors.osyster.monitor.DeviceDisplaySpecs
import dev.qtremors.osyster.monitor.DeviceHardwareMonitor
import dev.qtremors.osyster.monitor.GpuSpecs
import dev.qtremors.osyster.monitor.MediaDrmSpecs
import dev.qtremors.osyster.monitor.SensorItem
import dev.qtremors.osyster.monitor.StorageStats
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.monitor.SystemSpecs
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.settings.TemperatureUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DeviceInfoCategory {
    ALL,
    SYSTEM,
    DEVICE,
    SOC_GPU,
    STORAGE,
    BATTERY,
    CAMERA,
    SENSORS,
    CONNECTIVITY,
    DRM
}

data class DeviceInfoUiState(
    val selectedCategory: DeviceInfoCategory = DeviceInfoCategory.ALL,
    val batteryState: BatteryState = BatteryState(0, 0f, "", "", 0, ""),
    val manufacturer: String = Build.MANUFACTURER ?: "unknown",
    val model: String = Build.MODEL ?: "unknown",
    val board: String = Build.BOARD ?: "unknown",
    val hardware: String = Build.HARDWARE ?: "unknown",
    val supportedAbis: String = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "unknown",
    val androidVersion: String = Build.VERSION.RELEASE ?: "unknown",
    val apiLevel: String = Build.VERSION.SDK_INT.toString(),
    val securityPatch: String = Build.VERSION.SECURITY_PATCH ?: "unknown",
    val bootloader: String = Build.BOOTLOADER ?: "unknown",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val systemSpecs: SystemSpecs = SystemSpecs(),
    val displaySpecs: DeviceDisplaySpecs = DeviceDisplaySpecs(),
    val cpuClusters: List<CpuClusterInfo> = emptyList(),
    val gpuSpecs: GpuSpecs = GpuSpecs(),
    val storageStats: StorageStats = StorageStats(),
    val isLowRamDevice: Boolean = false,
    val cameras: List<CameraInfo> = emptyList(),
    val sensors: List<SensorItem> = emptyList(),
    val selectedSensor: SensorItem? = null,
    val sensorReadingsHistory: List<List<Float>> = emptyList(),
    val latestSensorValues: FloatArray? = null,
    val connectivitySpecs: ConnectivitySpecs = ConnectivitySpecs(),
    val mediaDrmSpecs: MediaDrmSpecs = MediaDrmSpecs()
)

class DeviceInfoViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle? = null
) : AndroidViewModel(application), SensorEventListener {

    private val preferencesManager = OsysterPreferencesManager.getInstance(application)
    private val _uiState = MutableStateFlow(DeviceInfoUiState())
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()

    init {
        loadHardwareSpecs()
        startStreaming()
        observePreferences()
    }

    private fun loadHardwareSpecs() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val system = DeviceHardwareMonitor.getSystemSpecs(context)
            val display = DeviceHardwareMonitor.getDisplaySpecs(context)
            val clusters = DeviceHardwareMonitor.getCpuClusters()
            val gpu = DeviceHardwareMonitor.getGpuSpecs(context)
            val storage = DeviceHardwareMonitor.getStorageStats()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val isLowRam = activityManager?.isLowRamDevice ?: false
            val cameras = DeviceHardwareMonitor.getCameraList(context)
            val sensors = DeviceHardwareMonitor.getSensorList(context)
            val connectivity = DeviceHardwareMonitor.getConnectivitySpecs(context)
            val drm = DeviceHardwareMonitor.getMediaDrmSpecs()

            _uiState.update { current ->
                current.copy(
                    systemSpecs = system,
                    displaySpecs = display,
                    cpuClusters = clusters,
                    gpuSpecs = gpu,
                    storageStats = storage,
                    isLowRamDevice = isLowRam,
                    cameras = cameras,
                    sensors = sensors,
                    connectivitySpecs = connectivity,
                    mediaDrmSpecs = drm,
                    manufacturer = display.manufacturer,
                    model = display.model,
                    board = system.board,
                    hardware = system.hardware,
                    androidVersion = system.androidVersion,
                    apiLevel = system.apiLevel,
                    securityPatch = system.securityPatch,
                    bootloader = system.bootloader
                )
            }
        }
    }

    fun selectCategory(category: DeviceInfoCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        if (category == DeviceInfoCategory.SENSORS && _uiState.value.selectedSensor == null) {
            val firstSensor = _uiState.value.sensors.firstOrNull()
            if (firstSensor != null) {
                selectSensor(firstSensor)
            }
        }
    }

    fun selectSensor(sensor: SensorItem?) {
        val sensorManager = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.unregisterListener(this)

        _uiState.update {
            it.copy(
                selectedSensor = sensor,
                sensorReadingsHistory = emptyList(),
                latestSensorValues = null
            )
        }

        if (sensor != null && sensorManager != null) {
            val hardwareSensor = sensorManager.getDefaultSensor(sensor.type)
            if (hardwareSensor != null) {
                sensorManager.registerListener(this, hardwareSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val values = event.values.clone()
        _uiState.update { current ->
            val numChannels = values.size.coerceAtMost(3)
            val oldHistory = current.sensorReadingsHistory
            val newHistory = (0 until numChannels).map { channelIdx ->
                val prevChannel = oldHistory.getOrNull(channelIdx) ?: emptyList()
                val updated = prevChannel + values[channelIdx]
                if (updated.size > 50) updated.takeLast(50) else updated
            }
            current.copy(
                latestSensorValues = values,
                sensorReadingsHistory = newHistory
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.state
                .map { it.temperatureUnit }
                .distinctUntilChanged()
                .collectLatest { unit ->
                    _uiState.update { it.copy(temperatureUnit = unit) }
                }
        }
    }

    private fun startStreaming() {
        viewModelScope.launchWhileSubscribed(_uiState) {
            SystemMonitor.streamBattery(getApplication(), 3000L).collectLatest { state ->
                _uiState.update { it.copy(batteryState = state) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val sensorManager = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.unregisterListener(this)
    }
}
