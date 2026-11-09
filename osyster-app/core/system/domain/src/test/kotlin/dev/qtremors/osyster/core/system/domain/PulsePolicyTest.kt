package dev.qtremors.osyster.core.system.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PulsePolicyTest {
    private fun pulse(
        enabled: Boolean = true,
        screenOn: Boolean = false,
        torchOn: Boolean = false,
        fromSelf: Boolean = false,
        ongoing: Boolean = false,
        since: Long = 10_000
    ) = PulsePolicy.shouldPulse(enabled, screenOn, torchOn, fromSelf, ongoing, since)

    @Test
    fun pulsesForANewNotificationWithTheScreenOff() = assertTrue(pulse())

    @Test
    fun neverPulsesWhenOffOrScreenOnOrTorchOn() {
        assertFalse(pulse(enabled = false))
        assertFalse(pulse(screenOn = true))
        assertFalse(pulse(torchOn = true))
    }

    @Test
    fun ignoresOngoingAndOwnNotifications() {
        assertFalse(pulse(ongoing = true))
        assertFalse(pulse(fromSelf = true))
    }

    @Test
    fun rateLimitsBursts() {
        assertFalse(pulse(since = PulsePolicy.MIN_GAP_MS - 1))
        assertTrue(pulse(since = PulsePolicy.MIN_GAP_MS))
    }
}
