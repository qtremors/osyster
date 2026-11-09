package dev.qtremors.osyster.core.device.data.read

import android.os.Build
import dev.qtremors.osyster.core.device.data.parse.CpuTimes
import dev.qtremors.osyster.core.device.data.parse.ProcParsers

/** One group of cores that share a clock domain (a big.LITTLE cluster). */
internal data class CpuCluster(
    val cores: List<Int>,
    val minKhz: Long?,
    val maxKhz: Long?,
    val governor: String?
)

/** Reads CPU topology, clocks, and load from sysfs and procfs. */
internal object CpuReader {
    private const val CPU = "/sys/devices/system/cpu"

    val coreCount: Int get() = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

    /** All cores the kernel knows, including offline ones, from /sys/devices/system/cpu/possible. */
    fun possibleCores(): List<Int> =
        SystemFiles.text("$CPU/possible")?.let(ProcParsers::cpuList)?.takeIf { it.isNotEmpty() }
            ?: (0 until coreCount).toList()

    fun clusters(): List<CpuCluster> {
        val seen = mutableSetOf<Int>()
        return possibleCores().mapNotNull { core ->
            if (core in seen) return@mapNotNull null
            val related = (SystemFiles.text("$CPU/cpu$core/cpufreq/related_cpus")
                ?: SystemFiles.text("$CPU/cpu$core/cpufreq/affected_cpus"))
                ?.let(ProcParsers::cpuList)
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(core)
            seen += related
            CpuCluster(
                cores = related,
                minKhz = SystemFiles.long("$CPU/cpu$core/cpufreq/cpuinfo_min_freq"),
                maxKhz = SystemFiles.long("$CPU/cpu$core/cpufreq/cpuinfo_max_freq"),
                governor = SystemFiles.text("$CPU/cpu$core/cpufreq/scaling_governor")
            )
        }
    }

    /** Current clock of one core in kHz; null when offline or unreadable. */
    fun currentKhz(core: Int): Long? = SystemFiles.long("$CPU/cpu$core/cpufreq/scaling_cur_freq")

    fun isOnline(core: Int): Boolean =
        SystemFiles.text("$CPU/cpu$core/online")?.let { it == "1" } ?: true

    /** Overall /proc/stat line, or null because Android 8+ blocks it for apps on most phones. */
    fun overallTimes(): CpuTimes? =
        SystemFiles.lines("/proc/stat")?.firstOrNull()?.let(ProcParsers::cpuTimes)

    /** "Qualcomm SM8475" on Android 12+, otherwise the kernel's hardware name. */
    fun chipName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val model = Build.SOC_MODEL.takeUnless { it.isBlank() || it == Build.UNKNOWN }
            val maker = Build.SOC_MANUFACTURER.takeUnless { it.isBlank() || it == Build.UNKNOWN }
            if (model != null) return listOfNotNull(maker, model).joinToString(" ")
        }
        return SystemFiles.lines("/proc/cpuinfo")
            ?.firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.takeIf(String::isNotEmpty)
            ?: Build.HARDWARE.takeUnless { it.isBlank() || it == Build.UNKNOWN }
    }

    /** Highest plausible temperature from thermal zones named like the CPU, in °C. */
    fun cpuTemperature(): Float? = SystemFiles.listDirs("/sys/class/thermal", "thermal_zone")
        .filter { zone ->
            val type = SystemFiles.text("${zone.path}/type")?.lowercase().orEmpty()
            "cpu" in type || "soc" in type || "cluster" in type
        }
        .mapNotNull { zone -> SystemFiles.long("${zone.path}/temp") }
        .map { raw -> if (raw > 1000) raw / 1000f else raw.toFloat() }
        .filter { it in 10f..120f }
        .maxOrNull()
}
