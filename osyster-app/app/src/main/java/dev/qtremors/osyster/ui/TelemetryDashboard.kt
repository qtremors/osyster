@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.R
import dev.qtremors.osyster.ui.util.OsysterHapticUtil

@Composable
fun TelemetryDashboard(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    hapticEnabled: Boolean = true
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val view = LocalView.current

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Material 3 Expressive Connected Button Group
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val tabs = listOf(
                Triple(0, Icons.Default.Speed, stringResource(R.string.telemetry_tab_cpu)),
                Triple(1, Icons.Default.Memory, stringResource(R.string.telemetry_tab_ram))
            )

            tabs.forEachIndexed { index, (_, iconVector, labelText) ->
                val isSelected = selectedTab == index
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = {
                        if (selectedTab != index) {
                            OsysterHapticUtil.performTick(view, hapticEnabled)
                            selectedTab = index
                        }
                    },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = labelText,
                        maxLines = 1,
                    )
                }
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "telemetry_content_transition",
            modifier = Modifier.fillMaxSize()
        ) { targetPage ->
            when (targetPage) {
                0 -> CpuDashboard(modifier = Modifier.fillMaxSize())
                1 -> MemoryDashboard(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
