package dev.qtremors.osyster.feature.appstopper

import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics
import dev.qtremors.osyster.core.ui.components.rememberAppIconBitmap
import dev.qtremors.osyster.core.usage.domain.ManagedApp

@Immutable
internal data class AppStopperActions(
    val onStop: (String) -> Unit,
    val onStopAll: () -> Unit,
    val onLaunch: (String) -> Unit,
    val onAppInfo: (String) -> Unit,
    val onStore: (String) -> Unit,
    val onRemove: (String) -> Unit,
    val onFreeze: (String, Boolean) -> Unit,
    val onColumns: (Int) -> Unit,
    val onOpenPicker: () -> Unit,
    val onPickerSystem: (Boolean) -> Unit,
    val onAdd: (Collection<String>) -> Unit
)

/** Everything the screen shows besides the actions. */
internal data class AppStopperUi(
    val apps: List<ManagedApp>?,
    val picker: PickerState,
    /** Shizuku or root: Stop works in one tap, and apps can be frozen. */
    val privileged: Boolean,
    val columns: Int
)

/**
 * A grid of the apps you manage, as the original App stopper had. Tap an icon to stop it; long-press
 * for open, App info, freeze, store, and remove. Stopped and frozen apps are shown faded and gray.
 */
@Composable
internal fun AppStopperScreen(ui: AppStopperUi, actions: AppStopperActions) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var sheetFor by remember { mutableStateOf<ManagedApp?>(null) }
    val apps = ui.apps
    val openPicker = {
        actions.onOpenPicker()
        showPicker = true
    }
    OsysterScaffold(
        title = stringResource(Tool.APP_STOPPER.label.title),
        actions = {
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = openPicker) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.stopper_add)) }
            // Columns only mean something once there is a grid to arrange.
            if (!apps.isNullOrEmpty()) ColumnsMenu(ui.columns, actions.onColumns)
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(ui.columns),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "how", span = { GridItemSpan(maxLineSpan) }) { HowItWorks(ui.privileged) }
            when {
                apps == null -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { LoadingState() }
                apps.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Rounded.StopCircle,
                        title = stringResource(R.string.stopper_empty_title),
                        message = stringResource(R.string.stopper_empty_message),
                        actionLabel = stringResource(R.string.stopper_add),
                        onAction = openPicker
                    )
                }
                else -> {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }) { Header(apps, ui.privileged, actions.onStopAll) }
                    items(apps, key = { it.packageName }) { app ->
                        AppTile(app, ui.columns, onClick = { if (app.installed && !app.frozen) actions.onStop(app.packageName) else sheetFor = app }, onLongClick = { sheetFor = app })
                    }
                }
            }
        }
    }
    sheetFor?.let { app -> AppSheet(app, ui.privileged, actions, onDismiss = { sheetFor = null }) }
    if (showPicker) {
        AppPickerSheet(
            state = ui.picker,
            alreadyAdded = apps.orEmpty().map(ManagedApp::packageName).toSet(),
            onIncludeSystem = actions.onPickerSystem,
            onConfirm = { chosen ->
                actions.onAdd(chosen)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/** One info row, styled like the hints on Widgets and Quick Settings tiles. */
@Composable
private fun HowItWorks(privileged: Boolean) {
    ConnectedGroup(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
        ConnectedRow(
            title = stringResource(R.string.stopper_how_title),
            supporting = stringResource(if (privileged) R.string.stopper_how_privileged else R.string.stopper_how_message),
            leading = { IconBadge(Icons.Rounded.Info, key = "stopper_how") }
        )
    }
}

@Composable
private fun Header(apps: List<ManagedApp>, privileged: Boolean, onStopAll: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
        Text(
            stringResource(
                R.string.stopper_counts_frozen,
                apps.count { it.installed && !it.stopped && !it.frozen },
                apps.count { it.installed && it.stopped && !it.frozen },
                apps.count { it.frozen }
            ) + apps.count { !it.installed }.let { ghosts ->
                if (ghosts > 0) stringResource(R.string.stopper_counts_uninstalled, ghosts) else ""
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (privileged && apps.any { it.installed && !it.stopped && !it.frozen }) {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onStopAll) { Text(stringResource(R.string.stopper_stop_all)) }
        }
    }
}

@Composable
private fun ColumnsMenu(columns: Int, onColumns: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(shapes = IconButtonDefaults.shapes(), onClick = { open = true }) { Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.stopper_columns)) }
        OsysterMenu(
            expanded = open,
            onDismissRequest = { open = false },
            groups = listOf(
                COLUMN_CHOICES.map { count ->
                    MenuAction(stringResource(R.string.stopper_columns_count, count), selected = count == columns) { onColumns(count) }
                }
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: ManagedApp, columns: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    val faded = app.stopped || app.frozen || !app.installed
    val iconSize = when (columns) {
        4 -> 52.dp
        5 -> 44.dp
        else -> 38.dp
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (faded) colors.surfaceContainerHigh else colors.surfaceBright,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .then(if (!app.installed) Modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp)) else Modifier)
            .combinedClickable(
                onClick = {
                    haptics.tap()
                    onClick()
                },
                onLongClick = {
                    haptics.longPress()
                    onLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                val bitmap = if (app.installed) rememberAppIconBitmap(app.packageName) else null
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        colorFilter = if (faded) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                        modifier = Modifier.size(iconSize).clip(RoundedCornerShape(12.dp)).alpha(if (faded) 0.5f else 1f)
                    )
                } else {
                    Icon(Icons.Rounded.Android, contentDescription = null, tint = colors.outline, modifier = Modifier.size(iconSize))
                }
                if (app.frozen) {
                    Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (faded) colors.onSurfaceVariant else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(
                    when {
                        !app.installed -> R.string.stopper_label_uninstalled
                        app.frozen -> R.string.stopper_label_frozen
                        app.stopped -> R.string.stopper_label_stopped
                        else -> R.string.stopper_label_running
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    !app.installed -> colors.tertiary
                    faded -> colors.outline
                    else -> colors.primary
                },
                maxLines = 1
            )
        }
    }
}

/** Long-press actions for one app. Freezing needs Shizuku or root, so it only shows then. */
@Composable
private fun AppSheet(app: ManagedApp, privileged: Boolean, actions: AppStopperActions, onDismiss: () -> Unit) {
    fun run(action: () -> Unit) {
        action()
        onDismiss()
    }
    OsysterBottomSheet(onDismissRequest = onDismiss, title = app.label) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ConnectedGroup {
                if (app.installed && !app.frozen) {
                    SheetRow(stringResource(R.string.stopper_stop), Icons.Rounded.StopCircle, enabled = !app.stopped) { run { actions.onStop(app.packageName) } }
                    SheetRow(stringResource(R.string.stopper_open), Icons.AutoMirrored.Rounded.OpenInNew) { run { actions.onLaunch(app.packageName) } }
                }
                if (app.installed) {
                    SheetRow(stringResource(R.string.stopper_app_info), Icons.Rounded.Info) { run { actions.onAppInfo(app.packageName) } }
                    if (privileged) {
                        SheetRow(
                            stringResource(if (app.frozen) R.string.stopper_unfreeze else R.string.stopper_freeze),
                            Icons.Rounded.AcUnit,
                            supporting = stringResource(if (app.frozen) R.string.stopper_unfreeze_summary else R.string.stopper_freeze_summary)
                        ) { run { actions.onFreeze(app.packageName, !app.frozen) } }
                    }
                }
                SheetRow(stringResource(if (app.installed) R.string.stopper_store else R.string.stopper_reinstall), Icons.Rounded.Shop) { run { actions.onStore(app.packageName) } }
                SheetRow(stringResource(R.string.stopper_remove_from_list), Icons.Rounded.Delete) { run { actions.onRemove(app.packageName) } }
            }
            if (!app.installed) {
                Text(
                    stringResource(R.string.stopper_uninstalled_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                )
            }
            if (!privileged) {
                Text(
                    stringResource(R.string.stopper_freeze_needs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 16.dp)
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SheetRow(title: String, icon: ImageVector, supporting: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    ConnectedRow(title = title, supporting = supporting, leading = { IconBadge(icon, key = title) }, onClick = onClick, enabled = enabled)
}

private val COLUMN_CHOICES = listOf(3, 4, 5, 6)
