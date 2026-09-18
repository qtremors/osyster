@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.screentime

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.qtremors.osyster.R
import dev.qtremors.osyster.monitor.ScreenUsageHelper
import dev.qtremors.osyster.ui.expressive.*
import dev.qtremors.osyster.ui.onboarding.PermissionBottomSheet
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import dev.qtremors.osyster.ui.util.rememberAsyncAppIcon
import dev.qtremors.osyster.ui.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun ScreenTimeDashboard(
    modifier: Modifier = Modifier,
    viewModel: ScreenTimeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bottomPadding = LocalBottomContentPadding.current

    LifecycleResumeEffect(Unit) {
        viewModel.loadUsage()
        onPauseOrDispose { }
    }

    val pullRefreshState = rememberPullToRefreshState()
    var showTargetSheet by rememberSaveable { mutableStateOf(false) }
    var showPermissionSheet by rememberSaveable { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.loadUsage(forceRefresh = true) },
        state = pullRefreshState,
        indicator = {
            if (uiState.isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ExpressiveContainedLoadingIndicator()
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission missing banner
            if (!uiState.hasPermission) {
                item(key = "perm_warning") {
                    PermissionWarningCard(
                        onRequestPermission = {
                            showPermissionSheet = true
                        }
                    )
                }
            }

            // Hero Daily Screen Time Card
            item(key = "usage_dashboard") {
                ScreenTimeHeroCard(
                    totalScreenTime = uiState.totalScreenTimeToday,
                    targetMinutes = uiState.targetMinutes,
                    streakDays = uiState.streakDays,
                    onEditTarget = { showTargetSheet = true }
                )
            }

            // Yesterday comparison & trend row
            item(key = "usage_trends") {
                ScreenTimeTrendsRow(
                    yesterdayScreenTime = uiState.yesterdayScreenTime,
                    percentageChange = uiState.percentageChange
                )
            }

            // 7-day interactive history card
            if (uiState.dailyHistory.isNotEmpty()) {
                item(key = "usage_history") {
                    ScreenTimeHistoryCard(
                        history = uiState.dailyHistory,
                        targetMinutes = uiState.targetMinutes,
                        selectedDateMillis = uiState.selectedDateMillis,
                        onSelectDate = { viewModel.selectDate(it) }
                    )
                }
            }

            // 24-hour hourly distribution card
            if (uiState.hourlyUsage.isNotEmpty()) {
                item(key = "hourly_usage") {
                    ScreenTimeHourlyCard(
                        hourlyUsage = uiState.hourlyUsage,
                        selectedHour = uiState.selectedHour,
                        onSelectHour = { viewModel.selectHour(it) }
                    )
                }
            }

            // Top apps section
            if (uiState.topApps.isNotEmpty() && uiState.searchQuery.isBlank() && uiState.selectedHour == null) {
                item(key = "top_apps") {
                    ScreenTimeTopAppsSection(topApps = uiState.topApps)
                }
            }

            // Full app list header & search
            item(key = "apps_list_header") {
                ScreenTimeAppsHeader(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedHour = uiState.selectedHour,
                    onClearHour = { viewModel.selectHour(null) }
                )
            }

            // App items list
            val filteredApps = if (uiState.searchQuery.isNotBlank()) {
                uiState.allApps.filter {
                    it.appName.contains(uiState.searchQuery, ignoreCase = true) ||
                    it.packageName.contains(uiState.searchQuery, ignoreCase = true)
                }
            } else {
                uiState.allApps
            }

            val (regularApps, lowUsageApps) = filteredApps.partition { it.usageTimeMillis >= 60000L }

            items(regularApps, key = { it.packageName }) { app ->
                AppUsageRow(
                    app = app,
                    onClick = { openAppInfo(context, app.packageName) }
                )
            }

            if (lowUsageApps.isNotEmpty() && uiState.searchQuery.isBlank()) {
                item(key = "low_usage_apps") {
                    LowUsageAppsCard(
                        apps = lowUsageApps,
                        onAppClick = { openAppInfo(context, it) }
                    )
                }
            }
        }
    }

    if (showTargetSheet) {
        ScreenTimeTargetBottomSheet(
            initialMinutes = uiState.targetMinutes,
            onDismiss = { showTargetSheet = false },
            onSave = {
                viewModel.setTargetMinutes(it)
                showTargetSheet = false
            }
        )
    }

    if (showPermissionSheet) {
        PermissionBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            onAllPermissionsGranted = {
                showPermissionSheet = false
                viewModel.loadUsage(forceRefresh = true)
            }
        )
    }
}

// =========================================================================
// Hero Daily Screen Time Card
// =========================================================================

@Composable
private fun ScreenTimeHeroCard(
    totalScreenTime: Long,
    targetMinutes: Int,
    streakDays: Int,
    onEditTarget: () -> Unit
) {
    val targetMillis = targetMinutes * 60 * 1000L
    val isTargetSet = targetMinutes > 0
    val isExceeded = isTargetSet && totalScreenTime > targetMillis

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (streakDays > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = stringResource(R.string.screentime_streak_label),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streakDays",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.screentime_daily_headline),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onEditTarget,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.screentime_set_target_title),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val hours = totalScreenTime / (1000 * 60 * 60)
            val minutes = (totalScreenTime / (1000 * 60)) % 60
            val seconds = (totalScreenTime / 1000) % 60

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                if (hours > 0) {
                    DigitTicker(
                        text = hours.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        prefix = "h"
                    )
                    TickerUnit("h")
                    Spacer(modifier = Modifier.width(10.dp))
                }

                if (minutes > 0 || hours > 0) {
                    DigitTicker(
                        text = minutes.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        prefix = "m"
                    )
                    TickerUnit("m")
                } else {
                    DigitTicker(
                        text = seconds.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        prefix = "s"
                    )
                    TickerUnit("s")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isTargetSet) {
                val remainingMillis = (targetMillis - totalScreenTime).coerceAtLeast(0L)
                val statusText = if (isExceeded) {
                    stringResource(R.string.screentime_target_exceeded)
                } else {
                    "${stringResource(R.string.screentime_target_label)}: ${ScreenUsageHelper.formatDuration(targetMillis)} (${stringResource(R.string.screentime_target_remaining, ScreenUsageHelper.formatDuration(remainingMillis))})"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                val progress = (totalScreenTime.toFloat() / targetMillis.toFloat()).coerceIn(0f, 1f)

                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 60f),
                    label = "progress"
                )

                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.screentime_no_target_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

// =========================================================================
// Trends Row
// =========================================================================

@Composable
private fun ScreenTimeTrendsRow(
    yesterdayScreenTime: Long,
    percentageChange: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.screentime_yesterday),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (yesterdayScreenTime > 0) ScreenUsageHelper.formatDuration(yesterdayScreenTime) else stringResource(R.string.screentime_none),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.screentime_trend),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (yesterdayScreenTime > 0) {
                    val isIncrease = percentageChange > 0f
                    val trendColor = if (isIncrease) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isIncrease) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = trendColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isIncrease) "+" else ""}${abs(percentageChange.toInt())}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = trendColor
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.not_applicable),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =========================================================================
// 7-Day History Card
// =========================================================================

@Composable
private fun ScreenTimeHistoryCard(
    history: List<DailyHistoryItem>,
    targetMinutes: Int,
    selectedDateMillis: Long?,
    onSelectDate: (Long?) -> Unit
) {
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val maxUsage = remember(history, targetMinutes) {
        val targetMillis = targetMinutes * 60 * 1000L
        (history.maxOfOrNull { it.totalTimeMillis } ?: 0L)
            .coerceAtLeast(targetMillis)
            .coerceAtLeast(60000L)
    }

    val selectedSummary = history.find { it.dateMillis == selectedDateMillis }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.screentime_history_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selectedSummary != null) {
                    Text(
                        text = ScreenUsageHelper.formatDuration(selectedSummary.totalTimeMillis),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                history.takeLast(7).forEach { item ->
                    val isSelected = (item.dateMillis == selectedDateMillis)
                    val fraction = (item.totalTimeMillis.toFloat() / maxUsage.toFloat()).coerceIn(0.06f, 1f)

                    val animFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 60f),
                        label = "bar_height"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectDate(if (isSelected) null else item.dateMillis) }
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .fillMaxHeight(animFraction)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else if (item.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (item.isToday) stringResource(R.string.screentime_today) else dayFormat.format(Date(item.dateMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected || item.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 24-Hour Hourly Distribution Card
// =========================================================================

@Composable
private fun ScreenTimeHourlyCard(
    hourlyUsage: List<HourlyUsageBar>,
    selectedHour: Int?,
    onSelectHour: (Int) -> Unit
) {
    val maxHourly = remember(hourlyUsage) {
        hourlyUsage.maxOfOrNull { it.usageMillis }?.coerceAtLeast(1L) ?: 1L
    }

    val selectedBar = hourlyUsage.find { it.hour == selectedHour }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.screentime_hourly_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedBar != null) {
                    Text(
                        text = "${String.format("%02d:00", selectedBar.hour)}: ${ScreenUsageHelper.formatDuration(selectedBar.usageMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.screentime_hourly_tap_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                hourlyUsage.forEach { bar ->
                    val isSelected = bar.hour == selectedHour
                    val fraction = (bar.usageMillis.toFloat() / maxHourly.toFloat()).coerceIn(0.08f, 1f)

                    val animHeight by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 80f),
                        label = "hourly_height"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(animHeight)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else if (bar.usageMillis > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                            )
                            .clickable { onSelectHour(bar.hour) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("06:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("12:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("18:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("23:59", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// =========================================================================
// Top Apps Section
// =========================================================================

@Composable
private fun ScreenTimeTopAppsSection(topApps: List<ScreenTimeAppItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.screentime_top_apps),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        topApps.forEachIndexed { index, app ->
            val context = LocalContext.current
            val shape = expressiveGroupShape(index, topApps.size)

            Surface(
                onClick = { openAppInfo(context, app.packageName) },
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppUsageListItem(app = app)
            }
        }
    }
}

// =========================================================================
// Apps Header with Search
// =========================================================================

@Composable
private fun ScreenTimeAppsHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedHour: Int?,
    onClearHour: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedHour != null) stringResource(R.string.screentime_apps_at_hour, selectedHour) else stringResource(R.string.screentime_all_apps),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (selectedHour != null) {
                FilterChip(
                    selected = true,
                    onClick = onClearHour,
                    label = { Text(stringResource(R.string.screentime_clear_filter)) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.screentime_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// =========================================================================
// App Usage Row
// =========================================================================

@Composable
private fun AppUsageRow(
    app: ScreenTimeAppItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        AppUsageListItem(app = app)
    }
}

@Composable
private fun AppUsageListItem(app: ScreenTimeAppItem) {
    val iconBitmap by rememberAsyncAppIcon(packageName = app.packageName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Android,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { app.percentageOfTotal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = ScreenUsageHelper.formatDuration(app.usageTimeMillis),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (app.sessionCount > 0) {
                Text(
                    text = stringResource(R.string.screentime_opens, app.sessionCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =========================================================================
// Low Usage Apps Card (< 1 min)
// =========================================================================

@Composable
private fun LowUsageAppsCard(
    apps: List<ScreenTimeAppItem>,
    onAppClick: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val totalLowTime = apps.sumOf { it.usageTimeMillis }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.screentime_other_apps),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${apps.size} apps · ${ScreenUsageHelper.formatDuration(totalLowTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    apps.forEach { app ->
                        AppUsageListItem(app = app)
                    }
                }
            }
        }
    }
}

// =========================================================================
// Screen Time Target Setter Bottom Sheet
// =========================================================================

@Composable
fun ScreenTimeTargetBottomSheet(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var hoursText by remember { mutableStateOf((initialMinutes / 60).toString()) }
    var minutesText by remember { mutableStateOf((initialMinutes % 60).toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.screentime_set_target_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.screentime_set_target_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.screentime_target_hours),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = hoursText,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) hoursText = it },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.screentime_target_minutes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = minutesText,
                            onValueChange = { if (it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) < 60) minutesText = it },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets
            OsysterGroupedButton {
                listOf(120, 240, 360, 480).forEach { preset ->
                    val label = "${preset / 60}h"
                    val isSelected = ((hoursText.toIntOrNull() ?: 0) * 60 + (minutesText.toIntOrNull() ?: 0)) == preset
                    OsysterButtonWeighted(
                        onClick = {
                            hoursText = (preset / 60).toString()
                            minutesText = (preset % 60).toString()
                        },
                        text = label,
                        type = if (isSelected) ExpressiveButtonType.Filled else ExpressiveButtonType.Tonal,
                        size = ExpressiveButtonSize.Small,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initialMinutes > 0) {
                    OsysterExpressiveButton(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onSave(0)
                            }
                        },
                        text = stringResource(R.string.screentime_remove_target),
                        type = ExpressiveButtonType.Tonal,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                OsysterExpressiveButton(
                    onClick = {
                        val h = hoursText.toIntOrNull() ?: 0
                        val m = minutesText.toIntOrNull() ?: 0
                        scope.launch {
                            sheetState.hide()
                            onSave(h * 60 + m)
                        }
                    },
                    text = stringResource(R.string.screentime_save_target),
                    type = ExpressiveButtonType.Filled,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

// =========================================================================
// Permission Missing Card
// =========================================================================

@Composable
private fun PermissionWarningCard(onRequestPermission: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.screentime_perm_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.screentime_perm_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.screentime_perm_grant), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun openAppInfo(context: android.content.Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(intent) }
}
