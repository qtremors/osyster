@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.apps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Terminal
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
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.settings.OsysterPreferencesState
import dev.qtremors.osyster.ui.screentime.ScreenTimeDashboard
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import kotlinx.coroutines.launch

@Composable
fun AppsDashboard(
    prefsState: OsysterPreferencesState,
    manager: OsysterPreferencesManager,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    hapticEnabled: Boolean = true
) {
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, 2),
        pageCount = { 3 }
    )
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(initialTab) {
        val target = initialTab.coerceIn(0, 2)
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

    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
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
                Triple(0, Icons.Default.PowerSettingsNew, stringResource(R.string.apps_tab_app_stopper)),
                Triple(1, Icons.Default.QueryStats, stringResource(R.string.screentime_title)),
                Triple(2, Icons.Default.Terminal, stringResource(R.string.apps_tab_running_processes))
            )

            tabs.forEachIndexed { index, (_, iconVector, labelText) ->
                val isSelected = pagerState.currentPage == index
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = {
                        if (pagerState.currentPage != index) {
                            OsysterHapticUtil.performTick(view, hapticEnabled)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { targetPage ->
            when (targetPage) {
                0 -> AppStopperDashboard(
                    prefsState = prefsState,
                    manager = manager,
                    onNavigateBack = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> ScreenTimeDashboard(modifier = Modifier.fillMaxSize())
                2 -> ProcessDashboard(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
