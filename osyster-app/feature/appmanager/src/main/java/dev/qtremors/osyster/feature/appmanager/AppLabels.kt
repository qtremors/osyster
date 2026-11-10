package dev.qtremors.osyster.feature.appmanager

import android.content.Context
import android.text.format.Formatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GetApp
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.qtremors.osyster.core.apps.domain.PermissionArea
import dev.qtremors.osyster.core.apps.domain.PermissionGroup
import dev.qtremors.osyster.core.apps.domain.Risk
import dev.qtremors.osyster.core.apps.domain.SpecialAccess

internal val PermissionGroup.label: Int
    get() = when (this) {
        PermissionGroup.PHONE_SMS_CONTACTS -> R.string.group_phone
        PermissionGroup.LOCATION -> R.string.group_location
        PermissionGroup.FILES_MEDIA -> R.string.group_files
        PermissionGroup.NOTIFICATIONS -> R.string.group_notifications
        PermissionGroup.CALENDAR -> R.string.group_calendar
        PermissionGroup.SENSORS -> R.string.group_sensors
    }

internal val PermissionArea.label: Int
    get() = when (this) {
        PermissionArea.CONTACTS -> R.string.area_contacts
        PermissionArea.SMS -> R.string.area_sms
        PermissionArea.CALL_LOG -> R.string.area_call_log
        PermissionArea.PHONE -> R.string.area_phone
        PermissionArea.LOCATION -> R.string.area_location
        PermissionArea.BACKGROUND_LOCATION -> R.string.area_background_location
        PermissionArea.NEARBY_DEVICES -> R.string.area_nearby
        PermissionArea.FILES -> R.string.area_files
        PermissionArea.PHOTOS_VIDEOS -> R.string.area_photos
        PermissionArea.MUSIC_AUDIO -> R.string.area_music
        PermissionArea.NOTIFICATIONS -> R.string.area_notifications
        PermissionArea.CALENDAR -> R.string.area_calendar
        PermissionArea.CAMERA -> R.string.area_camera
        PermissionArea.MICROPHONE -> R.string.area_microphone
        PermissionArea.BODY_SENSORS -> R.string.area_body_sensors
        PermissionArea.PHYSICAL_ACTIVITY -> R.string.area_activity
    }

internal val PermissionArea.icon: ImageVector
    get() = when (this) {
        PermissionArea.CONTACTS -> Icons.Rounded.Contacts
        PermissionArea.SMS -> Icons.AutoMirrored.Rounded.Message
        PermissionArea.CALL_LOG -> Icons.Rounded.History
        PermissionArea.PHONE -> Icons.Rounded.Call
        PermissionArea.LOCATION -> Icons.Rounded.LocationOn
        PermissionArea.BACKGROUND_LOCATION -> Icons.Rounded.MyLocation
        PermissionArea.NEARBY_DEVICES -> Icons.Rounded.Share
        PermissionArea.FILES -> Icons.Rounded.Folder
        PermissionArea.PHOTOS_VIDEOS -> Icons.Rounded.Photo
        PermissionArea.MUSIC_AUDIO -> Icons.Rounded.MusicNote
        PermissionArea.NOTIFICATIONS -> Icons.Rounded.Notifications
        PermissionArea.CALENDAR -> Icons.Rounded.CalendarMonth
        PermissionArea.CAMERA -> Icons.Rounded.CameraAlt
        PermissionArea.MICROPHONE -> Icons.Rounded.Mic
        PermissionArea.BODY_SENSORS -> Icons.Rounded.Favorite
        PermissionArea.PHYSICAL_ACTIVITY -> Icons.AutoMirrored.Rounded.DirectionsRun
    }

internal val SpecialAccess.label: Int
    get() = when (this) {
        SpecialAccess.ACCESSIBILITY -> R.string.special_accessibility
        SpecialAccess.NOTIFICATION_LISTENER -> R.string.special_notification_listener
        SpecialAccess.DEVICE_ADMIN -> R.string.special_device_admin
        SpecialAccess.ALL_FILES -> R.string.special_all_files
        SpecialAccess.INSTALL_APPS -> R.string.special_install_apps
        SpecialAccess.DRAW_OVER_APPS -> R.string.special_draw_over
        SpecialAccess.USAGE_ACCESS -> R.string.special_usage
        SpecialAccess.MODIFY_SETTINGS -> R.string.special_settings
        SpecialAccess.EXACT_ALARMS -> R.string.special_alarms
        SpecialAccess.UNRESTRICTED_BATTERY -> R.string.special_battery
    }

internal val SpecialAccess.icon: ImageVector
    get() = when (this) {
        SpecialAccess.ACCESSIBILITY -> Icons.Rounded.Accessibility
        SpecialAccess.NOTIFICATION_LISTENER -> Icons.Rounded.NotificationsActive
        SpecialAccess.DEVICE_ADMIN -> Icons.Rounded.AdminPanelSettings
        SpecialAccess.ALL_FILES -> Icons.Rounded.SdStorage
        SpecialAccess.INSTALL_APPS -> Icons.Rounded.GetApp
        SpecialAccess.DRAW_OVER_APPS -> Icons.Rounded.Layers
        SpecialAccess.USAGE_ACCESS -> Icons.Rounded.QueryStats
        SpecialAccess.MODIFY_SETTINGS -> Icons.Rounded.SettingsSuggest
        SpecialAccess.EXACT_ALARMS -> Icons.Rounded.Alarm
        SpecialAccess.UNRESTRICTED_BATTERY -> Icons.Rounded.BatteryChargingFull
    }

internal val Risk.label: Int
    get() = when (this) {
        Risk.HIGH -> R.string.apps_risk_high
        Risk.MEDIUM -> R.string.apps_risk_medium
        Risk.LOW -> R.string.apps_risk_low
    }

/** Error red for high, tertiary for medium, and the plain variant color for low. */
@Composable
internal fun Risk.color(): Color = when (this) {
    Risk.HIGH -> MaterialTheme.colorScheme.error
    Risk.MEDIUM -> MaterialTheme.colorScheme.tertiary
    Risk.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Android's release number for an API level, for the SDK rows. */
internal fun androidVersion(api: Int): String = when (api) {
    0 -> "?"
    in 1..22 -> "5.1 or older"
    23 -> "6"
    24 -> "7"
    25 -> "7.1"
    26 -> "8"
    27 -> "8.1"
    28 -> "9"
    29 -> "10"
    30 -> "11"
    31 -> "12"
    32 -> "12L"
    // From Android 13 (API 33) the version is the API level less 20.
    else -> (api - 20).toString()
}

internal fun size(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes)
