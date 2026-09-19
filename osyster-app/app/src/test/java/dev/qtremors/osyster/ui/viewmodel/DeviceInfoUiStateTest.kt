package dev.qtremors.osyster.ui.viewmodel

import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.CameraInfo
import dev.qtremors.osyster.monitor.CpuClusterInfo
import dev.qtremors.osyster.monitor.DeviceDisplaySpecs
import dev.qtremors.osyster.monitor.GpuSpecs
import dev.qtremors.osyster.monitor.MediaDrmSpecs
import dev.qtremors.osyster.monitor.SensorItem
import dev.qtremors.osyster.monitor.StorageStats
import dev.qtremors.osyster.monitor.SystemSpecs
import dev.qtremors.osyster.settings.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoUiStateTest {

    @Test
    fun defaultState_containsBatteryState() {
        val battery = BatteryState(
            levelPercentage = 85,
            tempCelsius = 32.5f,
            health = "Good",
            status = "Discharging",
            voltageMv = 4100,
            powerSource = "Battery",
            technology = "Li-poly",
            currentNowMa = -320,
            cycleCount = 142,
            thermalStatus = "None"
        )
        val state = DeviceInfoUiState(batteryState = battery)

        assertEquals(85, state.batteryState.levelPercentage)
        assertEquals(32.5f, state.batteryState.tempCelsius, 0.01f)
        assertEquals("Good", state.batteryState.health)
        assertEquals("Discharging", state.batteryState.status)
        assertEquals(4100, state.batteryState.voltageMv)
        assertEquals("Battery", state.batteryState.powerSource)
        assertEquals("Li-poly", state.batteryState.technology)
        assertEquals(-320, state.batteryState.currentNowMa)
        assertEquals(142, state.batteryState.cycleCount)
        assertEquals("None", state.batteryState.thermalStatus)
        assertEquals(TemperatureUnit.CELSIUS, state.temperatureUnit)
        assertEquals(DeviceInfoCategory.ALL, state.selectedCategory)
    }

    @Test
    fun customTemperatureUnit_retainsPreference() {
        val fState = DeviceInfoUiState(temperatureUnit = TemperatureUnit.FAHRENHEIT)
        assertEquals(TemperatureUnit.FAHRENHEIT, fState.temperatureUnit)

        val kState = DeviceInfoUiState(temperatureUnit = TemperatureUnit.KELVIN)
        assertEquals(TemperatureUnit.KELVIN, kState.temperatureUnit)
    }

    @Test
    fun categorySelection_updatesCorrectly() {
        val state = DeviceInfoUiState(selectedCategory = DeviceInfoCategory.SOC_GPU)
        assertEquals(DeviceInfoCategory.SOC_GPU, state.selectedCategory)

        val camState = state.copy(selectedCategory = DeviceInfoCategory.CAMERA)
        assertEquals(DeviceInfoCategory.CAMERA, camState.selectedCategory)
    }

    @Test
    fun hardwareSpecs_holdExpectedData() {
        val clusters = listOf(
            CpuClusterInfo(clusterId = 0, coreCount = 4, coreRange = "0-3", minFreqKhz = 300000L, maxFreqKhz = 1800000L, governor = "schedutil"),
            CpuClusterInfo(clusterId = 1, coreCount = 3, coreRange = "4-6", minFreqKhz = 700000L, maxFreqKhz = 2400000L, governor = "schedutil"),
            CpuClusterInfo(clusterId = 2, coreCount = 1, coreRange = "7", minFreqKhz = 800000L, maxFreqKhz = 2840000L, governor = "schedutil")
        )
        val gpu = GpuSpecs(renderer = "Adreno (TM) 650", vendor = "Qualcomm", glEsVersion = "OpenGL ES 3.2", vulkanVersion = "1.1.128")
        val storage = StorageStats(totalBytes = 128_000_000_000L, usedBytes = 64_000_000_000L, availableBytes = 64_000_000_000L)
        val systemSpecs = SystemSpecs(kernelVersion = "6.1.0-android14", rootStatus = "Not detected", isTrebleEnabled = true, pageSizeKb = 4L)
        val display = DeviceDisplaySpecs(resolution = "1080 x 2400", refreshRateHz = "120.0 Hz", hdrFormats = "HDR10, HLG")
        val drm = MediaDrmSpecs(widevineSecurityLevel = "L1", clearkeyVendor = "Google")

        val state = DeviceInfoUiState(
            cpuClusters = clusters,
            gpuSpecs = gpu,
            storageStats = storage,
            systemSpecs = systemSpecs,
            displaySpecs = display,
            mediaDrmSpecs = drm
        )

        assertEquals(3, state.cpuClusters.size)
        assertEquals("Adreno (TM) 650", state.gpuSpecs.renderer)
        assertEquals("Qualcomm", state.gpuSpecs.vendor)
        assertEquals(128_000_000_000L, state.storageStats.totalBytes)
        assertEquals("6.1.0-android14", state.systemSpecs.kernelVersion)
        assertEquals("120.0 Hz", state.displaySpecs.refreshRateHz)
        assertEquals("L1", state.mediaDrmSpecs.widevineSecurityLevel)
    }

    @Test
    fun sensorAndOscilloscopeState_updatesCorrectly() {
        val sensor = SensorItem(
            type = 1,
            name = "ICM42605 Accelerometer",
            vendor = "InvenSense",
            typeString = "android.sensor.accelerometer",
            maxRange = 78.4f,
            resolution = 0.002f,
            powerMa = 0.25f,
            version = 1
        )
        val state = DeviceInfoUiState(
            sensors = listOf(sensor),
            selectedSensor = sensor,
            sensorReadingsHistory = listOf(
                listOf(0.1f, 0.2f, 9.8f),
                listOf(0.12f, 0.18f, 9.79f)
            ),
            latestSensorValues = floatArrayOf(0.12f, 0.18f, 9.79f)
        )

        assertEquals(1, state.sensors.size)
        assertEquals(sensor, state.selectedSensor)
        assertEquals(2, state.sensorReadingsHistory.size)
        assertNotNull(state.latestSensorValues)
        assertEquals(9.79f, state.latestSensorValues!![2], 0.001f)
    }
}
