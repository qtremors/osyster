package dev.qtremors.osyster.shell

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import dev.qtremors.osyster.core.navigation.AppRoute
import dev.qtremors.osyster.core.navigation.AppStopperRoute
import dev.qtremors.osyster.core.navigation.DataUsageRoute
import dev.qtremors.osyster.core.navigation.ScreenUsageRoute
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.navigation.ToolRoute

/** Tools with their own feature module get their own route; the rest still show a placeholder. */
internal fun Tool.route(): AppRoute = when (this) {
    Tool.DATA_USAGE -> DataUsageRoute
    Tool.SCREEN_USAGE -> ScreenUsageRoute
    Tool.APP_STOPPER -> AppStopperRoute
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
        destination.hasRoute<ToolRoute>() -> Tool.fromId(entry.toRoute<ToolRoute>().toolId)
        else -> null
    }
}

internal fun NavDestination.isToolPage(): Boolean =
    hasRoute<DataUsageRoute>() || hasRoute<ScreenUsageRoute>() || hasRoute<AppStopperRoute>() || hasRoute<ToolRoute>()
