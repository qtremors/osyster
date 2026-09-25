package dev.qtremors.osyster.core.system.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Blinks the flashlight when a notification arrives. Needs notification access, which the music
 * widgets share. Only pulses with the screen off, never for ongoing notifications (music, calls,
 * downloads) or Osyster's own, and at most once every few seconds.
 */
interface FlashlightPulse {
    val enabled: StateFlow<Boolean>
    fun setEnabled(enabled: Boolean)
    fun hasNotificationAccess(): Boolean
    fun onNotificationPosted(packageName: String, ongoing: Boolean, postedAt: Long)
}

/** The rule behind [FlashlightPulse.onNotificationPosted], kept pure so it can be tested. */
object PulsePolicy {
    const val MIN_GAP_MS = 4_000L

    fun shouldPulse(
        enabled: Boolean,
        screenOn: Boolean,
        torchOn: Boolean,
        fromSelf: Boolean,
        ongoing: Boolean,
        sinceLastPulseMs: Long
    ): Boolean = enabled && !screenOn && !torchOn && !fromSelf && !ongoing && sinceLastPulseMs >= MIN_GAP_MS
}
