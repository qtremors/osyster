package dev.qtremors.osyster.core.system.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.system.domain.StayAwakeController
import dev.qtremors.osyster.core.system.domain.StayAwakeTimer
import dev.qtremors.osyster.core.system.domain.WhileChargingState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Timer mode runs in [StayAwakeService] so the screen stays on with Osyster closed; the service
 * reports back here. While-charging mode is Android's own developer setting.
 */
@Singleton
internal class StayAwakeControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val secureSettings: SecureSettingsAccessImpl
) : StayAwakeController {
    private val _timer = MutableStateFlow<StayAwakeTimer>(StayAwakeTimer.Off)
    override val timer: StateFlow<StayAwakeTimer> = _timer.asStateFlow()

    override fun start(minutes: Int?) {
        val intent = Intent(context, StayAwakeService::class.java)
            .setAction(StayAwakeService.ACTION_START)
            .putExtra(StayAwakeService.EXTRA_MINUTES, minutes ?: StayAwakeService.UNTIL_STOPPED)
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stop() {
        if (_timer.value is StayAwakeTimer.Off) return
        context.startService(Intent(context, StayAwakeService::class.java).setAction(StayAwakeService.ACTION_STOP))
    }

    /** Called by the service as it starts and stops. */
    internal fun report(timer: StayAwakeTimer) {
        _timer.value = timer
    }

    override fun whileCharging(): WhileChargingState {
        val value = Settings.Global.getInt(context.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0)
        return WhileChargingState(canChange = secureSettings.isGranted(), enabled = value != 0)
    }

    override fun setWhileCharging(enabled: Boolean): Boolean = runCatching {
        Settings.Global.putInt(context.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, if (enabled) ALL_POWER_SOURCES else 0)
    }.getOrDefault(false)

    private companion object {
        /** AC, USB, wireless, and dock, so the screen stays on however the phone charges. */
        const val ALL_POWER_SOURCES = 1 or 2 or 4 or 8
    }
}
