package dev.qtremors.osyster.monitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

// =========================================================================
// Section Comment: Data Models for System Diagnostics
// =========================================================================

data class CpuCoreState(
    val id: Int,
    val usagePercentage: Float,
    val currentFreqKhz: Long,
    val maxFreqKhz: Long
)

data class CpuTimeSnapshot(
    val coreId: Int?,
    val activeTime: Long,
    val idleTime: Long
)

data class CpuState(
    val overallUsage: TelemetryResult<Float> = TelemetryResult.Available(0f),
    val coreStates: List<CpuCoreState> = emptyList(),
    val cpuTempCelsius: TelemetryResult<Float> = TelemetryResult.Restricted,
    val cpuModel: String = "",
    val cpuArchitecture: String = ""
) {
    val overallUsageOrZero: Float
        get() = (overallUsage as? TelemetryResult.Available)?.value ?: 0f

    val isUsageRestricted: Boolean
        get() = overallUsage is TelemetryResult.Restricted

    val isTempRestricted: Boolean
        get() = cpuTempCelsius is TelemetryResult.Restricted
}

data class MemoryState(
    val ramTotalKb: Long,
    val ramUsedKb: Long,
    val ramAvailableKb: Long,
    val ramFreeKb: Long,
    val ramCachedKb: Long,
    val ramBuffersKb: Long,
    val swapTotalKb: Long,
    val swapUsedKb: Long,
    val swapFreeKb: Long
)

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val ramKb: Long,
    val user: String = "unknown"
)

data class BatteryState(
    val levelPercentage: Int,
    val tempCelsius: Float,
    val health: String,
    val status: String,
    val voltageMv: Int,
    val powerSource: String
)

object SystemMonitor {

    // =========================================================================
    // Subsection Comment: CPU Monitor Parser
    // =========================================================================

    fun parseProcStatLine(line: String): CpuTimeSnapshot? {
        val trimmed = line.trim()
        val isOverall = trimmed.startsWith("cpu ")
        val isCore = trimmed.startsWith("cpu") && trimmed.length > 3 && trimmed[3].isDigit()
        if (!isOverall && !isCore) return null

        val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.size < 5) return null

        val coreId = if (isOverall) null else parts.firstOrNull()?.removePrefix("cpu")?.toIntOrNull()
        if (!isOverall && coreId == null) return null

        val user = parts[1].toLongOrNull() ?: 0L
        val nice = parts[2].toLongOrNull() ?: 0L
        val system = parts[3].toLongOrNull() ?: 0L
        val idle = parts[4].toLongOrNull() ?: 0L
        val iowait = if (parts.size > 5) parts[5].toLongOrNull() ?: 0L else 0L
        val irq = if (parts.size > 6) parts[6].toLongOrNull() ?: 0L else 0L
        val softirq = if (parts.size > 7) parts[7].toLongOrNull() ?: 0L else 0L

        val active = user + nice + system + irq + softirq
        val totalIdle = idle + iowait
        return CpuTimeSnapshot(coreId = coreId, activeTime = active, idleTime = totalIdle)
    }

    fun calculateCpuUsage(deltaActive: Long, deltaTotal: Long): Float {
        if (deltaTotal <= 0L || deltaActive < 0L) return 0f
        return (deltaActive.toFloat() / deltaTotal.toFloat() * 100f).coerceIn(0f, 100f)
    }

    internal fun getCpuState(sampler: CpuUsageSampler = CpuUsageSampler()): CpuState {
        var overallUsage = 0f
        val coreStates = mutableListOf<CpuCoreState>()
        val cpuModel = detectCpuModel()
        val cpuArchitecture = System.getProperty("os.arch") ?: "unknown"
        var procStatReadSuccess = false

        try {
            // Read overall and per-core CPU usage from /proc/stat
            val statFile = File("/proc/stat")
            if (statFile.exists() && statFile.canRead()) {
                statFile.useLines { lines ->
                    lines.forEach { line ->
                        val snapshot = parseProcStatLine(line)
                        if (snapshot != null) {
                            procStatReadSuccess = true
                            val usage = sampler.sample(snapshot)
                            if (snapshot.coreId == null) {
                                overallUsage = usage
                            } else {
                                val coreId = snapshot.coreId
                                val coreUsage = usage

                                val freq = getCoreFrequencyKhz(coreId)
                                val maxFreq = getCoreMaxFrequencyKhz(coreId)
                                coreStates.add(CpuCoreState(coreId, coreUsage, freq, maxFreq))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val usageResult: TelemetryResult<Float>
        // Fallback for cores if /proc/stat is unreadable or restricted by SELinux
        if (!procStatReadSuccess && coreStates.isEmpty()) {
            val coresCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            for (i in 0 until coresCount) {
                coreStates.add(CpuCoreState(i, 0f, getCoreFrequencyKhz(i), getCoreMaxFrequencyKhz(i)))
            }
            // Clock frequency is not a measure of CPU utilization.
            usageResult = TelemetryResult.Restricted
        } else {
            // If /proc/stat was readable but overallUsage is still 0 (e.g. initial delta), derive from cores
            if (overallUsage == 0f && coreStates.any { it.usagePercentage > 0f }) {
                overallUsage = coreStates.map { it.usagePercentage }.average().toFloat().coerceIn(0f, 100f)
            }
            usageResult = TelemetryResult.Available(overallUsage)
        }

        val temp = getCpuTemperature()

        return CpuState(
            overallUsage = usageResult,
            coreStates = coreStates.sortedBy { it.id },
            cpuTempCelsius = temp,
            cpuModel = cpuModel,
            cpuArchitecture = cpuArchitecture
        )
    }

    private fun detectCpuModel(): String {
        // 1. Modern Android 12+ SoC Model API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            if (!socModel.isNullOrBlank() && !socModel.equals("unknown", ignoreCase = true)) {
                val socManufacturer = if (!Build.SOC_MANUFACTURER.isNullOrBlank() && !Build.SOC_MANUFACTURER.equals("unknown", ignoreCase = true)) {
                    "${Build.SOC_MANUFACTURER} "
                } else ""
                return "$socManufacturer$socModel".trim()
            }
        }

        // 2. /proc/cpuinfo inspection (ignoring numeric core lines like 'processor : 0')
        try {
            val cpuinfoFile = File("/proc/cpuinfo")
            if (cpuinfoFile.exists() && cpuinfoFile.canRead()) {
                cpuinfoFile.useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("Hardware", ignoreCase = true) ||
                            trimmed.startsWith("model name", ignoreCase = true)
                        ) {
                            val parts = trimmed.split(":")
                            if (parts.size > 1) {
                                val candidate = parts[1].trim()
                                if (candidate.isNotEmpty() && candidate.toIntOrNull() == null && !candidate.equals("unknown", ignoreCase = true)) {
                                    return candidate
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Android Build hardware / board fallbacks
        val hardware = Build.HARDWARE
        if (!hardware.isNullOrBlank() && !hardware.equals("unknown", ignoreCase = true)) {
            return hardware.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        val board = Build.BOARD
        if (!board.isNullOrBlank() && !board.equals("unknown", ignoreCase = true)) {
            return board.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        return "Unknown processor"
    }

    private fun getCoreFrequencyKhz(coreId: Int): Long {
        val paths = listOf(
            "/sys/devices/system/cpu/cpu$coreId/cpufreq/scaling_cur_freq",
            "/sys/devices/system/cpu/cpu$coreId/cpufreq/cpuinfo_cur_freq",
            "/sys/devices/system/cpu/cpufreq/policy$coreId/scaling_cur_freq",
            "/sys/devices/system/cpu/cpufreq/policy$coreId/cpuinfo_cur_freq"
        )
        for (path in paths) {
            val freq = readLongFromFile(path, 0L)
            if (freq > 0L) return freq
        }
        return 0L
    }

    private fun getCoreMaxFrequencyKhz(coreId: Int): Long {
        val paths = listOf(
            "/sys/devices/system/cpu/cpu$coreId/cpufreq/scaling_max_freq",
            "/sys/devices/system/cpu/cpu$coreId/cpufreq/cpuinfo_max_freq",
            "/sys/devices/system/cpu/cpufreq/policy$coreId/scaling_max_freq",
            "/sys/devices/system/cpu/cpufreq/policy$coreId/cpuinfo_max_freq"
        )
        for (path in paths) {
            val freq = readLongFromFile(path, 0L)
            if (freq > 0L) return freq
        }
        return 0L
    }

    fun parseThermalTemp(rawText: String): TelemetryResult<Float> {
        val raw = rawText.trim().toFloatOrNull() ?: 0f
        if (raw > 0f) {
            val temp = if (raw > 1000f) raw / 1000f else raw
            if (temp in 10f..105f) return TelemetryResult.Available(temp)
        }
        return TelemetryResult.Restricted
    }

    internal fun isCpuThermalZone(type: String): Boolean {
        val name = type.trim().lowercase(Locale.ROOT)
        return name.contains("cpu") || name.contains("soc") || name.contains("cluster")
    }

    private fun getCpuTemperature(): TelemetryResult<Float> {
        val zones = runCatching {
            File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
        }.getOrNull() ?: return TelemetryResult.Restricted
        for (zone in zones) {
            val result = runCatching {
                if (isCpuThermalZone(File(zone, "type").readText())) {
                    parseThermalTemp(File(zone, "temp").readText())
                } else TelemetryResult.Restricted
            }.getOrDefault(TelemetryResult.Restricted)
            if (result is TelemetryResult.Available) return result
        }
        return TelemetryResult.Restricted
    }

    // =========================================================================
    // Subsection Comment: Memory (RAM & SWAP) Monitor Parser
    // =========================================================================

    fun parseMemInfo(lines: Sequence<String>): MemoryState {
        var memTotal = 0L
        var memFree = 0L
        var memAvailable = 0L
        var buffers = 0L
        var cached = 0L
        var swapTotal = 0L
        var swapFree = 0L

        lines.forEach { line ->
            val parts = line.split(":")
            if (parts.size > 1) {
                val key = parts[0].trim()
                val value = parts[1].replace("kB", "").trim().toLongOrNull() ?: 0L
                when (key) {
                    "MemTotal" -> memTotal = value
                    "MemFree" -> memFree = value
                    "MemAvailable" -> memAvailable = value
                    "Buffers" -> buffers = value
                    "Cached" -> cached = value
                    "SwapTotal" -> swapTotal = value
                    "SwapFree" -> swapFree = value
                }
            }
        }

        // If MemAvailable is not reported, estimate it
        if (memAvailable == 0L) {
            memAvailable = memFree + buffers + cached
        }

        val ramUsed = (memTotal - memAvailable).coerceAtLeast(0L)
        val swapUsed = (swapTotal - swapFree).coerceAtLeast(0L)

        return MemoryState(
            ramTotalKb = memTotal,
            ramUsedKb = ramUsed,
            ramAvailableKb = memAvailable,
            ramFreeKb = memFree,
            ramCachedKb = cached,
            ramBuffersKb = buffers,
            swapTotalKb = swapTotal,
            swapUsedKb = swapUsed,
            swapFreeKb = swapFree
        )
    }

    fun getMemoryState(): MemoryState {
        try {
            val meminfoFile = File("/proc/meminfo")
            if (meminfoFile.exists() && meminfoFile.canRead()) {
                return meminfoFile.useLines { parseMemInfo(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return MemoryState(
            ramTotalKb = 0L,
            ramUsedKb = 0L,
            ramAvailableKb = 0L,
            ramFreeKb = 0L,
            ramCachedKb = 0L,
            ramBuffersKb = 0L,
            swapTotalKb = 0L,
            swapUsedKb = 0L,
            swapFreeKb = 0L
        )
    }

    // =========================================================================
    // Subsection Comment: Active Processes Parser
    // =========================================================================

    fun parseRssFromStatm(statmText: String, pageSizeBytes: Long = 4096L): Long {
        val parts = statmText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            val rssPages = parts[1].toLongOrNull() ?: 0L
            return (rssPages * pageSizeBytes) / 1024L
        }
        return 0L
    }

    fun parseStatusUid(lines: Sequence<String>): Int? {
        for (line in lines) {
            if (line.startsWith("Uid:")) {
                val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                if (parts.size >= 2) {
                    return parts[1].toIntOrNull()
                }
                break
            }
        }
        return null
    }

    fun getActiveProcesses(
        context: Context? = null,
        showKernelThreads: Boolean = false
    ): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        val packageManager = context?.packageManager
        val uidCache = mutableMapOf<Int, String>()

        try {
            val procDir = File("/proc")
            val files = procDir.listFiles() ?: return emptyList()

            // Android devices can use 4 KB or 16 KB kernel pages.
            val pageSizeBytes = android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE).coerceAtLeast(0L)

            for (file in files) {
                if (file.isDirectory) {
                    val pid = file.name.toIntOrNull() ?: continue
                    try {
                        // Read process command line / package name
                        val cmdlineFile = File(file, "cmdline")
                        var processName = ""
                        if (cmdlineFile.exists()) {
                            val cmdRaw = cmdlineFile.readText().trim()
                            // cmdline is null-terminated, replace null chars
                            processName = cmdRaw.replace('\u0000', ' ').trim()
                        }

                        // Fallback to name in stat parentheses if cmdline is empty
                        if (processName.isEmpty()) {
                            val statFile = File(file, "stat")
                            if (statFile.exists()) {
                                val statText = statFile.readText()
                                val start = statText.indexOf('(')
                                val end = statText.indexOf(')')
                                if (start in 0 until end) {
                                    processName = "[" + statText.substring(start + 1, end) + "]"
                                }
                            }
                        }

                        val isKernelThread = processName.startsWith("[")
                        if (processName.isEmpty() || (isKernelThread && !showKernelThreads)) {
                            // Skip system kernel threads unless showKernelThreads is enabled
                            continue
                        }

                        // Read RSS (Resident Set Size) from statm (2nd column is RSS page count)
                        val statmFile = File(file, "statm")
                        var ramKb = 0L
                        if (statmFile.exists()) {
                            ramKb = parseRssFromStatm(statmFile.readText(), pageSizeBytes)
                        }

                        // Parse UID status line from /proc/[pid]/status
                        var userLabel = "unknown"
                        val statusFile = File(file, "status")
                        if (statusFile.exists()) {
                            try {
                                val uid = statusFile.useLines { parseStatusUid(it) }
                                if (uid != null) {
                                    userLabel = resolveUid(uid, packageManager, uidCache)
                                }
                            } catch (_: Exception) {}
                        }

                        if (userLabel == "unknown" && pid == Process.myPid()) {
                            userLabel = resolveUid(Process.myUid(), packageManager, uidCache)
                        } else if (userLabel == "unknown" && isKernelThread) {
                            userLabel = "root"
                        }

                        if (ramKb > 0L || (showKernelThreads && isKernelThread)) {
                            processes.add(ProcessInfo(pid, processName, ramKb, user = userLabel))
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return processes.sortedByDescending { it.ramKb }
    }

    fun resolveUid(
        uid: Int,
        packageManager: PackageManager? = null,
        cache: MutableMap<Int, String>? = null
    ): String {
        cache?.get(uid)?.let { return it }

        val resolved = when (uid) {
            0 -> "root"
            1000 -> "system"
            1001 -> "radio"
            1002 -> "bluetooth"
            1010 -> "wifi"
            1013 -> "media"
            1014 -> "drm"
            1021 -> "gps"
            1023 -> "media_rw"
            1024 -> "mtp"
            1028 -> "audioserver"
            1037 -> "cameraserver"
            1066 -> "statsd"
            1073 -> "incidentd"
            2000 -> "shell"
            9999 -> "nobody"
            else -> {
                var pkgName: String? = null
                if (packageManager != null) {
                    try {
                        pkgName = packageManager.getNameForUid(uid)
                    } catch (_: Exception) {}
                }
                if (!pkgName.isNullOrBlank()) {
                    pkgName
                } else {
                    val userId = uid / 100000
                    val appId = uid % 100000
                    if (appId >= 10000) {
                        "u${userId}_a${appId - 10000}"
                    } else {
                        "uid:$uid"
                    }
                }
            }
        }

        cache?.put(uid, resolved)
        return resolved
    }

    // =========================================================================
    // Subsection Comment: Battery Diagnostics Collector
    // =========================================================================

    fun getBatteryState(context: Context): BatteryState {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)

        var level = 0
        var scale = 100
        var tempCelsius = 0.0f
        var healthStr = "Unknown"
        var statusStr = "Unknown"
        var voltage = 0
        var powerSource = "Battery"

        batteryStatus?.let { intent ->
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            tempCelsius = rawTemp / 10f // Celsius is represented in tenths

            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            healthStr = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            statusStr = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }

            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            powerSource = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Battery"
            }
        }

        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        return BatteryState(
            levelPercentage = percentage,
            tempCelsius = tempCelsius,
            health = healthStr,
            status = statusStr,
            voltageMv = voltage,
            powerSource = powerSource
        )
    }

    // =========================================================================
    // Subsection Comment: Helper File Readers
    // =========================================================================

    private fun readLongFromFile(path: String, default: Long): Long {
        val file = File(path)
        if (!file.exists()) return default
        return try {
            file.readText().trim().toLongOrNull() ?: default
        } catch (_: Exception) {
            default
        }
    }

    // =========================================================================
    // Subsection Comment: Real-Time Flow Streams
    // =========================================================================

    fun streamCpu(intervalMs: Long = 1000L): Flow<CpuState> = flow {
        val sampler = CpuUsageSampler()
        while (true) {
            emit(getCpuState(sampler))
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    fun streamMemory(intervalMs: Long = 1000L): Flow<MemoryState> = flow {
        while (true) {
            emit(getMemoryState())
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    fun streamBattery(context: Context, intervalMs: Long = 5000L): Flow<BatteryState> = flow {
        while (true) {
            emit(getBatteryState(context))
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)
}
