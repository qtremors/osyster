package dev.qtremors.osyster.feature.activitylauncher

import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import dev.qtremors.osyster.core.ui.components.FilterTop
import dev.qtremors.osyster.core.ui.components.ConnectedItem
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.system.domain.ActivityEntry
import dev.qtremors.osyster.core.system.domain.AppEntry
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

@Composable
internal fun ActivityLauncherScreen(
    state: LauncherState,
    message: LauncherMessage?,
    onSelect: (AppEntry?) -> Unit,
    onLaunch: (ActivityEntry) -> Unit,
    onPin: (ActivityEntry) -> Unit,
    onMessageShown: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val messageText = message?.let {
        stringResource(
            when (it) {
                LauncherMessage.OPEN_FAILED -> R.string.launcher_open_failed
                LauncherMessage.PIN_UNSUPPORTED -> R.string.launcher_pin_unsupported
                LauncherMessage.PIN_REQUESTED -> R.string.launcher_pin_requested
            }
        )
    }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            onMessageShown()
        }
    }
    // Back from an app's screens returns to the app list before leaving the tool.
    BackHandler(enabled = state.selected != null) { onSelect(null) }

    OsysterScaffold(
        title = state.selected?.label ?: stringResource(Tool.ACTIVITY_LAUNCHER.label.title),
        snackbarHost = { SnackbarHost(snackbar) },
        // Searching apps happens in the top bar, like every other list in Osyster.
        filter = if (state.selected == null) FilterTop(query, { query = it }, stringResource(R.string.launcher_search)) else null
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            val selected = state.selected
            if (selected == null) {
                val apps = state.apps
                if (apps == null) {
                    item(key = "loading") { LoadingState() }
                } else {
                    val words = query.trim().lowercase()
                    val shown = apps.filter { words.isEmpty() || words in it.label.lowercase() || words in it.packageName.lowercase() }
                    item(key = "apps_header") { SectionHeader(stringResource(R.string.launcher_apps, shown.size)) }
                    itemsIndexed(shown, key = { _, it -> it.packageName }) { index, app ->
                        ConnectedItem(index, shown.size) {
                            ConnectedRow(
                                title = app.label,
                                supporting = pluralStringResource(R.plurals.launcher_activity_count, app.activityCount, app.activityCount),
                                leading = { AppIcon(app.packageName) },
                                onClick = { onSelect(app) }
                            )
                        }
                    }
                }
            } else {
                val activities = state.activities
                if (activities == null) {
                    item(key = "activities_loading") { LoadingState() }
                } else {
                    item(key = "activities_header") {
                        Column {
                            SectionHeader(stringResource(R.string.launcher_screens))
                            Text(
                                stringResource(R.string.launcher_screens_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                    itemsIndexed(activities, key = { _, it -> it.className }) { index, activity ->
                        ConnectedItem(index, activities.size) { ActivityRow(activity, onLaunch, onPin) }
                    }
                }
            }
        }
    }
}

/** Exported screens open on tap; long-press pins one to the home screen. Others are locked. */
@Composable
private fun ActivityRow(activity: ActivityEntry, onLaunch: (ActivityEntry) -> Unit, onPin: (ActivityEntry) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        ConnectedRow(
            title = activity.label,
            supporting = activity.shortName,
            trailing = {
                Icon(
                    imageVector = if (activity.exported) Icons.AutoMirrored.Rounded.OpenInNew else Icons.Rounded.Lock,
                    contentDescription = if (activity.exported) null else stringResource(R.string.launcher_locked),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            enabled = activity.exported,
            onClick = { onLaunch(activity) },
            onLongClick = if (activity.exported) ({ menuOpen = true }) else null
        )
        OsysterMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            groups = listOf(
                listOf(MenuAction(stringResource(R.string.launcher_pin), Icons.AutoMirrored.Rounded.AddToHomeScreen) { onPin(activity) })
            )
        )
    }
}
