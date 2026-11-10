package dev.qtremors.osyster.feature.appmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.apps.domain.AppSummary
import dev.qtremors.osyster.core.apps.domain.PermissionArea
import dev.qtremors.osyster.core.apps.domain.PermissionGroup
import dev.qtremors.osyster.core.apps.domain.Risk
import dev.qtremors.osyster.core.apps.domain.SpecialAccess
import dev.qtremors.osyster.core.apps.domain.areaCounts
import dev.qtremors.osyster.core.apps.domain.riskCounts
import dev.qtremors.osyster.core.apps.domain.specialCounts
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

/** What every app may see and do, by area: counts first, then each area's apps on a tap. */
@Composable
internal fun PermissionsScreen(viewModel: AppManagerViewModel, onNavigate: (AppManagerDestination) -> Unit) {
    val all by viewModel.apps.collectAsStateWithLifecycle()
    var userOnly by rememberSaveable { mutableStateOf(true) }
    var openArea by rememberSaveable { mutableStateOf<PermissionArea?>(null) }
    var openSpecial by rememberSaveable { mutableStateOf<SpecialAccess?>(null) }

    OsysterScaffold(title = stringResource(R.string.apps_permissions_title)) { padding ->
        val loaded = all
        if (loaded == null) {
            LoadingState(modifier = Modifier.padding(padding))
            return@OsysterScaffold
        }
        val apps = remember(loaded, userOnly) { if (userOnly) loaded.filterNot { it.system } else loaded }
        val areas = remember(apps) { areaCounts(apps).associateBy { it.area } }
        val special = remember(apps) { specialCounts(apps) }
        val risks = remember(apps) { riskCounts(apps) }
        LazyColumn(contentPadding = padding) {
            item(key = "risk") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Count(Icons.Rounded.GppMaybe, stringResource(R.string.apps_risk_high), risks[Risk.HIGH] ?: 0, Risk.HIGH.color(), Modifier.weight(1f))
                    Count(Icons.Rounded.Shield, stringResource(R.string.apps_risk_medium), risks[Risk.MEDIUM] ?: 0, Risk.MEDIUM.color(), Modifier.weight(1f))
                    Count(Icons.Rounded.Star, stringResource(R.string.apps_special), apps.count { it.special.isNotEmpty() }, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
            item(key = "scope") {
                ConnectedChoices(
                    options = listOf(true, false),
                    selected = userOnly,
                    label = { stringResource(if (it) R.string.apps_scope_user else R.string.apps_scope_all) },
                    onSelect = { userOnly = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            }
            item(key = "bars") {
                // How many apps were allowed anything in each group, as bars against the busiest group.
                val totals = PermissionGroup.entries.associateWith { group -> apps.count { app -> app.allowedAreas.any { it.group == group } } }
                val max = (totals.values.maxOrNull() ?: 0).coerceAtLeast(1)
                Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(20.dp)) {
                        totals.forEach { (group, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(group.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(120.dp))
                                Box(modifier = Modifier.weight(1f).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                                    Box(modifier = Modifier.fillMaxWidth(count / max.toFloat()).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                }
                                Text(count.toString(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp).width(28.dp))
                            }
                        }
                    }
                }
            }
            PermissionGroup.entries.forEach { group ->
                item(key = "group-${group.name}") {
                    Column {
                        SectionHeader(stringResource(group.label))
                        ConnectedGroup {
                            PermissionArea.entries.filter { it.group == group }.forEach { area ->
                                val count = areas.getValue(area)
                                ConnectedRow(
                                    title = stringResource(area.label),
                                    supporting = stringResource(R.string.apps_area_count, count.allowed, count.requested),
                                    leading = { IconBadge(area.icon, key = "area-${area.name}") },
                                    trailing = { if (area.risk == Risk.HIGH && count.allowed > 0) RiskTag(Risk.HIGH) },
                                    onClick = { openArea = area }
                                )
                            }
                        }
                    }
                }
            }
            item(key = "special") {
                Column {
                    SectionHeader(stringResource(R.string.apps_special))
                    ConnectedGroup {
                        SpecialAccess.entries.forEach { access ->
                            ConnectedRow(
                                title = stringResource(access.label),
                                supporting = stringResource(R.string.apps_count, special.getValue(access)),
                                leading = { IconBadge(access.icon, key = "special-${access.name}") },
                                onClick = { openSpecial = access }
                            )
                        }
                    }
                }
            }
        }

        openArea?.let { area ->
            val asking = apps.filter { area in it.requestedAreas }.sortedWith(compareByDescending<AppSummary> { area in it.allowedAreas }.thenBy { it.label.lowercase() })
            AppsSheet(stringResource(area.label), asking, allowed = { area in it.allowedAreas }, onOpen = {
                openArea = null
                onNavigate(AppManagerDestination.Details(it))
            }, onDismiss = { openArea = null })
        }
        openSpecial?.let { access ->
            val having = apps.filter { access in it.special }.sortedBy { it.label.lowercase() }
            AppsSheet(stringResource(access.label), having, allowed = { true }, onOpen = {
                openSpecial = null
                onNavigate(AppManagerDestination.Details(it))
            }, onDismiss = { openSpecial = null })
        }
    }
}

@Composable
private fun Count(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, count: Int, tint: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.large, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** The apps behind one area or special access; those allowed first, each opening its page. */
@Composable
private fun AppsSheet(title: String, apps: List<AppSummary>, allowed: (AppSummary) -> Boolean, onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    OsysterBottomSheet(onDismissRequest = onDismiss, title = title) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            if (apps.isEmpty()) {
                Text(stringResource(R.string.apps_area_none), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            } else {
                ConnectedGroup {
                    apps.forEach { app ->
                        val on = allowed(app)
                        ConnectedRow(
                            title = app.label,
                            supporting = stringResource(if (on) R.string.apps_permissions_allowed else R.string.apps_permissions_denied),
                            leading = { AppIcon(app.packageName) },
                            trailing = {
                                Icon(
                                    if (on) Icons.Rounded.Check else Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { onOpen(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}
