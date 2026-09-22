package dev.qtremors.osyster.shell

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeRepairService
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.qtremors.osyster.R
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.navigation.AppRoute
import dev.qtremors.osyster.core.navigation.DevicePageRoute
import dev.qtremors.osyster.core.navigation.HomeRoute
import dev.qtremors.osyster.core.navigation.ScreenTesterRoute
import dev.qtremors.osyster.core.navigation.SettingsRoute
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.navigation.ToolRoute
import dev.qtremors.osyster.core.navigation.ToolsRoute
import dev.qtremors.osyster.core.navigation.TopLevelTab
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.FloatingTabBar
import dev.qtremors.osyster.core.ui.components.TabItem
import dev.qtremors.osyster.core.ui.theme.LocalReduceMotion
import dev.qtremors.osyster.feature.device.DeviceDestination
import dev.qtremors.osyster.feature.device.registerDevicePageRoute
import dev.qtremors.osyster.feature.home.HomeDestination
import dev.qtremors.osyster.feature.home.registerHomeRoute
import dev.qtremors.osyster.feature.settings.SettingsDestination
import dev.qtremors.osyster.feature.settings.registerSettingsRoute
import dev.qtremors.osyster.feature.tools.ToolsDestination
import dev.qtremors.osyster.feature.tools.registerToolsRoute

/**
 * The only place that knows every feature. It maps each feature's neutral destinations to routes
 * and owns the floating toolbar that sits above all screens.
 */
/** Things only the Activity can do: start other apps and system screens. */
@androidx.compose.runtime.Immutable
data class SystemActions(
    val openUrl: (String) -> Unit,
    val openBatteryUsage: () -> Unit,
    val openAppSettings: () -> Unit,
    val shareText: (String) -> Unit
)

@Composable
fun OsysterAppShell(
    appVersion: String,
    system: SystemActions
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val reduceMotion = LocalReduceMotion.current

    val tabs = listOf(
        TabItem(Icons.Rounded.Home, stringResource(R.string.tab_home)),
        TabItem(Icons.Rounded.HomeRepairService, stringResource(R.string.tab_tools)),
        TabItem(Icons.Rounded.Settings, stringResource(R.string.tab_settings))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { enter(reduceMotion) },
            exitTransition = { exit(reduceMotion) },
            popEnterTransition = { popEnter(reduceMotion) },
            popExitTransition = { popExit(reduceMotion) }
        ) {
            registerHomeRoute { target ->
                when (target) {
                    is HomeDestination.DevicePage -> navController.navigate(DevicePageRoute(target.pageId))
                    is HomeDestination.Tool -> navController.navigate(ToolRoute(target.toolId))
                    is HomeDestination.ShareReport -> system.shareText(target.text)
                }
            }
            registerDevicePageRoute { target ->
                when (target) {
                    DeviceDestination.SystemBatteryUsage -> system.openBatteryUsage()
                    DeviceDestination.ScreenTester -> navController.navigate(ScreenTesterRoute)
                    DeviceDestination.AppPermissionSettings -> system.openAppSettings()
                }
            }
            registerToolsRoute { target ->
                when (target) {
                    is ToolsDestination.OpenTool -> navController.navigate(ToolRoute(target.toolId))
                }
            }
            registerSettingsRoute(appVersion = appVersion) { target ->
                when (target) {
                    is SettingsDestination.OpenUrl -> system.openUrl(target.url)
                }
            }
            composable<ToolRoute> { entry ->
                val tool = Tool.fromId(entry.toRoute<ToolRoute>().toolId)
                PlaceholderPage(title = tool?.label?.title?.let { stringResource(it) }.orEmpty())
            }
        }

        // Full-screen tests need every pixel; Back still works through the system gesture.
        if (destination?.hasRoute<ScreenTesterRoute>() != true) {
            FloatingTabBar(
                tabs = tabs,
                selectedIndex = destination.selectedTabIndex(),
                onSelect = { index -> navController.navigateToTab(TopLevelTab.entries[index].route) },
                backTitle = backStackEntry.detailTitle(),
                onBack = if (destination.isTopLevel()) null else ({ navController.popBackStack() }),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun NavDestination?.isTopLevel(): Boolean =
    this == null || hasRoute<HomeRoute>() || hasRoute<ToolsRoute>() || hasRoute<SettingsRoute>()

private fun NavDestination?.selectedTabIndex(): Int = when {
    this == null -> 0
    hasRoute<ToolsRoute>() || hasRoute<ToolRoute>() -> 1
    hasRoute<SettingsRoute>() -> 2
    else -> 0
}

@Composable
private fun NavBackStackEntry?.detailTitle(): String? {
    val entry = this ?: return null
    val destination = entry.destination
    val titleRes = when {
        destination.hasRoute<DevicePageRoute>() -> DevicePage.fromId(entry.toRoute<DevicePageRoute>().pageId)?.label?.title
        destination.hasRoute<ToolRoute>() -> Tool.fromId(entry.toRoute<ToolRoute>().toolId)?.label?.title
        else -> null
    }
    return titleRes?.let { stringResource(it) }
}

/** Tabs keep their own back stacks and scroll positions, like the system's own apps. */
private fun NavHostController.navigateToTab(route: AppRoute) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private const val MOTION_MS = 320

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion || targetState.destination.isTopLevel()) fadeIn(tween(if (reduceMotion) 0 else 200))
    else slideInHorizontally(tween(MOTION_MS)) { it / 4 } + fadeIn(tween(MOTION_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion || targetState.destination.isTopLevel()) fadeOut(tween(if (reduceMotion) 0 else 150))
    else slideOutHorizontally(tween(MOTION_MS)) { -it / 8 } + fadeOut(tween(MOTION_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) fadeIn(tween(0))
    else slideInHorizontally(tween(MOTION_MS)) { -it / 8 } + fadeIn(tween(MOTION_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) fadeOut(tween(0))
    else slideOutHorizontally(tween(MOTION_MS)) { it / 4 } + fadeOut(tween(MOTION_MS))
