package dev.qtremors.osyster.ui.viewmodel

import dev.qtremors.osyster.monitor.BatteryState
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceInfoUiStateTest {

    @Test
    fun defaultState_containsBatteryState() {
        val battery = BatteryState(
            levelPercentage = 85,
            tempCelsius = 32.5f,
            health = "Good",
            status = "Discharging",
            voltageMv = 4100,
            powerSource = "Battery"
        )
        val state = DeviceInfoUiState(batteryState = battery)

        assertEquals(85, state.batteryState.levelPercentage)
        assertEquals(32.5f, state.batteryState.tempCelsius, 0.01f)
        assertEquals("Good", state.batteryState.health)
        assertEquals("Discharging", state.batteryState.status)
        assertEquals(4100, state.batteryState.voltageMv)
        assertEquals("Battery", state.batteryState.powerSource)
        assertEquals(dev.qtremors.osyster.settings.TemperatureUnit.CELSIUS, state.temperatureUnit)
    }

    @Test
    fun customTemperatureUnit_retainsPreference() {
        val state = DeviceInfoUiState(temperatureUnit = dev.qtremors.osyster.settings.TemperatureUnit.FAHRENHEIT)
        assertEquals(dev.qtremors.osyster.settings.TemperatureUnit.FAHRENHEIT, state.temperatureUnit)
    }
}
