package dev.qtremors.osyster.core.device.data

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.data.read.BatteryReader
import dev.qtremors.osyster.core.device.data.read.CpuReader
import dev.qtremors.osyster.core.device.data.read.MemoryReader
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.DeviceSummary
import dev.qtremors.osyster.core.device.domain.DeviceSummarySource
import dev.qtremors.osyster.core.device.domain.ReadingPreferences
import dev.qtremors.osyster.core.device.domain.scale
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Live numbers for the Home hero card, sampled every two seconds (scaled by the refresh setting) while Home is visible. */
@Singleton
internal class DeviceSummaryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
    private val cpuLoad: CpuLoadSampler,
    private val reading: ReadingPreferences
) : DeviceSummarySource {

    override fun summary(): Flow<DeviceSummary> = flow {
        while (true) {
            emit(read())
            delay(reading.prefs.value.refresh.scale(TICK_MS))
        }
    }.flowOn(dispatchers.io)

    private fun read(): DeviceSummary {
        val now = System.currentTimeMillis()
        fun <T> available(value: T, source: String) = Availability.Available(value, source, now)
        val memory = MemoryReader.memory(context)
        val storage = MemoryReader.internalSpace()
        val battery = BatteryReader.read(context)
        val fastestCore = CpuReader.possibleCores().mapNotNull(CpuReader::currentKhz).maxOrNull()
        return DeviceSummary(
            marketingName = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            cpuLoadPercent = cpuLoad.sample(),
            cpuClockMhz = fastestCore?.let { available((it / 1000).toInt(), "scaling_cur_freq") }
                ?: Availability.Restricted("Android blocks CPU clock files for apps"),
            ramUsedBytes = available(memory.usedBytes, "meminfo"),
            ramTotalBytes = available(memory.totalBytes, "meminfo"),
            storageUsedBytes = storage?.let { available(it.usedBytes, "StatFs") } ?: Availability.Unavailable("Storage not readable", true),
            storageTotalBytes = storage?.let { available(it.totalBytes, "StatFs") } ?: Availability.Unavailable("Storage not readable", true),
            batteryPercent = battery.percent?.let { available(it, "BatteryManager") } ?: Availability.Unavailable("Not reported", true),
            isCharging = battery.isCharging
        )
    }

    private companion object {
        const val TICK_MS = 2_000L
    }
}
