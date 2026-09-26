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

/** Search across pages, tools, settings, and device facts. */
@Serializable
data object SearchRoute : AppRoute

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

@Serializable
data object ActivityLauncherRoute : AppRoute

@Serializable
data object LinkActionsRoute : AppRoute

@Serializable
data object QuickTilesRoute : AppRoute

/** A tool page that has no feature module yet, addressed by its stable tool id. */
/** Every power tool shares one route; the power tools feature picks the page from [toolId]. */
@Serializable
data class PowerToolRoute(val toolId: String) : AppRoute

@Serializable
data class ToolRoute(val toolId: String) : AppRoute

/** The tabs in toolbar order. */
enum class TopLevelTab(val route: AppRoute) {
    HOME(HomeRoute),
    TOOLS(ToolsRoute),
    SETTINGS(SettingsRoute)
}
