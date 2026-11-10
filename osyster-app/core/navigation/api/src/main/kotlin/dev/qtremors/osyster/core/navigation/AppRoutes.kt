package dev.qtremors.osyster.core.navigation

import kotlinx.serialization.Serializable

/**
 * Every destination in the app. Features emit neutral destinations; only the app shell maps
 * them to these routes.
 */
sealed interface AppRoute

/** The three top-level tabs shown in the floating toolbar. */
@Serializable
data object HomeRoute : AppRoute

@Serializable
data object ToolsRoute : AppRoute

@Serializable
data object SettingsRoute : AppRoute

/**
 * One device information page, addressed by the stable id from the device page catalog
 * (for example "system" or "cpu"). Stable ids keep pinned favorites and saved order valid
 * when screens are renamed.
 */
@Serializable
data class DevicePageRoute(val pageId: String) : AppRoute

/** Settings sub-pages. */
@Serializable
data object AboutRoute : AppRoute

@Serializable
data object PrivacyRoute : AppRoute

@Serializable
data object LicensesRoute : AppRoute


/** Full-screen color and touch tests, opened from the Display page. */
@Serializable
data object ScreenTesterRoute : AppRoute

@Serializable
data object DataUsageRoute : AppRoute

@Serializable
data object ScreenUsageRoute : AppRoute

@Serializable
data object AppStopperRoute : AppRoute

@Serializable
data object StayAwakeRoute : AppRoute

@Serializable
data object FlashlightRoute : AppRoute

/** The Bluetooth remote: devices, then the remote once one connects. */
@Serializable
data object BtRemoteRoute : AppRoute

/**
 * The barcode scanner: camera, picture scanning, and results. [pickPicture] opens the photo
 * picker straight away, for the "Scan a picture" launcher shortcut.
 */
@Serializable
data class BarcodeRoute(val pickPicture: Boolean = false) : AppRoute

/** Saved notifications, newest first. */
@Serializable
data object NotificationHistoryRoute : AppRoute

/** Past scans. */
@Serializable
data object BarcodeHistoryRoute : AppRoute

/** The kinds of code the creator makes. */
@Serializable
data object BarcodeCreateRoute : AppRoute

/** One creator form; [kind] is a CreateKind name. */
@Serializable
data class BarcodeCreateFormRoute(val kind: String) : AppRoute

@Serializable
data object ActivityLauncherRoute : AppRoute

@Serializable
data object LinkActionsRoute : AppRoute

@Serializable
data object QuickTilesRoute : AppRoute

@Serializable
data object WidgetsRoute : AppRoute

/** One app's own page: its data when opened from Data usage, its screen time from Screen usage. */
@Serializable
data class AppUsageRoute(val packageName: String, val screenTime: Boolean = false) : AppRoute

/** The right pane on wide screens before a page is opened. */
@Serializable
data object EmptyDetailRoute : AppRoute

/** Every power tool shares one route; the power tools feature picks the page from [toolId]. */
@Serializable
data class PowerToolRoute(val toolId: String) : AppRoute

/** The tabs in toolbar order. */
enum class TopLevelTab(val route: AppRoute) {
    HOME(HomeRoute),
    TOOLS(ToolsRoute),
    SETTINGS(SettingsRoute)
}
