package dev.qtremors.osyster.feature.tools

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.navigation.ToolAccess
import dev.qtremors.osyster.core.navigation.ToolGroup
import dev.qtremors.osyster.core.navigation.ToolsRoute
import dev.qtremors.osyster.core.ui.R as UiR
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.catalog.title
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

/** Where the Tools tab wants to go next. The app shell maps these to routes. */
sealed interface ToolsDestination {
    data class OpenTool(val toolId: String) : ToolsDestination
}

fun NavGraphBuilder.registerToolsRoute(onNavigate: (ToolsDestination) -> Unit) {
    composable<ToolsRoute> {
        ToolsScreen(onOpen = { tool -> onNavigate(ToolsDestination.OpenTool(tool.id)) })
    }
}

private val toolGroups: List<Pair<ToolGroup, List<Tool>>> = Tool.entries.groupBy(Tool::group).toList()

@Composable
internal fun ToolsScreen(onOpen: (Tool) -> Unit) {
    OsysterScaffold(title = stringResource(R.string.tools_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            items(toolGroups, key = { (group, _) -> group.name }) { (group, tools) ->
                SectionHeader(stringResource(group.title))
                ConnectedGroup {
                    tools.forEach { tool ->
                        val label = tool.label
                        NavigationRow(
                            title = stringResource(label.title),
                            supporting = accessLine(tool.access)?.let { access ->
                                stringResource(label.summary) + "\n" + access
                            } ?: stringResource(label.summary),
                            icon = label.icon,
                            badgeKey = tool.id,
                            onClick = { onOpen(tool) }
                        )
                    }
                }
            }
        }
    }
}

/** Power tools say what unlocks them, so nobody taps into a page they cannot use. */
@Composable
private fun accessLine(access: ToolAccess): String? = when (access) {
    ToolAccess.NONE -> null
    ToolAccess.SECURE_SETTINGS -> stringResource(UiR.string.access_secure_settings)
    ToolAccess.SHIZUKU -> stringResource(UiR.string.access_shizuku)
}
