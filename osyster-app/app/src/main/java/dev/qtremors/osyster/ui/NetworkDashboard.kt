package dev.qtremors.osyster.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.R
import dev.qtremors.osyster.monitor.*
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Slate Tech color tokens for network telemetry
private val ColorCyanDownload = Color(0xFF00E6FF)
private val ColorEmeraldUpload = Color(0xFF00E676)
private val ColorAmberCellular = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkDashboard(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    hapticEnabled: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember { mutableStateOf(NetworkMonitor.hasUsageAccess(context)) }
    var hasPhonePermission by remember { mutableStateOf(NetworkMonitor.hasPhonePermission(context)) }
    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhonePermission = granted
    }

    var selectedInterval by remember { mutableStateOf(NetworkInterval.DAY) }
    var selectedFilter by remember { mutableStateOf(NetworkInterfaceFilter.ALL) }
    var intervalMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var targetDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var summary by remember { mutableStateOf(NetworkMonitor.emptySummary()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var selectedBucketIndex by remember { mutableStateOf<Int?>(null) }
    var selectedAppDetails by remember { mutableStateOf<AppNetworkUsage?>(null) }

    val realtimeSpeed by remember { NetworkMonitor.streamRealtimeSpeed() }
        .collectAsStateWithLifecycle(initialValue = RealtimeSpeed(0L, 0L))

    // Re-check permission when returning to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = NetworkMonitor.hasUsageAccess(context)
                hasPhonePermission = NetworkMonitor.hasPhonePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Refresh query when interval, filter, target date, or permission changes
    LaunchedEffect(selectedInterval, selectedFilter, targetDateMillis, hasPermission, hasPhonePermission) {
        if (!hasPermission) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        selectedBucketIndex = null
        val result = withContext(Dispatchers.IO) {
            NetworkMonitor.queryNetworkUsage(
                context = context,
                interval = selectedInterval,
                filter = selectedFilter,
                targetDateMillis = targetDateMillis
            )
        }
        summary = result
        isLoading = false
    }

    // Date formatting helper
    val dateLabel = remember(targetDateMillis, selectedInterval) {
        val cal = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
        when (selectedInterval) {
            NetworkInterval.DAY -> {
                val nowCal = Calendar.getInstance()
                if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
                ) {
                    "Today, " + SimpleDateFormat("d MMM", Locale.getDefault()).format(cal.time)
                } else {
                    SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(cal.time)
                }
            }
            NetworkInterval.WEEK -> {
                val endCal = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = targetDateMillis
                    add(Calendar.DAY_OF_YEAR, -6)
                }
                val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                "${fmt.format(startCal.time)} - ${fmt.format(endCal.time)}"
            }
            NetworkInterval.MONTH -> {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }
        }
    }

    val isCurrentPeriod = remember(targetDateMillis, selectedInterval) {
        val now = System.currentTimeMillis()
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calTarget = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
        when (selectedInterval) {
            NetworkInterval.DAY -> calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                    calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
            NetworkInterval.WEEK -> calTarget.timeInMillis >= now - 86400000L
            NetworkInterval.MONTH -> calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                    calNow.get(Calendar.MONTH) == calTarget.get(Calendar.MONTH)
        }
    }

    val filteredApps = remember(summary.apps, searchQuery) {
        if (searchQuery.isBlank()) {
            summary.apps
        } else {
            summary.apps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val maxAppBytes = remember(summary.apps) {
        summary.apps.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Top Header Row: Back & Title on Left, Real-Time Speed Pill on Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = {
                                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                                    onNavigateBack()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = stringResource(R.string.network_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Real-Time Speed Pill
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = ColorCyanDownload,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = NetworkMonitor.formatSpeed(realtimeSpeed.rxBytesPerSec),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorCyanDownload
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = ColorEmeraldUpload,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = NetworkMonitor.formatSpeed(realtimeSpeed.txBytesPerSec),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorEmeraldUpload
                            )
                        }
                    }
                }
            }

            // Permission Warning Prompt
            if (!hasPermission) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = stringResource(R.string.network_perm_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.network_perm_desc),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                                    NetworkMonitor.openUsageAccessSettings(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.network_perm_grant))
                            }
                        }
                    }
                }
            }

            // Chip Controls Row: Day Switcher on Left, Mobile/Data Filter on Right
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Day Switcher Chip (Day, Week, Month)
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = {
                                OsysterHapticUtil.performTick(view, hapticEnabled)
                                intervalMenuExpanded = true
                            },
                            label = {
                                Text(
                                    text = when (selectedInterval) {
                                        NetworkInterval.DAY -> stringResource(R.string.network_interval_day)
                                        NetworkInterval.WEEK -> stringResource(R.string.network_interval_week)
                                        NetworkInterval.MONTH -> stringResource(R.string.network_interval_month)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        DropdownMenu(
                            expanded = intervalMenuExpanded,
                            onDismissRequest = { intervalMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.network_interval_day)) },
                                onClick = {
                                    OsysterHapticUtil.performTick(view, hapticEnabled)
                                    selectedInterval = NetworkInterval.DAY
                                    intervalMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.network_interval_week)) },
                                onClick = {
                                    OsysterHapticUtil.performTick(view, hapticEnabled)
                                    selectedInterval = NetworkInterval.WEEK
                                    intervalMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.network_interval_month)) },
                                onClick = {
                                    OsysterHapticUtil.performTick(view, hapticEnabled)
                                    selectedInterval = NetworkInterval.MONTH
                                    intervalMenuExpanded = false
                                }
                            )
                        }
                    }

                    // Right: Mobile & Wi-Fi Filter Chip
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = {
                                OsysterHapticUtil.performTick(view, hapticEnabled)
                                filterMenuExpanded = true
                            },
                            label = {
                                Text(
                                    text = when (selectedFilter) {
                                        NetworkInterfaceFilter.ALL -> stringResource(R.string.network_filter_all)
                                        NetworkInterfaceFilter.MOBILE -> stringResource(R.string.network_filter_mobile)
                                        NetworkInterfaceFilter.WIFI -> stringResource(R.string.network_filter_wifi)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.network_filter_all)) },
                                onClick = {
                                    OsysterHapticUtil.performTick(view, hapticEnabled)
                                    selectedFilter = NetworkInterfaceFilter.ALL
                                    filterMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.network_filter_mobile)) },
                                onClick = {
                                    OsysterHapticUtil.performTick(view, hapticEnabled)
                                    selectedFilter = NetworkInterfaceFilter.MOBILE
                                    filterMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.network_filter_wifi)) },
                                onClick = {
                                    OsysterHapticUtil.performTick(view, hapticEnabled)
                                    selectedFilter = NetworkInterfaceFilter.WIFI
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Date Stepper Row (< Date >)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            OsysterHapticUtil.performTick(view, hapticEnabled)
                            val cal = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
                            when (selectedInterval) {
                                NetworkInterval.DAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
                                NetworkInterval.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
                                NetworkInterval.MONTH -> cal.add(Calendar.MONTH, -1)
                            }
                            targetDateMillis = cal.timeInMillis
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Period",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            if (!isCurrentPeriod) {
                                OsysterHapticUtil.performTick(view, hapticEnabled)
                                targetDateMillis = System.currentTimeMillis()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            if (!isCurrentPeriod) {
                                OsysterHapticUtil.performTick(view, hapticEnabled)
                                val cal = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
                                when (selectedInterval) {
                                    NetworkInterval.DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                                    NetworkInterval.WEEK -> cal.add(Calendar.DAY_OF_YEAR, 7)
                                    NetworkInterval.MONTH -> cal.add(Calendar.MONTH, 1)
                                }
                                targetDateMillis = minOf(cal.timeInMillis, System.currentTimeMillis())
                            }
                        },
                        enabled = !isCurrentPeriod,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Period",
                            tint = if (isCurrentPeriod) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Unified Bento Network Activity & Data Usage Card
            item {
                val maxBucketBytes = remember(summary.timeline) {
                    summary.timeline.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1024L) ?: 1024L
                }

                val (totalAmount, totalUnit) = remember(summary.totalBytes) {
                    NetworkMonitor.splitBytesAndUnit(summary.totalBytes)
                }

                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Upper Tier: Bento Split (Left: Compact Arc Ring, Right: 2x2 Stats)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Compact Circular Arc Ring (108.dp)
                            Box(
                                modifier = Modifier.size(108.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 6.dp.toPx()
                                    val arcDiameter = size.minDimension - stroke
                                    val radius = arcDiameter / 2f
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val arcTopLeft = Offset((size.width - arcDiameter) / 2f, (size.height - arcDiameter) / 2f)
                                    val arcSize = Size(arcDiameter, arcDiameter)

                                    // Track circle
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.08f),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = stroke)
                                    )

                                    if (summary.totalBytes > 0) {
                                        val hasBoth = summary.downloadBytes > 0 && summary.uploadBytes > 0
                                        if (hasBoth) {
                                            val capAngleDegrees = (stroke / (2f * Math.PI.toFloat() * radius)) * 360f
                                            val visualGapDegrees = (3.dp.toPx() / (2f * Math.PI.toFloat() * radius)) * 360f
                                            val totalGapAngle = capAngleDegrees + visualGapDegrees
                                            val availableSweep = 360f - 2f * totalGapAngle
                                            val txRatio = (summary.uploadBytes.toFloat() / summary.totalBytes.toFloat()).coerceIn(0f, 1f)
                                            val txSweep = (txRatio * availableSweep).coerceIn(8f, availableSweep - 8f)
                                            val rxSweep = availableSweep - txSweep

                                            val txStart = -90f - (txSweep / 2f)
                                            val rxStart = txStart + txSweep + totalGapAngle

                                            // Upload pill (Emerald) at top
                                            drawArc(
                                                color = ColorEmeraldUpload,
                                                startAngle = txStart,
                                                sweepAngle = txSweep,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                                            )

                                            // Download pill (Cyan) around bottom
                                            drawArc(
                                                color = ColorCyanDownload,
                                                startAngle = rxStart,
                                                sweepAngle = rxSweep,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                                            )
                                        } else if (summary.downloadBytes > 0) {
                                            drawArc(
                                                color = ColorCyanDownload,
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = stroke)
                                            )
                                        } else {
                                            drawArc(
                                                color = ColorEmeraldUpload,
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = stroke)
                                            )
                                        }
                                    }
                                }

                                val amountFontSize = when {
                                    totalAmount.length > 6 -> 16.sp
                                    totalAmount.length > 4 -> 19.sp
                                    else -> 22.sp
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = totalAmount,
                                        fontSize = amountFontSize,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = "$totalUnit TOTAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Right: 2x2 Metric Matrix
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Row 1: Download & Upload
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Download
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = ColorCyanDownload,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Download",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = NetworkMonitor.formatBytes(summary.downloadBytes),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorCyanDownload
                                        )
                                    }

                                    // Upload
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = ColorEmeraldUpload,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Upload",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = NetworkMonitor.formatBytes(summary.uploadBytes),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorEmeraldUpload
                                        )
                                    }
                                }

                                // Row 2: Mobile & Wi-Fi
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Mobile
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.SignalCellularAlt,
                                                contentDescription = null,
                                                tint = ColorAmberCellular,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Mobile",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = NetworkMonitor.formatBytes(summary.mobileBytes),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Wi-Fi
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Wifi,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Wi-Fi",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = NetworkMonitor.formatBytes(summary.wifiBytes),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Lower Tier: Timeline Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Activity Timeline",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Peak: ${NetworkMonitor.formatBytes(maxBucketBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Legend Dots
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(ColorCyanDownload)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rx", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(ColorEmeraldUpload)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tx", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        // Selected Bucket Inspector Banner
                        val activeBucket = selectedBucketIndex?.let { idx ->
                            summary.timeline.getOrNull(idx)
                        }

                        if (activeBucket != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (activeBucket.label.isNotEmpty()) "Slot: ${activeBucket.label}" else "Slot ${selectedBucketIndex!! + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "↓ ${NetworkMonitor.formatBytes(activeBucket.rxBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorCyanDownload
                                        )
                                        Text(
                                            text = "↑ ${NetworkMonitor.formatBytes(activeBucket.txBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorEmeraldUpload
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Canvas Bar Chart with Pill-Shaped Sections
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(summary.timeline) {
                                        detectTapGestures { offset ->
                                            val count = summary.timeline.size
                                            if (count > 0) {
                                                val colWidth = size.width / count
                                                val tapped = (offset.x / colWidth).toInt().coerceIn(0, count - 1)
                                                selectedBucketIndex = tapped
                                                OsysterHapticUtil.performTick(view, hapticEnabled)
                                            }
                                        }
                                    }
                            ) {
                                val count = summary.timeline.size
                                if (count > 0) {
                                    val colWidth = size.width / count
                                    val barWidth = (colWidth * 0.55f).coerceIn(6f, 22f)
                                    val chartHeight = size.height - 20f
                                    val pillRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                                    // Subtle baseline
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.08f),
                                        start = Offset(0f, chartHeight),
                                        end = Offset(size.width, chartHeight),
                                        strokeWidth = 2f
                                    )

                                    summary.timeline.forEachIndexed { index, bucket ->
                                        val centerX = index * colWidth + colWidth / 2f
                                        val barLeft = centerX - barWidth / 2f

                                        // Subtle background track capsule for each slot
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.04f),
                                            topLeft = Offset(barLeft, 0f),
                                            size = Size(barWidth, chartHeight),
                                            cornerRadius = pillRadius
                                        )

                                        // Only draw active bar if usage > 1024 bytes
                                        if (bucket.totalBytes > 1024L) {
                                            val fraction = (bucket.totalBytes.toFloat() / maxBucketBytes.toFloat()).coerceIn(0f, 1f)
                                            val totalBarHeight = (chartHeight * fraction).coerceIn(barWidth, chartHeight)
                                            val activeBarTop = chartHeight - totalBarHeight
                                            val segmentGap = 2.dp.toPx()

                                            if (bucket.txBytes > 0L && bucket.rxBytes > 0L && totalBarHeight > barWidth * 1.5f + segmentGap) {
                                                val txRatio = (bucket.txBytes.toFloat() / bucket.totalBytes.toFloat()).coerceIn(0.12f, 0.88f)
                                                val availableHeight = totalBarHeight - segmentGap
                                                val txH = (availableHeight * txRatio).coerceIn(barWidth * 0.6f, availableHeight - barWidth * 0.6f)
                                                val rxH = availableHeight - txH

                                                // Upload capsule on top
                                                drawRoundRect(
                                                    color = ColorEmeraldUpload,
                                                    topLeft = Offset(barLeft, activeBarTop),
                                                    size = Size(barWidth, txH),
                                                    cornerRadius = pillRadius
                                                )

                                                // Download capsule on bottom
                                                drawRoundRect(
                                                    color = ColorCyanDownload,
                                                    topLeft = Offset(barLeft, activeBarTop + txH + segmentGap),
                                                    size = Size(barWidth, rxH),
                                                    cornerRadius = pillRadius
                                                )
                                            } else if (bucket.txBytes > 0L && bucket.rxBytes <= 0L) {
                                                drawRoundRect(
                                                    color = ColorEmeraldUpload,
                                                    topLeft = Offset(barLeft, activeBarTop),
                                                    size = Size(barWidth, totalBarHeight),
                                                    cornerRadius = pillRadius
                                                )
                                            } else {
                                                drawRoundRect(
                                                    color = ColorCyanDownload,
                                                    topLeft = Offset(barLeft, activeBarTop),
                                                    size = Size(barWidth, totalBarHeight),
                                                    cornerRadius = pillRadius
                                                )
                                            }
                                        }

                                        // Highlight indicator on selected bucket
                                        if (selectedBucketIndex == index) {
                                            drawCircle(
                                                color = ColorCyanDownload,
                                                radius = 4.5f,
                                                center = Offset(centerX, chartHeight + 10f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Aligned X-Axis Time Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val labels = summary.timeline.filter { it.label.isNotEmpty() }
                            labels.forEach { b ->
                                Text(
                                    text = b.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }


            // Application Breakdown Header with Search Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedFilter) {
                            NetworkInterfaceFilter.ALL -> "Application Breakdown (${filteredApps.size})"
                            NetworkInterfaceFilter.MOBILE -> "Mobile Breakdown (${filteredApps.size})"
                            NetworkInterfaceFilter.WIFI -> "Wi-Fi Breakdown (${filteredApps.size})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        IconButton(
                            onClick = {
                                OsysterHapticUtil.performTick(view, hapticEnabled)
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search Applications",
                                tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Inline App Search Bar
            if (isSearchActive) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.network_search_hint), color = MaterialTheme.colorScheme.outline) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Cellular Phone Permission Prompt (shown when filtering by Mobile if permission not granted)
            if (selectedFilter == NetworkInterfaceFilter.MOBILE && !hasPhonePermission) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.network_phone_perm_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = stringResource(R.string.network_phone_perm_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Button(
                                onClick = {
                                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                                    phonePermissionLauncher.launch(android.Manifest.permission.READ_PHONE_STATE)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(stringResource(R.string.network_phone_perm_grant))
                            }
                        }
                    }
                }
            }

            // Application List Rows
            if (filteredApps.isEmpty() && !isLoading) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.network_no_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredApps, key = { "${it.uid}_${selectedFilter}_${selectedInterval}" }) { app ->
                    AppUsageItem(
                        app = app,
                        maxBytes = maxAppBytes,
                        filter = selectedFilter,
                        onClick = {
                            OsysterHapticUtil.performTick(view, hapticEnabled)
                            selectedAppDetails = app
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal Details Bottom Sheet
    selectedAppDetails?.let { app ->
        AppDetailsModalSheet(
            app = app,
            onDismiss = { selectedAppDetails = null },
            context = context
        )
    }
}

// =========================================================================
// Subsection Comment: App Usage List Row with Pill Segmented Progress Bar
// =========================================================================

@Composable
private fun AppUsageItem(
    app: AppNetworkUsage,
    maxBytes: Long,
    filter: NetworkInterfaceFilter,
    onClick: () -> Unit
) {
    val bitmap = remember(app.icon) {
        app.icon?.let { runCatching { it.toSafeBitmap() }.getOrNull() }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                app.uid == 1000 -> Icons.Default.Settings
                                app.uid == -4 -> Icons.Default.WifiTethering
                                else -> Icons.Default.Android
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // App Name, Subtitle, & Pill Progress Bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = when (filter) {
                            NetworkInterfaceFilter.ALL -> {
                                val m = NetworkMonitor.formatBytes(app.mobileBytes)
                                val w = NetworkMonitor.formatBytes(app.wifiBytes)
                                "Mobile: $m • Wi-Fi: $w"
                            }
                            NetworkInterfaceFilter.MOBILE -> {
                                val rx = NetworkMonitor.formatBytes(app.rxBytes)
                                val tx = NetworkMonitor.formatBytes(app.txBytes)
                                "Mobile • ↓ $rx  ↑ $tx"
                            }
                            NetworkInterfaceFilter.WIFI -> {
                                val rx = NetworkMonitor.formatBytes(app.rxBytes)
                                val tx = NetworkMonitor.formatBytes(app.txBytes)
                                "Wi-Fi • ↓ $rx  ↑ $tx"
                            }
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = NetworkMonitor.formatBytes(app.totalBytes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Segmented Multi-Color Pill Bar
                val totalFraction = (app.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.04f, 1f)
                val rxFraction = if (app.totalBytes > 0) (app.rxBytes.toFloat() / app.totalBytes.toFloat()).coerceIn(0f, 1f) else 0.5f
                val hasBoth = app.rxBytes > 0L && app.txBytes > 0L

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    if (app.totalBytes > 0L) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(totalFraction)
                                .fillMaxHeight(),
                            horizontalArrangement = if (hasBoth) Arrangement.spacedBy(2.dp) else Arrangement.Start
                        ) {
                            if (app.rxBytes > 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(rxFraction.coerceAtLeast(0.01f))
                                        .clip(CircleShape)
                                        .background(ColorCyanDownload)
                                )
                            }
                            if (app.txBytes > 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight((1f - rxFraction).coerceAtLeast(0.01f))
                                        .clip(CircleShape)
                                        .background(ColorEmeraldUpload)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// Subsection Comment: App Details Modal Sheet
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailsModalSheet(
    app: AppNetworkUsage,
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Statistics Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Consumption",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = NetworkMonitor.formatBytes(app.totalBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Download (Rx)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorCyanDownload
                )
                Text(
                    text = "${NetworkMonitor.formatBytes(app.rxBytes)} (${app.rxBytes} B)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Upload (Tx)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorEmeraldUpload
                )
                Text(
                    text = "${NetworkMonitor.formatBytes(app.txBytes)} (${app.txBytes} B)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Application UID",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "${app.uid}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action Button: Open Android System App Settings
            if (app.packageName.isNotBlank() && !app.packageName.startsWith("uid.") && app.uid > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        runCatching { context.startActivity(intent) }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.network_app_info))
                }
            }
        }
    }
}

private fun Drawable.toSafeBitmap(): Bitmap {
    val width = if (intrinsicWidth > 0) intrinsicWidth else 96
    val height = if (intrinsicHeight > 0) intrinsicHeight else 96
    return toBitmap(width.coerceIn(48, 144), height.coerceIn(48, 144))
}
