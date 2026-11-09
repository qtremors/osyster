package dev.qtremors.osyster.core.device.data

import android.content.Context
import android.net.TrafficStats
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.data.pages.BatteryPageCollector
import dev.qtremors.osyster.core.device.data.pages.BluetoothPageCollector
import dev.qtremors.osyster.core.device.data.pages.CameraPageCollector
import dev.qtremors.osyster.core.device.data.pages.CertificatePageCollector
import dev.qtremors.osyster.core.device.data.pages.DeviceInfoCollector
import dev.qtremors.osyster.core.device.data.pages.DeviceText
import dev.qtremors.osyster.core.device.data.pages.DisplayPageCollector
import dev.qtremors.osyster.core.device.data.pages.LinkSpeed
import dev.qtremors.osyster.core.device.data.pages.MediaPageCollector
import dev.qtremors.osyster.core.device.data.pages.MemoryPageCollector
import dev.qtremors.osyster.core.device.data.pages.NetworkPageCollector
import dev.qtremors.osyster.core.device.data.pages.ProcessorPageCollector
import dev.qtremors.osyster.core.device.data.pages.SensorPageCollector
import dev.qtremors.osyster.core.device.data.pages.SystemPageCollector
import dev.qtremors.osyster.core.device.data.read.BatteryReader
import dev.qtremors.osyster.core.device.data.read.CpuReader
import dev.qtremors.osyster.core.device.data.read.GpuInfo
import dev.qtremors.osyster.core.device.data.read.GpuProbe
import dev.qtremors.osyster.core.device.data.read.MemoryReader
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.DevicePageSource
import dev.qtremors.osyster.core.device.domain.InfoSection
import dev.qtremors.osyster.core.device.domain.ReadingPreferences
import dev.qtremors.osyster.core.device.domain.scale
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
internal class DevicePageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
    private val cpuLoad: CpuLoadSampler,
    private val reading: ReadingPreferences
) : DevicePageSource {
    private val unit = { reading.prefs.value.temperatureUnit }
    private val text = DeviceText(context)
    private val system = SystemPageCollector(context, text)
    private val device = DeviceInfoCollector(context, text)
    private val processor = ProcessorPageCollector(context, text, unit)
    private val memory = MemoryPageCollector(context, text)
    private val battery = BatteryPageCollector(text, unit)
    private val display = DisplayPageCollector(context, text)
    private val cameras = CameraPageCollector(context, text)
    private val sensors = SensorPageCollector(context, text)
    private val network = NetworkPageCollector(context, text)
    private val bluetooth = BluetoothPageCollector(context, text)
    private val media = MediaPageCollector(text)
    private val certificates = CertificatePageCollector(text)

    // The GPU never changes while the process lives, and probing it creates a GL context.
    private val gpuLock = Mutex()
    private var gpu: GpuInfo? = null

    override fun isBuilt(page: DevicePage): Boolean = true

    override fun sections(page: DevicePage): Flow<List<InfoSection>> = when (page) {
        DevicePage.SYSTEM -> once { system.collect() }
        DevicePage.DEVICE -> once { device.collect() }
        DevicePage.PROCESSOR -> ticking(PROCESSOR_TICK_MS) { processor.collect(CpuReader.clusters(), gpuInfo(), cpuLoad.sample()) }
        DevicePage.MEMORY -> ticking(MEMORY_TICK_MS) { memory.collect(MemoryReader.memory(context)) }
        DevicePage.BATTERY -> ticking(BATTERY_TICK_MS) { battery.collect(BatteryReader.read(context)) }
        DevicePage.DISPLAY -> once { display.collect() }
        DevicePage.CAMERAS -> once(dispatchers.heavy) { cameras.collect() }
        DevicePage.SENSORS -> once { sensors.collect() }
        DevicePage.NETWORK -> networkFlow()
        // Ticks slowly so turning Bluetooth on or off shows up without reopening the page.
        DevicePage.BLUETOOTH -> ticking(BLUETOOTH_TICK_MS) { bluetooth.collect() }
        DevicePage.MEDIA -> once(dispatchers.heavy) { media.collect() }
        DevicePage.CERTIFICATES -> once(dispatchers.heavy) { certificates.collect() }
    }

    /** Network refreshes every tick; speed is the byte delta since the previous tick. */
    private fun networkFlow(): Flow<List<InfoSection>> = flow {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastAt = SystemClock.elapsedRealtime()
        emit(network.collect(speed = null))
        while (true) {
            delay(reading.prefs.value.refresh.scale(NETWORK_TICK_MS))
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val now = SystemClock.elapsedRealtime()
            val seconds = ((now - lastAt) / 1000.0).coerceAtLeast(0.001)
            val speed = if (rx < 0 || tx < 0 || lastRx < 0 || lastTx < 0) null else LinkSpeed(
                downBytesPerSecond = ((rx - lastRx).coerceAtLeast(0) / seconds).toLong(),
                upBytesPerSecond = ((tx - lastTx).coerceAtLeast(0) / seconds).toLong()
            )
            lastRx = rx
            lastTx = tx
            lastAt = now
            emit(network.collect(speed))
        }
    }.flowOn(dispatchers.io)

    private suspend fun gpuInfo(): GpuInfo = gpuLock.withLock {
        gpu ?: GpuProbe.probe(context).also { gpu = it }
    }

    private fun once(dispatcher: kotlinx.coroutines.CoroutineDispatcher = dispatchers.io, block: suspend () -> List<InfoSection>): Flow<List<InfoSection>> =
        flow { emit(block()) }.flowOn(dispatcher)

    /** Emits now, then again every [periodMs] (scaled by the refresh setting) while the page is open. */
    private fun ticking(periodMs: Long, block: suspend () -> List<InfoSection>): Flow<List<InfoSection>> =
        flow {
            while (true) {
                emit(block())
                delay(reading.prefs.value.refresh.scale(periodMs))
            }
        }.flowOn(dispatchers.io)

    private companion object {
        const val PROCESSOR_TICK_MS = 1_500L
        const val MEMORY_TICK_MS = 2_000L
        const val BATTERY_TICK_MS = 3_000L
        const val NETWORK_TICK_MS = 2_000L
        const val BLUETOOTH_TICK_MS = 5_000L
    }
}
