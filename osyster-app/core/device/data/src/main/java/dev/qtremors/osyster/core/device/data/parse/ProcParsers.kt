package dev.qtremors.osyster.core.device.data.parse

/** Busy and idle jiffies for one line of /proc/stat. [core] is null for the overall "cpu" line. */
internal data class CpuTimes(val core: Int?, val busy: Long, val idle: Long) {
    val total: Long get() = busy + idle
}

/** Parsers for procfs text. Pure functions, so they are unit tested with captured samples. */
internal object ProcParsers {
    fun cpuTimes(line: String): CpuTimes? {
        val parts = line.trim().split(Regex("\\s+"))
        val head = parts.firstOrNull() ?: return null
        if (!head.startsWith("cpu") || parts.size < 5) return null
        val core = if (head == "cpu") null else head.removePrefix("cpu").toIntOrNull() ?: return null
        val numbers = parts.drop(1).map { it.toLongOrNull() ?: 0L }
        fun at(index: Int) = numbers.getOrElse(index) { 0L }
        // user nice system idle iowait irq softirq steal
        val busy = at(0) + at(1) + at(2) + at(5) + at(6) + at(7)
        val idle = at(3) + at(4)
        return CpuTimes(core, busy, idle)
    }

    /** Percent of time busy between two samples of the same line, or null if no time passed. */
    fun loadPercent(before: CpuTimes, after: CpuTimes): Int? {
        val total = after.total - before.total
        val busy = after.busy - before.busy
        if (total <= 0L || busy < 0L) return null
        return ((busy * 100L) / total).toInt().coerceIn(0, 100)
    }

    /** /proc/meminfo values in kilobytes, keyed by field name ("MemTotal", "SwapFree", ...). */
    fun memInfo(lines: List<String>): Map<String, Long> = lines.mapNotNull { line ->
        val key = line.substringBefore(':', missingDelimiterValue = "").trim()
        val value = line.substringAfter(':', missingDelimiterValue = "")
            .replace("kB", "").trim().toLongOrNull()
        if (key.isEmpty() || value == null) null else key to value
    }.toMap()

    /** "0-3 6" or "0 1 2 3" from related_cpus or a cpu list file, as core numbers. */
    fun cpuList(text: String): List<Int> = text.trim().split(Regex("[\\s,]+")).flatMap { token ->
        when {
            token.isEmpty() -> emptyList()
            '-' in token -> {
                val start = token.substringBefore('-').toIntOrNull()
                val end = token.substringAfter('-').toIntOrNull()
                if (start == null || end == null || end < start) emptyList() else (start..end).toList()
            }
            else -> listOfNotNull(token.toIntOrNull())
        }
    }.distinct().sorted()

    /** The kernel release from /proc/version, such as "5.10.252-android12-9-00029-g6315c5776365". */
    fun kernelRelease(procVersion: String): String? =
        Regex("""Linux version (\S+)""").find(procVersion)?.groupValues?.get(1)

    /** Vulkan's packed version (major << 22 | minor << 12 | patch) as "1.3.0". */
    fun vulkanVersion(packed: Int): String? {
        if (packed <= 0) return null
        return "${packed shr 22}.${(packed shr 12) and 0x3ff}.${packed and 0xfff}"
    }
}
