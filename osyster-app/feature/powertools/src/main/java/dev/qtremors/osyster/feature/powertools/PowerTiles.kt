package dev.qtremors.osyster.feature.powertools

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.privilege.domain.Keys
import dev.qtremors.osyster.core.privilege.domain.PowerApps
import dev.qtremors.osyster.core.privilege.domain.PowerCommands
import dev.qtremors.osyster.core.privilege.domain.PowerToolsStore
import dev.qtremors.osyster.core.privilege.domain.PrivateDnsMode
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SettingKey
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.privilege.domain.isValidDnsHost
import dev.qtremors.osyster.core.privilege.domain.nextPrivateDnsMode
import dev.qtremors.osyster.core.privilege.domain.readFlag
import dev.qtremors.osyster.core.privilege.domain.writeFlag
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Base for power tiles: refreshes while Quick Settings is open, and when access is missing,
 * a tap opens Power setup instead of failing silently.
 */
abstract class PowerTile : TileService() {
    @Inject lateinit var settings: SystemSettings
    @Inject lateinit var shell: PrivilegedShell

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Settings tiles need settings access; command tiles override this to need a shell or root. */
    protected open fun hasAccess(): Boolean = shell.status.value.canWriteSettings

    protected abstract suspend fun refresh()
    protected abstract suspend fun toggle()

    override fun onStartListening() {
        shell.refresh()
        scope.launch { if (hasAccess()) refresh() else showMissing() }
    }

    override fun onClick() {
        if (!hasAccess()) {
            openSetup()
            return
        }
        scope.launch {
            toggle()
            refresh()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    protected fun show(active: Boolean, subtitle: String? = null) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.updateTile()
    }

    private fun showMissing() = show(active = false, subtitle = getString(R.string.tile_set_up))

    @Suppress("DEPRECATION")
    private fun openSetup() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.putExtra(DeepLinks.EXTRA_OPEN_TOOL, Tool.POWER_SETUP.id) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            startActivityAndCollapse(intent)
        }
    }
}

/** A tile for one on/off setting. [inverted] is for "disable_" keys shown as a positive switch. */
abstract class SettingTile(private val key: SettingKey, private val inverted: Boolean = false, private val defaultOn: Boolean = false) : PowerTile() {
    private suspend fun isOn(): Boolean {
        val stored = settings.readFlag(key, default = if (inverted) !defaultOn else defaultOn)
        return if (inverted) !stored else stored
    }

    override suspend fun refresh() {
        val on = isOn()
        show(active = on, subtitle = getString(if (on) R.string.tile_on else R.string.tile_off))
    }

    override suspend fun toggle() {
        val next = !isOn()
        settings.writeFlag(key, if (inverted) !next else next)
    }
}

@AndroidEntryPoint
class UsbDebuggingTile : SettingTile(Keys.UsbDebugging)

@AndroidEntryPoint
class DeveloperOptionsTile : SettingTile(Keys.DeveloperOptions)

@AndroidEntryPoint
class BlurTile : SettingTile(Keys.DisableBlurs, inverted = true, defaultOn = true)

@AndroidEntryPoint
class TapToWakeTile : SettingTile(Keys.TapToWake)

/** Off, automatic, then the saved provider (if one was saved on the System tweaks page). */
@AndroidEntryPoint
class PrivateDnsTile : PowerTile() {
    override suspend fun refresh() {
        val mode = PrivateDnsMode.parse(settings.read(Keys.PrivateDnsMode))
        val subtitle = when (mode) {
            PrivateDnsMode.OFF -> getString(R.string.tile_off)
            PrivateDnsMode.AUTOMATIC -> getString(R.string.tweaks_dns_automatic)
            PrivateDnsMode.HOSTNAME -> settings.read(Keys.PrivateDnsHost) ?: getString(R.string.tweaks_dns_provider)
        }
        show(active = mode != PrivateDnsMode.OFF, subtitle = subtitle)
    }

    override suspend fun toggle() {
        val current = PrivateDnsMode.parse(settings.read(Keys.PrivateDnsMode))
        val hasHost = settings.read(Keys.PrivateDnsHost)?.let(::isValidDnsHost) == true
        settings.write(Keys.PrivateDnsMode, nextPrivateDnsMode(current, hasHost).value)
        PrivateDnsWidget.updateAll(this)
    }
}

/** Restarts SystemUI, for when the status bar or panel gets stuck. Root only. */
@AndroidEntryPoint
class RestartSystemUiTile : PowerTile() {
    override fun hasAccess(): Boolean = shell.status.value.root

    override suspend fun refresh() = show(active = false, subtitle = getString(R.string.tile_tap_to_restart))

    override suspend fun toggle() {
        shell.run(PowerCommands.restartSystemUi)
    }
}

/** Freezes every app on the Freeze list, or unfreezes them all when they are already frozen. */
@AndroidEntryPoint
class FreezeTile : PowerTile() {
    @Inject lateinit var store: PowerToolsStore
    @Inject lateinit var apps: PowerApps

    override fun hasAccess(): Boolean = shell.status.value.shell

    private suspend fun allFrozen(): Boolean? {
        val list = store.freezeList.value
        if (list.isEmpty()) return null
        return apps.apps().filter { it.packageName in list }.all { !it.enabled }
    }

    override suspend fun refresh() {
        when (allFrozen()) {
            null -> show(active = false, subtitle = getString(R.string.tile_freeze_empty))
            true -> show(active = true, subtitle = getString(R.string.freeze_frozen))
            false -> show(active = false, subtitle = getString(R.string.tile_freeze_count, store.freezeList.value.size))
        }
    }

    override suspend fun toggle() {
        val frozen = allFrozen() ?: return
        store.freezeList.value.forEach { pkg ->
            shell.run(if (frozen) PowerCommands.unfreeze(pkg) else PowerCommands.freeze(pkg))
        }
    }
}
