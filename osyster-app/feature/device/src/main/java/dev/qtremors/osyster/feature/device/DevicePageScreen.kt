package dev.qtremors.osyster.feature.device

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.ui.R as UiR
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.InfoSectionBlock
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import kotlinx.coroutines.launch

@Composable
internal fun DevicePageScreen(
    page: DevicePage?,
    state: DevicePageState,
    onOpenBatteryUsage: () -> Unit
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val copiedTemplate = stringResource(UiR.string.copied_value)
    val onCopy: (String, String) -> Unit = { label, value ->
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, value)))
            // Android 13+ confirms copies itself; a second message would be noise.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                snackbar.showSnackbar(copiedTemplate.format(label))
            }
        }
    }

    OsysterScaffold(
        title = page?.label?.title?.let { stringResource(it) }.orEmpty(),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            when (state) {
                DevicePageState.Loading -> item(key = "loading") { LoadingState() }
                DevicePageState.NotBuilt -> item(key = "not_built") {
                    EmptyState(
                        icon = Icons.Rounded.Construction,
                        title = stringResource(UiR.string.not_built_title),
                        message = stringResource(UiR.string.not_built_message)
                    )
                }
                DevicePageState.UnknownPage -> item(key = "unknown") {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.device_unknown_title),
                        message = stringResource(R.string.device_unknown_message)
                    )
                }
                is DevicePageState.Ready -> {
                    items(state.sections, key = { it.key }) { section ->
                        InfoSectionBlock(section = section, onCopy = onCopy)
                    }
                    if (page == DevicePage.BATTERY) {
                        item(key = "battery_usage") { BatteryUsageLink(onOpenBatteryUsage) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryUsageLink(onClick: () -> Unit) {
    Column {
        SectionHeader(stringResource(R.string.battery_usage_section))
        ConnectedGroup {
            NavigationRow(
                title = stringResource(R.string.battery_usage_title),
                supporting = stringResource(R.string.battery_usage_summary),
                icon = Icons.Rounded.BatteryChargingFull,
                badgeKey = "battery_usage",
                onClick = onClick
            )
        }
    }
}
