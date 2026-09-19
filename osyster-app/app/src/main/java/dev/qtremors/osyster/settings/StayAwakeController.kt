package dev.qtremors.osyster.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit

data class StayAwakeState(
    val enabled: Boolean,
    val canWrite: Boolean,
    val powerSourceMask: Int
)

enum class StayAwakeChangeResult {
    CHANGED,
    PERMISSION_REQUIRED,
    FAILED
}

/** Controls Android's developer "Stay awake" setting for plugged-in devices. */
object StayAwakeController {

    private const val PREFS_NAME = "osyster_stay_awake"
    private const val KEY_PREVIOUS_MASK = "previous_power_source_mask"
    private const val KEY_MANAGED = "managed_by_osyster"

    fun read(context: Context): StayAwakeState {
        val mask = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            0
        )
        return StayAwakeState(
            enabled = mask != 0,
            canWrite = hasWritePermission(context),
            powerSourceMask = mask
        )
    }

    fun setEnabled(context: Context, enabled: Boolean): StayAwakeChangeResult {
        if (!hasWritePermission(context)) return StayAwakeChangeResult.PERMISSION_REQUIRED

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentMask = read(context).powerSourceMask
        val targetMask = if (enabled) {
            supportedPowerSourceMask()
        } else {
            if (prefs.getBoolean(KEY_MANAGED, false)) {
                prefs.getInt(KEY_PREVIOUS_MASK, 0)
            } else {
                0
            }
        }

        return try {
            if (
                Settings.Global.putInt(
                    context.contentResolver,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    targetMask
                )
            ) {
                if (enabled) {
                    prefs.edit {
                        putInt(KEY_PREVIOUS_MASK, currentMask)
                        putBoolean(KEY_MANAGED, true)
                    }
                } else {
                    prefs.edit {
                        remove(KEY_PREVIOUS_MASK)
                        putBoolean(KEY_MANAGED, false)
                    }
                }
                StayAwakeChangeResult.CHANGED
            } else {
                StayAwakeChangeResult.FAILED
            }
        } catch (_: SecurityException) {
            StayAwakeChangeResult.PERMISSION_REQUIRED
        } catch (_: RuntimeException) {
            StayAwakeChangeResult.FAILED
        }
    }

    fun grantCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    fun developerOptionsIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)

    private fun hasWritePermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    private fun supportedPowerSourceMask(): Int {
        var mask = BatteryManager.BATTERY_PLUGGED_AC or
            BatteryManager.BATTERY_PLUGGED_USB or
            BatteryManager.BATTERY_PLUGGED_WIRELESS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mask = mask or BatteryManager.BATTERY_PLUGGED_DOCK
        }
        return mask
    }
}
