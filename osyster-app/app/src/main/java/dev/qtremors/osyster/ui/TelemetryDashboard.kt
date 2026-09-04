package dev.qtremors.osyster.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.ui.util.OsysterHapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryDashboard(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    hapticEnabled: Boolean = true
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val view = LocalView.current

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = {
                        if (selectedTab != 0) {
                            OsysterHapticUtil.performTick(view, hapticEnabled)
                            selectedTab = 0
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("CPU Telemetry")
                }

                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = {
                        if (selectedTab != 1) {
                            OsysterHapticUtil.performTick(view, hapticEnabled)
                            selectedTab = 1
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("RAM & Swap")
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
