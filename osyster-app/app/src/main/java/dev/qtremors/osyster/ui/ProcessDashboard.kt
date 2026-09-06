package dev.qtremors.osyster.ui

import android.app.ActivityManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.monitor.AppStopperMonitor
import dev.qtremors.osyster.monitor.ProcessInfo
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.viewmodel.ProcessUiState
import dev.qtremors.osyster.ui.viewmodel.ProcessViewModel

// =========================================================================
// Section Comment: Active Processes Dashboard
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessDashboard(
    modifier: Modifier = Modifier,
    viewModel: ProcessViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.searchQuery.isNotEmpty()) {
        viewModel.setSearchQuery("")
    }

    ProcessDashboardContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::setSearchQuery,
        onRefresh = { viewModel.refreshProcesses() },
        onSelectProcess = viewModel::selectProcess,
        onSetGuidanceTarget = viewModel::setGuidanceTarget,
        onOpenAppInfo = viewModel::openAppInfo,
        onKillBackgroundProcesses = viewModel::killBackgroundProcesses,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessDashboardContent(
    uiState: ProcessUiState,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectProcess: (ProcessInfo?) -> Unit,
    onSetGuidanceTarget: (ProcessInfo?) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onKillBackgroundProcesses: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery = uiState.searchQuery
    val filteredProcesses = uiState.filteredProcesses
    val isRefreshing = uiState.isRefreshing
    val selectedProcess = uiState.selectedProcess
    val processGuidanceTarget = uiState.processGuidanceTarget

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.process_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Title and total process count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.process_active_tasks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.process_running_count, filteredProcesses.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Processes List
        Box(modifier = Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredProcesses.isEmpty() && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.process_no_matches), color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = LocalBottomContentPadding.current)
                    ) {
                        items(filteredProcesses, key = { it.pid }) { proc ->
                            Card(
                                onClick = { onSelectProcess(proc) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = proc.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.process_item_subtext, proc.pid, proc.user),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatRamKb(proc.ramKb),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Process details bottom sheet dialog
        selectedProcess?.let { proc ->
            ModalBottomSheet(
                onDismissRequest = { onSelectProcess(null) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.process_details_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.process_name), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                            Text(proc.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.process_pid), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                            Text("${proc.pid}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.process_resident_memory), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                            Text(formatRamKb(proc.ramKb), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.process_user_context), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                            Text(proc.user, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val targetPackage = proc.name.substringBefore(':')
                    val isAppPackage = targetPackage.contains('.')

                    if (isAppPackage) {
                        Button(
                            onClick = {
                                onOpenAppInfo(targetPackage)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.app_stopper_action_force_stop), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }

                        Button(
                            onClick = {
                                onKillBackgroundProcesses(targetPackage)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100)
                        ) {
                            Text(stringResource(R.string.process_kill_background), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Direct process termination action with guidance
                    OutlinedButton(
                        onClick = {
                            onSetGuidanceTarget(proc)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(100)
                    ) {
                        Text(stringResource(R.string.process_terminate_button, proc.pid), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Process termination restriction and guidance dialog
        processGuidanceTarget?.let { proc ->
            val guidancePkg = proc.name.substringBefore(':')
            val isApp = guidancePkg.contains('.')
            AlertDialog(
                onDismissRequest = { onSetGuidanceTarget(null) },
                title = {
                    Text(stringResource(R.string.process_term_restricted_title), fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        stringResource(
                            if (isApp) R.string.process_term_restricted_desc_app else R.string.process_term_restricted_desc,
                            proc.pid
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { onSetGuidanceTarget(null) }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = if (isApp) {
                    {
                        TextButton(
                            onClick = {
                                onOpenAppInfo(guidancePkg)
                            }
                        ) {
                            Text(stringResource(R.string.process_open_app_info))
                        }
                    }
                } else null
            )
        }
    }
}

private fun formatRamKb(kb: Long): String {
    return if (kb > 1024) {
        String.format(Locale.getDefault(), "%.1f MB", kb.toFloat() / 1024f)
    } else {
        "$kb KB"
    }
}

@Preview(showBackground = true)
@Composable
private fun ProcessDashboardPreview() {
    OsysterTheme {
        ProcessDashboardContent(
            uiState = ProcessUiState(
                rawProcesses = listOf(
                    ProcessInfo(1234, "system_server", 245000L, "system"),
                    ProcessInfo(5678, "com.android.chrome", 185000L, "u0_a123"),
                    ProcessInfo(9012, "dev.qtremors.osyster", 62000L, "u0_a245")
                ),
                isLoading = false,
                isRefreshing = false,
                searchQuery = ""
            ),
            onSearchQueryChange = {},
            onRefresh = {},
            onSelectProcess = {},
            onSetGuidanceTarget = {},
            onOpenAppInfo = {},
            onKillBackgroundProcesses = {}
        )
    }
}
