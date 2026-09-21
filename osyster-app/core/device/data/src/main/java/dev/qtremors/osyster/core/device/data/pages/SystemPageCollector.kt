package dev.qtremors.osyster.core.device.data.pages

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.system.Os
import android.system.OsConstants
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.format.UnitFormat
import dev.qtremors.osyster.core.device.data.parse.ProcParsers
import dev.qtremors.osyster.core.device.data.read.SystemFiles
import dev.qtremors.osyster.core.device.data.read.SystemProps
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection
import java.util.Locale
import java.util.TimeZone

/** The System page: Android version and updates, build, firmware, and runtime. */
internal class SystemPageCollector(private val context: Context, private val text: DeviceText) {

    fun collect(): List<InfoSection> = listOf(android(), build(), firmware(), runtime())

    private fun android() = section(text, "android", R.string.section_android, "Build.VERSION") {
        row("version", R.string.row_android_version, "Android ${Build.VERSION.RELEASE}")
        row("api", R.string.row_api_level, apiLevel())
        row("patch", R.string.row_security_patch, Build.VERSION.SECURITY_PATCH.takeIf(String::isNotBlank)?.let(UnitFormat::patchDate))
        row("play_update", R.string.row_play_system_update, playSystemUpdate(), source = "PackageManager")
        row(
            "released_with",
            R.string.row_released_with,
            SystemProps.get("ro.product.first_api_level")?.toIntOrNull()?.let(::initialAndroid),
            source = "ro.product.first_api_level"
        )
        row("release_type", R.string.row_release_type, releaseType(), advanced = true)
        row("base_os", R.string.row_base_os, Build.VERSION.BASE_OS, advanced = true)
    }

    private fun build() = section(text, "build", R.string.section_build, "Build") {
        row("build_number", R.string.row_build_number, Build.DISPLAY)
        row("build_id", R.string.row_build_id, Build.ID)
        row("build_date", R.string.row_build_date, UnitFormat.dateTime(Build.TIME))
        row("build_type", R.string.row_build_type, Build.TYPE)
        row("build_tags", R.string.row_build_tags, Build.TAGS)
        row("incremental", R.string.row_incremental, Build.VERSION.INCREMENTAL, advanced = true)
        row("fingerprint", R.string.row_fingerprint, Build.FINGERPRINT, advanced = true)
    }

    private fun firmware() = section(text, "firmware", R.string.section_firmware, "Build") {
        val procVersion = SystemFiles.text("/proc/version")
        val kernel = procVersion?.let(ProcParsers::kernelRelease) ?: System.getProperty("os.version")
        row("kernel", R.string.row_kernel, kernel, source = "/proc/version")
        row("baseband", R.string.row_baseband, runCatching { Build.getRadioVersion() }.getOrNull())
        row("bootloader", R.string.row_bootloader, Build.BOOTLOADER)
        row("kernel_full", R.string.row_kernel_full, procVersion, advanced = true, source = "/proc/version")
    }

    private fun runtime() = section(text, "runtime", R.string.section_runtime, "System") {
        row("vm", R.string.row_java_vm, listOfNotNull(System.getProperty("java.vm.name"), System.getProperty("java.vm.version")).joinToString(" "))
        row("page_size", R.string.row_page_size, pageSize(), source = "Os.sysconf")
        row("performance_class", R.string.row_performance_class, performanceClass())
        row("uptime", R.string.row_uptime, UnitFormat.duration(SystemClock.elapsedRealtime()), copyable = false)
        val deepSleep = (SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()).coerceAtLeast(0)
        row("deep_sleep", R.string.row_deep_sleep, UnitFormat.duration(deepSleep), copyable = false)
        row("usb_debugging", R.string.row_usb_debugging, onOff(Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1), copyable = false)
        row("language", R.string.row_language, Locale.getDefault().displayName, advanced = true)
        row("time_zone", R.string.row_time_zone, TimeZone.getDefault().id, advanced = true)
        row("architecture", R.string.row_vm_architecture, System.getProperty("os.arch"), advanced = true)
    }

    private fun apiLevel(): String =
        if (Build.VERSION.SDK_INT >= 36) {
            val full = Build.VERSION.SDK_INT_FULL
            val minor = full % 100_000
            if (minor > 0) "${Build.VERSION.SDK_INT}.$minor" else Build.VERSION.SDK_INT.toString()
        } else {
            Build.VERSION.SDK_INT.toString()
        }

    private fun releaseType(): String =
        if (Build.VERSION.CODENAME == "REL") text[R.string.value_release]
        else text.format(R.string.value_preview, Build.VERSION.CODENAME)

    private fun initialAndroid(sdk: Int): String? = ANDROID_NAMES[sdk]?.let { "Android $it (API $sdk)" }
        ?: sdk.takeIf { it > 0 }?.let { "API $it" }

    private fun playSystemUpdate(): String? = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(MAINLINE_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(MAINLINE_PACKAGE, 0)
        }
        info.versionName
    }.getOrNull()

    private fun pageSize(): String? = runCatching { Os.sysconf(OsConstants._SC_PAGESIZE) }
        .getOrNull()?.takeIf { it > 0 }?.let { "${it / 1024} KB" }

    private fun performanceClass(): Availability<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val value = Build.VERSION.MEDIA_PERFORMANCE_CLASS
            if (value > 0) Availability.Available(text.format(R.string.value_performance_class, value), "Build.VERSION", System.currentTimeMillis())
            else Availability.Available(text[R.string.value_not_declared], "Build.VERSION", System.currentTimeMillis())
        } else {
            Availability.Unsupported("Needs Android 12")
        }

    private fun onOff(on: Boolean) = text[if (on) R.string.value_on else R.string.value_off]

    private companion object {
        const val MAINLINE_PACKAGE = "com.google.android.modulemetadata"
        val ANDROID_NAMES = mapOf(
            26 to "8.0", 27 to "8.1", 28 to "9", 29 to "10", 30 to "11", 31 to "12", 32 to "12L",
            33 to "13", 34 to "14", 35 to "15", 36 to "16", 37 to "17"
        )
    }
}
