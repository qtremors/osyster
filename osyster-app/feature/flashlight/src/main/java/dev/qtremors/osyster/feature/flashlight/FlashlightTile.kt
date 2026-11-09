package dev.qtremors.osyster.feature.flashlight

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.system.domain.TorchController
import javax.inject.Inject
import kotlin.math.roundToInt

/** Turns the flashlight on at the brightness last set in Osyster. */
@AndroidEntryPoint
class FlashlightTile : TileService() {
    @Inject lateinit var torch: TorchController

    override fun onStartListening() = render()

    override fun onClick() {
        val turnOn = !torch.state.value.on
        torch.setOn(turnOn)
        render(expectedOn = turnOn)
    }

    private fun render(expectedOn: Boolean? = null) {
        val tile = qsTile ?: return
        val state = torch.state.value
        val on = expectedOn ?: state.on
        tile.state = when {
            !state.hasFlash || !state.available -> Tile.STATE_UNAVAILABLE
            on -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.tile_flashlight)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (on && state.supportsBrightness) {
                getString(R.string.flash_level, (state.level * 100f / state.maxLevel).roundToInt())
            } else {
                getString(if (on) R.string.flash_on else R.string.flash_off)
            }
        }
        tile.updateTile()
    }
}
