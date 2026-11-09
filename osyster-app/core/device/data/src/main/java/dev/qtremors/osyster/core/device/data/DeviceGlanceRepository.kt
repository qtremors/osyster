package dev.qtremors.osyster.core.device.data

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.Display
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.data.format.UnitFormat
import dev.qtremors.osyster.core.device.data.read.BatteryReader
import dev.qtremors.osyster.core.device.data.read.MemoryReader
import dev.qtremors.osyster.core.device.domain.DeviceGlanceSource
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.ReadingPreferences
import dev.qtremors.osyster.core.device.domain.scale
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Cheap facts only: each comes from one system service call, never a scan. Static ones (model,
 * cameras, sensors) are read once; battery, memory, network, and Bluetooth refresh each tick.
 */
@Singleton
internal class DeviceGlanceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
    private val reading: ReadingPreferences
) : DeviceGlanceSource {
    private val res = context.resources
    private val static: Map<DevicePage, String> by lazy { readStatic() }

    override fun glances(): Flow<Map<DevicePage, String>> = flow {
        while (true) {
            emit(static + readLive())
            delay(reading.prefs.value.refresh.scale(TICK_MS))
        }
    }.distinctUntilChanged().flowOn(dispatchers.io)

    private fun readStatic(): Map<DevicePage, String> = buildMap {
        put(DevicePage.SYSTEM, listOfNotNull(res.getString(R.string.glance_android, Build.VERSION.RELEASE), patch()).joinToString(" · "))
        put(DevicePage.DEVICE, listOf(Build.MANUFACTURER.replaceFirstChar { it.titlecase() }, Build.MODEL).distinct().joinToString(" "))
        processor()?.let { put(DevicePage.PROCESSOR, it) }
        display()?.let { put(DevicePage.DISPLAY, it) }
        countOf { context.getSystemService(CameraManager::class.java)?.cameraIdList?.size }?.let {
            put(DevicePage.CAMERAS, res.getQuantityString(R.plurals.glance_cameras, it, it))
        }
        countOf { context.getSystemService(SensorManager::class.java)?.getSensorList(android.hardware.Sensor.TYPE_ALL)?.size }?.let {
            put(DevicePage.SENSORS, res.getQuantityString(R.plurals.glance_sensors, it, it))
        }
    }

    private fun readLive(): Map<DevicePage, String> = buildMap {
        runCatching { MemoryReader.memory(context) }.getOrNull()?.let { memory ->
            if (memory.totalBytes > 0) put(DevicePage.MEMORY, res.getString(R.string.glance_ram, UnitFormat.usedOfTotal(memory.totalBytes - memory.availableBytes, memory.totalBytes)))
        }
        runCatching { BatteryReader.read(context) }.getOrNull()?.let { battery ->
            val percent = battery.percent ?: return@let
            val state = res.getString(if (battery.plugged != 0) R.string.glance_charging else R.string.glance_on_battery)
            put(DevicePage.BATTERY, "$percent% · $state")
        }
        network()?.let { put(DevicePage.NETWORK, it) }
        bluetooth()?.let { put(DevicePage.BLUETOOTH, it) }
    }

    private fun patch(): String? = Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() }?.let { res.getString(R.string.glance_patch, it) }

    private fun processor(): String? {
        val cores = Runtime.getRuntime().availableProcessors()
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN } else null
        val coreText = res.getQuantityString(R.plurals.glance_cores, cores, cores)
        return listOfNotNull(soc, coreText).joinToString(" · ")
    }

    private fun display(): String? = runCatching {
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return null
        val mode = display.mode
        val (w, h) = listOf(mode.physicalWidth, mode.physicalHeight).sorted()
        "${h}×$w · ${mode.refreshRate.roundToInt()} Hz"
    }.getOrNull()

    private fun network(): String? = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val caps = manager.getNetworkCapabilities(manager.activeNetwork)
        res.getString(
            when {
                caps == null -> R.string.glance_offline
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> R.string.glance_wifi
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> R.string.glance_mobile
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> R.string.glance_ethernet
                else -> R.string.glance_connected
            }
        )
    }.getOrNull()

    private fun bluetooth(): String? = runCatching {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return res.getString(R.string.glance_bluetooth_none)
        res.getString(if (adapter.isEnabled) R.string.glance_bluetooth_on else R.string.glance_bluetooth_off)
    }.getOrNull()

    private inline fun countOf(read: () -> Int?): Int? = runCatching(read).getOrNull()?.takeIf { it > 0 }

    private companion object {
        const val TICK_MS = 5_000L
    }
}

