package dev.qtremors.osyster.feature.stayawake

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.system.domain.StayAwakeController
import dev.qtremors.osyster.core.system.domain.StayAwakeStep
import dev.qtremors.osyster.core.system.domain.StayAwakeTimer
import dev.qtremors.osyster.core.system.domain.nextStayAwakeStep
import javax.inject.Inject

/** Each tap steps 15 min, 30 min, 1 h, 2 h, until stopped, then off. */
@AndroidEntryPoint
class StayAwakeTimerTile : TileService() {
    @Inject lateinit var controller: StayAwakeController

    override fun onStartListening() = render()

    override fun onClick() {
        when (val step = nextStayAwakeStep(controller.timer.value)) {
            is StayAwakeStep.Start -> controller.start(step.minutes)
            StayAwakeStep.Stop -> controller.stop()
        }
        // The service reports back asynchronously; show the expected state right away.
        render(expected = nextStayAwakeStep(controller.timer.value))
    }

    private fun render(expected: StayAwakeStep? = null) {
        val tile = qsTile ?: return
        val running = when (expected) {
            is StayAwakeStep.Start -> true
            StayAwakeStep.Stop -> false
            null -> controller.timer.value is StayAwakeTimer.Running
        }
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_stay_awake_timer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val minutes = (expected as? StayAwakeStep.Start)?.minutes ?: (controller.timer.value as? StayAwakeTimer.Running)?.minutes
            tile.subtitle = when {
                !running -> getString(R.string.tile_off)
                minutes == null -> getString(R.string.stay_awake_duration_forever)
                minutes < 60 -> getString(R.string.stay_awake_duration_minutes, minutes)
                else -> getString(R.string.stay_awake_duration_hours, minutes / 60)
            }
        }
        tile.updateTile()
    }
}

/** Toggles Android's "Stay awake while charging"; opens setup when the grant is missing. */
@AndroidEntryPoint
class StayAwakeChargingTile : TileService() {
    @Inject lateinit var controller: StayAwakeController

    override fun onStartListening() = render()

    override fun onClick() {
        val state = controller.whileCharging()
        if (!state.canChange) {
            openSetup()
            return
        }
        controller.setWhileCharging(!state.enabled)
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val state = controller.whileCharging()
        tile.state = when {
            !state.canChange -> Tile.STATE_INACTIVE
            state.enabled -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.tile_stay_awake_charging)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when {
                    !state.canChange -> R.string.tile_needs_setup
                    state.enabled -> R.string.tile_on
                    else -> R.string.tile_off
                }
            )
        }
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun openSetup() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.putExtra(DeepLinks.EXTRA_OPEN_TOOL, Tool.STAY_AWAKE.id) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
