package dev.qtremors.osyster.feature.tools

import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.PushPin
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
import dev.qtremors.osyster.core.privilege.domain.PrivilegeStatus
import dev.qtremors.osyster.core.privilege.domain.ShellRoute
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.qtremors.osyster.core.ui.R as UiR
import dev.qtremors.osyster.core.ui.catalog.help
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.catalog.title
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.HelpIcon
import dev.qtremors.osyster.core.ui.components.HelpSheet
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
        val access by viewModel.access.collectAsStateWithLifecycle()
        LifecycleResumeEffect(Unit) {
            viewModel.refreshAccess()
            onPauseOrDispose { }
        }
        ToolsScreen(
            pinned = pinned,
            access = access,
            onOpen = { tool -> onNavigate(ToolsDestination.OpenTool(tool.id)) },
            onTogglePin = viewModel::togglePin
        )
    }
}

private val toolGroups: List<Pair<ToolGroup, List<Tool>>> = Tool.entries.groupBy(Tool::group).toList()

@Composable
internal fun ToolsScreen(pinned: Set<String>, access: PrivilegeStatus, onOpen: (Tool) -> Unit, onTogglePin: (Tool) -> Unit) {
    OsysterScaffold(title = stringResource(R.string.tools_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            items(toolGroups, key = { (group, _) -> group.name }) { (group, tools) ->
                Column {
                    SectionHeader(stringResource(group.title))
                    ConnectedGroup {
                        tools.forEach { tool ->
                            ToolRow(tool = tool, pinned = tool.id in pinned, access = access, onOpen = onOpen, onTogglePin = onTogglePin)
                        }
                    }
                }
            }
        }
    }
}

/** Long-press a tool to pin it to Home or read what it does. */
@Composable
private fun ToolRow(tool: Tool, pinned: Boolean, access: PrivilegeStatus, onOpen: (Tool) -> Unit, onTogglePin: (Tool) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    val label = tool.label
    val summary = stringResource(label.summary)
    val needs = accessLine(tool.access)
    // Only say what a tool needs while it is missing; once unlocked the line would be noise.
    val missing = when (tool.access) {
        ToolAccess.NONE -> false
        ToolAccess.SECURE_SETTINGS -> !access.canWriteSettings
        ToolAccess.SHIZUKU -> !access.shell
    }
    // Once something is unlocked, Power setup shows what instead of how.
    val unlocked = if (tool == Tool.POWER_SETUP) unlockedLine(access) else null
    val extra = if (missing) needs else null
    Box {
        NavigationRow(
            title = stringResource(label.title),
            supporting = if (unlocked != null) unlocked else if (extra == null) summary else summary + "\n" + extra,
            icon = label.icon,
            badgeKey = tool.id,
            onClick = { onOpen(tool) },
            onLongClick = { menuOpen = true }
        )
        OsysterMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            groups = listOf(
                listOf(
                    MenuAction(
                        stringResource(if (pinned) R.string.tools_unpin else R.string.tools_pin),
                        if (pinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin
                    ) { onTogglePin(tool) }
                ),
                listOf(MenuAction(stringResource(UiR.string.help_about_tool), HelpIcon) { helpOpen = true })
            )
        )
    }
    if (helpOpen) HelpSheet(label = label, help = tool.help, badgeKey = tool.id, onDismiss = { helpOpen = false }, needs = needs)
}

/** What Power setup has unlocked so far, or null when nothing is. */
@Composable
private fun unlockedLine(access: PrivilegeStatus): String? = when {
    access.shellRoute == ShellRoute.SHIZUKU -> stringResource(R.string.tools_access_shizuku)
    access.shellRoute == ShellRoute.ROOT -> stringResource(R.string.tools_access_root)
    access.secureSettings -> stringResource(R.string.tools_access_settings)
    else -> null
}

/** Power tools say what unlocks them, so nobody taps into a page they cannot use. */
@Composable
private fun accessLine(access: ToolAccess): String? = when (access) {
    ToolAccess.NONE -> null
    ToolAccess.SECURE_SETTINGS -> stringResource(UiR.string.access_secure_settings)
    ToolAccess.SHIZUKU -> stringResource(UiR.string.access_shizuku)
}
