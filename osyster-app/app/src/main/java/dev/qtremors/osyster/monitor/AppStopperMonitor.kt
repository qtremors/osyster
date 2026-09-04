package dev.qtremors.osyster.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

// =========================================================================
// Section Comment: Data Models for Managed App Stopper
// =========================================================================

data class ManagedAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isStopped: Boolean,
    val isSystemApp: Boolean
)

data class InstalledAppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
)

// =========================================================================
// Section Comment: App Stopper Package Utility & State Evaluator
// =========================================================================

object AppStopperMonitor {

    /**
     * Determines whether an application package is currently in the stopped state
     * by querying the Android system FLAG_STOPPED flag on its ApplicationInfo.
     */
    fun isPackageStopped(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Loads full metadata and current stopped status for a set of managed package names.
     */
    fun loadManagedApps(context: Context, packageNames: Set<String>): List<ManagedAppInfo> {
        val pm = context.packageManager
        val list = mutableListOf<ManagedAppInfo>()

        for (pkg in packageNames) {
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }

                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isStopped = (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                list.add(
                    ManagedAppInfo(
                        packageName = pkg,
                        label = label,
                        icon = icon,
                        isStopped = isStopped,
                        isSystemApp = isSystem
                    )
                )
            } catch (_: Exception) {
                // If package was uninstalled, omit
            }
        }

        // Sort: Active apps first, then alphabetically by label
        return list.sortedWith(
            compareBy<ManagedAppInfo> { it.isStopped }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )
    }

    /**
     * Loads installed launchable applications excluding any already added packages and self.
     */
    fun loadAllInstalledApps(
        context: Context,
        excludePackages: Set<String> = emptySet(),
        includeSystemApps: Boolean = false
    ): List<InstalledAppItem> {
        val pm = context.packageManager
        val selfPackage = context.packageName
        val seenPackages = mutableSetOf<String>()
        val results = mutableListOf<InstalledAppItem>()

        if (includeSystemApps) {
            val allApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

            for (appInfo in allApps) {
                val pkg = appInfo.packageName ?: continue
                if (pkg == selfPackage || pkg in excludePackages || pkg in seenPackages) continue
                seenPackages.add(pkg)

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = pm.getApplicationLabel(appInfo).toString().ifBlank { pkg }
                val icon = pm.getApplicationIcon(appInfo)

                results.add(
                    InstalledAppItem(
                        packageName = pkg,
                        label = label,
                        icon = icon,
                        isSystemApp = isSystem
                    )
                )
            }
        } else {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            for (info in resolveInfos) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (pkg == selfPackage || pkg in excludePackages || pkg in seenPackages) continue
                seenPackages.add(pkg)

                val appInfo = info.activityInfo.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystem) continue

                val label = info.loadLabel(pm).toString().ifBlank {
                    pm.getApplicationLabel(appInfo).toString()
                }
                val icon = info.loadIcon(pm)

                results.add(
                    InstalledAppItem(
                        packageName = pkg,
                        label = label,
                        icon = icon,
                        isSystemApp = isSystem
                    )
                )
            }
        }

        return results.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /**
     * Launches the system App Info settings screen for the specified package,
     * placing the user one tap away from the native Force stop action.
     */
    fun openAppInfo(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open App Info: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Attempts to launch the specified package via its launcher intent.
     */
    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                Toast.makeText(context, "No launcher entry found for $packageName", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens the app entry in Google Play Store or web fallback.
     */
    fun openInPlayStore(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open Play Store", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
