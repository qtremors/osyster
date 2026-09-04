package dev.qtremors.osyster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.qtremors.osyster.navigation.AppRoutes
import dev.qtremors.osyster.ui.BentoDashboard
import dev.qtremors.osyster.ui.CpuDashboard
import dev.qtremors.osyster.ui.DeviceInfoDashboard
import dev.qtremors.osyster.ui.MemoryDashboard
import dev.qtremors.osyster.ui.ProcessDashboard
import dev.qtremors.osyster.ui.theme.OsysterTheme

// =========================================================================
// Section Comment: Main Activity Entrypoint & Theme Integration
// =========================================================================

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OsysterTheme {
                val navController = rememberNavController()
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Osyster",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 8.dp
                        ) {
                            val items = listOf(
                                Triple(AppRoutes.Bento, Icons.Default.Dashboard, "Dashboard"),
                                Triple(AppRoutes.Cpu, Icons.Default.Speed, "CPU"),
                                Triple(AppRoutes.Memory, Icons.Default.Memory, "RAM"),
                                Triple(AppRoutes.Processes, Icons.Default.Terminal, "Tasks"),
                                Triple(AppRoutes.DeviceInfo, Icons.Default.Info, "Info")
                            )

                            items.forEach { (route, icon, label) ->
                                val isSelected = currentDestination?.hasRoute(route::class) == true
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isSelected) {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.Bento,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        composable<AppRoutes.Bento> {
                            BentoDashboard(onNavigateTo = { route ->
                                navController.navigate(route)
                            })
                        }
                        composable<AppRoutes.Cpu> {
                            CpuDashboard()
                        }
                        composable<AppRoutes.Memory> {
                            MemoryDashboard()
                        }
                        composable<AppRoutes.Processes> {
                            ProcessDashboard()
                        }
                        composable<AppRoutes.DeviceInfo> {
                            DeviceInfoDashboard()
                        }
                    }
                }
            }
        }
    }
}
