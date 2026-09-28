package dev.qtremors.osyster.core.ui.catalog

import androidx.annotation.StringRes
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.R

/** The longer explanation shown by "About this page" in a row's long-press menu. */
@get:StringRes
val DevicePage.help: Int
    get() = when (this) {
        DevicePage.SYSTEM -> R.string.help_page_system
        DevicePage.DEVICE -> R.string.help_page_device
        DevicePage.PROCESSOR -> R.string.help_page_processor
        DevicePage.MEMORY -> R.string.help_page_memory
        DevicePage.BATTERY -> R.string.help_page_battery
        DevicePage.DISPLAY -> R.string.help_page_display
        DevicePage.CAMERAS -> R.string.help_page_cameras
        DevicePage.SENSORS -> R.string.help_page_sensors
        DevicePage.NETWORK -> R.string.help_page_network
        DevicePage.BLUETOOTH -> R.string.help_page_bluetooth
        DevicePage.MEDIA -> R.string.help_page_media
        DevicePage.CERTIFICATES -> R.string.help_page_certificates
    }

/** The longer explanation shown by "About this tool" in a row's long-press menu. */
@get:StringRes
val Tool.help: Int
    get() = when (this) {
        Tool.DATA_USAGE -> R.string.help_tool_data_usage
        Tool.SCREEN_USAGE -> R.string.help_tool_screen_usage
        Tool.APP_STOPPER -> R.string.help_tool_app_stopper
        Tool.STAY_AWAKE -> R.string.help_tool_stay_awake
        Tool.FLASHLIGHT -> R.string.help_tool_flashlight
        Tool.ACTIVITY_LAUNCHER -> R.string.help_tool_activity_launcher
        Tool.LINK_ACTIONS -> R.string.help_tool_link_actions
        Tool.WIDGETS -> R.string.help_tool_widgets
        Tool.QUICK_TILES -> R.string.help_tool_quick_tiles
        Tool.POWER_SETUP -> R.string.help_tool_power_setup
        Tool.POWER_SAVING -> R.string.help_tool_power_saving
        Tool.DISPLAY_SCALE -> R.string.help_tool_display_scale
        Tool.STATUS_BAR_ICONS -> R.string.help_tool_status_bar_icons
        Tool.SYSTEM_TWEAKS -> R.string.help_tool_system_tweaks
        Tool.SHUT_UP -> R.string.help_tool_shut_up
        Tool.STANDBY_APPS -> R.string.help_tool_standby_apps
        Tool.FREEZE -> R.string.help_tool_freeze
    }
