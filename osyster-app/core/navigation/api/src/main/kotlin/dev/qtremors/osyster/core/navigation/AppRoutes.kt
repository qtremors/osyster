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

/** One tool page, addressed by its stable tool id (for example "data_usage"). */
@Serializable
data class ToolRoute(val toolId: String) : AppRoute

/** The tabs in toolbar order. */
enum class TopLevelTab(val route: AppRoute) {
    HOME(HomeRoute),
    TOOLS(ToolsRoute),
    SETTINGS(SettingsRoute)
}
