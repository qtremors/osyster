package dev.qtremors.osyster.core.device.data.pages

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.format.UnitFormat
import dev.qtremors.osyster.core.device.data.read.CpuCluster
import dev.qtremors.osyster.core.device.data.read.CpuReader
import dev.qtremors.osyster.core.device.data.read.GpuInfo
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection

/**
 * The CPU and GPU page. Topology and GPU strings are read once; clocks, load, and thermals are
 * passed in fresh on every tick by the repository.
 */
internal class ProcessorPageCollector(private val context: Context, private val text: DeviceText) {

    fun collect(clusters: List<CpuCluster>, gpu: GpuInfo, load: Availability<Int>): List<InfoSection> =
        listOf(cpu(clusters, load)) + clusters.mapIndexed { index, cluster -> cluster(index, cluster) } +
            listOf(liveClocks(), gpu(gpu), thermal())

    private fun cpu(clusters: List<CpuCluster>, load: Availability<Int>) = section(text, "cpu", R.string.section_cpu, "sysfs") {
        row("chip", R.string.row_chip, CpuReader.chipName())
        row("cores", R.string.row_cores, CpuReader.possibleCores().size.toString(), copyable = false)
        row("clusters", R.string.row_clusters, clusters.size.toString(), copyable = false)
        row("abi", R.string.row_primary_abi, Build.SUPPORTED_ABIS.firstOrNull())
        row("load", R.string.row_cpu_load, when (load) {
            is Availability.Available -> available("${load.value}%", "/proc/stat")
            is Availability.Restricted -> load
            is Availability.Unavailable -> load
            else -> Availability.Unavailable("No sample yet", retryable = true)
        }, copyable = false)
    }

    private fun cluster(index: Int, cluster: CpuCluster): InfoSection {
        val title = text.format(R.string.section_cluster, index + 1, coreRange(cluster.cores))
        val builder = SectionBuilder(text, "cpufreq")
        builder.row("min", R.string.row_min_frequency, cluster.minKhz?.let(UnitFormat::frequencyKhz))
        builder.row("max", R.string.row_max_frequency, cluster.maxKhz?.let(UnitFormat::frequencyKhz))
        builder.row("governor", R.string.row_governor, cluster.governor)
        val online = cluster.cores.count(CpuReader::isOnline)
        builder.row("online", R.string.row_online_cores, "$online / ${cluster.cores.size}", copyable = false)
        return builder.build("cluster_$index", title)
    }

    private fun liveClocks(): InfoSection {
        val builder = SectionBuilder(text, "scaling_cur_freq")
        CpuReader.possibleCores().forEach { core ->
            val label = text.format(R.string.row_core, core + 1)
            val value = when {
                !CpuReader.isOnline(core) -> builder.available(text[R.string.value_offline])
                else -> CpuReader.currentKhz(core)?.let { builder.available(UnitFormat.frequencyKhz(it)) }
                    ?: SectionBuilder.restrictedProc
            }
            builder.row("core_$core", label, value)
        }
        return builder.build("live_clocks", text[R.string.section_live_clocks])
    }

    private fun gpu(gpu: GpuInfo) = section(text, "gpu", R.string.section_gpu, "EGL / OpenGL ES") {
        row("vendor", R.string.row_gpu_vendor, gpu.vendor)
        row("renderer", R.string.row_gpu_renderer, gpu.renderer)
        row("gles", R.string.row_opengl_es, gpu.glVersion ?: gpu.glEsDeclared)
        row("vulkan", R.string.row_vulkan, if (gpu.vulkanSupported) gpu.vulkanVersion ?: text[R.string.value_supported] else text[R.string.value_not_supported], source = "PackageManager")
        row("egl", R.string.row_egl, gpu.eglVersion, advanced = true)
        row("glsl", R.string.row_glsl, gpu.shadingLanguage, advanced = true)
        row("max_texture", R.string.row_max_texture, gpu.maxTextureSize?.let { "$it × $it px" }, advanced = true)
        row("extensions", R.string.row_gl_extensions, gpu.extensionCount?.toString(), advanced = true, copyable = false)
    }

    private fun thermal() = section(text, "thermal", R.string.section_thermal, "PowerManager") {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && power != null) {
            row("status", R.string.row_thermal_status, text[thermalStatus(power.currentThermalStatus)], copyable = false)
        } else {
            row("status", R.string.row_thermal_status, Availability.Unsupported("Needs Android 10"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && power != null) {
            val headroom = runCatching { power.getThermalHeadroom(0) }.getOrNull()
            row("headroom", R.string.row_thermal_headroom, if (headroom != null && headroom.isFinite() && headroom >= 0f) {
                available(text.format(R.string.value_headroom, (headroom * 100).toInt(), text[headroomMeaning(headroom)]))
            } else {
                Availability.Unavailable("Not ready yet", retryable = true)
            }, copyable = false)
        } else {
            row("headroom", R.string.row_thermal_headroom, Availability.Unsupported("Needs Android 11"))
        }
        row("cpu_temp", R.string.row_cpu_temperature, CpuReader.cpuTemperature()?.let { available(UnitFormat.celsius(it), "thermal sysfs") }
            ?: Availability.Restricted("No readable CPU thermal zone"), copyable = false)
    }

    private fun coreRange(cores: List<Int>): String =
        if (cores.size == 1) "${cores.first() + 1}" else "${cores.first() + 1}–${cores.last() + 1}"

    private fun thermalStatus(status: Int): Int = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> R.string.thermal_none
        PowerManager.THERMAL_STATUS_LIGHT -> R.string.thermal_light
        PowerManager.THERMAL_STATUS_MODERATE -> R.string.thermal_moderate
        PowerManager.THERMAL_STATUS_SEVERE -> R.string.thermal_severe
        PowerManager.THERMAL_STATUS_CRITICAL -> R.string.thermal_critical
        PowerManager.THERMAL_STATUS_EMERGENCY -> R.string.thermal_emergency
        PowerManager.THERMAL_STATUS_SHUTDOWN -> R.string.thermal_shutdown
        else -> R.string.value_unknown
    }

    /** Android defines 1.0 as the point where the device starts severe throttling. */
    private fun headroomMeaning(value: Float): Int = when {
        value >= 1f -> R.string.headroom_throttling
        value >= 0.8f -> R.string.headroom_close
        value >= 0.5f -> R.string.headroom_warm
        else -> R.string.headroom_cool
    }
}
