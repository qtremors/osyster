package dev.qtremors.osyster.core.device.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.data.pages.BatteryPageCollector
import dev.qtremors.osyster.core.device.data.pages.DeviceInfoCollector
import dev.qtremors.osyster.core.device.data.pages.DeviceText
import dev.qtremors.osyster.core.device.data.pages.MemoryPageCollector
import dev.qtremors.osyster.core.device.data.pages.ProcessorPageCollector
import dev.qtremors.osyster.core.device.data.pages.SystemPageCollector
import dev.qtremors.osyster.core.device.data.read.BatteryReader
import dev.qtremors.osyster.core.device.data.read.CpuReader
import dev.qtremors.osyster.core.device.data.read.GpuInfo
import dev.qtremors.osyster.core.device.data.read.GpuProbe
import dev.qtremors.osyster.core.device.data.read.MemoryReader
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.DevicePageSource
import dev.qtremors.osyster.core.device.domain.InfoSection
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
    private val cpuLoad: CpuLoadSampler
) : DevicePageSource {
    private val text = DeviceText(context)
    private val system = SystemPageCollector(context, text)
    private val device = DeviceInfoCollector(context, text)
    private val processor = ProcessorPageCollector(context, text)
    private val memory = MemoryPageCollector(context, text)
    private val battery = BatteryPageCollector(text)

    // The GPU never changes while the process lives, and probing it creates a GL context.
    private val gpuLock = Mutex()
    private var gpu: GpuInfo? = null

    override fun isBuilt(page: DevicePage): Boolean = page in BUILT

    override fun sections(page: DevicePage): Flow<List<InfoSection>> = when (page) {
        DevicePage.SYSTEM -> once { system.collect() }
        DevicePage.DEVICE -> once { device.collect() }
        DevicePage.PROCESSOR -> ticking(PROCESSOR_TICK_MS) {
            val clusters = CpuReader.clusters()
            processor.collect(clusters, gpuInfo(), cpuLoad.sample())
        }
        DevicePage.MEMORY -> ticking(MEMORY_TICK_MS) { memory.collect(MemoryReader.memory(context)) }
        DevicePage.BATTERY -> ticking(BATTERY_TICK_MS) { battery.collect(BatteryReader.read(context)) }
        else -> flow { emit(emptyList()) }
    }

    private suspend fun gpuInfo(): GpuInfo = gpuLock.withLock {
        gpu ?: GpuProbe.probe(context).also { gpu = it }
    }

    private fun once(block: suspend () -> List<InfoSection>): Flow<List<InfoSection>> =
        flow { emit(block()) }.flowOn(dispatchers.io)

    /** Emits now, then again every [periodMs] while someone collects (the page is open). */
    private fun ticking(periodMs: Long, block: suspend () -> List<InfoSection>): Flow<List<InfoSection>> =
        flow {
            while (true) {
                emit(block())
                delay(periodMs)
            }
        }.flowOn(dispatchers.io)

    private companion object {
        val BUILT = setOf(DevicePage.SYSTEM, DevicePage.DEVICE, DevicePage.PROCESSOR, DevicePage.MEMORY, DevicePage.BATTERY)
        const val PROCESSOR_TICK_MS = 1_500L
        const val MEMORY_TICK_MS = 2_000L
        const val BATTERY_TICK_MS = 3_000L
    }
}
