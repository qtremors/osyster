@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.hardware

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
fun DrmDashboard(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsVisibleState()
    val view = LocalView.current
    val drm = uiState.mediaDrmSpecs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Widevine Hero Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.drm_widevine),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = drm.widevineSecurityLevel.ifBlank { stringResource(R.string.not_supported) },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Widevine Details
        Text(
            text = stringResource(R.string.drm_widevine),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.drm_vendor), drm.widevineVendor, InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.drm_version), drm.widevineVersion, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.drm_system_id), drm.widevineSystemId, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.drm_algorithms), drm.widevineAlgorithms, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.drm_hdcp_level), drm.widevineHdcpLevel, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.drm_max_hdcp), drm.widevineMaxHdcpLevel, InfoGroupPosition.Bottom)
        }

        // ClearKey DRM
        if (drm.clearkeyVendor.isNotBlank() || drm.clearkeyVersion.isNotBlank()) {
            Text(
                text = stringResource(R.string.drm_clearkey),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.drm_vendor), drm.clearkeyVendor, InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.drm_version), drm.clearkeyVersion, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.drm_hdcp_level), drm.clearkeyHdcpLevel, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.drm_max_hdcp), drm.clearkeyMaxHdcpLevel, InfoGroupPosition.Bottom)
            }
        }

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}
