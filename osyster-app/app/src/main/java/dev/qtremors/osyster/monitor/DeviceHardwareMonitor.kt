package dev.qtremors.osyster.monitor

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaDrm
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

// =========================================================================
// Data Models for Hardware & System Specifications
// =========================================================================

data class DeviceHeroState(
    val deviceName: String = Build.MODEL ?: "Android Device",
    val manufacturer: String = Build.MANUFACTURER ?: "",
    val model: String = Build.MODEL ?: "",
    val androidVersion: String = "Android " + (Build.VERSION.RELEASE ?: ""),
    val apiLevel: String = Build.VERSION.SDK_INT.toString(),
    val securityPatch: String = Build.VERSION.SECURITY_PATCH ?: "",
    val uptimeMillis: Long = 0L,
    val awakeMillis: Long = 0L,
    val deepSleepMillis: Long = 0L,
    val deepSleepPercentage: Float = 0f,
    val awakePercentage: Float = 0f,
    val formattedUptime: String = "00:00:00",
    val formattedDeepSleep: String = "0m",
    val formattedAwake: String = "0m"
)

data class SystemSpecs(
    val androidVersion: String = Build.VERSION.RELEASE ?: "unknown",
    val apiLevel: String = Build.VERSION.SDK_INT.toString(),
    val releasedWith: String = "unknown",
    val playUpdate: String = "unknown",
    val buildNumber: String = Build.DISPLAY ?: Build.ID ?: "unknown",
    val securityPatch: String = Build.VERSION.SECURITY_PATCH ?: "unknown",
    val board: String = Build.BOARD ?: "unknown",
    val hardware: String = Build.HARDWARE ?: "unknown",
    val kernelVersion: String = "unknown",
    val basebandVersion: String = "unknown",
    val buildTags: String = Build.TAGS ?: "unknown",
    val buildFingerprint: String = Build.FINGERPRINT ?: "unknown",
    val buildType: String = Build.TYPE ?: "unknown",
    val bootloader: String = Build.BOOTLOADER ?: "unknown",
    val performanceClass: String = "unknown",
    val minTargetSdk: String = "unknown",
    val language: String = Locale.getDefault().displayName,
    val uptime: String = "unknown",
    val isUsbDebugging: Boolean = false,
    val selinuxStatus: String = "unknown",
    val rootStatus: String = "Not detected",
    val isEmulator: Boolean = false,
    val pageSizeKb: Long = 4L,
    val isTrebleEnabled: Boolean = false,
    val seamlessUpdates: Boolean = false,
    val activeSlot: String = "unknown"
)

data class DeviceDisplaySpecs(
    val brand: String = Build.BRAND ?: "unknown",
    val manufacturer: String = Build.MANUFACTURER ?: "unknown",
    val product: String = Build.PRODUCT ?: "unknown",
    val model: String = Build.MODEL ?: "unknown",
    val sku: String = "unknown",
    val displayName: String = "Built-in Screen",
    val resolution: String = "unknown",
    val diagonalInches: String = "unknown",
    val aspectRatio: String = "unknown",
    val refreshRateHz: String = "unknown",
    val supportedRefreshRates: String = "unknown",
    val pixelDensityDpi: String = "unknown",
    val densityBucket: String = "unknown",
    val defaultOrientation: String = "Portrait",
    val hdrSupported: Boolean = false,
    val hdrFormats: String = "None",
    val hasFingerprint: Boolean = false,
    val hasFaceAuth: Boolean = false,
    val hasVulkan: Boolean = false,
    val hasEncryption: Boolean = false
)

data class CpuClusterInfo(
    val clusterId: Int,
    val coreCount: Int,
    val coreRange: String,
    val minFreqKhz: Long,
    val maxFreqKhz: Long,
    val governor: String
)

data class GpuSpecs(
    val vendor: String = "unknown",
    val renderer: String = "unknown",
    val glEsVersion: String = "unknown",
    val vulkanVersion: String = "unknown"
)

data class StorageStats(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val usedBytes: Long = 0L
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f
}

data class CameraInfo(
    val id: String,
    val facing: String,
    val megapixels: Float,
    val resolution: String,
    val apertures: List<String>,
    val oisSupported: Boolean,
    val eisSupported: Boolean,
    val flashSupported: Boolean,
    val focalLengths: List<String>
)

data class SensorItem(
    val type: Int,
    val name: String,
    val vendor: String,
    val typeString: String,
    val maxRange: Float,
    val resolution: Float,
    val powerMa: Float,
    val version: Int
)

data class ConnectivitySpecs(
    val networkType: String = "None",
    val isConnected: Boolean = false,
    val linkDownMbps: Int = 0,
    val linkUpMbps: Int = 0,
    val wifiSsid: String = "unknown",
    val wifiFrequencyMhz: Int = 0,
    val wifiRssi: Int = 0,
    val wifiLinkSpeedMbps: Int = 0,
    val localIpv4: String = "unknown",
    val localIpv6: String = "unknown",
    val gateway: String = "unknown",
    val dnsServers: String = "unknown",
    val subnetMask: String = "unknown",
    val bluetoothSupported: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val bluetoothLeSupported: Boolean = false
)

data class MediaDrmSpecs(
    val widevineSecurityLevel: String = "Not detected",
    val widevineVendor: String = "unknown",
    val widevineVersion: String = "unknown",
    val widevineSystemId: String = "unknown",
    val widevineAlgorithms: String = "unknown",
    val widevineHdcpLevel: String = "unknown",
    val widevineMaxHdcpLevel: String = "unknown",
    val clearkeyVendor: String = "unknown",
    val clearkeyVersion: String = "unknown",
    val clearkeyHdcpLevel: String = "unknown",
    val clearkeyMaxHdcpLevel: String = "unknown"
)

// =========================================================================
// Native Telemetry Collector Engine
// =========================================================================

object DeviceHardwareMonitor {

    private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
    private val CLEARKEY_UUID = UUID(-0x1d8e40a7e7e24c6dL, -0x4ff6a85e3cd8f1f0L)

    fun getSystemProperty(key: String, default: String = "unknown"): String {
        return runCatching {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            getMethod.invoke(null, key, default) as? String ?: default
        }.getOrDefault(default)
    }

    fun getSystemSpecs(context: Context): SystemSpecs {
        val radioVersion = runCatching { Build.getRadioVersion() ?: "unknown" }.getOrDefault("unknown")

        // Read kernel version from /proc/version
        val kernelVersion = runCatching {
            val versionFile = File("/proc/version")
            if (versionFile.canRead()) {
                versionFile.readText().trim()
            } else {
                System.getProperty("os.version") ?: "unknown"
            }
        }.getOrDefault("unknown")

        // Google Play system update
        val playUpdate = runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    "com.google.android.modulemetadata",
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo("com.google.android.modulemetadata", 0)
            }
            packageInfo.versionName ?: "unknown"
        }.getOrDefault("unknown")

        // Released with Android version
        val firstApi = getSystemProperty("ro.product.first_api_level", "").toIntOrNull()
            ?: getSystemProperty("ro.board.first_api_level", "").toIntOrNull()
        val releasedWith = if (firstApi != null && firstApi > 0) {
            getAndroidVersionNameForApi(firstApi)
        } else {
            "Android " + (Build.VERSION.RELEASE ?: "unknown")
        }

        // Performance Class
        val performanceClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pc = Build.VERSION.MEDIA_PERFORMANCE_CLASS
            if (pc > 0) "Class $pc" else "Not declared"
        } else {
            "Not supported"
        }

        // Min Target SDK
        val minTargetSdk = context.applicationInfo.minSdkVersion.toString()

        // Uptime formatted
        val uptimeMillis = SystemClock.elapsedRealtime()
        val uptimeHours = uptimeMillis / (1000 * 60 * 60)
        val uptimeMinutes = (uptimeMillis % (1000 * 60 * 60)) / (1000 * 60)
        val uptimeSeconds = (uptimeMillis % (1000 * 60)) / 1000
        val formattedUptime = String.format(Locale.getDefault(), "%02d:%02d:%02d", uptimeHours, uptimeMinutes, uptimeSeconds)

        // USB Debugging
        val isAdb = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

        // SELinux Status
        val selinuxStatus = runCatching {
            val enforceFile = File("/sys/fs/selinux/enforce")
            if (enforceFile.exists() && enforceFile.canRead()) {
                val value = enforceFile.readText().trim()
                if (value == "1") "Enforcing" else "Permissive"
            } else {
                val prop = getSystemProperty("ro.boot.selinux", "")
                if (prop.equals("permissive", ignoreCase = true)) "Permissive" else "Enforcing"
            }
        }.getOrDefault("Enforcing")

        // Root Detection check
        val rootPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup"
        )
        val hasRootBinary = rootPaths.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
        val isTestKeys = Build.TAGS?.contains("test-keys") == true
        val rootStatus = if (hasRootBinary) "Detected" else if (isTestKeys) "Test Keys detected" else "Not detected"

        // Emulator Detection
        val isEmulator = (Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.lowercase(Locale.ROOT).contains("droid4x") ||
                Build.MODEL.contains("Emulator") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu"))

        // Kernel page size
        val pageSizeKb = runCatching {
            val bytes = android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
            if (bytes > 0) bytes / 1024L else 4L
        }.getOrDefault(4L)

        // Treble & A/B slot
        val trebleProp = getSystemProperty("ro.treble.enabled", "false")
        val isTreble = trebleProp.equals("true", ignoreCase = true)
        val abUpdateProp = getSystemProperty("ro.build.ab_update", "false")
        val isSeamless = abUpdateProp.equals("true", ignoreCase = true)
        val slotProp = getSystemProperty("ro.boot.slot_suffix", "")
        val activeSlot = if (slotProp.isNotBlank()) slotProp.removePrefix("_") else "Single"

        return SystemSpecs(
            androidVersion = "Android " + (Build.VERSION.RELEASE ?: "unknown"),
            apiLevel = Build.VERSION.SDK_INT.toString(),
            releasedWith = releasedWith,
            playUpdate = playUpdate,
            buildNumber = Build.DISPLAY ?: Build.ID ?: "unknown",
            securityPatch = Build.VERSION.SECURITY_PATCH ?: "unknown",
            board = Build.BOARD ?: "unknown",
            hardware = Build.HARDWARE ?: "unknown",
            kernelVersion = kernelVersion,
            basebandVersion = radioVersion,
            buildTags = Build.TAGS ?: "unknown",
            buildFingerprint = Build.FINGERPRINT ?: "unknown",
            buildType = Build.TYPE ?: "unknown",
            bootloader = Build.BOOTLOADER ?: "unknown",
            performanceClass = performanceClass,
            minTargetSdk = minTargetSdk,
            language = Locale.getDefault().displayName,
            uptime = formattedUptime,
            isUsbDebugging = isAdb,
            selinuxStatus = selinuxStatus,
            rootStatus = rootStatus,
            isEmulator = isEmulator,
            pageSizeKb = pageSizeKb,
            isTrebleEnabled = isTreble,
            seamlessUpdates = isSeamless,
            activeSlot = activeSlot
        )
    }

    private fun getAndroidVersionNameForApi(apiLevel: Int): String {
        return when (apiLevel) {
            24 -> "Android 7.0 (Nougat)"
            25 -> "Android 7.1 (Nougat)"
            26 -> "Android 8.0 (Oreo)"
            27 -> "Android 8.1 (Oreo)"
            28 -> "Android 9 (Pie)"
            29 -> "Android 10"
            30 -> "Android 11"
            31 -> "Android 12"
            32 -> "Android 12L"
            33 -> "Android 13"
            34 -> "Android 14"
            35 -> "Android 15"
            36 -> "Android 16"
            else -> "Android API $apiLevel"
        }
    }

    fun getDisplaySpecs(context: Context): DeviceDisplaySpecs {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val metrics = DisplayMetrics()

        @Suppress("DEPRECATION")
        val display = windowManager?.defaultDisplay
        @Suppress("DEPRECATION")
        display?.getRealMetrics(metrics)

        val widthPx = metrics.widthPixels
        val heightPx = metrics.heightPixels
        val xdpi = if (metrics.xdpi > 0f) metrics.xdpi else metrics.densityDpi.toFloat()
        val ydpi = if (metrics.ydpi > 0f) metrics.ydpi else metrics.densityDpi.toFloat()

        val widthInches = widthPx / xdpi
        val heightInches = heightPx / ydpi
        val diagonal = sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
        val diagonalStr = String.format(Locale.getDefault(), "%.2f\"", diagonal)

        val refreshRateHz = display?.refreshRate?.roundToInt() ?: 60
        val supportedRefreshRates = runCatching {
            display?.supportedModes?.map { it.refreshRate.roundToInt() }?.distinct()?.sorted()?.joinToString(", ") { "${it} Hz" }
        }.getOrNull() ?: "${refreshRateHz} Hz"

        // Aspect ratio simplification
        val gcdVal = gcd(widthPx, heightPx)
        val aspectW = widthPx / gcdVal
        val aspectH = heightPx / gcdVal
        val ratioStr = if (aspectH > 0 && aspectW > 0) {
            val r = heightPx.toFloat() / widthPx.toFloat()
            String.format(Locale.getDefault(), "%.1f:9 (%d:%d)", r * 9f, aspectH, aspectW)
        } else "unknown"

        val densityBucket = when (metrics.densityDpi) {
            DisplayMetrics.DENSITY_LOW -> "ldpi"
            DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
            DisplayMetrics.DENSITY_HIGH -> "hdpi"
            DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
            DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
            DisplayMetrics.DENSITY_XXXHIGH -> "xxxhdpi"
            else -> "dpi"
        }

        // Orientation
        val isPortrait = heightPx >= widthPx
        val orientation = if (isPortrait) "Portrait" else "Landscape"

        // HDR
        var hdrSupported = false
        var hdrFormats = "None"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hdrCapabilities = display?.hdrCapabilities
            val types = hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
            hdrSupported = types.isNotEmpty()
            if (hdrSupported) {
                hdrFormats = types.joinToString(", ") { type ->
                    when (type) {
                        DisplayMetrics.DENSITY_LOW -> "HDR10"
                        1 -> "Dolby Vision"
                        2 -> "HDR10"
                        3 -> "HLG"
                        4 -> "HDR10+"
                        else -> "Type $type"
                    }
                }
            }
        }

        // Hardware Features
        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else false
        val hasVulkan = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val hasEncryption = dpm?.storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                dpm?.storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING

        val sku = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SKU.ifEmpty { getSystemProperty("ro.boot.hardware.sku", "unknown") }
        } else {
            getSystemProperty("ro.boot.hardware.sku", "unknown")
        }

        return DeviceDisplaySpecs(
            brand = Build.BRAND ?: "unknown",
            manufacturer = Build.MANUFACTURER ?: "unknown",
            product = Build.PRODUCT ?: "unknown",
            model = Build.MODEL ?: "unknown",
            sku = sku,
            displayName = display?.name ?: "Built-in Screen",
            resolution = "$widthPx x $heightPx",
            diagonalInches = diagonalStr,
            aspectRatio = ratioStr,
            refreshRateHz = "$refreshRateHz Hz",
            supportedRefreshRates = supportedRefreshRates,
            pixelDensityDpi = "${metrics.densityDpi} dpi",
            densityBucket = "${metrics.densityDpi} dpi ($densityBucket)",
            defaultOrientation = orientation,
            hdrSupported = hdrSupported,
            hdrFormats = hdrFormats,
            hasFingerprint = hasFingerprint,
            hasFaceAuth = hasFace,
            hasVulkan = hasVulkan,
            hasEncryption = hasEncryption
        )
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return if (x == 0) 1 else x
    }

    fun getCpuClusters(): List<CpuClusterInfo> {
        val clusters = mutableListOf<CpuClusterInfo>()
        val coresCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        val visitedCores = mutableSetOf<Int>()
        var clusterId = 1

        for (core in 0 until coresCount) {
            if (visitedCores.contains(core)) continue

            // Read related or affected cores
            val relatedFile = File("/sys/devices/system/cpu/cpu$core/cpufreq/related_cpus")
            val affectedFile = File("/sys/devices/system/cpu/cpu$core/cpufreq/affected_cpus")
            val targetCores = mutableListOf<Int>()

            val fileToRead = if (relatedFile.exists() && relatedFile.canRead()) relatedFile
            else if (affectedFile.exists() && affectedFile.canRead()) affectedFile else null

            if (fileToRead != null) {
                runCatching {
                    val content = fileToRead.readText().trim()
                    content.split("\\s+".toRegex()).forEach { token ->
                        token.toIntOrNull()?.let { targetCores.add(it) }
                    }
                }
            }

            if (targetCores.isEmpty()) {
                targetCores.add(core)
            }

            visitedCores.addAll(targetCores)

            // Frequencies & Governor
            val minFreq = readLongFromFile("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_min_freq", 0L)
                .takeIf { it > 0 } ?: readLongFromFile("/sys/devices/system/cpu/cpu$core/cpufreq/scaling_min_freq", 0L)
            val maxFreq = readLongFromFile("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq", 0L)
                .takeIf { it > 0 } ?: readLongFromFile("/sys/devices/system/cpu/cpu$core/cpufreq/scaling_max_freq", 0L)
            val governor = runCatching {
                File("/sys/devices/system/cpu/cpu$core/cpufreq/scaling_governor").readText().trim()
            }.getOrDefault("N/A")

            val sortedCores = targetCores.sorted()
            val rangeStr = if (sortedCores.size == 1) {
                "Core ${sortedCores.first()}"
            } else {
                "Cores ${sortedCores.first()}-${sortedCores.last()}"
            }

            clusters.add(
                CpuClusterInfo(
                    clusterId = clusterId++,
                    coreCount = sortedCores.size,
                    coreRange = rangeStr,
                    minFreqKhz = minFreq,
                    maxFreqKhz = maxFreq,
                    governor = governor
                )
            )
        }

        return clusters
    }

    private fun readLongFromFile(path: String, default: Long): Long {
        val file = File(path)
        if (!file.exists()) return default
        return try {
            file.readText().trim().toLongOrNull() ?: default
        } catch (_: Exception) {
            default
        }
    }

    fun getGpuSpecs(context: Context): GpuSpecs {
        var vendor = "unknown"
        var renderer = "unknown"
        var glEsVersion = "unknown"
        var vulkanVersion = "unknown"

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val configInfo = activityManager?.deviceConfigurationInfo
        if (configInfo != null) {
            glEsVersion = configInfo.glEsVersion ?: "unknown"
        }

        // Off-screen EGL context query to extract GPU Vendor and Renderer safely
        try {
            val dpy: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            if (EGL14.eglInitialize(dpy, version, 0, version, 1)) {
                val configAttribs = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (EGL14.eglChooseConfig(dpy, configAttribs, 0, configs, 0, 1, numConfigs, 0) && numConfigs[0] > 0) {
                    val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                    val ctx: EGLContext = EGL14.eglCreateContext(dpy, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
                    val pbufferAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                    val surface: EGLSurface = EGL14.eglCreatePbufferSurface(dpy, configs[0], pbufferAttribs, 0)

                    if (EGL14.eglMakeCurrent(dpy, surface, surface, ctx)) {
                        renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "unknown"
                        vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "unknown"
                    }
                    EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    EGL14.eglDestroySurface(dpy, surface)
                    EGL14.eglDestroyContext(dpy, ctx)
                }
                EGL14.eglTerminate(dpy)
            }
        } catch (_: Exception) {}

        // Fallback for vendor/renderer if offscreen query is blocked on some devices
        if (renderer == "unknown") {
            val hardware = Build.HARDWARE.lowercase(Locale.ROOT)
            if (hardware.contains("qcom") || hardware.contains("qualcomm") || hardware.contains("taro") || hardware.contains("lahaina")) {
                vendor = "Qualcomm"
                renderer = "Adreno GPU"
            } else if (hardware.contains("mali") || hardware.contains("exynos") || hardware.contains("tensor")) {
                vendor = "ARM"
                renderer = "Mali GPU"
            }
        }

        // Vulkan version
        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
            val featureInfo = pm.systemAvailableFeatures.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
            val versionInt = featureInfo?.version ?: 0
            if (versionInt > 0) {
                val major = versionInt shr 22
                val minor = (versionInt shr 12) and 0x3ff
                val patch = versionInt and 0xfff
                vulkanVersion = "$major.$minor.$patch"
            } else {
                vulkanVersion = "Supported"
            }
        } else {
            vulkanVersion = "Not supported"
        }

        return GpuSpecs(
            vendor = vendor,
            renderer = renderer,
            glEsVersion = glEsVersion,
            vulkanVersion = vulkanVersion
        )
    }

    fun getStorageStats(): StorageStats {
        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)

            StorageStats(
                totalBytes = totalBytes,
                availableBytes = availableBytes,
                usedBytes = usedBytes
            )
        }.getOrDefault(StorageStats())
    }

    fun getCameraList(context: Context): List<CameraInfo> {
        val cameras = mutableListOf<CameraInfo>()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return emptyList()

        runCatching {
            val idList = cameraManager.cameraIdList
            for (id in idList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
                val facing = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front Camera"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back Camera"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External Camera"
                    else -> "Camera #$id"
                }

                // Megapixels & Resolution
                val pixelSize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                    ?: chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)?.let { android.util.Size(it.width(), it.height()) }
                val width = pixelSize?.width ?: 0
                val height = pixelSize?.height ?: 0
                val megapixels = if (width > 0 && height > 0) {
                    (width.toFloat() * height.toFloat()) / 1_000_000f
                } else 0f
                val resolutionStr = if (width > 0 && height > 0) "$width x $height" else "unknown"

                // Apertures
                val aperturesArray = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES) ?: floatArrayOf()
                val apertures = aperturesArray.map { String.format(Locale.getDefault(), "f/%.2f", it) }

                // Stabilization
                val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: intArrayOf()
                val oisSupported = oisModes.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON)

                val eisModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: intArrayOf()
                val eisSupported = eisModes.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON)

                // Flash
                val flashSupported = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

                // Focal lengths
                val focalArray = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf()
                val focalLengths = focalArray.map { String.format(Locale.getDefault(), "%.2f mm", it) }

                cameras.add(
                    CameraInfo(
                        id = id,
                        facing = facing,
                        megapixels = megapixels,
                        resolution = resolutionStr,
                        apertures = apertures,
                        oisSupported = oisSupported,
                        eisSupported = eisSupported,
                        flashSupported = flashSupported,
                        focalLengths = focalLengths
                    )
                )
            }
        }

        return cameras
    }

    fun getSensorList(context: Context): List<SensorItem> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return emptyList()
        val allSensors = runCatching { sensorManager.getSensorList(Sensor.TYPE_ALL) }.getOrDefault(emptyList())

        // Filter and deduplicate sensors
        return allSensors.map { s ->
            SensorItem(
                type = s.type,
                name = s.name,
                vendor = s.vendor ?: "unknown",
                typeString = s.stringType ?: "android.sensor.${s.type}",
                maxRange = s.maximumRange,
                resolution = s.resolution,
                powerMa = s.power,
                version = s.version
            )
        }.distinctBy { it.name }
    }

    fun getConnectivitySpecs(context: Context): ConnectivitySpecs {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

        var networkType = "None"
        var isConnected = false
        var downSpeed = 0
        var upSpeed = 0

        if (caps != null) {
            isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            networkType = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Active"
            }
            downSpeed = caps.linkDownstreamBandwidthKbps / 1000
            upSpeed = caps.linkUpstreamBandwidthKbps / 1000
        }

        // Wi-Fi Specifics
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiInfo = runCatching { wifiManager?.connectionInfo }.getOrNull()

        val rawSsid = wifiInfo?.ssid?.removeSurrounding("\"")
        val wifiSsid = if (rawSsid.isNullOrBlank() || rawSsid == "<unknown ssid>") "Permission required" else rawSsid
        val wifiFreq = wifiInfo?.frequency ?: 0
        val wifiRssi = wifiInfo?.rssi ?: 0
        val wifiLinkSpeed = wifiInfo?.linkSpeed ?: 0

        // Local IP addresses
        var localIpv4 = "unknown"
        var localIpv6 = "unknown"
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress && localIpv4 == "unknown") {
                        localIpv4 = addr.hostAddress ?: "unknown"
                    } else if (addr is Inet6Address && !addr.isLoopbackAddress && localIpv6 == "unknown") {
                        localIpv6 = addr.hostAddress?.split("%")?.firstOrNull() ?: "unknown"
                    }
                }
            }
        }

        // DHCP / Gateway / DNS
        val dhcpInfo = runCatching { wifiManager?.dhcpInfo }.getOrNull()
        val gateway = if (dhcpInfo != null && dhcpInfo.gateway != 0) intToIp(dhcpInfo.gateway) else "unknown"
        val subnetMask = if (dhcpInfo != null && dhcpInfo.netmask != 0) intToIp(dhcpInfo.netmask) else "unknown"
        val dns1 = if (dhcpInfo != null && dhcpInfo.dns1 != 0) intToIp(dhcpInfo.dns1) else null
        val dns2 = if (dhcpInfo != null && dhcpInfo.dns2 != 0) intToIp(dhcpInfo.dns2) else null
        val dnsServers = listOfNotNull(dns1, dns2).joinToString(", ").ifEmpty { "unknown" }

        // Bluetooth
        val pm = context.packageManager
        val bluetoothSupported = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val bluetoothLeSupported = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val bluetoothEnabled = runCatching {
            val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            adapter?.isEnabled ?: false
        }.getOrDefault(false)

        return ConnectivitySpecs(
            networkType = networkType,
            isConnected = isConnected,
            linkDownMbps = downSpeed,
            linkUpMbps = upSpeed,
            wifiSsid = wifiSsid,
            wifiFrequencyMhz = wifiFreq,
            wifiRssi = wifiRssi,
            wifiLinkSpeedMbps = wifiLinkSpeed,
            localIpv4 = localIpv4,
            localIpv6 = localIpv6,
            gateway = gateway,
            dnsServers = dnsServers,
            subnetMask = subnetMask,
            bluetoothSupported = bluetoothSupported,
            bluetoothEnabled = bluetoothEnabled,
            bluetoothLeSupported = bluetoothLeSupported
        )
    }

    private fun intToIp(i: Int): String {
        return "${i and 0xFF}.${i shr 8 and 0xFF}.${i shr 16 and 0xFF}.${i shr 24 and 0xFF}"
    }

    fun getMediaDrmSpecs(): MediaDrmSpecs {
        var widevineSecurity = "Not detected"
        var widevineVendor = "unknown"
        var widevineVersion = "unknown"
        var widevineSystemId = "unknown"
        var widevineAlgorithms = "unknown"
        var widevineHdcp = "unknown"
        var widevineMaxHdcp = "unknown"

        var clearkeyVendor = "unknown"
        var clearkeyVersion = "unknown"
        var clearkeyHdcp = "unknown"
        var clearkeyMaxHdcp = "unknown"

        // Query Widevine CDM
        runCatching {
            if (MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
                val drm = MediaDrm(WIDEVINE_UUID)
                widevineSecurity = drm.getPropertyString("securityLevel").ifEmpty { "L3" }
                widevineVendor = drm.getPropertyString(MediaDrm.PROPERTY_VENDOR).ifEmpty { "Google" }
                widevineVersion = drm.getPropertyString(MediaDrm.PROPERTY_VERSION).ifEmpty { "unknown" }
                widevineSystemId = drm.getPropertyString("systemId").ifEmpty { "unknown" }
                widevineAlgorithms = drm.getPropertyString(MediaDrm.PROPERTY_ALGORITHMS).ifEmpty { "AES/CBC/NoPadding,HmacSHA256" }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    widevineHdcp = parseHdcpLevel(drm.connectedHdcpLevel)
                    widevineMaxHdcp = parseHdcpLevel(drm.maxHdcpLevel)
                }
                drm.close()
            }
        }

        // Query ClearKey CDM
        runCatching {
            if (MediaDrm.isCryptoSchemeSupported(CLEARKEY_UUID)) {
                val drm = MediaDrm(CLEARKEY_UUID)
                clearkeyVendor = drm.getPropertyString(MediaDrm.PROPERTY_VENDOR).ifEmpty { "Google" }
                clearkeyVersion = drm.getPropertyString(MediaDrm.PROPERTY_VERSION).ifEmpty { "1.2" }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clearkeyHdcp = parseHdcpLevel(drm.connectedHdcpLevel)
                    clearkeyMaxHdcp = parseHdcpLevel(drm.maxHdcpLevel)
                }
                drm.close()
            }
        }

        return MediaDrmSpecs(
            widevineSecurityLevel = widevineSecurity,
            widevineVendor = widevineVendor,
            widevineVersion = widevineVersion,
            widevineSystemId = widevineSystemId,
            widevineAlgorithms = widevineAlgorithms,
            widevineHdcpLevel = widevineHdcp,
            widevineMaxHdcpLevel = widevineMaxHdcp,
            clearkeyVendor = clearkeyVendor,
            clearkeyVersion = clearkeyVersion,
            clearkeyHdcpLevel = clearkeyHdcp,
            clearkeyMaxHdcpLevel = clearkeyMaxHdcp
        )
    }

    private fun parseHdcpLevel(level: Int): String {
        return when (level) {
            MediaDrm.HDCP_NONE -> "None"
            MediaDrm.HDCP_V1 -> "HDCP 1.0"
            MediaDrm.HDCP_V2 -> "HDCP 2.0"
            MediaDrm.HDCP_V2_1 -> "HDCP 2.1"
            MediaDrm.HDCP_V2_2 -> "HDCP 2.2"
            MediaDrm.HDCP_V2_3 -> "HDCP 2.3"
            MediaDrm.HDCP_NO_DIGITAL_OUTPUT -> "No digital output"
            else -> "Level $level"
        }
    }

    fun formatDurationCompact(millis: Long): String {
        val totalSeconds = millis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = (totalSeconds / 3600) % 24
        val days = totalSeconds / 86400
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun getDeviceHeroState(): DeviceHeroState {
        val elapsedRealtime = SystemClock.elapsedRealtime()
        val uptimeMillis = SystemClock.uptimeMillis()
        val deepSleepMillis = (elapsedRealtime - uptimeMillis).coerceAtLeast(0L)
        val deepSleepPercent = if (elapsedRealtime > 0L) {
            (deepSleepMillis.toFloat() / elapsedRealtime.toFloat()) * 100f
        } else 0f
        val awakePercent = (100f - deepSleepPercent).coerceIn(0f, 100f)

        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val model = Build.MODEL.orEmpty()
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else if (manufacturer.isNotBlank()) {
            "$manufacturer $model"
        } else {
            model.ifBlank { "Android Device" }
        }

        val uptimeHours = elapsedRealtime / (1000 * 60 * 60)
        val uptimeMinutes = (elapsedRealtime % (1000 * 60 * 60)) / (1000 * 60)
        val uptimeSeconds = (elapsedRealtime % (1000 * 60)) / 1000
        val formattedUptime = if (uptimeHours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", uptimeHours, uptimeMinutes, uptimeSeconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", uptimeMinutes, uptimeSeconds)
        }

        return DeviceHeroState(
            deviceName = deviceName,
            manufacturer = manufacturer,
            model = model,
            androidVersion = "Android " + (Build.VERSION.RELEASE ?: "unknown"),
            apiLevel = Build.VERSION.SDK_INT.toString(),
            securityPatch = Build.VERSION.SECURITY_PATCH ?: "unknown",
            uptimeMillis = elapsedRealtime,
            awakeMillis = uptimeMillis,
            deepSleepMillis = deepSleepMillis,
            deepSleepPercentage = deepSleepPercent,
            awakePercentage = awakePercent,
            formattedUptime = formattedUptime,
            formattedDeepSleep = formatDurationCompact(deepSleepMillis),
            formattedAwake = formatDurationCompact(uptimeMillis)
        )
    }
}
