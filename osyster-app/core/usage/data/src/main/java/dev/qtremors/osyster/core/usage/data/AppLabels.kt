package dev.qtremors.osyster.core.usage.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import java.util.concurrent.ConcurrentHashMap

/** Whether the user granted Usage access in Settings. */
internal fun Context.usageAccessState(): UsageAccessState {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return UsageAccessState.MISSING
    // The only check that works from Android 8 up without a privileged permission; newer
    // replacements need API levels Osyster still supports below.
    @Suppress("DEPRECATION")
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    } else {
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    }
    return if (mode == AppOpsManager.MODE_ALLOWED) UsageAccessState.GRANTED else UsageAccessState.MISSING
}

/** A package and the name people know it by. */
internal data class AppName(val packageName: String, val label: String)

/**
 * Resolves package names and Linux user ids to app names, cached for the process. Traffic that no
 * app owns (removed apps, tethering, the system) gets a readable name instead of a number.
 */
internal class AppLabels(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val byPackage = ConcurrentHashMap<String, String>()
    private val byUid = ConcurrentHashMap<Int, AppName>()

    fun label(packageName: String): String = byPackage.getOrPut(packageName) {
        runCatching { pm.getApplicationLabel(applicationInfo(packageName)).toString() }.getOrDefault(packageName)
    }

    /** The uid Android gave [packageName], or null when it is not installed. */
    fun uidOf(packageName: String): Int? = runCatching { applicationInfo(packageName).uid }.getOrNull()

    fun forUid(uid: Int): AppName = byUid.getOrPut(uid) {
        when (uid) {
            NetworkStats.Bucket.UID_REMOVED -> AppName("uid:removed", context.getString(R.string.uid_removed))
            NetworkStats.Bucket.UID_TETHERING -> AppName("uid:tethering", context.getString(R.string.uid_tethering))
            Process.SYSTEM_UID, ROOT_UID -> AppName("android", context.getString(R.string.uid_system))
            else -> {
                val packages = runCatching { pm.getPackagesForUid(uid) }.getOrNull().orEmpty()
                // Shared uids list several packages; the first with a launcher entry reads best.
                val chosen = packages.firstOrNull { pm.getLaunchIntentForPackage(it) != null } ?: packages.firstOrNull()
                if (chosen == null) AppName("uid:$uid", context.getString(R.string.uid_unknown, uid))
                else AppName(chosen, label(chosen))
            }
        }
    }

    fun isInstalled(packageName: String): Boolean = runCatching { applicationInfo(packageName) }.isSuccess

    /** Frozen (disabled) apps are included, so they never look uninstalled. */
    fun applicationInfo(packageName: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()))
        } else {
            pm.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
        }

    private companion object {
        const val ROOT_UID = 0
    }
}
