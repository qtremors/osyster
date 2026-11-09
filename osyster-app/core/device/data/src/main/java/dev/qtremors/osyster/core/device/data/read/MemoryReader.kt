package dev.qtremors.osyster.core.device.data.read

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import dev.qtremors.osyster.core.device.data.parse.ProcParsers

/** RAM and swap in bytes. [fromProc] is false when only ActivityManager's totals were readable. */
internal data class MemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val freeBytes: Long?,
    val cachedBytes: Long?,
    val buffersBytes: Long?,
    val swapTotalBytes: Long?,
    val swapFreeBytes: Long?,
    val lowMemoryThresholdBytes: Long,
    val isLowMemory: Boolean,
    val fromProc: Boolean
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
}

internal data class SpaceSnapshot(val totalBytes: Long, val freeBytes: Long) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
}

internal object MemoryReader {
    fun memory(context: Context): MemorySnapshot {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(info)
        val proc = SystemFiles.lines("/proc/meminfo")?.let(ProcParsers::memInfo).orEmpty()
        fun kb(key: String): Long? = proc[key]?.times(1024)
        val total = kb("MemTotal") ?: info.totalMem
        return MemorySnapshot(
            totalBytes = total,
            availableBytes = kb("MemAvailable") ?: info.availMem,
            freeBytes = kb("MemFree"),
            cachedBytes = kb("Cached"),
            buffersBytes = kb("Buffers"),
            swapTotalBytes = kb("SwapTotal"),
            swapFreeBytes = kb("SwapFree"),
            lowMemoryThresholdBytes = info.threshold,
            isLowMemory = info.lowMemory,
            fromProc = proc.isNotEmpty()
        )
    }

    /** Space on the app data partition, which is what "internal storage" means to users. */
    fun internalSpace(): SpaceSnapshot? = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        SpaceSnapshot(stat.totalBytes, stat.availableBytes)
    }.getOrNull()

    fun space(path: String): SpaceSnapshot? = runCatching {
        val stat = StatFs(path)
        SpaceSnapshot(stat.totalBytes, stat.availableBytes)
    }.getOrNull()
}
