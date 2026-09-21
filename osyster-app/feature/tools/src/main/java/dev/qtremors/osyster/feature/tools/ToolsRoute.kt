package dev.qtremors.osyster.feature.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        val viewModel: ToolsViewModel = hiltViewModel()
        val pinned by viewModel.pinnedTools.collectAsStateWithLifecycle()
        ToolsScreen(
            pinned = pinned,
            onOpen = { tool -> onNavigate(ToolsDestination.OpenTool(tool.id)) },
            onTogglePin = viewModel::togglePin
        )
    }
}

private val toolGroups: List<Pair<ToolGroup, List<Tool>>> = Tool.entries.groupBy(Tool::group).toList()

@Composable
internal fun ToolsScreen(pinned: Set<String>, onOpen: (Tool) -> Unit, onTogglePin: (Tool) -> Unit) {
    OsysterScaffold(title = stringResource(R.string.tools_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            items(toolGroups, key = { (group, _) -> group.name }) { (group, tools) ->
                Column {
                    SectionHeader(stringResource(group.title))
                    ConnectedGroup {
                        tools.forEach { tool ->
                            ToolRow(tool = tool, pinned = tool.id in pinned, onOpen = onOpen, onTogglePin = onTogglePin)
                        }
                    }
                }
            }
        }
    }
}

/** Long-press a tool to pin it to Home. */
@Composable
private fun ToolRow(tool: Tool, pinned: Boolean, onOpen: (Tool) -> Unit, onTogglePin: (Tool) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val label = tool.label
    val summary = stringResource(label.summary)
    val access = accessLine(tool.access)
    Box {
        NavigationRow(
            title = stringResource(label.title),
            supporting = if (access == null) summary else summary + "\n" + access,
            icon = label.icon,
            badgeKey = tool.id,
            onClick = { onOpen(tool) },
            onLongClick = { menuOpen = true }
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (pinned) R.string.tools_unpin else R.string.tools_pin)) },
                leadingIcon = { Icon(if (pinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onTogglePin(tool)
                }
            )
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
