package dev.qtremors.osyster.shell

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import dev.qtremors.osyster.core.navigation.AppRoute
import dev.qtremors.osyster.core.navigation.ActivityLauncherRoute
import dev.qtremors.osyster.core.navigation.AppStopperRoute
import dev.qtremors.osyster.core.navigation.BarcodeCreateFormRoute
import dev.qtremors.osyster.core.navigation.BarcodeCreateRoute
import dev.qtremors.osyster.core.navigation.BarcodeHistoryRoute
import dev.qtremors.osyster.core.navigation.BarcodeRoute
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.BtRemoteRoute
import dev.qtremors.osyster.core.navigation.FlashlightRoute
import dev.qtremors.osyster.core.navigation.LinkActionsRoute
import dev.qtremors.osyster.core.navigation.PowerToolRoute
import dev.qtremors.osyster.core.navigation.QuickTilesRoute
import dev.qtremors.osyster.core.navigation.StayAwakeRoute
import dev.qtremors.osyster.core.navigation.DataUsageRoute
import dev.qtremors.osyster.core.navigation.ScreenUsageRoute
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.navigation.WidgetsRoute
import dev.qtremors.osyster.core.navigation.ToolsRoute

/** Tools with their own feature module get their own route; power tools share one. */
internal fun Tool.route(): AppRoute = when (this) {
    Tool.DATA_USAGE -> DataUsageRoute
    Tool.SCREEN_USAGE -> ScreenUsageRoute
    Tool.APP_STOPPER -> AppStopperRoute
    Tool.STAY_AWAKE -> StayAwakeRoute
    Tool.FLASHLIGHT -> FlashlightRoute
    Tool.BT_REMOTE -> BtRemoteRoute
    Tool.BARCODE -> BarcodeRoute()
    Tool.ACTIVITY_LAUNCHER -> ActivityLauncherRoute
    Tool.LINK_ACTIONS -> LinkActionsRoute
    Tool.QUICK_TILES -> QuickTilesRoute
    Tool.WIDGETS -> WidgetsRoute
    else -> PowerToolRoute(id)
}

/**
 * The pages a launcher shortcut opens, bottom first: a tool's own page, then any page inside it,
 * so Back from the creator lands on the scanner. An unknown tool opens nothing.
 */
internal fun shortcutRoutes(toolId: String, page: String?): List<AppRoute> {
    val tool = Tool.fromId(toolId) ?: return emptyList()
    return when {
        tool == Tool.BARCODE && page == DeepLinks.PAGE_SCAN_PICTURE -> listOf(BarcodeRoute(pickPicture = true))
        tool == Tool.BARCODE && page == DeepLinks.PAGE_CREATE_CODE -> listOf(BarcodeRoute(), BarcodeCreateRoute)
        else -> listOf(tool.route())
    }
}

/** An unknown id (from an old pin or shortcut) falls back to the Tools tab instead of a blank page. */
internal fun toolRoute(toolId: String): AppRoute = Tool.fromId(toolId)?.route() ?: ToolsRoute

/** The tool shown by [entry], if any, so the toolbar can title the page and select the Tools tab. */
internal fun toolOf(entry: NavBackStackEntry): Tool? {
    val destination = entry.destination
    return when {
        destination.hasRoute<DataUsageRoute>() -> Tool.DATA_USAGE
        destination.hasRoute<ScreenUsageRoute>() -> Tool.SCREEN_USAGE
        destination.hasRoute<AppStopperRoute>() -> Tool.APP_STOPPER
        destination.hasRoute<StayAwakeRoute>() -> Tool.STAY_AWAKE
        destination.hasRoute<FlashlightRoute>() -> Tool.FLASHLIGHT
        destination.hasRoute<BtRemoteRoute>() -> Tool.BT_REMOTE
        destination.hasRoute<BarcodeRoute>() || destination.hasRoute<BarcodeHistoryRoute>() ||
            destination.hasRoute<BarcodeCreateRoute>() || destination.hasRoute<BarcodeCreateFormRoute>() -> Tool.BARCODE
        destination.hasRoute<ActivityLauncherRoute>() -> Tool.ACTIVITY_LAUNCHER
        destination.hasRoute<LinkActionsRoute>() -> Tool.LINK_ACTIONS
        destination.hasRoute<QuickTilesRoute>() -> Tool.QUICK_TILES
        destination.hasRoute<WidgetsRoute>() -> Tool.WIDGETS
        destination.hasRoute<PowerToolRoute>() -> Tool.fromId(entry.toRoute<PowerToolRoute>().toolId)
        else -> null
    }
}

internal fun NavDestination.isToolPage(): Boolean =
    hasRoute<DataUsageRoute>() || hasRoute<ScreenUsageRoute>() || hasRoute<AppStopperRoute>() ||
        hasRoute<StayAwakeRoute>() || hasRoute<FlashlightRoute>() || hasRoute<BtRemoteRoute>() || hasRoute<BarcodeRoute>() || hasRoute<BarcodeHistoryRoute>() ||
        hasRoute<BarcodeCreateRoute>() || hasRoute<BarcodeCreateFormRoute>() || hasRoute<ActivityLauncherRoute>() ||
        hasRoute<LinkActionsRoute>() || hasRoute<QuickTilesRoute>() || hasRoute<WidgetsRoute>() || hasRoute<PowerToolRoute>()
