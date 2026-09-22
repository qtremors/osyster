package dev.qtremors.osyster.feature.home

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

data class HomeState(
    val hero: HeroIdentity,
    val summary: DeviceSummary?,
    /** Null until the saved arrangement loads, so Home never flashes the default order. */
    val layout: HomeLayout?
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    summarySource: DeviceSummarySource,
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

    val state: StateFlow<HomeState> = combine(
        summarySource.summary().onStart<DeviceSummary?> { emit(null) },
        layoutRepository.layout.onStart<HomeLayout?> { emit(null) }
    ) { summary, layout -> HomeState(identity, summary, layout) }
        // Sampling stops shortly after Home leaves the screen.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), HomeState(identity, null, null))

    fun togglePin(key: PinKey) = update { it.togglePin(key) }
    fun move(page: DevicePage, up: Boolean) = update { it.move(page, up) }
    fun hide(page: DevicePage) = update { it.hide(page) }
    fun show(page: DevicePage) = update { it.show(page) }

    private fun update(transform: (HomeLayout) -> HomeLayout) {
        viewModelScope.launch { layoutRepository.update(transform) }
    }

    companion object {
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
