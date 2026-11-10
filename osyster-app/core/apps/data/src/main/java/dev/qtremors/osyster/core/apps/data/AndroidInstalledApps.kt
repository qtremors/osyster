package dev.qtremors.osyster.core.apps.data

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.storage.StorageManager
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.apps.domain.AppComponents
import dev.qtremors.osyster.core.apps.domain.AppDetails
import dev.qtremors.osyster.core.apps.domain.AppStorage
import dev.qtremors.osyster.core.apps.domain.AppSummary
import dev.qtremors.osyster.core.apps.domain.InstalledApps
import dev.qtremors.osyster.core.apps.domain.PermissionArea
import dev.qtremors.osyster.core.apps.domain.RequestedPermission
import dev.qtremors.osyster.core.apps.domain.SpecialAccess
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Installed apps through public Android APIs only: PackageManager for packages and runtime
 * grants, AppOpsManager (via its public permission-to-op mapping) for special access, the
 * accessibility, notification, device admin, and power managers for the rest, and storage stats
 * where usage access is on. The list is kept for a short while so moving between pages is quick.
 */
@Singleton
internal class AndroidInstalledApps @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : InstalledApps {
    private val pm: PackageManager = context.packageManager
    private val lock = Mutex()
    private var cached: Pair<Long, List<AppSummary>>? = null

    override suspend fun apps(fresh: Boolean): List<AppSummary> = lock.withLock {
        if (!fresh) cached?.takeIf { System.currentTimeMillis() - it.first < CACHE_MS }?.let { return it.second }
        val fresh = withContext(dispatchers.io) { load() }
        cached = System.currentTimeMillis() to fresh
        fresh
    }

    override suspend fun details(packageName: String): AppDetails? = withContext(dispatchers.io) {
        val summary = apps().firstOrNull { it.packageName == packageName } ?: return@withContext null
        val info = runCatching { packageInfo(packageName, DETAIL_FLAGS) }.getOrNull() ?: return@withContext null
        val app = info.applicationInfo ?: return@withContext null
        val apks = listOfNotNull(app.sourceDir) + app.splitSourceDirs.orEmpty()
        val libraries = apks.flatMap(::nativeLibraries)
        AppDetails(
            summary = summary,
            abis = libraries.map { it.first }.distinct(),
            nativeLibraries = libraries.map { it.second }.distinct().sorted(),
            components = AppComponents(
                activities = info.activities.orEmpty().map { it.name }.sorted(),
                services = info.services.orEmpty().map { it.name }.sorted(),
                receivers = info.receivers.orEmpty().map { it.name }.sorted(),
                providers = info.providers.orEmpty().map { it.name }.sorted()
            ),
            signerSha256 = signer(info),
            storage = storage(packageName),
            apkPaths = apks,
            permissionLabels = summary.permissions.associate { it.name to label(it.name) }
        )
    }

    private fun load(): List<AppSummary> {
        val packages = runCatching { installedPackages() }.getOrDefault(emptyList())
        val launchable = runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).map { it.activityInfo.packageName }.toSet()
        }.getOrDefault(emptySet())
        val accessibility = runCatching {
            (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .mapNotNull { it.resolveInfo?.serviceInfo?.packageName }.toSet()
        }.getOrDefault(emptySet())
        val listeners = runCatching { NotificationManagerCompat.getEnabledListenerPackages(context) }.getOrDefault(emptySet())
        val admins = runCatching {
            (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).activeAdmins.orEmpty().map { it.packageName }.toSet()
        }.getOrDefault(emptySet())
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val installers = mutableMapOf<String, String?>()

        return packages.mapNotNull { info ->
            val app = info.applicationInfo ?: return@mapNotNull null
            val requested = info.requestedPermissions.orEmpty()
            val flags = info.requestedPermissionsFlags ?: IntArray(0)
            val permissions = requested.mapIndexed { i, name ->
                RequestedPermission(name, PermissionArea.of(name), granted = (flags.getOrElse(i) { 0 } and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0)
            }.filterNot { it.name.endsWith(ANDROIDX_INTERNAL) }
            val asked = requested.toSet()
            val special = buildSet {
                if (info.packageName in accessibility) add(SpecialAccess.ACCESSIBILITY)
                if (info.packageName in listeners) add(SpecialAccess.NOTIFICATION_LISTENER)
                if (info.packageName in admins) add(SpecialAccess.DEVICE_ADMIN)
                if (runCatching { power?.isIgnoringBatteryOptimizations(info.packageName) == true }.getOrDefault(false)) add(SpecialAccess.UNRESTRICTED_BATTERY)
                // Only apps that ask for a special permission are checked, so this stays cheap.
                OPS.forEach { (permission, access) ->
                    if (permission in asked && ops.allows(permission, app.uid, info.packageName, permissions)) add(access)
                }
            }
            val installer = installers.getOrPut(info.packageName) { installerLabel(info.packageName) }
            AppSummary(
                packageName = info.packageName,
                label = runCatching { app.loadLabel(pm).toString() }.getOrDefault(info.packageName),
                versionName = info.versionName,
                versionCode = PackageInfoCompat.getLongVersionCode(info),
                system = app.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                enabled = app.enabled,
                installer = installer,
                installedAt = info.firstInstallTime,
                updatedAt = info.lastUpdateTime,
                targetSdk = app.targetSdkVersion,
                minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) app.minSdkVersion else 0,
                apkBytes = (listOfNotNull(app.sourceDir) + app.splitSourceDirs.orEmpty()).sumOf { runCatching { File(it).length() }.getOrDefault(0L) },
                permissions = permissions,
                special = special,
                launchable = info.packageName in launchable
            )
        }
    }

    /**
     * Whether an app-op permission is allowed. "Default" means the plain permission grant decides,
     * which is how Android itself reads it.
     */
    private fun AppOpsManager?.allows(permission: String, uid: Int, packageName: String, permissions: List<RequestedPermission>): Boolean {
        this ?: return false
        val op = AppOpsManager.permissionToOp(permission) ?: return permissions.any { it.name == permission && it.granted }
        val mode = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) unsafeCheckOpNoThrow(op, uid, packageName)
            else @Suppress("DEPRECATION") checkOpNoThrow(op, uid, packageName)
        }.getOrDefault(AppOpsManager.MODE_ERRORED)
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_DEFAULT -> permissions.any { it.name == permission && it.granted }
            else -> false
        }
    }

    @Suppress("DEPRECATION")
    private fun installedPackages(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        else pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String, flags: Int): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        else pm.getPackageInfo(packageName, flags)

    /** The app that installed [packageName], by name; null for preinstalled and side-loaded ones. */
    @Suppress("DEPRECATION")
    private fun installerLabel(packageName: String): String? {
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) pm.getInstallSourceInfo(packageName).installingPackageName
            else pm.getInstallerPackageName(packageName)
        }.getOrNull() ?: return null
        return runCatching { pm.getApplicationLabel(pm.getApplicationInfo(installer, 0)).toString() }.getOrDefault(installer)
    }

    @Suppress("DEPRECATION")
    private fun signer(info: PackageInfo): String? {
        val bytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // The certificate the APK is signed with now, after any key rotation.
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            info.signatures?.firstOrNull()?.toByteArray()
        } ?: return null
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(":") { "%02X".format(it) }
    }

    private fun storage(packageName: String): AppStorage? = runCatching {
        val stats = (context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager)
            .queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName, Process.myUserHandle())
        AppStorage(appBytes = stats.appBytes, dataBytes = (stats.dataBytes - stats.cacheBytes).coerceAtLeast(0), cacheBytes = stats.cacheBytes)
    }.getOrNull()

    /** (ABI, library file) pairs from an APK's lib/ folder; APKs are readable by every app. */
    private fun nativeLibraries(apk: String): List<Pair<String, String>> = runCatching {
        ZipFile(apk).use { zip ->
            zip.entries().asSequence().map { it.name }.filter { it.startsWith("lib/") && it.endsWith(".so") }.mapNotNull { name ->
                val parts = name.split('/')
                if (parts.size >= 3) parts[1] to parts.last() else null
            }.toList()
        }
    }.getOrDefault(emptyList())

    private fun label(permission: String): String = runCatching {
        pm.getPermissionInfo(permission, 0).loadLabel(pm).toString()
    }.getOrNull()?.takeIf { it.isNotBlank() && !it.equals(permission, ignoreCase = true) }?.replaceFirstChar { it.uppercase() } ?: permission.substringAfterLast('.').lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    private companion object {
        const val CACHE_MS = 30_000L

        /** AndroidX declares this in every app that uses it; it means nothing to people. */
        const val ANDROIDX_INTERNAL = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

        @Suppress("DEPRECATION")
        val DETAIL_FLAGS = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)

        /** Special access granted through app-ops, by the permission that asks for it. */
        val OPS = listOf(
            Manifest.permission.SYSTEM_ALERT_WINDOW to SpecialAccess.DRAW_OVER_APPS,
            Manifest.permission.REQUEST_INSTALL_PACKAGES to SpecialAccess.INSTALL_APPS,
            Manifest.permission.PACKAGE_USAGE_STATS to SpecialAccess.USAGE_ACCESS,
            Manifest.permission.WRITE_SETTINGS to SpecialAccess.MODIFY_SETTINGS,
            "android.permission.MANAGE_EXTERNAL_STORAGE" to SpecialAccess.ALL_FILES,
            "android.permission.SCHEDULE_EXACT_ALARM" to SpecialAccess.EXACT_ALARMS
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppsDataModule {
    @Binds
    abstract fun installedApps(impl: AndroidInstalledApps): InstalledApps
}
