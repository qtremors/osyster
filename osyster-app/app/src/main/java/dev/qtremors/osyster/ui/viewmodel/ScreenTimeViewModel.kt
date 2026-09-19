package dev.qtremors.osyster.ui.viewmodel

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.monitor.ScreenUsageHelper
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class ScreenTimeAppItem(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val sessionCount: Int,
    val percentageOfTotal: Float
)

data class HourlyUsageBar(
    val hour: Int,
    val usageMillis: Long,
    val appUsageMap: Map<String, Long> = emptyMap()
)

data class DailyHistoryItem(
    val dateMillis: Long,
    val totalTimeMillis: Long,
    val isToday: Boolean,
    val appUsageMap: Map<String, Long>
)

data class ScreenTimeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasPermission: Boolean = false,
    val totalScreenTimeToday: Long = 0L,
    val yesterdayScreenTime: Long = 0L,
    val percentageChange: Float = 0f,
    val streakDays: Int = 0,
    val dailyHistory: List<DailyHistoryItem> = emptyList(),
    val selectedDateMillis: Long? = null,
    val hourlyUsage: List<HourlyUsageBar> = emptyList(),
    val selectedHour: Int? = null,
    val allApps: List<ScreenTimeAppItem> = emptyList(),
    val topApps: List<ScreenTimeAppItem> = emptyList(),
    val searchQuery: String = "",
    val targetMinutes: Int = 0
)

class ScreenTimeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val preferencesManager = OsysterPreferencesManager.getInstance(context)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val _uiState = MutableStateFlow(ScreenTimeUiState())
    val uiState: StateFlow<ScreenTimeUiState> = _uiState.asStateFlow()

    private var appLabelCache = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            preferencesManager.state.collect { prefs ->
                _uiState.update { it.copy(targetMinutes = prefs.screenTimeTargetMinutes) }
            }
        }
        loadUsage()
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun loadUsage(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hasPerm = hasUsageStatsPermission()
            _uiState.update { it.copy(hasPermission = hasPerm) }
            if (!hasPerm || usageStatsManager == null) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                return@launch
            }

            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
                ScreenUsageHelper.clearCache()
            }

            withContext(Dispatchers.IO) {
                val preferSystem = preferencesManager.state.value.preferSystemUsageHistory
                val dailySummaries = ScreenUsageHelper.fetchDailySummaries(
                    usageStatsManager = usageStatsManager,
                    daysCount = 7,
                    preferSystemHistory = preferSystem
                )
                val todaySummary = dailySummaries.lastOrNull { it.isToday }
                val yesterdaySummary = if (dailySummaries.size >= 2) dailySummaries[dailySummaries.size - 2] else null

                val todayTotal = todaySummary?.totalTimeMillis ?: 0L
                val yesterdayTotal = yesterdaySummary?.totalTimeMillis ?: 0L

                val changePercent = if (yesterdayTotal > 0) {
                    ((todayTotal - yesterdayTotal).toFloat() / yesterdayTotal.toFloat()) * 100f
                } else 0f

                // Detailed today including hourly
                val todayDetailed = ScreenUsageHelper.fetchDetailedUsageToday(
                    usageStatsManager = usageStatsManager,
                    includeHourly = true,
                    forceRefresh = forceRefresh
                )

                val hourlyBars = (0 until 24).map { h ->
                    val hMap = todayDetailed.hourlyUsageMap[h] ?: emptyMap()
                    HourlyUsageBar(
                        hour = h,
                        usageMillis = hMap.values.sum(),
                        appUsageMap = hMap
                    )
                }

                val historyItems = dailySummaries.map {
                    DailyHistoryItem(
                        dateMillis = it.dateMillis,
                        totalTimeMillis = it.totalTimeMillis,
                        isToday = it.isToday,
                        appUsageMap = it.appUsageMap
                    )
                }

                // Compute streak: consecutive days with screen time > 0 and within target if target > 0
                val targetMillis = preferencesManager.state.value.screenTimeTargetMinutes * 60 * 1000L
                var streak = 0
                for (item in historyItems.reversed()) {
                    if (item.totalTimeMillis > 0L) {
                        if (targetMillis <= 0L || item.totalTimeMillis <= targetMillis) {
                            streak++
                        } else break
                    } else if (!item.isToday) break
                }

                val effectiveDateMillis = _uiState.value.selectedDateMillis ?: todaySummary?.dateMillis
                val activeUsageMap = if (effectiveDateMillis != null && effectiveDateMillis != todaySummary?.dateMillis) {
                    historyItems.find { it.dateMillis == effectiveDateMillis }?.appUsageMap ?: emptyMap()
                } else {
                    todayDetailed.appUsageMap
                }

                val appItems = buildAppItems(activeUsageMap, todayDetailed.sessionCounts)

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        hasPermission = true,
                        totalScreenTimeToday = todayTotal,
                        yesterdayScreenTime = yesterdayTotal,
                        percentageChange = changePercent,
                        streakDays = streak,
                        dailyHistory = historyItems,
                        selectedDateMillis = effectiveDateMillis,
                        hourlyUsage = hourlyBars,
                        allApps = appItems,
                        topApps = appItems.take(5)
                    )
                }
            }
        }
    }

    fun selectDate(dateMillis: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _uiState.value
            val targetDate = dateMillis ?: currentState.dailyHistory.lastOrNull { it.isToday }?.dateMillis
            val selectedHistory = currentState.dailyHistory.find { it.dateMillis == targetDate }
            val usageStatsMgr = usageStatsManager ?: return@launch

            val isToday = selectedHistory?.isToday == true
            val preferSystem = preferencesManager.state.value.preferSystemUsageHistory
            val (usageMap, hourlyBars) = if (isToday) {
                val detailed = ScreenUsageHelper.fetchDetailedUsageToday(usageStatsMgr, includeHourly = true)
                val bars = (0 until 24).map { h ->
                    val hMap = detailed.hourlyUsageMap[h] ?: emptyMap()
                    HourlyUsageBar(hour = h, usageMillis = hMap.values.sum(), appUsageMap = hMap)
                }
                detailed.appUsageMap to bars
            } else if (targetDate != null && preferSystem) {
                val dayEnd = ScreenUsageHelper.getDayEndTime(targetDate)
                val detailed = ScreenUsageHelper.fetchUsageForRange(usageStatsMgr, targetDate, dayEnd, includeHourly = true)
                val bars = (0 until 24).map { h ->
                    val hMap = detailed.hourlyUsageMap[h] ?: emptyMap()
                    HourlyUsageBar(hour = h, usageMillis = hMap.values.sum(), appUsageMap = hMap)
                }
                detailed.appUsageMap to bars
            } else {
                emptyMap<String, Long>() to emptyList()
            }

            val appItems = buildAppItems(usageMap, emptyMap())

            _uiState.update {
                it.copy(
                    selectedDateMillis = targetDate,
                    selectedHour = null,
                    hourlyUsage = hourlyBars,
                    allApps = appItems,
                    topApps = appItems.take(5)
                )
            }
        }
    }

    fun selectHour(hour: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _uiState.value
            val newHour = if (currentState.selectedHour == hour) null else hour

            val activeUsageMap = if (newHour != null) {
                currentState.hourlyUsage.find { it.hour == newHour }?.appUsageMap ?: emptyMap()
            } else {
                val selectedDay = currentState.dailyHistory.find { it.dateMillis == currentState.selectedDateMillis }
                selectedDay?.appUsageMap ?: emptyMap()
            }

            val appItems = buildAppItems(activeUsageMap, emptyMap())

            _uiState.update {
                it.copy(
                    selectedHour = newHour,
                    allApps = appItems,
                    topApps = appItems.take(5)
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setTargetMinutes(minutes: Int) {
        preferencesManager.setScreenTimeTargetMinutes(minutes)
        _uiState.update { it.copy(targetMinutes = minutes) }
    }

    private fun buildAppItems(
        usageMap: Map<String, Long>,
        sessionCounts: Map<String, Int>
    ): List<ScreenTimeAppItem> {
        val totalMillis = usageMap.values.sum().coerceAtLeast(1L)
        val pm = context.packageManager

        return usageMap.entries
            .filter { it.value > 0L }
            .sortedByDescending { it.value }
            .map { (pkg, time) ->
                val label = appLabelCache.getOrPut(pkg) {
                    runCatching {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    }.getOrDefault(pkg)
                }
                ScreenTimeAppItem(
                    packageName = pkg,
                    appName = label,
                    usageTimeMillis = time,
                    sessionCount = sessionCounts[pkg] ?: 0,
                    percentageOfTotal = (time.toFloat() / totalMillis).coerceIn(0f, 1f)
                )
            }
    }
}
