package dev.qtremors.osyster.shell

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import dev.qtremors.osyster.core.navigation.AppRoute
import dev.qtremors.osyster.core.navigation.ActivityLauncherRoute
import dev.qtremors.osyster.core.navigation.AppStopperRoute
import dev.qtremors.osyster.core.navigation.FlashlightRoute
import dev.qtremors.osyster.core.navigation.LinkActionsRoute
import dev.qtremors.osyster.core.navigation.QuickTilesRoute
import dev.qtremors.osyster.core.navigation.StayAwakeRoute
import dev.qtremors.osyster.core.navigation.DataUsageRoute
import dev.qtremors.osyster.core.navigation.ScreenUsageRoute
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.navigation.ToolRoute

/** Tools with their own feature module get their own route; the rest still show a placeholder. */
internal fun Tool.route(): AppRoute = when (this) {
    Tool.DATA_USAGE -> DataUsageRoute
    Tool.SCREEN_USAGE -> ScreenUsageRoute
    Tool.APP_STOPPER -> AppStopperRoute
    Tool.STAY_AWAKE -> StayAwakeRoute
    Tool.FLASHLIGHT -> FlashlightRoute
    Tool.ACTIVITY_LAUNCHER -> ActivityLauncherRoute
    Tool.LINK_ACTIONS -> LinkActionsRoute
    Tool.QUICK_TILES -> QuickTilesRoute
    else -> ToolRoute(id)
}

internal fun toolRoute(toolId: String): AppRoute = Tool.fromId(toolId)?.route() ?: ToolRoute(toolId)

/** The tool shown by [entry], if any, so the toolbar can title the page and select the Tools tab. */
internal fun toolOf(entry: NavBackStackEntry): Tool? {
    val destination = entry.destination
    return when {
        destination.hasRoute<DataUsageRoute>() -> Tool.DATA_USAGE
        destination.hasRoute<ScreenUsageRoute>() -> Tool.SCREEN_USAGE
        destination.hasRoute<AppStopperRoute>() -> Tool.APP_STOPPER
        destination.hasRoute<StayAwakeRoute>() -> Tool.STAY_AWAKE
        destination.hasRoute<FlashlightRoute>() -> Tool.FLASHLIGHT
        destination.hasRoute<ActivityLauncherRoute>() -> Tool.ACTIVITY_LAUNCHER
        destination.hasRoute<LinkActionsRoute>() -> Tool.LINK_ACTIONS
        destination.hasRoute<QuickTilesRoute>() -> Tool.QUICK_TILES
        destination.hasRoute<ToolRoute>() -> Tool.fromId(entry.toRoute<ToolRoute>().toolId)
        else -> null
    }
}

internal fun NavDestination.isToolPage(): Boolean =
    hasRoute<DataUsageRoute>() || hasRoute<ScreenUsageRoute>() || hasRoute<AppStopperRoute>() ||
        hasRoute<StayAwakeRoute>() || hasRoute<FlashlightRoute>() || hasRoute<ActivityLauncherRoute>() ||
        hasRoute<LinkActionsRoute>() || hasRoute<QuickTilesRoute>() || hasRoute<ToolRoute>()
