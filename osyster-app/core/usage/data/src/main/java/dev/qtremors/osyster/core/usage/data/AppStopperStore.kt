package dev.qtremors.osyster.core.usage.data

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import dev.qtremors.osyster.core.usage.domain.AppStopperRepository
import dev.qtremors.osyster.core.usage.domain.InstalledApp
import dev.qtremors.osyster.core.usage.domain.ManagedApp
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.appStopper: DataStore<Preferences> by preferencesDataStore("app_stopper")

/**
 * The user's app stopper list, stored as package names. State comes from PackageManager each time:
 * the stopped flag, whether the app is still installed, and its current name.
 *
 * Each app's name is also saved, so an app that gets uninstalled stays on the list under its
 * real name (a ghost entry) instead of a bare package name, and picks up again on reinstall.
 */
@Singleton
internal class AppStopperStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : AppStopperRepository {
    private val key = stringSetPreferencesKey("packages")
    private val columnsKey = intPreferencesKey("columns")
    private val labels = AppLabels(context)
    private val refreshTick = MutableStateFlow(0)

    override val apps: Flow<List<ManagedApp>> = combine(context.appStopper.data, refreshTick) { prefs, _ ->
        prefs[key].orEmpty()
            .map { describe(it, prefs[labelKey(it)]) }
            // Running first, then stopped, then frozen, then uninstalled, each by name.
            .sortedWith(compareBy({ it.order() }, { it.label.lowercase() }))
    }.flowOn(dispatchers.io)

    override suspend fun installedApps(includeSystem: Boolean): List<InstalledApp> = withContext(dispatchers.io) {
        val pm = context.packageManager
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            pm.getInstalledApplications(0)
        }
        infos.asSequence()
            .filter { it.packageName != context.packageName }
            .filter { includeSystem || !it.isSystem() || pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { InstalledApp(it.packageName, labels.label(it.packageName), it.isSystem()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    override suspend fun add(packageNames: Collection<String>) {
        val names = withContext(dispatchers.io) { packageNames.associateWith { labels.label(it) } }
        context.appStopper.edit { prefs ->
            prefs[key] = prefs[key].orEmpty() + packageNames
            names.forEach { (pkg, name) -> prefs[labelKey(pkg)] = name }
        }
    }

    override suspend fun remove(packageName: String) {
        context.appStopper.edit { prefs ->
            prefs[key] = prefs[key].orEmpty() - packageName
            prefs.remove(labelKey(packageName))
        }
    }

    override suspend fun refresh() {
        // Keep saved names current while apps are installed, so a later ghost shows the latest name.
        withContext(dispatchers.io) {
            context.appStopper.edit { prefs ->
                prefs[key].orEmpty().forEach { pkg ->
                    if (labels.isInstalled(pkg)) prefs[labelKey(pkg)] = labels.label(pkg)
                }
            }
        }
        refreshTick.value++
    }

    private fun labelKey(packageName: String) = stringPreferencesKey("label:$packageName")

    private fun ManagedApp.order(): Int = when {
        !installed -> 3
        frozen -> 2
        stopped -> 1
        else -> 0
    }

    private fun describe(packageName: String, savedLabel: String?): ManagedApp {
        val info = runCatching { labels.applicationInfo(packageName) }.getOrNull()
        return ManagedApp(
            packageName = packageName,
            label = if (info != null) labels.label(packageName) else savedLabel ?: packageName,
            installed = info != null,
            stopped = info != null && info.flags and ApplicationInfo.FLAG_STOPPED != 0,
            system = info?.isSystem() == true,
            frozen = info != null && !info.enabled
        )
    }

    override val columns: Flow<Int> = context.appStopper.data.map { (it[columnsKey] ?: DEFAULT_COLUMNS).coerceIn(3, 6) }

    override suspend fun setColumns(count: Int) {
        context.appStopper.edit { it[columnsKey] = count.coerceIn(3, 6) }
    }

    private fun ApplicationInfo.isSystem(): Boolean = flags and ApplicationInfo.FLAG_SYSTEM != 0
}

private const val DEFAULT_COLUMNS = 4
