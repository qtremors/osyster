package dev.qtremors.osyster.core.device.data.pages

import dev.qtremors.osyster.core.device.domain.TemperatureUnit
import dev.qtremors.osyster.core.device.domain.formatTemperature
import android.os.BatteryManager
import android.os.Build
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.format.UnitFormat
import dev.qtremors.osyster.core.device.data.read.BatterySnapshot
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection

/**
 * The Battery page. Only what BatteryManager reports: no estimated capacity, health score, or
 * per-app drain, because Android gives apps no honest source for them.
 */
internal class BatteryPageCollector(private val text: DeviceText, private val unit: () -> TemperatureUnit) {

    fun collect(battery: BatterySnapshot): List<InfoSection> = listOf(status(battery), health(battery), power(battery))

    private fun status(b: BatterySnapshot) = section(text, "status", R.string.section_battery_status, "BatteryManager") {
        row("level", R.string.row_battery_level, b.percent?.let { "$it%" }, copyable = false)
        row("charging", R.string.row_charging_state, text[statusLabel(b.status)], copyable = false)
        row("source", R.string.row_power_source, text[pluggedLabel(b.plugged)], copyable = false)
        row("time_to_full", R.string.row_time_to_full, when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> Availability.Unsupported("Needs Android 9")
            !b.isCharging -> available(text[R.string.value_not_charging])
            b.chargeTimeRemainingMs != null -> available(UnitFormat.duration(b.chargeTimeRemainingMs))
            else -> Availability.Unavailable("Still estimating", retryable = true)
        }, copyable = false)
        yesNo("saver", R.string.row_battery_saver, b.powerSaveMode)
    }

    private fun health(b: BatterySnapshot) = section(text, "health", R.string.section_battery_health, "BatteryManager") {
        row("health", R.string.row_health, text[healthLabel(b.health)], copyable = false)
        row("temperature", R.string.row_temperature, b.temperatureC?.let { formatTemperature(it, unit()) }, copyable = false)
        row("voltage", R.string.row_voltage, b.voltageMv?.let(UnitFormat::millivolts), copyable = false)
        row("technology", R.string.row_technology, b.technology)
        row("cycles", R.string.row_cycle_count, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            b.cycleCount?.let { available(it.toString()) } ?: Availability.Unavailable("Not reported by the device", retryable = false)
        } else {
            Availability.Unsupported("Needs Android 14")
        }, copyable = false)
        yesNo("present", R.string.row_battery_present, b.present, advanced = true)
    }

    private fun power(b: BatterySnapshot) = section(text, "power", R.string.section_battery_power, "BatteryManager") {
        row("current_now", R.string.row_current_now, b.currentNowMa?.let(UnitFormat::milliamps), copyable = false)
        row("current_average", R.string.row_current_average, b.currentAverageMa?.let(UnitFormat::milliamps), copyable = false)
        val watts = if (b.currentNowMa != null && b.voltageMv != null) b.currentNowMa.toDouble() * b.voltageMv / 1_000_000.0 else null
        row("power", R.string.row_power_calculated, watts?.takeIf { it != 0.0 }?.let {
            text.format(if (it > 0) R.string.value_power_in else R.string.value_power_out, UnitFormat.watts(it))
        }, copyable = false)
        row("charge_counter", R.string.row_charge_counter, b.chargeCounterMah?.let { "$it mAh" }, advanced = true, copyable = false)
        row("energy_counter", R.string.row_energy_counter, b.energyCounterNwh?.let { "${it / 1_000_000} mWh" }, advanced = true, copyable = false)
    }

    private fun statusLabel(status: Int) = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> R.string.battery_charging
        BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.battery_discharging
        BatteryManager.BATTERY_STATUS_FULL -> R.string.battery_full
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.battery_not_charging
        else -> R.string.value_unknown
    }

    private fun pluggedLabel(plugged: Int) = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> R.string.plugged_ac
        BatteryManager.BATTERY_PLUGGED_USB -> R.string.plugged_usb
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> R.string.plugged_wireless
        PLUGGED_DOCK -> R.string.plugged_dock
        0 -> R.string.plugged_battery
        else -> R.string.value_unknown
    }

    private fun healthLabel(health: Int) = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> R.string.health_good
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> R.string.health_overheat
        BatteryManager.BATTERY_HEALTH_DEAD -> R.string.health_dead
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> R.string.health_over_voltage
        BatteryManager.BATTERY_HEALTH_COLD -> R.string.health_cold
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> R.string.health_failure
        else -> R.string.value_unknown
    }

    private companion object {
        /** BatteryManager.BATTERY_PLUGGED_DOCK, public only from Android 13 but reported earlier. */
        const val PLUGGED_DOCK = 8
    }
}
