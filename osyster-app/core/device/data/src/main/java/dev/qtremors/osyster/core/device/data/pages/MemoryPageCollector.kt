package dev.qtremors.osyster.core.device.data.pages

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.format.UnitFormat
import dev.qtremors.osyster.core.device.data.read.MemoryReader
import dev.qtremors.osyster.core.device.data.read.MemorySnapshot
import dev.qtremors.osyster.core.device.data.read.SystemFiles
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection

/** The Memory and storage page: RAM, swap, every storage volume Android exposes, and this app. */
internal class MemoryPageCollector(private val context: Context, private val text: DeviceText) {

    fun collect(memory: MemorySnapshot): List<InfoSection> =
        listOf(ram(memory), swap(memory)) + storage() + listOf(app())

    private fun ram(memory: MemorySnapshot) = section(text, "ram", R.string.section_ram, if (memory.fromProc) "/proc/meminfo" else "ActivityManager") {
        row("total", R.string.row_total, UnitFormat.bytes(memory.totalBytes))
        row("used", R.string.row_used, UnitFormat.usedOfTotal(memory.usedBytes, memory.totalBytes), copyable = false)
        row("available", R.string.row_available, UnitFormat.bytes(memory.availableBytes), copyable = false)
        row("low_threshold", R.string.row_low_memory_threshold, UnitFormat.bytes(memory.lowMemoryThresholdBytes), copyable = false)
        yesNo("low_now", R.string.row_low_memory_now, memory.isLowMemory)
        row("free", R.string.row_free, memory.freeBytes?.let(UnitFormat::bytes), advanced = true, copyable = false)
        row("cached", R.string.row_cached, memory.cachedBytes?.let(UnitFormat::bytes), advanced = true, copyable = false)
        row("buffers", R.string.row_buffers, memory.buffersBytes?.let(UnitFormat::bytes), advanced = true, copyable = false)
    }

    private fun swap(memory: MemorySnapshot) = section(text, "swap", R.string.section_swap, "/proc/meminfo") {
        val total = memory.swapTotalBytes
        val free = memory.swapFreeBytes
        if (total == null || free == null) {
            row("swap_total", R.string.row_total, Availability.Restricted("Android blocks /proc/meminfo"))
        } else if (total == 0L) {
            row("swap_total", R.string.row_total, text[R.string.value_no_swap], copyable = false)
        } else {
            row("swap_total", R.string.row_total, UnitFormat.bytes(total))
            row("swap_used", R.string.row_used, UnitFormat.usedOfTotal(total - free, total), copyable = false)
            row("swap_free", R.string.row_free, UnitFormat.bytes(free), copyable = false)
        }
        val zram = SystemFiles.long("/sys/block/zram0/disksize")
        row("zram", R.string.row_zram, zram?.takeIf { it > 0 }?.let { available(UnitFormat.bytes(it), "zram sysfs") }
            ?: Availability.Restricted("Android blocks zram details for apps"), advanced = true, copyable = false)
    }

    private fun storage(): List<InfoSection> {
        val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager)?.storageVolumes.orEmpty()
        } else emptyList()
        if (volumes.isEmpty()) {
            val internal = MemoryReader.internalSpace()
            return listOf(volumeSection("storage_internal", text[R.string.section_internal_storage], internal?.totalBytes, internal?.freeBytes, primary = true, removable = false))
        }
        return volumes.mapIndexed { index, volume -> volume(index, volume) }
    }

    private fun volume(index: Int, volume: StorageVolume): InfoSection {
        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) volume.directory?.path else null
        val space = if (volume.isPrimary) MemoryReader.internalSpace() else path?.let(MemoryReader::space)
        val title = if (volume.isPrimary) text[R.string.section_internal_storage]
        else runCatching { volume.getDescription(context) }.getOrNull() ?: text[R.string.section_external_storage]
        return volumeSection("storage_$index", title, space?.totalBytes, space?.freeBytes, volume.isPrimary, volume.isRemovable)
    }

    private fun volumeSection(key: String, title: String, total: Long?, free: Long?, primary: Boolean, removable: Boolean): InfoSection {
        val builder = SectionBuilder(text, "StatFs")
        if (total == null || free == null) {
            builder.row("total", R.string.row_total, Availability.Unavailable("Android did not expose this volume to apps", retryable = false))
        } else {
            builder.row("total", R.string.row_total, UnitFormat.bytes(total))
            builder.row("used", R.string.row_used, UnitFormat.usedOfTotal(total - free, total), copyable = false)
            builder.row("free", R.string.row_free, UnitFormat.bytes(free), copyable = false)
        }
        builder.yesNo("removable", R.string.row_removable, removable, advanced = primary)
        return builder.build(key, title)
    }

    private fun app() = section(text, "this_app", R.string.section_this_app, "Runtime") {
        val runtime = Runtime.getRuntime()
        row("heap_used", R.string.row_heap_used, UnitFormat.bytes(runtime.totalMemory() - runtime.freeMemory()), advanced = true, copyable = false)
        row("heap_max", R.string.row_heap_max, UnitFormat.bytes(runtime.maxMemory()), advanced = true, copyable = false)
        row("native_heap", R.string.row_native_heap, UnitFormat.bytes(Debug.getNativeHeapAllocatedSize()), advanced = true, copyable = false)
    }
}
