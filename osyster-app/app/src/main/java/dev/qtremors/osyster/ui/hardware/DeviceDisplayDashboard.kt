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
fun DeviceDisplayDashboard(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsVisibleState()
    val view = LocalView.current
    val displaySpecs = uiState.displaySpecs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Hardware Identity
        Text(
            text = stringResource(R.string.device_hardware_specs),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.device_brand), displaySpecs.brand, InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.device_manufacturer), displaySpecs.manufacturer, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_model), displaySpecs.model, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_product), displaySpecs.product, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_sku), displaySpecs.sku, InfoGroupPosition.Bottom)
        }

        // Display Specs
        Text(
            text = stringResource(R.string.display_section_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.display_name), displaySpecs.displayName, InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.display_resolution), displaySpecs.resolution, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.display_size), displaySpecs.diagonalInches, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.display_aspect_ratio), displaySpecs.aspectRatio, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.display_refresh_rate), displaySpecs.refreshRateHz, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.display_supported_refresh_rates), displaySpecs.supportedRefreshRates, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.display_pixel_density), displaySpecs.pixelDensityDpi, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.display_density_bucket), displaySpecs.densityBucket, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.display_orientation), displaySpecs.defaultOrientation, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.display_hdr), if (displaySpecs.hdrSupported) displaySpecs.hdrFormats else stringResource(R.string.not_supported), InfoGroupPosition.Bottom)
        }

        // System Capabilities & Security Features
        Text(
            text = stringResource(R.string.features_section_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.feature_fingerprint), if (displaySpecs.hasFingerprint) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.feature_face_auth), if (displaySpecs.hasFaceAuth) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.feature_encryption), if (displaySpecs.hasEncryption) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Bottom)
        }

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}
