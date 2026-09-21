package dev.qtremors.osyster.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.HomeLayout
import dev.qtremors.osyster.core.device.domain.PinKey
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.catalog.title
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.HeroCard
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

@Immutable
internal data class HomeActions(
    val onOpenPage: (DevicePage) -> Unit,
    val onOpenTool: (Tool) -> Unit,
    val onTogglePin: (PinKey) -> Unit,
    val onMove: (DevicePage, Boolean) -> Unit,
    val onHide: (DevicePage) -> Unit,
    val onShow: (DevicePage) -> Unit
)

@Composable
internal fun HomeScreen(state: HomeState, actions: HomeActions) {
    var showHidden by rememberSaveable { mutableStateOf(false) }
    OsysterScaffold(title = stringResource(R.string.home_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "hero") {
                HeroCard(
                    title = state.hero.name,
                    subtitle = stringResource(R.string.home_hero_subtitle, state.hero.androidRelease, state.hero.apiLevel),
                    metrics = heroMetrics(state.summary),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            val layout = state.layout
            if (layout == null) {
                item(key = "loading") { LoadingState() }
                return@LazyColumn
            }
            if (layout.pinned.isNotEmpty()) {
                item(key = "pinned") { PinnedSection(layout, actions) }
            }
            val groups = layout.visiblePages.groupBy(DevicePage::group).toList()
            items(groups, key = { (group, _) -> group.name }) { (group, pages) ->
                Column {
                    SectionHeader(stringResource(group.title))
                    ConnectedGroup {
                        pages.forEachIndexed { index, page ->
                            PageRow(
                                page = page,
                                pinned = layout.isPinned(PinKey.page(page.id)),
                                canMoveUp = index > 0,
                                canMoveDown = index < pages.lastIndex,
                                actions = actions
                            )
                        }
                    }
                }
            }
            if (layout.hiddenPages.isNotEmpty()) {
                item(key = "hidden") { HiddenPagesRow(layout.hiddenPages.size) { showHidden = true } }
            }
        }
    }
    val hidden = state.layout?.hiddenPages.orEmpty()
    if (showHidden && hidden.isNotEmpty()) {
        HiddenPagesSheet(hidden = hidden, onShow = actions.onShow, onDismiss = { showHidden = false })
    }
}

@Composable
private fun PinnedSection(layout: HomeLayout, actions: HomeActions) {
    Column {
        SectionHeader(stringResource(R.string.home_pinned))
        ConnectedGroup {
            layout.pinned.forEach { key ->
                when (key.kind) {
                    PinKey.Kind.PAGE -> DevicePage.fromId(key.id)?.let { page ->
                        PinnedRow(
                            label = page.label,
                            badgeKey = page.id,
                            onOpen = { actions.onOpenPage(page) },
                            onUnpin = { actions.onTogglePin(key) }
                        )
                    }
                    PinKey.Kind.TOOL -> Tool.fromId(key.id)?.let { tool ->
                        PinnedRow(
                            label = tool.label,
                            badgeKey = tool.id,
                            onOpen = { actions.onOpenTool(tool) },
                            onUnpin = { actions.onTogglePin(key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenPagesRow(count: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.padding(top = 20.dp)) {
        ConnectedGroup {
            ConnectedRow(
                title = pluralStringResource(R.plurals.home_hidden_pages, count, count),
                supporting = stringResource(R.string.home_hidden_pages_summary),
                leading = {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = onClick
            )
        }
    }
}
