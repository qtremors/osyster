package dev.qtremors.osyster.core.device.data.read

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

/** One reading of the battery. Fields are null when the device does not report them. */
internal data class BatterySnapshot(
    val percent: Int?,
    val status: Int,
    val plugged: Int,
    val health: Int,
    val temperatureC: Float?,
    val voltageMv: Int?,
    val technology: String?,
    val present: Boolean,
    val cycleCount: Int?,
    val currentNowMa: Int?,
    val currentAverageMa: Int?,
    val chargeCounterMah: Int?,
    val energyCounterNwh: Long?,
    val chargeTimeRemainingMs: Long?,
    val powerSaveMode: Boolean
) {
    val isCharging: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
}

internal object BatteryReader {
    fun read(context: Context): BatterySnapshot {
        // A null receiver returns the sticky ACTION_BATTERY_CHANGED intent without registering.
        val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        fun intExtra(name: String): Int? = intent?.getIntExtra(name, Int.MIN_VALUE)?.takeUnless { it == Int.MIN_VALUE }
        fun property(id: Int): Int? = runCatching { manager?.getIntProperty(id) }.getOrNull()
            ?.takeUnless { it == Int.MIN_VALUE || it == 0 }

        val level = intExtra(BatteryManager.EXTRA_LEVEL)
        val scale = intExtra(BatteryManager.EXTRA_SCALE)
        return BatterySnapshot(
            percent = if (level != null && scale != null && scale > 0) level * 100 / scale else null,
            status = intExtra(BatteryManager.EXTRA_STATUS) ?: BatteryManager.BATTERY_STATUS_UNKNOWN,
            plugged = intExtra(BatteryManager.EXTRA_PLUGGED) ?: 0,
            health = intExtra(BatteryManager.EXTRA_HEALTH) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN,
            temperatureC = intExtra(BatteryManager.EXTRA_TEMPERATURE)?.takeIf { it != 0 }?.div(10f),
            voltageMv = intExtra(BatteryManager.EXTRA_VOLTAGE)?.takeIf { it > 0 },
            technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf(String::isNotBlank),
            present = intent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: true,
            cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                intExtra(BatteryManager.EXTRA_CYCLE_COUNT)?.takeIf { it >= 0 }
            } else null,
            // Vendors disagree on units; most report microamps. Values under 10 A in µA become mA.
            currentNowMa = property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.let(::toMilliamps),
            currentAverageMa = property(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)?.let(::toMilliamps),
            chargeCounterMah = property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.div(1000),
            energyCounterNwh = runCatching { manager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) }
                .getOrNull()?.takeUnless { it == Long.MIN_VALUE || it == 0L },
            chargeTimeRemainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { manager?.computeChargeTimeRemaining() }.getOrNull()?.takeIf { it > 0 }
            } else null,
            powerSaveMode = power?.isPowerSaveMode == true
        )
    }

    /** Most kernels report µA; a few report mA already. Anything over 10,000 is treated as µA. */
    internal fun toMilliamps(raw: Int): Int = if (kotlin.math.abs(raw) > 10_000) raw / 1000 else raw
}
