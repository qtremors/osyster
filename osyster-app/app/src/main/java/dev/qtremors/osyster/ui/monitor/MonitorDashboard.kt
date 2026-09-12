@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.monitor

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.R
import dev.qtremors.osyster.ui.util.LocalTelemetryVisible
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import kotlinx.coroutines.launch

@Composable
fun MonitorDashboard(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    hapticEnabled: Boolean = true
) {
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, 3),
        pageCount = { 4 }
    )
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(initialTab) {
        val target = initialTab.coerceIn(0, 3)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(pagerState) {
        var isFirst = true
        snapshotFlow { pagerState.currentPage }
            .collect {
                if (isFirst) {
                    isFirst = false
                } else {
                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Material 3 Expressive Connected Button Group (4 Tabs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val tabs = listOf(
                Triple(0, Icons.Default.Speed, stringResource(R.string.telemetry_tab_cpu)),
                Triple(1, Icons.Default.Memory, stringResource(R.string.telemetry_tab_ram)),
                Triple(2, Icons.Default.Wifi, stringResource(R.string.telemetry_tab_network)),
                Triple(3, Icons.Default.BatteryChargingFull, stringResource(R.string.telemetry_tab_battery))
            )

            tabs.forEachIndexed { index, (_, iconVector, labelText) ->
                val isSelected = pagerState.currentPage == index
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = {
                        if (!isSelected) {
                            OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.Tab },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = labelText,
                        maxLines = 1
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { targetPage ->
            CompositionLocalProvider(LocalTelemetryVisible provides (LocalTelemetryVisible.current && targetPage == pagerState.currentPage)) {
                when (targetPage) {
                    0 -> CpuDashboard(modifier = Modifier.fillMaxSize())
                    1 -> MemoryDashboard(modifier = Modifier.fillMaxSize())
                    2 -> NetworkDashboard(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateBack = null,
                        hapticEnabled = hapticEnabled
                    )
                    3 -> BatteryDashboard(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Deprecated(
    message = "Use MonitorDashboard instead",
    replaceWith = ReplaceWith("MonitorDashboard(modifier, initialTab, hapticEnabled)")
)
@Composable
fun TelemetryDashboard(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    hapticEnabled: Boolean = true
) {
    MonitorDashboard(
        modifier = modifier,
        initialTab = initialTab,
        hapticEnabled = hapticEnabled
    )
}

