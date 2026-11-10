package dev.qtremors.osyster.feature.appmanager

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.apps.domain.AppSort
import dev.qtremors.osyster.core.apps.domain.AppSummary
import dev.qtremors.osyster.core.apps.domain.Risk
import dev.qtremors.osyster.core.apps.domain.matches
import dev.qtremors.osyster.core.apps.domain.riskCounts
import dev.qtremors.osyster.core.apps.domain.sortApps
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedItem
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.InlineSearchField
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import dev.qtremors.osyster.core.ui.components.OsysterScaffold

/** Every installed app with an overview on top: counts, risk, and the way into permissions. */
@Composable
internal fun AppListScreen(viewModel: AppManagerViewModel, onNavigate: (AppManagerDestination) -> Unit) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    var sortMenu by remember { mutableStateOf(false) }
    // Coming back from uninstalling or updating an app shows the change.
    var resumedOnce by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (resumedOnce) viewModel.refresh() else resumedOnce = true
        onPauseOrDispose { }
    }

    OsysterScaffold(
        title = stringResource(R.string.apps_title),
        actions = {
            Box {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { sortMenu = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(R.string.apps_sort))
                }
                OsysterMenu(
                    expanded = sortMenu,
                    onDismissRequest = { sortMenu = false },
                    groups = listOf(AppSort.entries.map { option ->
                        MenuAction(stringResource(option.label), selected = option == sort) {
                            viewModel.sort(option)
                            sortMenu = false
                        }
                    })
                )
            }
        }
    ) { padding ->
        val all = apps
        if (all == null) {
            LoadingState(modifier = Modifier.padding(padding))
            return@OsysterScaffold
        }
        val shown = remember(all, filter, sort, search) {
            sortApps(all.filter { filter.includes(it) && it.matches(search) }, sort)
        }
        AppList(all, shown, filter, search, padding, viewModel, onNavigate)
    }
}

@Composable
private fun AppList(
    all: List<AppSummary>,
    shown: List<AppSummary>,
    filter: AppFilter,
    search: String,
    padding: PaddingValues,
    viewModel: AppManagerViewModel,
    onNavigate: (AppManagerDestination) -> Unit
) {
    val context = LocalContext.current
    val users = all.filterNot { it.system }
    val risks = remember(all) { riskCounts(users) }
    val todayStart = remember { System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS }
    LazyColumn(contentPadding = padding) {
        item(key = "overview") {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(24.dp)) {
                    Text(all.size.toString(), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column {
                        Text(stringResource(R.string.apps_installed), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(stringResource(R.string.apps_installed_split, users.size, all.size - users.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        val updated = all.count { it.updatedAt >= todayStart && it.updatedAt > it.installedAt }
                        if (updated > 0) {
                            Text(stringResource(R.string.apps_updated_today) + " · " + updated, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        item(key = "risk") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                RiskCard(Icons.Rounded.GppMaybe, stringResource(R.string.apps_risk_high), risks[Risk.HIGH] ?: 0, Risk.HIGH, Modifier.weight(1f))
                RiskCard(Icons.Rounded.Shield, stringResource(R.string.apps_risk_medium), risks[Risk.MEDIUM] ?: 0, Risk.MEDIUM, Modifier.weight(1f))
                RiskCard(Icons.Rounded.Star, stringResource(R.string.apps_special), users.count { it.special.isNotEmpty() }, Risk.LOW, Modifier.weight(1f))
            }
        }
        item(key = "permissions") {
            ConnectedGroup(modifier = Modifier.padding(vertical = 12.dp)) {
                NavigationRow(
                    stringResource(R.string.apps_explore_permissions),
                    Icons.Rounded.Shield,
                    "apps-permissions",
                    { onNavigate(AppManagerDestination.Permissions) },
                    supporting = stringResource(R.string.apps_explore_permissions_detail)
                )
            }
        }
        item(key = "search") { InlineSearchField(query = search, onQueryChange = viewModel::search, placeholder = stringResource(R.string.apps_search)) }
        item(key = "filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(AppFilter.entries) { option ->
                    FilterChip(selected = filter == option, onClick = { viewModel.filter(option) }, label = { Text(stringResource(option.label)) })
                }
            }
        }
        if (shown.isEmpty()) {
            item(key = "none") { EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.apps_none_title), stringResource(R.string.apps_none_message)) }
        } else {
            itemsIndexed(shown, key = { _, app -> app.packageName }) { index, app ->
                ConnectedItem(index, shown.size) {
                    val detail = listOfNotNull(app.versionName, app.installer ?: if (!app.enabled) stringResource(R.string.apps_disabled) else null).joinToString(" · ")
                    ConnectedRow(
                        title = app.label,
                        supporting = detail.ifEmpty { app.packageName },
                        leading = { AppIcon(app.packageName) },
                        trailing = { if (app.risk != Risk.LOW) RiskTag(app.risk) },
                        onClick = { onNavigate(AppManagerDestination.Details(app.packageName)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskCard(icon: ImageVector, label: String, count: Int, risk: Risk, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.large, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
            Icon(icon, contentDescription = null, tint = if (risk == Risk.LOW) MaterialTheme.colorScheme.primary else risk.color())
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A small pill naming an app's risk, in its color. */
@Composable
internal fun RiskTag(risk: Risk) {
    Surface(color = risk.color().copy(alpha = 0.16f), shape = MaterialTheme.shapes.extraLarge) {
        Text(stringResource(risk.label), style = MaterialTheme.typography.labelSmall, color = risk.color(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

private fun AppFilter.includes(app: AppSummary): Boolean = when (this) {
    AppFilter.USER -> !app.system
    AppFilter.SYSTEM -> app.system
    AppFilter.ALL -> true
}

private val AppFilter.label: Int
    get() = when (this) {
        AppFilter.USER -> R.string.apps_filter_user
        AppFilter.SYSTEM -> R.string.apps_filter_system
        AppFilter.ALL -> R.string.apps_filter_all
    }

private val AppSort.label: Int
    get() = when (this) {
        AppSort.NAME -> R.string.apps_sort_name
        AppSort.UPDATED -> R.string.apps_sort_updated
        AppSort.INSTALLED -> R.string.apps_sort_installed
        AppSort.SIZE -> R.string.apps_sort_size
    }
