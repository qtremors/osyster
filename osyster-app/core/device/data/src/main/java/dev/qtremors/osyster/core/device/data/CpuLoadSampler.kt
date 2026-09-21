package dev.qtremors.osyster.core.device.data

import dev.qtremors.osyster.core.device.data.parse.CpuTimes
import dev.qtremors.osyster.core.device.data.parse.ProcParsers
import dev.qtremors.osyster.core.device.data.read.CpuReader
import dev.qtremors.osyster.core.device.domain.Availability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Overall CPU load needs two /proc/stat samples. Android 8+ blocks that file for apps on most
 * phones; then the answer is Restricted, never 0%.
 */
@Singleton
internal class CpuLoadSampler @Inject constructor() {
    private var previous: CpuTimes? = null

    @Synchronized
    fun sample(): Availability<Int> {
        val now = CpuReader.overallTimes()
            ?: return Availability.Restricted("Android blocks /proc/stat for apps")
        val before = previous
        previous = now
        val percent = before?.let { ProcParsers.loadPercent(it, now) }
            ?: return Availability.Unavailable("Waiting for a second sample", retryable = true)
        return Availability.Available(percent, "/proc/stat", System.currentTimeMillis())
    }
}
