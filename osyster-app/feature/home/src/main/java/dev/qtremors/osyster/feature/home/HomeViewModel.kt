package dev.qtremors.osyster.feature.home

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onEach
import dev.qtremors.osyster.core.usage.domain.NetworkFilter
import dev.qtremors.osyster.core.usage.domain.TrafficTotals
import dev.qtremors.osyster.core.device.domain.HeroTile
import dev.qtremors.osyster.core.device.domain.ReadingPreferences
import dev.qtremors.osyster.core.device.domain.TemperatureUnit
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.ScreenUsageSource
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import dev.qtremors.osyster.core.device.domain.DeviceGlanceSource
import dev.qtremors.osyster.core.device.domain.pushTrend
import dev.qtremors.osyster.core.device.domain.getOrNull
import kotlinx.coroutines.flow.scan
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.DeviceReportSource
import dev.qtremors.osyster.core.device.domain.DeviceSummary
import dev.qtremors.osyster.core.device.domain.DeviceSummarySource
import dev.qtremors.osyster.core.device.domain.HomeLayout
import dev.qtremors.osyster.core.device.domain.HomeLayoutRepository
import dev.qtremors.osyster.core.device.domain.PinKey
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Who the phone is. Read once from Build; live readings come from [DeviceSummary]. */
data class HeroIdentity(
    val name: String,
    val androidRelease: String,
    val apiLevel: Int
)

/** Recent CPU readings from 0 to 1 for the sparkline, and the fastest clock seen to scale clocks. */
data class Trends(val cpu: List<Float> = emptyList(), val ram: List<Float> = emptyList(), val maxClockMhz: Int = 0)

/** Today's data and screen time for the dashboard; null values mean not read yet or not allowed. */
data class UsageToday(
    val allowed: Boolean = true,
    /** Today's traffic, and the network the tile shows: the one in use now, or both when offline. */
    val traffic: TrafficTotals? = null,
    val network: NetworkFilter = NetworkFilter.ALL,
    val screenMillis: Long? = null,
    /** Screen time in each hour of today, for the tile's bars. */
    val screenHourly: List<Long> = emptyList()
)

data class HomeState(
    val hero: HeroIdentity,
    val usage: UsageToday = UsageToday(),
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val summary: DeviceSummary?,
    val trends: Trends = Trends(),
    /** Live one-line facts for device page rows; pages missing here keep their fixed summary. */
    val glances: Map<DevicePage, String> = emptyMap(),
    /** Null until the saved arrangement loads, so Home never flashes the default order. */
    val layout: HomeLayout?
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    summarySource: DeviceSummarySource,
    glanceSource: DeviceGlanceSource,
    private val dataUsage: DataUsageSource,
    private val screenUsage: ScreenUsageSource,
    reading: ReadingPreferences,
    private val layoutRepository: HomeLayoutRepository,
    private val reportSource: DeviceReportSource
) : ViewModel() {
    /** Builds the shareable text report; reads every page once, so it takes a moment. */
    suspend fun buildReport(): String = reportSource.report()

    private val identity = HeroIdentity(
        name = displayName(Build.MANUFACTURER, Build.MODEL),
        androidRelease = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT
    )

    // Each source restarts from its last value when Home comes back, not from blank. Starting
    // blank briefly shrank the page list to a spinner, which threw away the scroll position and
    // made every tile flash "…". Trends still restart, since sampling paused while away.
    private var lastSummary: DeviceSummary? = null
    private var lastGlances: Map<DevicePage, String> = emptyMap()
    private var lastLayout: HomeLayout? = null
    private var lastUsage: UsageToday = UsageToday()

    private val summaries = flow {
        emitAll(summarySource.summary().scan(lastSummary to Trends()) { (_, trends), summary ->
            val maxClock = maxOf(trends.maxClockMhz, summary.cpuClockMhz.getOrNull() ?: 0)
            summary to Trends(
                cpu = trends.cpu.pushTrend(cpuFraction(summary, maxClock)),
                ram = trends.ram.pushTrend(ramFraction(summary)),
                maxClockMhz = maxClock
            )
        })
    }.onEach { (summary, _) -> if (summary != null) lastSummary = summary }

    // Usage totals change slowly; once a minute is plenty and keeps Android's stats service quiet.
    private val usage = flow {
        while (true) {
            emit(readUsage())
            delay(USAGE_TICK_MS)
        }
    }.onEach { lastUsage = it }

    val state: StateFlow<HomeState> = combine(
        summaries,
        glanceSource.glances().onEach { lastGlances = it }.onStart { emit(lastGlances) },
        layoutRepository.layout.onEach { lastLayout = it }.onStart<HomeLayout?> { emit(lastLayout) },
        usage.onStart { emit(lastUsage) },
        reading.prefs
    ) { (summary, trends), glances, layout, today, prefs ->
        HomeState(hero = identity, usage = today, unit = prefs.temperatureUnit, summary = summary, trends = trends, glances = glances, layout = layout)
    }
        // Sampling stops shortly after Home leaves the screen.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), HomeState(hero = identity, summary = null, layout = null))

    fun togglePin(key: PinKey) = update { it.togglePin(key) }
    fun move(page: DevicePage, up: Boolean) = update { it.move(page, up) }
    fun hide(page: DevicePage) = update { it.hide(page) }
    fun show(page: DevicePage) = update { it.show(page) }
    fun setVisible(page: DevicePage, visible: Boolean) = update { if (visible) it.show(page) else it.hide(page) }
    fun reorderGroup(pages: List<DevicePage>) = update { it.reorderGroup(pages) }
    fun reorderPinned(keys: List<PinKey>) = update { it.reorderPinned(keys) }
    fun reorderTiles(tiles: List<HeroTile>) = update { it.reorderTiles(tiles) }
    fun setTileVisible(tile: HeroTile, visible: Boolean) = update { it.setTileVisible(tile, visible) }

    private suspend fun readUsage(): UsageToday {
        if (dataUsage.accessState() != UsageAccessState.GRANTED) return UsageToday(allowed = false)
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val totals = runCatchingCancellable { dataUsage.report(DataPeriod.TODAY, includeApps = false).totals }.getOrNull()
        val screen = runCatchingCancellable { screenUsage.day(todayStart) }.getOrNull()
        return UsageToday(
            allowed = true,
            traffic = totals,
            network = dataUsage.activeNetwork(),
            screenMillis = screen?.totalMillis,
            screenHourly = screen?.hourlyMillis.orEmpty()
        )
    }

    private fun update(transform: (HomeLayout) -> HomeLayout) {
        viewModelScope.launch { layoutRepository.update(transform) }
    }

    companion object {
        private const val USAGE_TICK_MS = 60_000L

        /**
         * Load when Android allows it. Otherwise the fastest core's clock against the fastest
         * clock seen, which still shows how busy the phone is; the tile labels it as a clock.
         */
        fun cpuFraction(summary: DeviceSummary, maxClockMhz: Int): Float? {
            summary.cpuLoadPercent.getOrNull()?.let { return it / 100f }
            val clock = summary.cpuClockMhz.getOrNull() ?: return null
            return if (maxClockMhz > 0) clock.toFloat() / maxClockMhz else null
        }

        fun ramFraction(summary: DeviceSummary): Float? {
            val used = summary.ramUsedBytes.getOrNull() ?: return null
            val total = summary.ramTotalBytes.getOrNull()?.takeIf { it > 0 } ?: return null
            return used.toFloat() / total
        }

        /**
         * "Google" + "Pixel 7" reads "Pixel 7"; "samsung" + "SM-S918B" reads "Samsung SM-S918B".
         * Models that already start with the brand are not doubled.
         */
        fun displayName(manufacturer: String, model: String): String {
            val brand = manufacturer.trim().replaceFirstChar { it.titlecase() }
            val cleanModel = model.trim()
            return when {
                brand.isEmpty() -> cleanModel
                cleanModel.startsWith(brand, ignoreCase = true) -> cleanModel
                brand.equals("Google", ignoreCase = true) -> cleanModel
                else -> "$brand $cleanModel"
            }
        }
    }
}
