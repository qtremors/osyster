package dev.qtremors.osyster.ui

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.monitor.AppStopperMonitor
import dev.qtremors.osyster.monitor.ProcessInfo
import dev.qtremors.osyster.monitor.SystemMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// =========================================================================
// Section Comment: Active Processes Dashboard
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessDashboard(modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = searchQuery.isNotEmpty()) {
        searchQuery = ""
    }
    var rawProcesses by remember { mutableStateOf<List<ProcessInfo>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedProcess by remember { mutableStateOf<ProcessInfo?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filteredProcesses = remember(rawProcesses, searchQuery) {
        if (searchQuery.isBlank()) {
            rawProcesses
        } else {
            rawProcesses.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.pid.toString() == searchQuery
            }
        }
    }

    // Refresh processes list
    val refreshProcesses = {
        isRefreshing = true
        scope.launch(Dispatchers.IO) {
            val list = SystemMonitor.getActiveProcesses()
            withContext(Dispatchers.Main) {
                rawProcesses = list
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshProcesses()
        // Automatically poll processes every 5 seconds
        while (true) {
            delay(5000)
            if (!isRefreshing) {
                val list = withContext(Dispatchers.IO) { SystemMonitor.getActiveProcesses() }
                rawProcesses = list
            }
        }
    }

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
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search process by name or PID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
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
                text = "Active Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${filteredProcesses.size} running",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Processes List
        Box(modifier = Modifier.weight(1f)) {
            if (filteredProcesses.isEmpty() && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching processes found", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredProcesses, key = { it.pid }) { proc ->
                        Card(
                            onClick = { selectedProcess = proc },
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
                                        text = "PID: ${proc.pid}",
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

            if (isRefreshing) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }

        // Process details bottom sheet dialog
        selectedProcess?.let { proc ->
            ModalBottomSheet(
                onDismissRequest = { selectedProcess = null },
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
                            text = "Process Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Name / Command", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                            Text(proc.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 2, modifier = Modifier.padding(start = 16.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Process ID (PID)", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                            Text(proc.pid.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Resident Memory", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                            Text(formatRamKb(proc.ramKb), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("User Context", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                            Text(proc.user, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val targetPackage = proc.name.substringBefore(':')
                    val isAppPackage = targetPackage.contains('.')

                    if (isAppPackage) {
                        Button(
                            onClick = {
                                AppStopperMonitor.openAppInfo(context, targetPackage)
                                selectedProcess = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Force Stop (App Info)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    // End process / force kill action button
                    OutlinedButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val success = killProcessByPid(proc.pid)
                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(context, "Sigkill sent to PID ${proc.pid}", Toast.LENGTH_SHORT).show()
                                        selectedProcess = null
                                        refreshProcesses()
                                    } else {
                                        Toast.makeText(context, "Failed to terminate PID ${proc.pid}. Permission denied.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(100)
                    ) {
                        Text("Send Sigkill (PID ${proc.pid})", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
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

private fun killProcessByPid(pid: Int): Boolean {
    return try {
        val process = Runtime.getRuntime().exec("kill -9 $pid")
        process.waitFor()
        process.exitValue() == 0
    } catch (_: Exception) {
        false
    }
}
