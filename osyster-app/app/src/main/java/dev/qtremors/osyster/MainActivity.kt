@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.qtremors.osyster.navigation.AppRoutes
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.ui.apps.AppsDashboard
import dev.qtremors.osyster.ui.hardware.*
import dev.qtremors.osyster.ui.monitor.CpuDashboard
import dev.qtremors.osyster.ui.monitor.MemoryDashboard
import dev.qtremors.osyster.ui.monitor.MonitorDashboard
import dev.qtremors.osyster.ui.monitor.NetworkDashboard
import dev.qtremors.osyster.ui.overview.OverviewDashboard
import dev.qtremors.osyster.ui.navigation.OsysterDock
import dev.qtremors.osyster.ui.navigation.OsysterDockItem
import dev.qtremors.osyster.ui.onboarding.OnboardingScreen
import dev.qtremors.osyster.ui.settings.AboutScreen
import dev.qtremors.osyster.ui.settings.LicensesScreen
import dev.qtremors.osyster.ui.settings.SettingsScreen
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import dev.qtremors.osyster.ui.util.LocalTelemetryVisible
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoCategory
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SettingsSubpage {
    SETTINGS, ABOUT, LICENSES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val view = LocalView.current
            val preferencesManager = remember { OsysterPreferencesManager.getInstance(context) }
            val prefsState by preferencesManager.state.collectAsStateWithLifecycle()

            LaunchedEffect(prefsState.blockScreenCapture) {
                if (prefsState.blockScreenCapture) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            OsysterTheme(
                themeMode = prefsState.themeMode,
                accentPalette = prefsState.accentPalette,
                dynamicColor = prefsState.dynamicColor
            ) {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination
                val currentRoute = currentDestination?.route
                val isOnboarding = currentDestination?.hasRoute(AppRoutes.Onboarding::class) == true || currentRoute?.contains("Onboarding") == true

                val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
                val scope = rememberCoroutineScope()
                var appsInitialTab by remember { mutableIntStateOf(0) }
                val deviceInfoViewModel: DeviceInfoViewModel = viewModel()

                var showSettings by rememberSaveable { mutableStateOf(false) }
                var settingsSubpage by rememberSaveable { mutableStateOf(SettingsSubpage.SETTINGS) }

                LaunchedEffect(showSettings) {
                    if (!showSettings) {
                        settingsSubpage = SettingsSubpage.SETTINGS
                    }
                }

                val pullRefreshState = rememberPullToRefreshState()
                var isRefreshing by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(isRefreshing) {
                    if (isRefreshing) {
                        OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                        showSettings = true
                        isRefreshing = false
                    }
                }

                var lastHapticBucket by remember { mutableIntStateOf(0) }
                LaunchedEffect(pullRefreshState.distanceFraction) {
                    if (!prefsState.hapticFeedback) return@LaunchedEffect
                    val fraction = pullRefreshState.distanceFraction
                    val currentBucket = (fraction * 10).toInt()
                    if (fraction >= 1f && lastHapticBucket < 10) {
                        OsysterHapticUtil.performVirtualKey(view, true)
                    } else if (fraction > 0f && fraction < 1f) {
                        if (currentBucket > lastHapticBucket) {
                            OsysterHapticUtil.performSegmentTick(view, true)
                        }
                        lastHapticBucket = currentBucket
                    }
                }

                BackHandler(enabled = showSettings) {
                    if (settingsSubpage == SettingsSubpage.LICENSES) {
                        settingsSubpage = SettingsSubpage.ABOUT
                    } else if (settingsSubpage == SettingsSubpage.ABOUT) {
                        settingsSubpage = SettingsSubpage.SETTINGS
                    } else {
                        showSettings = false
                    }
                }

                val backProgress = remember { Animatable(0f) }

                val isBento = currentDestination?.hasRoute(AppRoutes.Bento::class) == true || currentRoute?.contains("Bento") == true

                PredictiveBackHandler(enabled = !showSettings && !isOnboarding && isBento && pagerState.currentPage != 0) { progress ->
                    try {
                        progress.collect { backEvent ->
                            backProgress.snapTo(backEvent.progress)
                        }
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                        scope.launch {
                            backProgress.animateTo(0f, animationSpec = tween(350))
                        }
                    } catch (_: java.util.concurrent.CancellationException) {
                        scope.launch {
                            backProgress.animateTo(0f, animationSpec = tween(250))
                        }
                    }
                }

                var lastSwipeHapticBucket by remember { mutableIntStateOf(0) }
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPageOffsetFraction }
                        .collect { offset ->
                            if (!prefsState.hapticFeedback) return@collect
                            val fraction = abs(offset)
                            val currentBucket = (fraction * 10).toInt()
                            if (currentBucket != lastSwipeHapticBucket) {
                                if (fraction > 0f) {
                                    OsysterHapticUtil.performSegmentTick(view, true)
                                }
                                lastSwipeHapticBucket = currentBucket
                            }
                        }
                }

                LaunchedEffect(pagerState) {
                    var isFirst = true
                    snapshotFlow { pagerState.currentPage }
                        .collect {
                            if (isFirst) {
                                isFirst = false
                            } else {
                                OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                            }
                        }
                }

                val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val isDockVisible = !isOnboarding && !showSettings && isBento
                val dynamicBottomPadding = if (isDockVisible) navBarsBottom + 108.dp else navBarsBottom + 16.dp

                CompositionLocalProvider(LocalBottomContentPadding provides dynamicBottomPadding) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = if (prefsState.isOnboardingCompleted) AppRoutes.Bento else AppRoutes.Onboarding,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable<AppRoutes.Onboarding> {
                                OnboardingScreen(
                                    prefsState = prefsState,
                                    preferencesManager = preferencesManager,
                                    onFinish = {
                                        navController.navigate(AppRoutes.Bento) {
                                            popUpTo(AppRoutes.Onboarding) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable<AppRoutes.Bento> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    PullToRefreshBox(
                                        isRefreshing = isRefreshing,
                                        onRefresh = { isRefreshing = true },
                                        state = pullRefreshState,
                                        indicator = { },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    val p = backProgress.value
                                                    scaleX = 1f - (p * 0.04f)
                                                    scaleY = 1f - (p * 0.04f)
                                                    translationX = p * 40.dp.toPx()
                                                }
                                        ) { page ->
                                            CompositionLocalProvider(LocalTelemetryVisible provides (page == pagerState.currentPage)) {
                                                when (page) {
                                                    0 -> OverviewDashboard(
                                                        onNavigateTo = { route ->
                                                            when (route) {
                                                                is AppRoutes.Network -> {
                                                                    scope.launch { pagerState.animateScrollToPage(1) }
                                                                }
                                                                is AppRoutes.Processes -> {
                                                                    appsInitialTab = 1
                                                                    scope.launch { pagerState.animateScrollToPage(2) }
                                                                }
                                                                is AppRoutes.AppStopper -> {
                                                                    appsInitialTab = 0
                                                                    scope.launch { pagerState.animateScrollToPage(2) }
                                                                }
                                                                else -> {
                                                                    navController.navigate(route)
                                                                }
                                                            }
                                                        },
                                                        onOpenSettings = {
                                                            showSettings = true
                                                        }
                                                    )
                                                    1 -> NetworkDashboard(
                                                        modifier = Modifier.fillMaxSize(),
                                                        onNavigateBack = null,
                                                        hapticEnabled = prefsState.hapticFeedback
                                                    )
                                                    2 -> AppsDashboard(
                                                        prefsState = prefsState,
                                                        manager = preferencesManager,
                                                        initialTab = appsInitialTab,
                                                        hapticEnabled = prefsState.hapticFeedback
                                                    )
                                                }
                                            }
                                        }

                                        // Minimal pull-down settings pill indicator (only reveals when pulling down)
                                        val displayFraction = pullRefreshState.distanceFraction.coerceIn(0f, 1.5f)
                                        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                                        if (displayFraction > 0.05f) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (displayFraction >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                                contentColor = if (displayFraction >= 1f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .offset(y = statusBarTop + (36.dp * displayFraction.coerceIn(0f, 1f)))
                                                    .graphicsLayer {
                                                        alpha = (displayFraction * 2.5f).coerceIn(0f, 1f)
                                                        scaleX = 0.85f + (0.15f * displayFraction.coerceIn(0f, 1f))
                                                        scaleY = 0.85f + (0.15f * displayFraction.coerceIn(0f, 1f))
                                                    }
                                                    .zIndex(5f)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (displayFraction >= 1f) {
                                                            stringResource(R.string.release_settings_hint)
                                                        } else {
                                                            stringResource(R.string.pull_down_settings_hint)
                                                        },
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (displayFraction >= 1f) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            composable<AppRoutes.Cpu> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    CpuDashboard(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.telemetry_tab_cpu),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.Memory> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    MemoryDashboard(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.telemetry_tab_ram),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.Gpu> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    GpuDashboard(
                                        onNavigateBack = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback,
                                        viewModel = deviceInfoViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.bento_gpu_title),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.Storage> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    StorageDashboard(
                                        onNavigateBack = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback,
                                        viewModel = deviceInfoViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.storage_section_title),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.Battery> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    BatteryDiagnosticsDashboard(
                                        onNavigateBack = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback,
                                        viewModel = deviceInfoViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.telemetry_tab_battery),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.Display> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    DeviceDisplayDashboard(
                                        onNavigateBack = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback,
                                        viewModel = deviceInfoViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.category_device),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.System> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    SystemDashboard(
                                        onNavigateBack = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback,
                                        viewModel = deviceInfoViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.category_system),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.Camera> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    CameraDashboard(
                                        onNavigateBack = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback,
                                        viewModel = deviceInfoViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.camera_section_title),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.Sensors> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    SensorsDashboard(
                                        onNavigateBack = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback,
                                        viewModel = deviceInfoViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.category_sensors),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                            composable<AppRoutes.Drm> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                ) {
                                    DrmDashboard(
                                        onNavigateBack = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback,
                                        viewModel = deviceInfoViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    OsysterDock(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        title = stringResource(R.string.drm_section_title),
                                        onBackClick = { navController.popBackStack() },
                                        hapticEnabled = prefsState.hapticFeedback
                                    )
                                }
                            }
                        }

                        // Settings and About Overlay with Acqua-style Slide-Down Transition from Top
                        AnimatedVisibility(
                            visible = showSettings,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = tween(300)
                            ) + fadeOut(),
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(10f)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                AnimatedContent(
                                    targetState = settingsSubpage,
                                    transitionSpec = {
                                        val order = listOf(
                                            SettingsSubpage.SETTINGS,
                                            SettingsSubpage.ABOUT,
                                            SettingsSubpage.LICENSES
                                        )
                                        val targetIndex = order.indexOf(targetState)
                                        val initialIndex = order.indexOf(initialState)
                                        if (targetIndex > initialIndex) {
                                            (slideInHorizontally { it } + fadeIn()).togetherWith(
                                                slideOutHorizontally { -it } + fadeOut()
                                            )
                                        } else {
                                            (slideInHorizontally { -it } + fadeIn()).togetherWith(
                                                slideOutHorizontally { it } + fadeOut()
                                            )
                                        }
                                    },
                                    label = "settings_subpage_transition"
                                ) { subpage ->
                                    when (subpage) {
                                        SettingsSubpage.SETTINGS -> SettingsScreen(
                                            state = prefsState,
                                            manager = preferencesManager,
                                            onNavigateBack = { showSettings = false },
                                            onNavigateToAbout = { settingsSubpage = SettingsSubpage.ABOUT }
                                        )
                                        SettingsSubpage.ABOUT -> AboutScreen(
                                            onNavigateBack = { settingsSubpage = SettingsSubpage.SETTINGS },
                                            onNavigateToLicenses = { settingsSubpage = SettingsSubpage.LICENSES }
                                        )
                                        SettingsSubpage.LICENSES -> LicensesScreen(
                                            onNavigateBack = { settingsSubpage = SettingsSubpage.ABOUT }
                                        )
                                    }
                                }
                            }
                        }

                        // Unified Bottom Dock (4 Main Destinations, No FAB)
                        AnimatedVisibility(
                            visible = isDockVisible,
                            enter = fadeIn(tween(250)) + slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it },
                            exit = fadeOut(tween(200)) + slideOutVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            val dashboardLabel = stringResource(R.string.dock_dashboard)
                            val networkLabel = stringResource(R.string.dock_network)
                            val appsLabel = stringResource(R.string.dock_tasks)
                            val dockItems = remember(dashboardLabel, networkLabel, appsLabel) {
                                listOf(
                                    OsysterDockItem(
                                        icon = Icons.Default.Dashboard,
                                        label = dashboardLabel,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                                    ),
                                    OsysterDockItem(
                                        icon = Icons.Default.Wifi,
                                        label = networkLabel,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                                    ),
                                    OsysterDockItem(
                                        icon = Icons.Default.Apps,
                                        label = appsLabel,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } }
                                    )
                                )
                            }

                            OsysterDock(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                items = dockItems,
                                selectedIndex = pagerState.currentPage,
                                hapticEnabled = prefsState.hapticFeedback
                            )
                        }
                    }
                }
            }
        }
    }
}
