package dev.qtremors.osyster.core.device.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingPreferencesTest {
    @Test
    fun formatsEachUnit() {
        assertEquals("31.0 °C", formatTemperature(31f, TemperatureUnit.CELSIUS, Locale.US))
        assertEquals("87.8 °F", formatTemperature(31f, TemperatureUnit.FAHRENHEIT, Locale.US))
        assertEquals("304.1 K", formatTemperature(31f, TemperatureUnit.KELVIN, Locale.US))
    }

    @Test
    fun scalesIntervals() {
        assertEquals(1_000L, RefreshSpeed.FAST.scale(2_000L))
        assertEquals(4_000L, RefreshSpeed.BATTERY_SAVER.scale(2_000L))
        assertEquals(250L, RefreshSpeed.FAST.scale(100L))
    }
}
