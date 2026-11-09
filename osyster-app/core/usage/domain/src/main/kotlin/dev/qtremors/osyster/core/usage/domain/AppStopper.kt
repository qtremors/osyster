package dev.qtremors.osyster.core.usage.domain

import kotlinx.coroutines.flow.Flow

/**
 * An app on the stopper list. [stopped] is Android's own "stopped" flag (set after a force stop,
 * cleared when the app runs again); it does not mean the app has no process right now.
 * Uninstalled apps stay on the list, marked, until the user removes them.
 */
data class ManagedApp(
    val packageName: String,
    val label: String,
    val installed: Boolean,
    val stopped: Boolean,
    val system: Boolean,
    /** Disabled for this user (frozen through Shizuku or root); it cannot run until unfrozen. */
    val frozen: Boolean = false
)

/** An installed app the user can add to the list. */
data class InstalledApp(val packageName: String, val label: String, val system: Boolean)

interface AppStopperRepository {
    /** The managed list, refreshed whenever [refresh] runs or the list changes. */
    val apps: Flow<List<ManagedApp>>
    suspend fun installedApps(includeSystem: Boolean): List<InstalledApp>
    suspend fun add(packageNames: Collection<String>)
    suspend fun remove(packageName: String)
    /** Re-reads the stopped flags; call when the screen comes back into view. */
    suspend fun refresh()

    /** Grid columns on the App stopper page. */
    val columns: Flow<Int>
    suspend fun setColumns(count: Int)
}
