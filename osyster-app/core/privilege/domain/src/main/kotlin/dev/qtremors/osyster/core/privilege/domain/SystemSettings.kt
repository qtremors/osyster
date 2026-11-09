package dev.qtremors.osyster.core.privilege.domain

import kotlinx.coroutines.flow.Flow

/** The three tables of Android's settings provider, named as the `settings` command names them. */
enum class SettingsTable(val shellName: String) {
    SYSTEM("system"),
    SECURE("secure"),
    GLOBAL("global")
}

data class SettingKey(val table: SettingsTable, val name: String) {
    init {
        SettingKeys.require(name)
    }
}

/**
 * Reads any setting, and writes the ones power tools change. Writes go straight to the settings
 * provider when Osyster holds WRITE_SECURE_SETTINGS, and through the shell otherwise.
 */
interface SystemSettings {
    /**
     * The current value, or null when unset. Since Android 12 apps cannot read many settings
     * directly; those are read through the shell when Shizuku or root is available, and read as
     * null (the system default) otherwise.
     */
    suspend fun read(key: SettingKey): String?

    /** Emits once at start and again whenever any of [keys] changes, from Osyster or elsewhere. */
    fun changes(keys: Set<SettingKey>): Flow<Unit>

    /** Writes [value], or deletes the setting when [value] is null. False when no route works. */
    suspend fun write(key: SettingKey, value: String?): Boolean
}

suspend fun SystemSettings.readInt(key: SettingKey): Int? = read(key)?.trim()?.toIntOrNull()
suspend fun SystemSettings.readFloat(key: SettingKey): Float? = read(key)?.trim()?.toFloatOrNull()
suspend fun SystemSettings.readFlag(key: SettingKey, default: Boolean = false): Boolean = readInt(key)?.let { it != 0 } ?: default

suspend fun SystemSettings.writeFlag(key: SettingKey, on: Boolean): Boolean = write(key, if (on) "1" else "0")

/** Settings the power tools use. Names are the provider's own; none of them are hidden APIs. */
object Keys {
    val LowPower = SettingKey(SettingsTable.GLOBAL, "low_power")
    val LowPowerTrigger = SettingKey(SettingsTable.GLOBAL, "low_power_trigger_level")
    val BatterySaverConstants = SettingKey(SettingsTable.GLOBAL, "battery_saver_constants")

    val WindowAnimation = SettingKey(SettingsTable.GLOBAL, "window_animation_scale")
    val TransitionAnimation = SettingKey(SettingsTable.GLOBAL, "transition_animation_scale")
    val AnimatorDuration = SettingKey(SettingsTable.GLOBAL, "animator_duration_scale")
    val FontScale = SettingKey(SettingsTable.SYSTEM, "font_scale")

    val IconBlacklist = SettingKey(SettingsTable.SECURE, "icon_blacklist")
    val BatteryPercent = SettingKey(SettingsTable.SYSTEM, "status_bar_show_battery_percent")

    val DeveloperOptions = SettingKey(SettingsTable.GLOBAL, "development_settings_enabled")
    val UsbDebugging = SettingKey(SettingsTable.GLOBAL, "adb_enabled")
    val WirelessDebugging = SettingKey(SettingsTable.GLOBAL, "adb_wifi_enabled")
    val DisableBlurs = SettingKey(SettingsTable.GLOBAL, "disable_window_blurs")
    val TapToWake = SettingKey(SettingsTable.SECURE, "doze_tap_gesture")
    val DoubleTapToWake = SettingKey(SettingsTable.SECURE, "double_tap_to_wake")
    val HeadsUp = SettingKey(SettingsTable.GLOBAL, "heads_up_notifications_enabled")
    val PrivateDnsMode = SettingKey(SettingsTable.GLOBAL, "private_dns_mode")
    val PrivateDnsHost = SettingKey(SettingsTable.GLOBAL, "private_dns_specifier")
    val ShowTouches = SettingKey(SettingsTable.SYSTEM, "show_touches")
    val PointerLocation = SettingKey(SettingsTable.SYSTEM, "pointer_location")
    val StayOnWhilePluggedIn = SettingKey(SettingsTable.GLOBAL, "stay_on_while_plugged_in")
}
