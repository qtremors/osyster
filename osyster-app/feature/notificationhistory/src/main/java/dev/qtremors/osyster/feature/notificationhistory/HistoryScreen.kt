package dev.qtremors.osyster.feature.notificationhistory

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.notifications.domain.HistoryEntry
import dev.qtremors.osyster.core.notifications.domain.RemovalReason
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedItem
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.InlineSearchField
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Notification history: a gate while access is off, an intro while history is off, then every
 * saved notification by day, with search and an app filter. Tapping one opens all of it.
 */
@Composable
internal fun NotificationHistoryScreen(viewModel: NotificationHistoryViewModel, onNavigate: (NotificationHistoryDestination) -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    val open by viewModel.open.collectAsStateWithLifecycle()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshAccess()
        onPauseOrDispose { }
    }

    OsysterScaffold(
        title = stringResource(R.string.history_title),
        actions = {
            if (settings?.enabled == true) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { settingsOpen = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.history_settings))
                }
            }
        }
    ) { padding ->
        val loaded = settings
        when {
            loaded == null -> LoadingState(modifier = Modifier.padding(padding))
            !access -> Box(modifier = Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Rounded.NotificationsOff,
                    title = stringResource(R.string.history_access_title),
                    message = stringResource(R.string.history_access_message),
                    actionLabel = stringResource(R.string.history_access_action),
                    onAction = { onNavigate(NotificationHistoryDestination.NotificationAccess) }
                )
            }
            !loaded.enabled -> Intro(padding, onTurnOn = { viewModel.update { it.copy(enabled = true) } })
            else -> HistoryList(viewModel, padding)
        }
    }

    open?.let { entry ->
        EntrySheet(
            entry = entry,
            viewModel = viewModel,
            onOpenApp = { onNavigate(NotificationHistoryDestination.OpenApp(entry.packageName)) },
            onDismiss = { viewModel.open(null) }
        )
    }
    if (settingsOpen) settings?.let { HistorySettingsSheet(it, viewModel, onDismiss = { settingsOpen = false }) }
}

@Composable
private fun Intro(padding: PaddingValues, onTurnOn: () -> Unit) {
    LazyColumn(contentPadding = padding) {
        item(key = "intro") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                IconBadge(icon = Icons.Rounded.NotificationsActive, key = "history-intro", size = 72.dp, iconSize = 36.dp)
                Text(stringResource(R.string.history_intro_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.history_intro_message), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        item(key = "points") {
            ConnectedGroup {
                ConnectedRow(
                    title = stringResource(R.string.history_intro_full),
                    supporting = stringResource(R.string.history_intro_full_detail),
                    leading = { IconBadge(icon = Icons.AutoMirrored.Rounded.Notes, key = "history-full") }
                )
                ConnectedRow(
                    title = stringResource(R.string.history_intro_private),
                    supporting = stringResource(R.string.history_intro_private_detail),
                    leading = { IconBadge(icon = Icons.Rounded.Lock, key = "history-private") }
                )
                ConnectedRow(
                    title = stringResource(R.string.history_intro_light),
                    supporting = stringResource(R.string.history_intro_light_detail),
                    leading = { IconBadge(icon = Icons.Rounded.BatterySaver, key = "history-light") }
                )
            }
        }
        item(key = "action") {
            Button(shapes = ButtonDefaults.shapes(), onClick = onTurnOn, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp)) {
                Text(stringResource(R.string.history_intro_action))
            }
        }
    }
}

@Composable
private fun HistoryList(viewModel: NotificationHistoryViewModel, padding: PaddingValues) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    val list = entries
    val days = remember(list) { list.orEmpty().groupBy { Instant.ofEpochMilli(it.postedAt).atZone(zone).toLocalDate() } }

    LazyColumn(contentPadding = padding) {
        item(key = "search") {
            InlineSearchField(query = query.search.orEmpty(), onQueryChange = viewModel::search, placeholder = stringResource(R.string.history_search))
        }
        if (apps.size > 1) {
            item(key = "apps") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    item(key = "all") {
                        FilterChip(selected = query.packageName == null, onClick = { viewModel.filter(null) }, label = { Text(stringResource(R.string.history_all_apps)) })
                    }
                    items(apps, key = { it.packageName }) { app ->
                        FilterChip(
                            selected = query.packageName == app.packageName,
                            onClick = { viewModel.filter(if (query.packageName == app.packageName) null else app.packageName) },
                            label = { Text(app.label) },
                            leadingIcon = { AppIcon(app.packageName, size = 18.dp) }
                        )
                    }
                }
            }
        }
        when {
            list == null -> item(key = "loading") { LoadingState() }
            list.isEmpty() && (query.search.isNullOrBlank() && query.packageName == null) -> item(key = "empty") {
                EmptyState(Icons.Rounded.NotificationsActive, stringResource(R.string.history_empty_title), stringResource(R.string.history_empty_message))
            }
            list.isEmpty() -> item(key = "none") {
                EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.history_no_results_title), stringResource(R.string.history_no_results_message))
            }
            else -> {
                days.forEach { (day, dayEntries) ->
                    item(key = "day-${day.toEpochDay()}") { SectionHeader(dayLabel(day)) }
                    itemsIndexed(dayEntries, key = { _, entry -> entry.id }) { index, entry ->
                        ConnectedItem(index = index, count = dayEntries.size) {
                            NotificationRow(entry, onClick = { viewModel.open(entry) })
                        }
                    }
                }
                if (list.size >= query.limit) {
                    item(key = "more") {
                        FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = viewModel::loadMore, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                            Text(stringResource(R.string.history_more))
                        }
                    }
                }
            }
        }
    }
}

/** One entry: the app and time on top, then the title and up to three lines of text. */
@Composable
private fun NotificationRow(entry: HistoryEntry, onClick: () -> Unit) {
    val context = LocalContext.current
    val time = remember(entry.postedAt) { DateFormat.getTimeFormat(context).format(java.util.Date(entry.postedAt)) }
    val you = stringResource(R.string.history_you)
    Surface(color = MaterialTheme.colorScheme.surfaceBright, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            AppIcon(entry.packageName, size = 40.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.history_meta_line, entry.appLabel, time),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    entry.removal?.let { reason ->
                        if (reason == RemovalReason.APP_REMOVED) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.history_removed_app), tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                        }
                    }
                }
                entry.title?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                val body = entry.text?.let { text ->
                    if (entry.conversation != null) stringResource(R.string.history_sender_line, entry.sender ?: you, text) else text
                }
                body?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            }
            rememberPicture(entry.picturePath, THUMB_PX)?.let { picture ->
                Image(
                    bitmap = picture,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                )
            }
        }
    }
}

@Composable
private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> stringResource(R.string.history_today)
        today.minusDays(1) -> stringResource(R.string.history_yesterday)
        else -> {
            val millis = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or (if (day.year == today.year) DateUtils.FORMAT_NO_YEAR else 0)
            DateUtils.formatDateTime(LocalContext.current, millis, flags)
        }
    }
}


private const val THUMB_PX = 168
