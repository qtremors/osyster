package dev.qtremors.osyster.core.system.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import dev.qtremors.osyster.core.system.domain.ActivityEntry
import dev.qtremors.osyster.core.system.domain.AppActivities
import dev.qtremors.osyster.core.system.domain.AppEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
internal class AppActivitiesImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : AppActivities {
    private val pm = context.packageManager

    override suspend fun apps(): List<AppEntry> = withContext(dispatchers.io) {
        installedPackages(PackageManager.GET_ACTIVITIES)
            .mapNotNull { info ->
                val count = info.activities?.size ?: 0
                if (count == 0 || info.packageName == context.packageName) null
                else AppEntry(info.packageName, info.applicationInfo?.loadLabel(pm)?.toString() ?: info.packageName, count)
            }
            .sortedBy { it.label.lowercase() }
    }

    override suspend fun activities(packageName: String): List<ActivityEntry> = withContext(dispatchers.io) {
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
            } else {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            }
        }.getOrNull() ?: return@withContext emptyList()
        info.activities.orEmpty().map { activity ->
            ActivityEntry(
                packageName = packageName,
                className = activity.name,
                label = activity.loadLabel(pm).toString(),
                exported = activity.exported && activity.permission == null
            )
        }.sortedWith(compareByDescending<ActivityEntry> { it.exported }.thenBy { it.shortName })
    }

    override fun launch(activity: ActivityEntry): Boolean = runCatching {
        context.startActivity(intentFor(activity))
        true
    }.getOrDefault(false)

    override fun pinShortcut(activity: ActivityEntry): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val icon = runCatching { pm.getActivityIcon(ComponentName(activity.packageName, activity.className)).toBitmap(192, 192) }.getOrNull()
        val shortcut = ShortcutInfoCompat.Builder(context, "activity:${activity.packageName}/${activity.className}")
            .setShortLabel(activity.label.take(25))
            .setLongLabel(activity.label)
            .setIntent(intentFor(activity).setAction(Intent.ACTION_MAIN))
            .apply { if (icon != null) setIcon(IconCompat.createWithBitmap(icon)) }
            .build()
        return runCatching { ShortcutManagerCompat.requestPinShortcut(context, shortcut, null) }.getOrDefault(false)
    }

    private fun intentFor(activity: ActivityEntry) = Intent()
        .setComponent(ComponentName(activity.packageName, activity.className))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun installedPackages(flags: Int) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getInstalledPackages(flags)
        }
}
