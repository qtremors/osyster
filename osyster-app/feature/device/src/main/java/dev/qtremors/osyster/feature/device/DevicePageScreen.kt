package dev.qtremors.osyster.feature.device

import dev.qtremors.osyster.core.ui.components.FilterTop
import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.RowAction
import dev.qtremors.osyster.core.device.domain.SensorReading
import dev.qtremors.osyster.core.ui.R as UiR
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.InfoSectionBlock
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
internal fun DevicePageScreen(
    page: DevicePage?,
    state: DevicePageState,
    readings: (String) -> Flow<SensorReading>,
    onNavigate: (DeviceDestination) -> Unit
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var liveSensor by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    val copiedTemplate = stringResource(UiR.string.copied_value)
    val deniedMessage = stringResource(R.string.permission_denied)
    val openSettingsLabel = stringResource(R.string.permission_open_settings)
    val requestPermission = rememberPermissionRequester { granted ->
        if (!granted) {
            scope.launch {
                val result = snackbar.showSnackbar(deniedMessage, actionLabel = openSettingsLabel, withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) onNavigate(DeviceDestination.AppPermissionSettings)
            }
        }
    }
    val onCopy: (String, String) -> Unit = { label, value ->
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, value)))
            // Android 13+ confirms copies itself; a second message would be noise.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) snackbar.showSnackbar(copiedTemplate.format(label))
        }
    }

    OsysterScaffold(
        title = page?.label?.title?.let { stringResource(it) }.orEmpty(),
        snackbarHost = { SnackbarHost(snackbar) },
        // The same top-bar search as Home, scoped to this page's rows.
        filter = if (state is DevicePageState.Ready) {
            FilterTop(query, { query = it }, stringResource(R.string.search_page_placeholder), onOpenChange = { searching = it })
        } else null
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            when (state) {
                DevicePageState.Loading -> item(key = "loading") { LoadingState() }
                DevicePageState.NotBuilt -> item(key = "not_built") {
                    EmptyState(Icons.Rounded.Construction, stringResource(UiR.string.not_built_title), stringResource(UiR.string.not_built_message))
                }
                DevicePageState.UnknownPage -> item(key = "unknown") {
                    EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.device_unknown_title), stringResource(R.string.device_unknown_message))
                }
                is DevicePageState.Ready -> {
                    if (page == DevicePage.DISPLAY && !searching) {
                        item(key = "tester") {
                            PageLink(R.string.screen_tester_section, R.string.screen_tester_title, R.string.screen_tester_summary, Icons.Rounded.Gradient, "tester") {
                                onNavigate(DeviceDestination.ScreenTester)
                            }
                        }
                    }
                    val shown = filterSections(state.sections, query)
                    if (shown.isEmpty()) {
                        item(key = "no_matches") {
                            EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.search_no_matches_title), stringResource(R.string.search_no_matches_message, query.trim()))
                        }
                    }
                    items(shown, key = { it.key }) { section ->
                        InfoSectionBlock(
                            section = section,
                            onCopy = onCopy,
                            onRowAction = { action ->
                                when (action) {
                                    is RowAction.LiveSensor -> liveSensor = action.sensorKey
                                }
                            },
                            onGrant = requestPermission
                        )
                    }
                    if (page == DevicePage.BATTERY && !searching) {
                        item(key = "battery_history") {
                            PageLink(R.string.battery_history_section, R.string.battery_history_title, R.string.battery_history_summary, Icons.Rounded.Timeline, "battery_history") {
                                onNavigate(DeviceDestination.BatteryHistory)
                            }
                        }
                        item(key = "battery_usage") {
                            PageLink(R.string.battery_usage_section, R.string.battery_usage_title, R.string.battery_usage_summary, Icons.Rounded.BatteryChargingFull, "battery_usage") {
                                onNavigate(DeviceDestination.SystemBatteryUsage)
                            }
                        }
                    }
                }
            }
        }
    }
    liveSensor?.let { key ->
        SensorLiveSheet(readings = remember(key) { readings(key) }, onDismiss = { liveSensor = null })
    }
}

@Composable
private fun PageLink(section: Int, title: Int, summary: Int, icon: ImageVector, key: String, onClick: () -> Unit) {
    Column {
        SectionHeader(stringResource(section))
        ConnectedGroup {
            NavigationRow(
                title = stringResource(title),
                supporting = stringResource(summary),
                icon = icon,
                badgeKey = key,
                onClick = onClick
            )
        }
    }
}
