@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.qtremors.osyster.monitor.AppStopperMonitor
import dev.qtremors.osyster.monitor.InstalledAppItem
import dev.qtremors.osyster.monitor.ManagedAppInfo
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.settings.OsysterPreferencesState
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =========================================================================
// Section Comment: App Stopper Screen
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppStopperScreen(
    prefsState: OsysterPreferencesState,
    manager: OsysterPreferencesManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    showAddSheet: Boolean = false,
    onShowAddSheetChange: (Boolean) -> Unit = {},
    searchQuery: String = ""
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var managedApps by remember { mutableStateOf<List<ManagedAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var localShowAddSheet by remember { mutableStateOf(false) }
    val isAddSheetOpen = showAddSheet || localShowAddSheet
    val setAddSheetOpen: (Boolean) -> Unit = { open ->
        localShowAddSheet = open
        onShowAddSheetChange(open)
    }
    var selectedAppForOptions by remember { mutableStateOf<ManagedAppInfo?>(null) }
    var showGridMenu by remember { mutableStateOf(false) }

    val gridColumns = prefsState.appStopperGridColumns.coerceIn(4, 6)

    // Helper to reload managed apps
    val reloadApps = {
        scope.launch(Dispatchers.IO) {
            val list = AppStopperMonitor.loadManagedApps(context, prefsState.managedStopPackages)
            withContext(Dispatchers.Main) {
                managedApps = list
                isLoading = false
            }
        }
    }

    // Refresh automatically whenever the user resumes Osyster (e.g. back from App Info)
    LifecycleResumeEffect(prefsState.managedStopPackages) {
        reloadApps()
        onPauseOrDispose { }
    }

    val filteredApps = remember(managedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            managedApps
        } else {
            managedApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val uninstalledCount = remember(managedApps) { managedApps.count { it.isUninstalled } }
    val stoppedCount = remember(managedApps) { managedApps.count { it.isStopped && !it.isUninstalled } }
    val activeCount = remember(managedApps) { managedApps.count { !it.isStopped && !it.isUninstalled } }

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
                                text = "$activeCount Active",
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
                                text = "$stoppedCount Stopped",
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
                                    text = "$uninstalledCount Ghost",
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
                    Surface(
                        onClick = {
                            OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                            showGridMenu = true
                        },
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

                    DropdownMenu(
                        expanded = showGridMenu,
                        onDismissRequest = { showGridMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        listOf(4, 5, 6).forEach { cols ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$cols Columns",
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
                                    OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                    manager.setAppStopperGridColumns(cols)
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
                                    text = "No Managed Apps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Add apps to monitor their stopped state in a high-density grid. Tap an app to open its App Info and force stop it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        OsysterHapticUtil.performVirtualKey(view, prefsState.hapticFeedback)
                                        setAddSheetOpen(true)
                                    },
                                    shape = RoundedCornerShape(100)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Applications", fontWeight = FontWeight.Bold)
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
                                text = "No apps matching \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = 110.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                ManagedAppGridItem(
                                    app = app,
                                    columns = gridColumns,
                                    hapticEnabled = prefsState.hapticFeedback,
                                    onClick = {
                                        if (app.isUninstalled) {
                                            selectedAppForOptions = app
                                        } else {
                                            AppStopperMonitor.openAppInfo(context, app.packageName)
                                        }
                                    },
                                    onLongClick = {
                                        selectedAppForOptions = app
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Hold Options Bottom Sheet
    selectedAppForOptions?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selectedAppForOptions = null },
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
                    val bitmap = remember(app.icon) {
                        app.icon?.let { runCatching { it.toSafeBitmap() }.getOrNull() }
                    }
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
                            bitmap = bitmap.asImageBitmap(),
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
                                app.isUninstalled -> "UNINSTALLED"
                                app.isStopped -> "STOPPED"
                                else -> "RUNNING"
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
                            text = "This app is uninstalled. Osyster keeps its ghost entry and will automatically resume monitoring when reinstalled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    ListItem(
                        supportingContent = { Text("Open Google Play Store to reinstall") },
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
                                    selectedAppForOptions = null
                                    AppStopperMonitor.openInPlayStore(context, app.packageName)
                                }
                            ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text("Install from Play Store", fontWeight = FontWeight.Bold)
                    }

                    ListItem(
                        supportingContent = { Text("Stop monitoring and remove ghost app") },
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
                                    selectedAppForOptions = null
                                    manager.removeManagedStopPackage(app.packageName)
                                    AppStopperMonitor.removeAppLabel(context, app.packageName)
                                    reloadApps()
                                }
                            ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            "Remove from List",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Actions List for Installed App
                    ListItem(
                        supportingContent = { Text("Go to system settings to force stop this app") },
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
                                    selectedAppForOptions = null
                                    AppStopperMonitor.openAppInfo(context, app.packageName)
                                }
                            ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text("Open App Info (Force Stop)", fontWeight = FontWeight.Bold)
                    }

                    ListItem(
                        supportingContent = { Text("Open application main activity") },
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
                                    selectedAppForOptions = null
                                    AppStopperMonitor.launchApp(context, app.packageName)
                                }
                            ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text("Launch Application", fontWeight = FontWeight.Bold)
                    }

                    ListItem(
                        supportingContent = { Text("Open Google Play Store listing") },
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
                                    selectedAppForOptions = null
                                    AppStopperMonitor.openInPlayStore(context, app.packageName)
                                }
                            ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text("View in Play Store", fontWeight = FontWeight.Bold)
                    }

                    ListItem(
                        supportingContent = { Text("Stop monitoring this application in Osyster") },
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
                                    selectedAppForOptions = null
                                    manager.removeManagedStopPackage(app.packageName)
                                    AppStopperMonitor.removeAppLabel(context, app.packageName)
                                    reloadApps()
                                }
                            ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            "Remove from List",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add Apps Bottom Sheet
    if (isAddSheetOpen) {
        AddAppsBottomSheet(
            alreadyManagedPackages = prefsState.managedStopPackages,
            hapticEnabled = prefsState.hapticFeedback,
            onDismiss = { setAddSheetOpen(false) },
            onAddPackages = { selectedPackages ->
                val current = prefsState.managedStopPackages
                manager.setManagedStopPackages(current + selectedPackages)
                setAddSheetOpen(false)
                reloadApps()
            }
        )
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
    val bitmap = remember(app.icon) {
        app.icon?.let { runCatching { it.toSafeBitmap() }.getOrNull() }
    }

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
                    bitmap = bitmap.asImageBitmap(),
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
                    app.isUninstalled -> "Ghost"
                    app.isStopped -> "Stopped"
                    else -> "Active"
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
private fun AddAppsBottomSheet(
    alreadyManagedPackages: Set<String>,
    hapticEnabled: Boolean,
    onDismiss: () -> Unit,
    onAddPackages: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var allInstalledApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var includeSystemApps by remember { mutableStateOf(false) }
    val selectedPackages = remember { mutableStateListOf<String>() }

    // Load installed apps when includeSystemApps changes
    LaunchedEffect(includeSystemApps) {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val list = AppStopperMonitor.loadAllInstalledApps(
                context = context,
                excludePackages = alreadyManagedPackages,
                includeSystemApps = includeSystemApps
            )
            withContext(Dispatchers.Main) {
                allInstalledApps = list
                isLoading = false
            }
        }
    }

    val filteredApps = remember(allInstalledApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allInstalledApps
        } else {
            allInstalledApps.filter {
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
                        text = "Add Applications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedPackages.size} selected (${filteredApps.size} available)",
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
                    Text("Add to List", fontWeight = FontWeight.Bold)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
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
                    includeSystemApps = !includeSystemApps
                },
                label = { Text("Include System Apps") },
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
                                text = "No available applications found",
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
                                val bitmap = remember(appItem.icon) {
                                    appItem.icon?.let { runCatching { it.toSafeBitmap() }.getOrNull() }
                                }

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
                                                bitmap = bitmap.asImageBitmap(),
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

// =========================================================================
// Subsection Comment: Drawable to Bitmap Converter
// =========================================================================

private fun Drawable.toSafeBitmap(): Bitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    return toBitmap(width.coerceIn(48, 144), height.coerceIn(48, 144))
}
