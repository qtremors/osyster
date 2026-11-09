package dev.qtremors.osyster.core.device.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvailabilityTest {
    @Test
    fun availableHasNoLabelAndMapsValue() {
        val value: Availability<Int> = Availability.Available(42, "test", 0L)
        assertNull(value.label)
        assertEquals("42", value.map(Int::toString).getOrNull())
    }

    @Test
    fun failuresNeverBecomeValues() {
        val restricted: Availability<Int> = Availability.Restricted("Android 8+")
        assertNull(restricted.map { it * 2 }.getOrNull())
        assertEquals(AvailabilityLabel.RESTRICTED_BY_ANDROID, restricted.label)
    }

    @Test
    fun retryableDecidesUnavailableLabel() {
        assertEquals(AvailabilityLabel.TEMPORARILY_UNAVAILABLE, Availability.Unavailable("busy", true).label)
        assertEquals(AvailabilityLabel.NOT_REPORTED, Availability.Unavailable("vendor", false).label)
        assertEquals(AvailabilityLabel.COLLECTION_FAILED, Availability.Failed("io", false).label)
    }
}
