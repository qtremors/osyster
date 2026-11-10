package dev.qtremors.osyster.feature.home.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.feature.home.R
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.IconBadge

/**
 * Search results shown in place of Home's content while the top-bar search is open. Before
 * anything is typed it suggests common places; after, it lists matches grouped by kind.
 */
@Composable
internal fun SearchResults(query: String, facts: List<FactHit>, padding: PaddingValues, onOpen: (SearchTarget) -> Unit) {
    val entries = rememberEntries(facts)
    val results = remember(query, entries) { searchEntries(query, entries) }
    val icons = remember(entries) { entryIcons() }
    LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
        when {
            query.isBlank() -> item(key = "suggested") {
                val suggested = SUGGESTED.mapNotNull { key -> entries.firstOrNull { it.key == key } }
                ResultGroups(suggested, icons, onOpen, header = stringResource(R.string.search_suggested))
            }
            results.isEmpty() -> item(key = "none") {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = stringResource(R.string.search_none_title),
                    message = stringResource(R.string.search_none_message, query)
                )
            }
            else -> item(key = "results") { ResultGroups(results, icons, onOpen) }
        }
    }
}

@Composable
private fun ResultGroups(results: List<SearchEntry>, icons: Map<String, ImageVector>, onOpen: (SearchTarget) -> Unit, header: String? = null) {
    val byKind = if (header != null) mapOf(SearchKind.PAGE to results) else results.groupBy(SearchEntry::kind)
    Column(modifier = Modifier.padding(top = 8.dp)) {
        SearchKind.entries.forEach { kind ->
            val group = byKind[kind] ?: return@forEach
            Text(
                text = header ?: stringResource(kind.title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            ConnectedGroup {
                group.forEach { entry ->
                    ConnectedRow(
                        title = entry.title,
                        supporting = entry.detail.ifBlank { null },
                        leading = { IconBadge(icons[entry.key] ?: Icons.Rounded.Description, key = entry.target.badgeKey()) },
                        onClick = { onOpen(entry.target) }
                    )
                }
            }
        }
    }
}

/** Pages, tools, and settings with their localized names, plus every collected device fact. */
@Composable
private fun rememberEntries(facts: List<FactHit>): List<SearchEntry> {
    val resources = LocalResources.current
    return remember(facts, resources) {
        val pageTitles = DevicePage.entries.associate { it.id to resources.getString(it.label.title) }
        buildList {
            DevicePage.entries.forEach { page ->
                add(SearchEntry("page:${page.id}", SearchKind.PAGE, resources.getString(page.label.title), resources.getString(page.label.summary), SearchTarget.Page(page.id)))
            }
            Tool.entries.filter(OPEN_TOOLS::contains).forEach { tool ->
                add(SearchEntry("tool:${tool.id}", SearchKind.TOOL, resources.getString(tool.label.title), resources.getString(tool.label.summary), SearchTarget.Tool(tool.id)))
            }
            SETTINGS.forEach { setting ->
                add(SearchEntry("setting:${setting.key}", SearchKind.SETTING, resources.getString(setting.title), resources.getString(setting.detail), SearchTarget.Setting(setting.place)))
            }
            facts.forEach { fact ->
                val detail = listOfNotNull(fact.value, pageTitles[fact.pageId]).joinToString(" · ")
                add(SearchEntry("fact:${fact.key}", SearchKind.FACT, fact.label, detail, SearchTarget.Page(fact.pageId)))
            }
        }
    }
}

private fun entryIcons(): Map<String, ImageVector> = buildMap {
    DevicePage.entries.forEach { put("page:${it.id}", it.label.icon) }
    Tool.entries.forEach { put("tool:${it.id}", it.label.icon) }
    SETTINGS.forEach { put("setting:${it.key}", it.icon) }
}

private fun SearchTarget.badgeKey(): String = when (this) {
    is SearchTarget.Page -> pageId
    is SearchTarget.Tool -> toolId
    is SearchTarget.Setting -> place.name
}

private val SearchKind.title: Int
    get() = when (this) {
        SearchKind.PAGE -> R.string.search_kind_pages
        SearchKind.TOOL -> R.string.search_kind_tools
        SearchKind.SETTING -> R.string.search_kind_settings
        SearchKind.FACT -> R.string.search_kind_facts
    }

private data class SettingEntry(val key: String, val title: Int, val detail: Int, val icon: ImageVector, val place: SettingsPlace)

private val SETTINGS = listOf(
    SettingEntry("theme", R.string.search_setting_theme, R.string.search_setting_theme_detail, Icons.Rounded.Palette, SettingsPlace.SETTINGS),
    SettingEntry("feel", R.string.search_setting_feel, R.string.search_setting_feel_detail, Icons.Rounded.Vibration, SettingsPlace.SETTINGS),
    SettingEntry("backup", R.string.search_setting_backup, R.string.search_setting_backup_detail, Icons.Rounded.Settings, SettingsPlace.SETTINGS),
    SettingEntry("about", R.string.search_setting_about, R.string.search_setting_about_detail, Icons.Rounded.Info, SettingsPlace.ABOUT),
    SettingEntry("privacy", R.string.search_setting_privacy, R.string.search_setting_privacy_detail, Icons.Rounded.Lock, SettingsPlace.PRIVACY),
    SettingEntry("licenses", R.string.search_setting_licenses, R.string.search_setting_licenses_detail, Icons.Rounded.Gavel, SettingsPlace.LICENSES)
)

/** Tools that already have their own page; the rest still show a placeholder, so search skips them. */
private val OPEN_TOOLS = setOf(
    Tool.DATA_USAGE, Tool.SCREEN_USAGE, Tool.APP_STOPPER, Tool.NOTIFICATION_HISTORY, Tool.BATTERY_HISTORY, Tool.STAY_AWAKE, Tool.FLASHLIGHT, Tool.BT_REMOTE, Tool.BARCODE,
    Tool.ACTIVITY_LAUNCHER, Tool.LINK_ACTIONS, Tool.QUICK_TILES, Tool.WIDGETS,
    Tool.POWER_SETUP, Tool.POWER_SAVING, Tool.DISPLAY_SCALE, Tool.STATUS_BAR_ICONS, Tool.SYSTEM_TWEAKS,
    Tool.SHUT_UP, Tool.STANDBY_APPS
)

/** Shown before anything is typed: the places people open most. */
private val SUGGESTED = listOf(
    "page:battery", "page:processor", "page:memory", "tool:data_usage",
    "tool:screen_usage", "tool:stay_awake", "tool:flashlight", "setting:theme"
)
