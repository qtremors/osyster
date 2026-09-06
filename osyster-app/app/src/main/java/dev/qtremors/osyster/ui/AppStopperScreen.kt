@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import dev.qtremors.osyster.R
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.qtremors.osyster.ui.util.collectAsVisibleState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import dev.qtremors.osyster.monitor.InstalledAppItem
import dev.qtremors.osyster.monitor.ManagedAppInfo
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.settings.OsysterPreferencesState
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import dev.qtremors.osyster.ui.util.rememberAsyncAppIcon
import dev.qtremors.osyster.ui.viewmodel.AppStopperUiState
import dev.qtremors.osyster.ui.viewmodel.AppStopperViewModel

// =========================================================================
// Section Comment: App Stopper Screen
// =========================================================================

@Composable
fun AppStopperScreen(
    prefsState: OsysterPreferencesState,
    manager: OsysterPreferencesManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    showAddSheet: Boolean = false,
    onShowAddSheetChange: (Boolean) -> Unit = {},
    searchQuery: String = "",
    viewModel: AppStopperViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsVisibleState()

    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    LaunchedEffect(showAddSheet) {
        if (showAddSheet != uiState.showAddSheet) {
            viewModel.setShowAddSheet(showAddSheet)
        }
    }

    LifecycleResumeEffect(prefsState.managedStopPackages) {
        viewModel.loadManagedApps(prefsState.managedStopPackages)
        onPauseOrDispose { }
    }

    val gridColumns = prefsState.appStopperGridColumns.coerceIn(4, 6)

    AppStopperContent(
        uiState = uiState,
        gridColumns = gridColumns,
        hapticFeedback = prefsState.hapticFeedback,
        onSetGridColumns = { manager.setAppStopperGridColumns(it) },
        onOpenAddSheet = {
            viewModel.setShowAddSheet(true)
            onShowAddSheetChange(true)
        },
        onAppClick = { app ->
            if (app.isUninstalled) {
                viewModel.setSelectedAppForOptions(app)
            } else {
                viewModel.openAppInfo(app.packageName)
            }
        },
        onAppLongClick = { app ->
            viewModel.setSelectedAppForOptions(app)
        },
        modifier = modifier
    )

    // Hold Options Bottom Sheet
    uiState.selectedAppForOptions?.let { app ->
        AppStopperHoldOptionsSheet(
            app = app,
            onDismiss = { viewModel.setSelectedAppForOptions(null) },
            onOpenAppInfo = { viewModel.openAppInfo(app.packageName) },
            onLaunchApp = { viewModel.launchApp(app.packageName) },
            onOpenPlayStore = { viewModel.openInPlayStore(app.packageName) },
            onRemoveApp = { viewModel.removeManagedApp(app.packageName, manager) }
        )
    }

    // Add Apps Bottom Sheet
    if (uiState.showAddSheet) {
        LaunchedEffect(prefsState.managedStopPackages) {
            viewModel.loadInstalledApps(
                alreadyManaged = prefsState.managedStopPackages,
                includeSystem = uiState.includeSystemApps
            )
        }

        AddAppsBottomSheet(
            installedApps = uiState.installedApps,
            isLoading = uiState.isInstalledLoading,
            includeSystemApps = uiState.includeSystemApps,
            hapticEnabled = prefsState.hapticFeedback,
            onIncludeSystemAppsChange = { includeSystem ->
                viewModel.loadInstalledApps(
                    alreadyManaged = prefsState.managedStopPackages,
                    includeSystem = includeSystem
                )
            },
            onDismiss = {
                viewModel.setShowAddSheet(false)
                onShowAddSheetChange(false)
            },
            onAddPackages = { selectedPackages ->
                viewModel.addManagedPackages(selectedPackages, manager)
                onShowAddSheetChange(false)
                viewModel.loadManagedApps(manager.state.value.managedStopPackages)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppStopperContent(
    uiState: AppStopperUiState,
    gridColumns: Int,
    hapticFeedback: Boolean,
    onSetGridColumns: (Int) -> Unit,
    onOpenAddSheet: () -> Unit,
    onAppClick: (ManagedAppInfo) -> Unit,
    onAppLongClick: (ManagedAppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showGridMenu by remember { mutableStateOf(false) }

    val managedApps = uiState.managedApps
    val isLoading = uiState.isLoading
    val searchQuery = uiState.searchQuery
    val filteredApps = uiState.filteredApps
    val uninstalledCount = uiState.uninstalledCount
    val stoppedCount = uiState.stoppedCount
    val activeCount = uiState.activeCount

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Status pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.app_stopper_status_active, activeCount),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.app_stopper_status_stopped, stoppedCount),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (uninstalledCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = stringResource(R.string.app_stopper_status_ghost, uninstalledCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // Right: Grid size dropdown button
                Box {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                OsysterHapticUtil.performVirtualKey(view, hapticFeedback)
                                showGridMenu = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${gridColumns}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showGridMenu,
                        onDismissRequest = { showGridMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        listOf(4, 5, 6).forEach { cols ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.app_stopper_columns_format, cols),
                                        fontWeight = if (gridColumns == cols) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    if (gridColumns == cols) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    OsysterHapticUtil.performVirtualKey(view, hapticFeedback)
                                    onSetGridColumns(cols)
                                    showGridMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Grid Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }

                    managedApps.isEmpty() -> {
                        // Empty state banner
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Apps,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.app_stopper_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.app_stopper_empty_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        OsysterHapticUtil.performVirtualKey(view, hapticFeedback)
                                        onOpenAddSheet()
                                    },
                                    shape = RoundedCornerShape(100)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.app_stopper_add_apps_button), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    filteredApps.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.app_stopper_no_matching_query, searchQuery),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = LocalBottomContentPadding.current
                            ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                ManagedAppGridItem(
                                    app = app,
                                    columns = gridColumns,
                                    hapticEnabled = hapticFeedback,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// Subsection Comment: App Stopper Hold Options Sheet
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppStopperHoldOptionsSheet(
    app: ManagedAppInfo,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onLaunchApp: () -> Unit,
    onOpenPlayStore: () -> Unit,
    onRemoveApp: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val bitmap = rememberAsyncAppIcon(app.packageName, app.icon).value
                if (app.isUninstalled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                } else if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        colorFilter = if (app.isStopped) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                        } else null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(100),
                    color = when {
                        app.isUninstalled -> MaterialTheme.colorScheme.tertiaryContainer
                        app.isStopped -> MaterialTheme.colorScheme.surfaceContainerHighest
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = when {
                            app.isUninstalled -> stringResource(R.string.app_stopper_badge_uninstalled)
                            app.isStopped -> stringResource(R.string.app_stopper_badge_stopped)
                            else -> stringResource(R.string.app_stopper_badge_running)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            app.isUninstalled -> MaterialTheme.colorScheme.onTertiaryContainer
                            app.isStopped -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            if (app.isUninstalled) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.app_stopper_uninstalled_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                ListItem(
                    supportingContent = { Text(stringResource(R.string.app_stopper_action_reinstall_desc)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Shop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = {
                                onDismiss()
                                onOpenPlayStore()
                            }
                        ),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(stringResource(R.string.app_stopper_action_reinstall), fontWeight = FontWeight.Bold)
                }

                ListItem(
                    supportingContent = { Text(stringResource(R.string.app_stopper_action_remove_ghost_desc)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = {
                                onDismiss()
                                onRemoveApp()
                            }
                        ),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(
                        stringResource(R.string.app_stopper_action_remove),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // Actions List for Installed App
                ListItem(
                    supportingContent = { Text(stringResource(R.string.app_stopper_action_force_stop_desc)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = {
                                onDismiss()
                                onOpenAppInfo()
                            }
                        ),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(stringResource(R.string.app_stopper_action_force_stop), fontWeight = FontWeight.Bold)
                }

                ListItem(
                    supportingContent = { Text(stringResource(R.string.app_stopper_action_launch_desc)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = {
                                onDismiss()
                                onLaunchApp()
                            }
                        ),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(stringResource(R.string.app_stopper_action_launch), fontWeight = FontWeight.Bold)
                }

                ListItem(
                    supportingContent = { Text(stringResource(R.string.app_stopper_action_store_desc)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Shop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = {
                                onDismiss()
                                onOpenPlayStore()
                            }
                        ),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(stringResource(R.string.app_stopper_action_store), fontWeight = FontWeight.Bold)
                }

                ListItem(
                    supportingContent = { Text(stringResource(R.string.app_stopper_action_remove_desc)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = {
                                onDismiss()
                                onRemoveApp()
                            }
                        ),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(
                        stringResource(R.string.app_stopper_action_remove),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// =========================================================================
// Subsection Comment: High-Density Grid Item
// =========================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManagedAppGridItem(
    app: ManagedAppInfo,
    columns: Int,
    hapticEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val view = LocalView.current
    val bitmap = rememberAsyncAppIcon(app.packageName, app.icon).value

    val iconSize = when (columns) {
        4 -> 50.dp
        5 -> 44.dp
        else -> 38.dp
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                app.isUninstalled -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
                app.isStopped -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        border = if (app.isUninstalled) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                    onClick()
                },
                onLongClick = {
                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                    onLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            if (app.isUninstalled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    modifier = Modifier.size(iconSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(iconSize * 0.65f)
                        )
                    }
                }
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    colorFilter = if (app.isStopped) {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    } else null,
                    alpha = if (app.isStopped) 0.45f else 1f,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = if (app.isStopped) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // App Label
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (app.isStopped || app.isUninstalled) FontWeight.Normal else FontWeight.Bold,
                color = if (app.isStopped || app.isUninstalled) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = if (columns >= 6) 10.sp else 11.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Status Text
            Text(
                text = when {
                    app.isUninstalled -> stringResource(R.string.app_stopper_label_ghost)
                    app.isStopped -> stringResource(R.string.app_stopper_label_stopped)
                    else -> stringResource(R.string.app_stopper_label_active)
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    app.isUninstalled -> MaterialTheme.colorScheme.tertiary
                    app.isStopped -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.primary
                },
                fontSize = 9.sp
            )
        }
    }
}

// =========================================================================
// Subsection Comment: Add Apps Bottom Sheet (Full Height & Scrollable)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppsBottomSheet(
    installedApps: List<InstalledAppItem>,
    isLoading: Boolean,
    includeSystemApps: Boolean,
    hapticEnabled: Boolean,
    onIncludeSystemAppsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onAddPackages: (Set<String>) -> Unit
) {
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    val selectedPackages = remember { mutableStateListOf<String>() }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_stopper_add_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_stopper_add_sheet_subtitle, selectedPackages.size, filteredApps.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = {
                        OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                        onAddPackages(selectedPackages.toSet())
                    },
                    enabled = selectedPackages.isNotEmpty(),
                    shape = RoundedCornerShape(100)
                ) {
                    Text(stringResource(R.string.app_stopper_add_to_list), fontWeight = FontWeight.Bold)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.app_stopper_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )

            // System apps toggle chip
            FilterChip(
                selected = includeSystemApps,
                onClick = {
                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                    onIncludeSystemAppsChange(!includeSystemApps)
                },
                label = { Text(stringResource(R.string.app_stopper_include_system)) },
                leadingIcon = {
                    if (includeSystemApps) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            )

            // Installed Apps List (Full scrollable height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }

                    filteredApps.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.app_stopper_no_available_apps),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredApps, key = { it.packageName }) { appItem ->
                                val isSelected = appItem.packageName in selectedPackages
                                val bitmap = rememberAsyncAppIcon(appItem.packageName, appItem.icon).value

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            onClick = {
                                                OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                                                if (isSelected) {
                                                    selectedPackages.remove(appItem.packageName)
                                                } else {
                                                    selectedPackages.add(appItem.packageName)
                                                }
                                            }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Android,
                                                contentDescription = null,
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = appItem.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = appItem.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                                                if (checked) {
                                                    selectedPackages.add(appItem.packageName)
                                                } else {
                                                    selectedPackages.remove(appItem.packageName)
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
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun AppStopperContentPreview() {
    OsysterTheme {
        AppStopperContent(
            uiState = AppStopperUiState(
                managedApps = listOf(
                    ManagedAppInfo(
                        packageName = "com.android.camera",
                        label = "Camera",
                        icon = null,
                        isStopped = false,
                        isSystemApp = false
                    ),
                    ManagedAppInfo(
                        packageName = "com.android.settings",
                        label = "Settings",
                        icon = null,
                        isStopped = true,
                        isSystemApp = false
                    )
                ),
                isLoading = false
            ),
            gridColumns = 4,
            hapticFeedback = true,
            onSetGridColumns = {},
            onOpenAddSheet = {},
            onAppClick = {},
            onAppLongClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppStopperHoldOptionsPreview() {
    OsysterTheme {
        AppStopperHoldOptionsSheet(
            app = ManagedAppInfo(
                packageName = "com.example.sample",
                label = "Sample App",
                icon = null,
                isStopped = false,
                isSystemApp = false
            ),
            onDismiss = {},
            onOpenAppInfo = {},
            onLaunchApp = {},
            onOpenPlayStore = {},
            onRemoveApp = {}
        )
    }
}
