package dev.qtremors.osyster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.qtremors.osyster.navigation.AppRoutes
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.ui.AppStopperScreen
import dev.qtremors.osyster.ui.BentoDashboard
import dev.qtremors.osyster.ui.DeviceInfoDashboard
import dev.qtremors.osyster.ui.NetworkDashboard
import dev.qtremors.osyster.ui.ProcessDashboard
import dev.qtremors.osyster.ui.TelemetryDashboard
import dev.qtremors.osyster.ui.navigation.OsysterDock
import dev.qtremors.osyster.ui.navigation.OsysterDockItem
import dev.qtremors.osyster.ui.onboarding.OnboardingScreen
import dev.qtremors.osyster.ui.settings.AboutScreen
import dev.qtremors.osyster.ui.settings.LicensesScreen
import dev.qtremors.osyster.ui.settings.SettingsScreen
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val view = LocalView.current
            val preferencesManager = remember { OsysterPreferencesManager.getInstance(context) }
            val prefsState by preferencesManager.state.collectAsStateWithLifecycle()

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
                val isDeviceInfo = currentDestination?.hasRoute(AppRoutes.DeviceInfo::class) == true || currentRoute?.contains("DeviceInfo") == true
                val isSettings = currentDestination?.hasRoute(AppRoutes.Settings::class) == true || currentRoute?.contains("Settings") == true
                val isAbout = currentDestination?.hasRoute(AppRoutes.About::class) == true || currentRoute?.contains("About") == true
                val isLicenses = currentDestination?.hasRoute(AppRoutes.Licenses::class) == true || currentRoute?.contains("Licenses") == true
                val isNetwork = currentDestination?.hasRoute(AppRoutes.Network::class) == true || currentRoute?.contains("Network") == true
                val isAppStopper = currentDestination?.hasRoute(AppRoutes.AppStopper::class) == true || currentRoute?.contains("AppStopper") == true
                val isSubpage = isDeviceInfo || isSettings || isAbout || isLicenses || isNetwork

                val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
                val scope = rememberCoroutineScope()
                var telemetryInitialTab by remember { mutableIntStateOf(0) }
                var showAppStopperAddSheet by remember { mutableStateOf(false) }
                var appStopperSearchQuery by remember { mutableStateOf("") }
                var isAppStopperSearchActive by remember { mutableStateOf(false) }

                LaunchedEffect(isAppStopper) {
                    if (!isAppStopper) {
                        showAppStopperAddSheet = false
                        isAppStopperSearchActive = false
                        appStopperSearchQuery = ""
                    }
                }

                BackHandler(enabled = isAppStopper && isAppStopperSearchActive) {
                    isAppStopperSearchActive = false
                    appStopperSearchQuery = ""
                }

                val backProgress = remember { Animatable(0f) }

                PredictiveBackHandler(enabled = !isSubpage && !isAppStopper && !isOnboarding && pagerState.currentPage != 0) { progress ->
                    try {
                        progress.collect { backEvent ->
                            backProgress.snapTo(backEvent.progress)
                        }
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                        scope.launch {
                            backProgress.animateTo(0f, animationSpec = tween(400))
                        }
                    } catch (_: java.util.concurrent.CancellationException) {
                        scope.launch {
                            backProgress.animateTo(0f, animationSpec = tween(300))
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
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(1f - (backProgress.value * 0.05f))
                                        .alpha(1f - (backProgress.value * 0.3f))
                                ) { page ->
                                    when (page) {
                                        0 -> BentoDashboard(
                                            onNavigateTo = { route ->
                                                when (route) {
                                                    is AppRoutes.Cpu -> {
                                                        telemetryInitialTab = 0
                                                        scope.launch { pagerState.animateScrollToPage(1) }
                                                    }
                                                    is AppRoutes.Memory -> {
                                                        telemetryInitialTab = 1
                                                        scope.launch { pagerState.animateScrollToPage(1) }
                                                    }
                                                    is AppRoutes.Processes -> {
                                                        scope.launch { pagerState.animateScrollToPage(2) }
                                                    }
                                                    is AppRoutes.DeviceInfo -> {
                                                        navController.navigate(AppRoutes.DeviceInfo)
                                                    }
                                                    else -> {
                                                        navController.navigate(route)
                                                    }
                                                }
                                            }
                                        )
                                        1 -> TelemetryDashboard(
                                            initialTab = telemetryInitialTab,
                                            hapticEnabled = prefsState.hapticFeedback
                                        )
                                        2 -> ProcessDashboard()
                                    }
                                }
                            }
                        }
                        composable<AppRoutes.DeviceInfo> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            ) {
                                DeviceInfoDashboard(
                                    onNavigateBack = { navController.navigateUp() },
                                    hapticEnabled = prefsState.hapticFeedback
                                )
                            }
                        }
                        composable<AppRoutes.Settings> {
                            SettingsScreen(
                                state = prefsState,
                                manager = preferencesManager,
                                onNavigateBack = { navController.navigateUp() },
                                onNavigateToAbout = { navController.navigate(AppRoutes.About) }
                            )
                        }
                        composable<AppRoutes.About> {
                            AboutScreen(
                                onNavigateBack = { navController.navigateUp() },
                                onNavigateToLicenses = { navController.navigate(AppRoutes.Licenses) }
                            )
                        }
                        composable<AppRoutes.Licenses> {
                            LicensesScreen(
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable<AppRoutes.Network> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            ) {
                                NetworkDashboard(
                                    onNavigateBack = { navController.navigateUp() },
                                    hapticEnabled = prefsState.hapticFeedback
                                )
                            }
                        }
                        composable<AppRoutes.AppStopper> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            ) {
                                AppStopperScreen(
                                    prefsState = prefsState,
                                    manager = preferencesManager,
                                    onNavigateBack = { navController.navigateUp() },
                                    showAddSheet = showAppStopperAddSheet,
                                    onShowAddSheetChange = { showAppStopperAddSheet = it },
                                    searchQuery = appStopperSearchQuery
                                )
                            }
                        }
                    }

                    when {
                        isOnboarding || isSubpage -> Unit
                        isAppStopper -> {
                            if (isAppStopperSearchActive) {
                                OsysterDock(
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    hapticEnabled = prefsState.hapticFeedback,
                                    floatingActionButton = {
                                        FloatingActionButton(
                                            onClick = {
                                                OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                                showAppStopperAddSheet = true
                                            },
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            shape = MaterialTheme.shapes.large,
                                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Applications"
                                            )
                                        }
                                    },
                                    customContent = {
                                        IconButton(
                                            onClick = {
                                                OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                                isAppStopperSearchActive = false
                                                appStopperSearchQuery = ""
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .align(Alignment.CenterVertically),
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Close search",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        val focusRequester = remember { FocusRequester() }
                                        LaunchedEffect(Unit) {
                                            focusRequester.requestFocus()
                                        }

                                        val screenWidth = LocalConfiguration.current.screenWidthDp
                                        val maxSearchWidth = (screenWidth - 190).coerceAtLeast(100).dp

                                        BasicTextField(
                                            value = appStopperSearchQuery,
                                            onValueChange = { appStopperSearchQuery = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .widthIn(min = 100.dp, max = maxSearchWidth)
                                                .focusRequester(focusRequester),
                                            decorationBox = { innerTextField ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                ) {
                                                    Box(modifier = Modifier.weight(1f, fill = false)) {
                                                        if (appStopperSearchQuery.isEmpty()) {
                                                            Text(
                                                                text = "Search apps...",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.outline,
                                                                maxLines = 1
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                }
                                            }
                                        )

                                        if (appStopperSearchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                                    appStopperSearchQuery = ""
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .align(Alignment.CenterVertically)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear search",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            } else {
                                OsysterDock(
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    title = "App Stopper",
                                    onBackClick = {
                                        OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                        navController.navigateUp()
                                    },
                                    actionButton = {
                                        IconButton(
                                            onClick = {
                                                OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                                isAppStopperSearchActive = true
                                            },
                                            modifier = Modifier.size(44.dp),
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    hapticEnabled = prefsState.hapticFeedback,
                                    floatingActionButton = {
                                        FloatingActionButton(
                                            onClick = {
                                                OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                                showAppStopperAddSheet = true
                                            },
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            shape = MaterialTheme.shapes.large,
                                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Applications"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        else -> {
                            val dockItems = remember {
                                listOf(
                                    OsysterDockItem(
                                        icon = Icons.Default.Dashboard,
                                        label = "Dashboard",
                                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                                    ),
                                    OsysterDockItem(
                                        icon = Icons.Default.Speed,
                                        label = "Telemetry",
                                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                                    ),
                                    OsysterDockItem(
                                        icon = Icons.Default.Terminal,
                                        label = "Tasks",
                                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } }
                                    )
                                )
                            }

                            OsysterDock(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                items = dockItems,
                                selectedIndex = pagerState.currentPage,
                                hapticEnabled = prefsState.hapticFeedback,
                                floatingActionButton = {
                                    when (pagerState.currentPage) {
                                        0 -> {
                                            FloatingActionButton(
                                                onClick = {
                                                    OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                                    navController.navigate(AppRoutes.Settings)
                                                },
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                shape = MaterialTheme.shapes.large,
                                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Settings"
                                                )
                                            }
                                        }
                                        1 -> {
                                            FloatingActionButton(
                                                onClick = {
                                                    OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                                    telemetryInitialTab = if (telemetryInitialTab == 0) 1 else 0
                                                },
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                shape = MaterialTheme.shapes.large,
                                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Toggle CPU / Memory"
                                                )
                                            }
                                        }
                                        2 -> {
                                            FloatingActionButton(
                                                onClick = {
                                                    OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                                    navController.navigate(AppRoutes.AppStopper)
                                                },
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                shape = MaterialTheme.shapes.large,
                                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PowerSettingsNew,
                                                    contentDescription = "App Stopper"
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
