package dev.qtremors.osyster.shell

import androidx.compose.ui.platform.LocalContext
import dev.qtremors.osyster.core.navigation.AppUsageRoute
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import dev.qtremors.osyster.core.navigation.EmptyDetailRoute
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
import dev.qtremors.osyster.core.navigation.AboutRoute
import dev.qtremors.osyster.core.navigation.AppRoute
import dev.qtremors.osyster.core.navigation.LicensesRoute
import dev.qtremors.osyster.core.navigation.PowerToolRoute
import dev.qtremors.osyster.core.navigation.PrivacyRoute
import dev.qtremors.osyster.core.navigation.DevicePageRoute
import dev.qtremors.osyster.core.navigation.HomeRoute
import dev.qtremors.osyster.core.navigation.ScreenTesterRoute
import dev.qtremors.osyster.core.navigation.SettingsRoute
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.navigation.ToolsRoute
import dev.qtremors.osyster.core.navigation.TopLevelTab
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.FloatingTabBar
import dev.qtremors.osyster.core.ui.components.TabItem
import dev.qtremors.osyster.core.ui.theme.LocalReduceMotion
import androidx.compose.runtime.LaunchedEffect
import dev.qtremors.osyster.feature.activitylauncher.registerActivityLauncherRoute
import dev.qtremors.osyster.core.navigation.BarcodeCreateFormRoute
import dev.qtremors.osyster.core.navigation.BarcodeCreateRoute
import dev.qtremors.osyster.core.navigation.BarcodeHistoryRoute
import dev.qtremors.osyster.feature.barcode.BarcodeDestination
import dev.qtremors.osyster.feature.barcode.registerBarcodeRoutes
import dev.qtremors.osyster.feature.btremote.BtRemoteDestination
import dev.qtremors.osyster.feature.btremote.registerBtRemoteRoute
import dev.qtremors.osyster.feature.flashlight.FlashlightDestination
import dev.qtremors.osyster.feature.flashlight.registerFlashlightRoute
import dev.qtremors.osyster.feature.linkactions.LinkActionsDestination
import dev.qtremors.osyster.feature.notificationhistory.NotificationHistoryDestination
import dev.qtremors.osyster.feature.notificationhistory.registerNotificationHistoryRoute
import dev.qtremors.osyster.feature.linkactions.registerLinkActionsRoute
import dev.qtremors.osyster.feature.powertools.PowerToolsDestination
import dev.qtremors.osyster.feature.powertools.registerPowerToolsRoute
import dev.qtremors.osyster.feature.quicktiles.registerQuickTilesRoute
import dev.qtremors.osyster.feature.quicktiles.registerWidgetsRoute
import dev.qtremors.osyster.feature.stayawake.StayAwakeDestination
import dev.qtremors.osyster.feature.stayawake.registerStayAwakeRoute
import dev.qtremors.osyster.feature.appstopper.AppStopperDestination
import dev.qtremors.osyster.feature.appstopper.registerAppStopperRoute
import dev.qtremors.osyster.feature.datausage.DataUsageDestination
import dev.qtremors.osyster.feature.datausage.registerDataUsageRoute
import dev.qtremors.osyster.feature.screenusage.ScreenUsageDestination
import dev.qtremors.osyster.feature.screenusage.registerScreenUsageRoute
import dev.qtremors.osyster.feature.device.DeviceDestination
import dev.qtremors.osyster.feature.device.registerDevicePageRoute
import dev.qtremors.osyster.feature.home.HomeDestination
import dev.qtremors.osyster.feature.home.registerHomeRoute
import dev.qtremors.osyster.feature.settings.SettingsDestination
import dev.qtremors.osyster.feature.settings.registerSettingsRoute
import dev.qtremors.osyster.feature.tools.ToolsDestination
import dev.qtremors.osyster.feature.tools.registerToolsRoute

/** Things only the Activity can do: start other apps and system screens. */
@androidx.compose.runtime.Immutable
data class SystemActions(
    val openUrl: (String) -> Unit,
    val openBatteryUsage: () -> Unit,
    val openAppSettings: () -> Unit,
    val shareText: (String) -> Unit,
    val openUsageAccess: () -> Unit,
    val openAppInfo: (packageName: String) -> Unit,
    val openDeveloperOptions: () -> Unit,
    val openDefaultApps: () -> Unit,
    val openNotificationAccess: () -> Unit,
    val launchApp: (packageName: String) -> Unit,
    val openStoreListing: (packageName: String) -> Unit
)

/**
 * The only place that knows every feature. It maps each feature's neutral destinations to routes
 * and owns the floating toolbar that sits above all screens.
 */
@Composable
fun OsysterAppShell(
    appVersion: String,
    system: SystemActions,
    openTool: String? = null,
    onToolOpened: () -> Unit = {}
) {
    val navController = rememberNavController()
    // A widget or tile asked for a specific tool; open it once, on top of Home.
    // openTool is a tool id, or "tool/page" from a launcher shortcut that opens a page inside it.
    LaunchedEffect(openTool) {
        val request = openTool ?: return@LaunchedEffect
        shortcutRoutes(request.substringBefore('/'), request.substringAfter('/', "").ifEmpty { null }).forEach { navController.navigate(it) }
        onToolOpened()
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val reduceMotion = LocalReduceMotion.current

    val tabs = listOf(
        TabItem(Icons.Rounded.Home, stringResource(R.string.tab_home)),
        TabItem(Icons.Rounded.HomeRepairService, stringResource(R.string.tab_tools)),
        TabItem(Icons.Rounded.Settings, stringResource(R.string.tab_settings))
    )

    val wide = LocalConfiguration.current.screenWidthDp >= WIDE_SCREEN_DP
    val listController = rememberNavController()
    val listEntry by listController.currentBackStackEntryAsState()
    val selectTab: (AppRoute) -> Unit = { route -> (if (wide) listController else navController).navigateToTab(route) }
    val graph: NavGraphBuilder.() -> Unit = { osysterGraph(navController, appVersion, system, selectTab) }

    if (wide) {
        // Tablets and unfolded foldables: tabs stay on the left, pages open on the right.
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(LIST_PANE_WIDTH).fillMaxHeight()) {
                ShellNavHost(listController, HomeRoute, reduceMotion, graph)
                FloatingTabBar(
                    tabs = tabs,
                    selectedIndex = listEntry?.destination.selectedTabIndex(),
                    onSelect = { index -> listController.navigateToTab(TopLevelTab.entries[index].route) },
                    backTitle = null,
                    onBack = null,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ShellNavHost(navController, EmptyDetailRoute, reduceMotion) {
                    graph()
                    composable<EmptyDetailRoute> { EmptyDetailPane() }
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ShellNavHost(navController, HomeRoute, reduceMotion, graph)

        // Full-screen tests need every pixel; Back still works through the system gesture.
        if (destination?.hasRoute<ScreenTesterRoute>() != true) {
            FloatingTabBar(
                tabs = tabs,
                selectedIndex = destination.selectedTabIndex(),
                onSelect = { index -> navController.navigateToTab(TopLevelTab.entries[index].route) },
                backTitle = backStackEntry.detailTitle(),
                onBack = if (destination.isTopLevelPage()) null else ({ navController.popBackStack() }),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** One NavHost with the shell's spring transitions, including the predictive back ones. */
@Composable
private fun ShellNavHost(controller: NavHostController, start: AppRoute, reduceMotion: Boolean, builder: NavGraphBuilder.() -> Unit) {
    NavHost(
        navController = controller,
        startDestination = start,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { shellEnter(reduceMotion) },
        exitTransition = { shellExit(reduceMotion) },
        popEnterTransition = { shellPopEnter(reduceMotion) },
        popExitTransition = { shellPopExit(reduceMotion) },
        // Navigation 2.10 plays these, not the pop transitions, while the back gesture is in
        // progress. Left at their defaults, the page shrinks into a sharp-cornered card and then
        // switches animation on release. Using the same motion keeps the gesture continuous.
        predictivePopEnterTransition = { shellPopEnter(reduceMotion) },
        predictivePopExitTransition = { shellPopExit(reduceMotion) },
        builder = builder
    )
}

/**
 * Every route in the app. [navController] always receives page navigation, so on wide screens a
 * row tapped in the left pane opens its page on the right; [selectTab] switches tabs in whichever
 * pane shows them.
 */
private fun NavGraphBuilder.osysterGraph(
    navController: NavHostController,
    appVersion: String,
    system: SystemActions,
    selectTab: (AppRoute) -> Unit
) {
    registerHomeRoute { target ->
        when (target) {
            is HomeDestination.DevicePage -> navController.navigate(DevicePageRoute(target.pageId))
            is HomeDestination.Tool -> navController.navigate(toolRoute(target.toolId))
            is HomeDestination.ShareReport -> system.shareText(target.text)
            HomeDestination.Settings -> selectTab(SettingsRoute)
            HomeDestination.About -> navController.navigate(AboutRoute)
            HomeDestination.Privacy -> navController.navigate(PrivacyRoute)
            HomeDestination.Licenses -> navController.navigate(LicensesRoute)
            HomeDestination.UsageAccess -> system.openUsageAccess()
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
            is ToolsDestination.OpenTool -> navController.navigate(toolRoute(target.toolId))
        }
    }
    registerSettingsRoute(appVersion = appVersion) { target ->
        when (target) {
            is SettingsDestination.OpenUrl -> system.openUrl(target.url)
            SettingsDestination.About -> navController.navigate(AboutRoute)
            SettingsDestination.Privacy -> navController.navigate(PrivacyRoute)
            SettingsDestination.Licenses -> navController.navigate(LicensesRoute)
        }
    }
    registerDataUsageRoute { target ->
        when (target) {
            DataUsageDestination.UsageAccessSettings -> system.openUsageAccess()
            is DataUsageDestination.AppInfo -> system.openAppInfo(target.packageName)
            is DataUsageDestination.AppUsage -> navController.navigate(AppUsageRoute(target.packageName))
        }
    }
    registerScreenUsageRoute { target ->
        when (target) {
            ScreenUsageDestination.UsageAccessSettings -> system.openUsageAccess()
            is ScreenUsageDestination.AppInfo -> system.openAppInfo(target.packageName)
            is ScreenUsageDestination.AppUsage -> navController.navigate(AppUsageRoute(target.packageName, screenTime = true))
        }
    }
    registerAppStopperRoute { target ->
        when (target) {
            is AppStopperDestination.AppInfo -> system.openAppInfo(target.packageName)
            is AppStopperDestination.Launch -> system.launchApp(target.packageName)
            is AppStopperDestination.Store -> system.openStoreListing(target.packageName)
        }
    }
    registerStayAwakeRoute { target ->
        when (target) {
            StayAwakeDestination.DeveloperOptions -> system.openDeveloperOptions()
        }
    }
    registerFlashlightRoute { target ->
        when (target) {
            FlashlightDestination.NotificationAccess -> system.openNotificationAccess()
        }
    }
    registerBtRemoteRoute { target ->
        when (target) {
            is BtRemoteDestination.AppSettings -> system.openAppInfo(target.packageName)
        }
    }
    registerBarcodeRoutes { target ->
        when (target) {
            BarcodeDestination.History -> navController.navigate(BarcodeHistoryRoute)
            BarcodeDestination.Create -> navController.navigate(BarcodeCreateRoute)
            is BarcodeDestination.CreateForm -> navController.navigate(BarcodeCreateFormRoute(target.kind))
            is BarcodeDestination.AppSettings -> system.openAppInfo(target.packageName)
        }
    }
    registerNotificationHistoryRoute { target ->
        when (target) {
            NotificationHistoryDestination.NotificationAccess -> system.openNotificationAccess()
            is NotificationHistoryDestination.OpenApp -> system.launchApp(target.packageName)
        }
    }
    registerActivityLauncherRoute()
    registerLinkActionsRoute { target ->
        when (target) {
            LinkActionsDestination.DefaultApps -> system.openDefaultApps()
            is LinkActionsDestination.Share -> system.shareText(target.text)
        }
    }
    registerQuickTilesRoute()
    registerWidgetsRoute()
    registerPowerToolsRoute { target ->
        when (target) {
            PowerToolsDestination.Setup -> navController.navigate(PowerToolRoute(Tool.POWER_SETUP.id))
            is PowerToolsDestination.OpenUrl -> system.openUrl(target.url)
        }
    }
}

/** The right pane before anything is opened. */
@Composable
private fun EmptyDetailPane() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.empty_detail),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val WIDE_SCREEN_DP = 840
private val LIST_PANE_WIDTH = 400.dp

private fun NavDestination?.selectedTabIndex(): Int = when {
    this == null -> 0
    hasRoute<ToolsRoute>() || isToolPage() -> 1
    hasRoute<SettingsRoute>() || isSettingsPage() -> 2
    else -> 0
}

@Composable
private fun NavBackStackEntry?.detailTitle(): String? {
    val entry = this ?: return null
    val destination = entry.destination
    // Pages titled by something other than a string resource.
    if (destination.hasRoute<AppUsageRoute>()) {
        val packageName = entry.toRoute<AppUsageRoute>().packageName
        val context = LocalContext.current
        return remember(packageName) {
            runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString() }
                .getOrDefault(packageName)
        }
    }
    val titleRes = when {
        destination.hasRoute<DevicePageRoute>() -> DevicePage.fromId(entry.toRoute<DevicePageRoute>().pageId)?.label?.title
        destination.isToolPage() -> toolOf(entry)?.label?.title
        destination.hasRoute<AboutRoute>() -> R.string.title_about
        destination.hasRoute<PrivacyRoute>() -> R.string.title_privacy
        destination.hasRoute<LicensesRoute>() -> R.string.title_licenses
        else -> null
    }
    return titleRes?.let { stringResource(it) }
}

private fun NavDestination.isSettingsPage(): Boolean =
    hasRoute<AboutRoute>() || hasRoute<PrivacyRoute>() || hasRoute<LicensesRoute>()

/** Tabs keep their own back stacks and scroll positions, like the system's own apps. */
private fun NavHostController.navigateToTab(route: AppRoute) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
