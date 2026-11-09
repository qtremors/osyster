package dev.qtremors.osyster.core.privilege.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** An installed app as the power tools see it. [enabled] is false while it is frozen. */
data class PowerApp(
    val packageName: String,
    val label: String,
    val system: Boolean,
    val enabled: Boolean,
    val launchable: Boolean
)

interface PowerApps {
    /** Every app for the current user, frozen ones included, Osyster itself excluded, sorted by name. */
    suspend fun apps(): List<PowerApp>

    /** Opens the app's launcher screen. False when it has none or is frozen. */
    fun launch(packageName: String): Boolean
}

/** What the power tools remember between launches. */
interface PowerToolsStore {
    /** Apps that open with debugging hidden (Shut-Up!). */
    val quietApps: StateFlow<Set<String>>
    fun setQuietApps(apps: Set<String>)

    /** Apps the user manages on the Freeze page and the Freeze tile. */
    val freezeList: StateFlow<Set<String>>
    fun setFreezeList(apps: Set<String>)

    /** Debugging switches saved before a quiet launch, until Restore puts them back. */
    val debugSnapshot: StateFlow<DebugSnapshot?>
    fun setDebugSnapshot(snapshot: DebugSnapshot?)
}

/**
 * Shut-Up!: some banking and payment apps refuse to run while developer options or USB debugging
 * are on. [hide] turns all three switches off and remembers how they were; [restore] puts back
 * exactly that. Hiding twice keeps the first snapshot, so a second app cannot overwrite it with
 * "all off".
 */
class QuietLaunch @Inject constructor(
    private val settings: SystemSettings,
    private val store: PowerToolsStore
) {
    /** False when the switches could not be written (no access); the app should still open. */
    suspend fun hide(): Boolean {
        val current = DebugSnapshot(
            developerOptions = settings.readFlag(Keys.DeveloperOptions),
            usbDebugging = settings.readFlag(Keys.UsbDebugging),
            wirelessDebugging = settings.readFlag(Keys.WirelessDebugging)
        )
        if (!current.anyOn) return true
        if (store.debugSnapshot.value == null) store.setDebugSnapshot(current)
        // Order matters: debugging switches first, since turning off developer options can reset them.
        val wrote = listOf(Keys.WirelessDebugging, Keys.UsbDebugging, Keys.DeveloperOptions)
            .map { settings.writeFlag(it, false) }
        return wrote.all { it }
    }

    /** True when there was something to restore and every switch was written back. */
    suspend fun restore(): Boolean {
        val snapshot = store.debugSnapshot.value ?: return false
        // Developer options must be on before debugging can be turned back on.
        val wrote = listOf(
            settings.writeFlag(Keys.DeveloperOptions, snapshot.developerOptions),
            settings.writeFlag(Keys.UsbDebugging, snapshot.usbDebugging),
            settings.writeFlag(Keys.WirelessDebugging, snapshot.wirelessDebugging)
        )
        if (wrote.all { it }) store.setDebugSnapshot(null)
        return wrote.all { it }
    }
}
