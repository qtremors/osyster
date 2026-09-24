package dev.qtremors.osyster.feature.quicktiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.device.domain.DeviceSummarySource
import dev.qtremors.osyster.core.device.domain.getOrNull
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Tiles that show a reading instead of toggling something. They read once each time Quick
 * Settings opens and stop when it closes, so they cost nothing in the background.
 */
abstract class InfoTile : TileService() {
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { it.launch { refresh() } }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
    }

    protected abstract suspend fun refresh()

    protected fun show(label: String, subtitle: String, active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = label
            tile.subtitle = subtitle
        } else {
            tile.label = "$label $subtitle"
        }
        tile.updateTile()
    }
}

/** RAM in use, such as "5.1 GB of 7.5 GB". */
@AndroidEntryPoint
class RamTile : InfoTile() {
    @Inject lateinit var summary: DeviceSummarySource

    override suspend fun refresh() {
        val reading = runCatchingCancellable { summary.summary().first() }.getOrNull()
        val used = reading?.ramUsedBytes?.getOrNull()
        val total = reading?.ramTotalBytes?.getOrNull()
        show(
            label = getString(R.string.tile_ram),
            subtitle = if (used != null && total != null) getString(R.string.tile_ram_value, UsageFormat.bytes(used), UsageFormat.bytes(total)) else getString(R.string.tile_unknown),
            active = true
        )
    }
}

/** Mobile and Wi-Fi data used today; asks for Usage access when it is missing. */
@AndroidEntryPoint
class DataTodayTile : InfoTile() {
    @Inject lateinit var data: DataUsageSource

    override suspend fun refresh() {
        if (data.accessState() != UsageAccessState.GRANTED) {
            show(getString(R.string.tile_data_today), getString(R.string.tile_needs_access), active = false)
            return
        }
        val totals = runCatchingCancellable { data.report(DataPeriod.TODAY, includeApps = false).totals }.getOrNull()
        show(
            label = getString(R.string.tile_data_today),
            subtitle = totals?.let { getString(R.string.tile_data_value, UsageFormat.bytes(it.mobile), UsageFormat.bytes(it.wifi)) } ?: getString(R.string.tile_unknown),
            active = true
        )
    }

    override fun onClick() {
        // A tap refreshes the numbers.
        onStartListening()
    }
}
