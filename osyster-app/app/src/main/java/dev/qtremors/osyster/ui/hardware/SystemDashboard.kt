@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.hardware

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.qtremors.osyster.R
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import dev.qtremors.osyster.ui.util.collectAsVisibleState
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoViewModel

@Composable
fun SystemDashboard(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsVisibleState()
    val view = LocalView.current
    val systemSpecs = uiState.systemSpecs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Android Platform Information
        Text(
            text = stringResource(R.string.device_android_specs),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.device_android_version), systemSpecs.androidVersion, InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.device_api_level), systemSpecs.apiLevel, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_released_with), systemSpecs.releasedWith, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_play_update), systemSpecs.playUpdate, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_security_patch), systemSpecs.securityPatch, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_build_number), systemSpecs.buildNumber, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_board_hardware), systemSpecs.board, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_processor_platform), systemSpecs.hardware, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_kernel), systemSpecs.kernelVersion, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_baseband), systemSpecs.basebandVersion, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_bootloader_release), systemSpecs.bootloader, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_build_fingerprint), systemSpecs.buildFingerprint, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_build_type), systemSpecs.buildType, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_build_tags), systemSpecs.buildTags, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_performance_class), systemSpecs.performanceClass, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_min_target_sdk), systemSpecs.minTargetSdk, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_language), systemSpecs.language, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_uptime), systemSpecs.uptime, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_usb_debugging), if (systemSpecs.isUsbDebugging) stringResource(R.string.enabled) else stringResource(R.string.disabled), InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_selinux), systemSpecs.selinuxStatus, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_root), systemSpecs.rootStatus, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_emulator), if (systemSpecs.isEmulator) stringResource(R.string.yes) else stringResource(R.string.no), InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_page_size), "${systemSpecs.pageSizeKb} KB", InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_treble), if (systemSpecs.isTrebleEnabled) stringResource(R.string.enabled) else stringResource(R.string.disabled), InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_seamless_updates), if (systemSpecs.seamlessUpdates) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.system_active_slot), systemSpecs.activeSlot, InfoGroupPosition.Bottom)
        }

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}
