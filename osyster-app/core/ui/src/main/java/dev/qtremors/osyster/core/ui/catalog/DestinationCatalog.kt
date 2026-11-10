package dev.qtremors.osyster.core.ui.catalog

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.DevicePageGroup
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.navigation.ToolGroup
import dev.qtremors.osyster.core.ui.R

/**
 * How each destination looks wherever it is listed: Home's device list, the Tools tab, pinned
 * favorites, search results, and the toolbar title on its own page. Kept in one place so a page
 * never has two names.
 */
data class DestinationLabel(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    val icon: ImageVector
)

val DevicePage.label: DestinationLabel
    get() = when (this) {
        DevicePage.SYSTEM -> DestinationLabel(R.string.page_system, R.string.page_system_summary, Icons.Rounded.Android)
        DevicePage.DEVICE -> DestinationLabel(R.string.page_device, R.string.page_device_summary, Icons.Rounded.Smartphone)
        DevicePage.PROCESSOR -> DestinationLabel(R.string.page_processor, R.string.page_processor_summary, Icons.Rounded.DeveloperBoard)
        DevicePage.MEMORY -> DestinationLabel(R.string.page_memory, R.string.page_memory_summary, Icons.Rounded.Storage)
        DevicePage.BATTERY -> DestinationLabel(R.string.page_battery, R.string.page_battery_summary, Icons.Rounded.BatteryStd)
        DevicePage.DISPLAY -> DestinationLabel(R.string.page_display, R.string.page_display_summary, Icons.Rounded.DisplaySettings)
        DevicePage.CAMERAS -> DestinationLabel(R.string.page_cameras, R.string.page_cameras_summary, Icons.Rounded.PhotoCamera)
        DevicePage.SENSORS -> DestinationLabel(R.string.page_sensors, R.string.page_sensors_summary, Icons.Rounded.Sensors)
        DevicePage.NETWORK -> DestinationLabel(R.string.page_network, R.string.page_network_summary, Icons.Rounded.SignalCellularAlt)
        DevicePage.BLUETOOTH -> DestinationLabel(R.string.page_bluetooth, R.string.page_bluetooth_summary, Icons.Rounded.Bluetooth)
        DevicePage.MEDIA -> DestinationLabel(R.string.page_media, R.string.page_media_summary, Icons.Rounded.PlayCircle)
        DevicePage.CERTIFICATES -> DestinationLabel(R.string.page_certificates, R.string.page_certificates_summary, Icons.Rounded.VerifiedUser)
    }

@get:StringRes
val DevicePageGroup.title: Int
    get() = when (this) {
        DevicePageGroup.ABOUT -> R.string.group_about
        DevicePageGroup.PERFORMANCE -> R.string.group_performance
        DevicePageGroup.HARDWARE -> R.string.group_hardware
        DevicePageGroup.CONNECTIVITY -> R.string.group_connectivity
        DevicePageGroup.MEDIA_SECURITY -> R.string.group_media_security
    }

val Tool.label: DestinationLabel
    get() = when (this) {
        Tool.DATA_USAGE -> DestinationLabel(R.string.tool_data_usage, R.string.tool_data_usage_summary, Icons.Rounded.DataUsage)
        Tool.SCREEN_USAGE -> DestinationLabel(R.string.tool_screen_usage, R.string.tool_screen_usage_summary, Icons.Rounded.HourglassBottom)
        Tool.APP_STOPPER -> DestinationLabel(R.string.tool_app_stopper, R.string.tool_app_stopper_summary, Icons.Rounded.StopCircle)
        Tool.STAY_AWAKE -> DestinationLabel(R.string.tool_stay_awake, R.string.tool_stay_awake_summary, Icons.Rounded.Coffee)
        Tool.FLASHLIGHT -> DestinationLabel(R.string.tool_flashlight, R.string.tool_flashlight_summary, Icons.Rounded.FlashlightOn)
        Tool.BT_REMOTE -> DestinationLabel(R.string.tool_bt_remote, R.string.tool_bt_remote_summary, Icons.Rounded.SettingsRemote)
        Tool.BARCODE -> DestinationLabel(R.string.tool_barcode, R.string.tool_barcode_summary, Icons.Rounded.QrCodeScanner)
        Tool.ACTIVITY_LAUNCHER -> DestinationLabel(R.string.tool_activity_launcher, R.string.tool_activity_launcher_summary, Icons.Rounded.RocketLaunch)
        Tool.LINK_ACTIONS -> DestinationLabel(R.string.tool_link_actions, R.string.tool_link_actions_summary, Icons.Rounded.Link)
        Tool.WIDGETS -> DestinationLabel(R.string.tool_widgets, R.string.tool_widgets_summary, Icons.Rounded.Widgets)
        Tool.QUICK_TILES -> DestinationLabel(R.string.tool_quick_tiles, R.string.tool_quick_tiles_summary, Icons.Rounded.SpaceDashboard)
        Tool.POWER_SETUP -> DestinationLabel(R.string.tool_power_setup, R.string.tool_power_setup_summary, Icons.Rounded.AdminPanelSettings)
        Tool.POWER_SAVING -> DestinationLabel(R.string.tool_power_saving, R.string.tool_power_saving_summary, Icons.Rounded.EnergySavingsLeaf)
        Tool.DISPLAY_SCALE -> DestinationLabel(R.string.tool_display_scale, R.string.tool_display_scale_summary, Icons.Rounded.FormatSize)
        Tool.STATUS_BAR_ICONS -> DestinationLabel(R.string.tool_status_bar_icons, R.string.tool_status_bar_icons_summary, Icons.Rounded.VisibilityOff)
        Tool.SYSTEM_TWEAKS -> DestinationLabel(R.string.tool_system_tweaks, R.string.tool_system_tweaks_summary, Icons.Rounded.Tune)
        Tool.SHUT_UP -> DestinationLabel(R.string.tool_shut_up, R.string.tool_shut_up_summary, Icons.Rounded.DoNotDisturbOn)
        Tool.STANDBY_APPS -> DestinationLabel(R.string.tool_standby_apps, R.string.tool_standby_apps_summary, Icons.Rounded.Bedtime)
    }

@get:StringRes
val ToolGroup.title: Int
    get() = when (this) {
        ToolGroup.USAGE -> R.string.tool_group_usage
        ToolGroup.UTILITIES -> R.string.tool_group_utilities
        ToolGroup.POWER -> R.string.tool_group_power
    }
