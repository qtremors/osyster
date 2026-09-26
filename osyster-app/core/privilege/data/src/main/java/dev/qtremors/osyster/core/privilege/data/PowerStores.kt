package dev.qtremors.osyster.core.privilege.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.privilege.domain.DebugSnapshot
import dev.qtremors.osyster.core.privilege.domain.PackageNames
import dev.qtremors.osyster.core.privilege.domain.PowerApp
import dev.qtremors.osyster.core.privilege.domain.PowerApps
import dev.qtremors.osyster.core.privilege.domain.PowerToolsStore
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
internal class PowerAppsImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : PowerApps {
    override suspend fun apps(): List<PowerApp> = withContext(dispatchers.io) {
        val pm = context.packageManager
        // Frozen apps are disabled, so they only appear when disabled packages are asked for.
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()))
        } else {
            pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        }
        infos.asSequence()
            .filter { it.packageName != context.packageName }
            .map { info ->
                PowerApp(
                    packageName = info.packageName,
                    label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(info.packageName),
                    system = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    enabled = info.enabled,
                    launchable = pm.getLaunchIntentForPackage(info.packageName) != null
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    override fun launch(packageName: String): Boolean {
        if (!PackageNames.isValid(packageName)) return false
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }
}

/** Small lists and one snapshot; SharedPreferences keeps them readable from tiles without coroutines. */
@Singleton
internal class PowerToolsStoreImpl @Inject constructor(@ApplicationContext context: Context) : PowerToolsStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _quietApps = MutableStateFlow(readSet(KEY_QUIET))
    override val quietApps: StateFlow<Set<String>> = _quietApps.asStateFlow()

    private val _freezeList = MutableStateFlow(readSet(KEY_FREEZE))
    override val freezeList: StateFlow<Set<String>> = _freezeList.asStateFlow()

    private val _snapshot = MutableStateFlow(DebugSnapshot.decode(prefs.getString(KEY_SNAPSHOT, null)))
    override val debugSnapshot: StateFlow<DebugSnapshot?> = _snapshot.asStateFlow()

    override fun setQuietApps(apps: Set<String>) {
        val clean = apps.filter(PackageNames::isValid).toSet()
        prefs.edit().putStringSet(KEY_QUIET, clean).apply()
        _quietApps.value = clean
    }

    override fun setFreezeList(apps: Set<String>) {
        val clean = apps.filter(PackageNames::isValid).toSet()
        prefs.edit().putStringSet(KEY_FREEZE, clean).apply()
        _freezeList.value = clean
    }

    override fun setDebugSnapshot(snapshot: DebugSnapshot?) {
        // commit, not apply: the snapshot must survive even if the process dies right after.
        prefs.edit().putString(KEY_SNAPSHOT, snapshot?.encode()).commit()
        _snapshot.value = snapshot
    }

    private fun readSet(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty().toSet()

    private companion object {
        const val PREFS = "power_tools"
        const val KEY_QUIET = "quiet_apps"
        const val KEY_FREEZE = "freeze_list"
        const val KEY_SNAPSHOT = "debug_snapshot"
    }
}
