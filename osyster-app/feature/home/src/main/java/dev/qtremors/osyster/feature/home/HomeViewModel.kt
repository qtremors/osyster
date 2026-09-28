package dev.qtremors.osyster.feature.home

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

/** Recent CPU and RAM readings from 0 to 1, for the hero card sparklines. */
data class Trends(val cpu: List<Float> = emptyList(), val ram: List<Float> = emptyList())

data class HomeState(
    val hero: HeroIdentity,
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

    // Trends are kept in the flow itself, so they reset when Home stops sampling.
    private val summaries = summarySource.summary()
        .scan(null as DeviceSummary? to Trends()) { (_, trends), summary ->
            summary to Trends(cpu = trends.cpu.pushTrend(cpuFraction(summary)), ram = trends.ram.pushTrend(ramFraction(summary)))
        }

    val state: StateFlow<HomeState> = combine(
        summaries,
        glanceSource.glances().onStart { emit(emptyMap()) },
        layoutRepository.layout.onStart<HomeLayout?> { emit(null) }
    ) { (summary, trends), glances, layout -> HomeState(identity, summary, trends, glances, layout) }
        // Sampling stops shortly after Home leaves the screen.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), HomeState(identity, summary = null, layout = null))

    fun togglePin(key: PinKey) = update { it.togglePin(key) }
    fun move(page: DevicePage, up: Boolean) = update { it.move(page, up) }
    fun hide(page: DevicePage) = update { it.hide(page) }
    fun show(page: DevicePage) = update { it.show(page) }
    fun setVisible(page: DevicePage, visible: Boolean) = update { if (visible) it.show(page) else it.hide(page) }
    fun reorderGroup(pages: List<DevicePage>) = update { it.reorderGroup(pages) }
    fun reorderPinned(keys: List<PinKey>) = update { it.reorderPinned(keys) }

    private fun update(transform: (HomeLayout) -> HomeLayout) {
        viewModelScope.launch { layoutRepository.update(transform) }
    }

    companion object {
        /** Load when Android allows it; otherwise nothing, since a clock speed is not a load. */
        fun cpuFraction(summary: DeviceSummary): Float? = summary.cpuLoadPercent.getOrNull()?.div(100f)

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
