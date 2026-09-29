package dev.qtremors.osyster.feature.powertools

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.qtremors.osyster.core.navigation.PowerToolRoute
import dev.qtremors.osyster.core.navigation.Tool

/** Where the power tools send people. The app shell maps these to routes or system screens. */
sealed interface PowerToolsDestination {
    /** The Power setup page, from any tool that is missing access. */
    data object Setup : PowerToolsDestination
    data class OpenUrl(val url: String) : PowerToolsDestination
}

/** Every power tool lives behind one route; [PowerToolRoute.toolId] picks the page. */
fun NavGraphBuilder.registerPowerToolsRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    composable<PowerToolRoute> { entry ->
        when (Tool.fromId(entry.toRoute<PowerToolRoute>().toolId)) {
            Tool.POWER_SAVING -> PowerSavingRoute(onNavigate)
            Tool.DISPLAY_SCALE -> DisplayScaleRoute(onNavigate)
            Tool.STATUS_BAR_ICONS -> StatusBarIconsRoute(onNavigate)
            Tool.SYSTEM_TWEAKS -> SystemTweaksRoute(onNavigate)
            Tool.SHUT_UP -> ShutUpRoute(onNavigate)
            Tool.STANDBY_APPS -> StandbyAppsRoute(onNavigate)
            else -> PowerSetupRoute(onNavigate)
        }
    }
}

/** Tools this feature draws; the shell sends them to [PowerToolRoute]. */
val PowerTools: Set<Tool> = setOf(
    Tool.POWER_SETUP,
    Tool.POWER_SAVING,
    Tool.DISPLAY_SCALE,
    Tool.STATUS_BAR_ICONS,
    Tool.SYSTEM_TWEAKS,
    Tool.SHUT_UP,
    Tool.STANDBY_APPS
)
