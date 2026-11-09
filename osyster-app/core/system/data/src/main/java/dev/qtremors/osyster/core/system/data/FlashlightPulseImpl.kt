package dev.qtremors.osyster.core.system.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.system.domain.FlashlightPulse
import dev.qtremors.osyster.core.system.domain.PulsePolicy
import dev.qtremors.osyster.core.system.domain.TorchController
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
internal class FlashlightPulseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val torch: TorchController
) : FlashlightPulse {
    private val prefs = context.getSharedPreferences("flashlight_pulse", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private var lastPulseAt = 0L

    override fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    override fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    override fun onNotificationPosted(packageName: String, ongoing: Boolean, postedAt: Long) {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val pulse = PulsePolicy.shouldPulse(
            enabled = _enabled.value,
            screenOn = power?.isInteractive != false,
            torchOn = torch.state.value.on,
            fromSelf = packageName == context.packageName,
            ongoing = ongoing,
            sinceLastPulseMs = postedAt - lastPulseAt
        )
        if (!pulse) return
        lastPulseAt = postedAt
        blink(times = 2)
    }

    /** Short on and off flashes, then guaranteed off. */
    private fun blink(times: Int) {
        var delay = 0L
        repeat(times) {
            handler.postDelayed({ torch.setOn(true) }, delay)
            handler.postDelayed({ torch.setOn(false) }, delay + FLASH_MS)
            delay += FLASH_MS * 2
        }
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val FLASH_MS = 140L
    }
}
