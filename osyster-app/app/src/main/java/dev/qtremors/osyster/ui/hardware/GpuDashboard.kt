@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.hardware

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun GpuDashboard(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsVisibleState()
    val view = LocalView.current
    val gpu = uiState.gpuSpecs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // GPU Hardware Identity
        Text(
            text = stringResource(R.string.bento_gpu_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.gpu_vendor), gpu.vendor, InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.gpu_renderer), gpu.renderer, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.gpu_gles_version), gpu.glEsVersion, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.gpu_vulkan_version), gpu.vulkanVersion, InfoGroupPosition.Bottom)
        }

        // SoC Processor & Graphics Platform
        Text(
            text = stringResource(R.string.device_processor_platform),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.device_processor_platform), uiState.systemSpecs.hardware, InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.device_board_hardware), uiState.systemSpecs.board, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.feature_vulkan), if (uiState.displaySpecs.hasVulkan) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Bottom)
        }

        // SoC Core Clusters Topology
        if (uiState.cpuClusters.isNotEmpty()) {
            Text(
                text = stringResource(R.string.soc_clusters_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            uiState.cpuClusters.forEach { cluster ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.soc_cluster_item, cluster.clusterId, cluster.coreCount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.soc_cluster_cores, cluster.coreRange),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        val minMhz = if (cluster.minFreqKhz > 0) "${cluster.minFreqKhz / 1000} MHz" else "N/A"
                        val maxMhz = if (cluster.maxFreqKhz > 0) "${cluster.maxFreqKhz / 1000} MHz" else "N/A"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.soc_cluster_freq_range), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            Text("$minMhz - $maxMhz", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.soc_cluster_governor), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            Text(cluster.governor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}
