package dev.qtremors.osyster.feature.batteryhistory

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.battery.domain.BatteryFacts
import dev.qtremors.osyster.core.battery.domain.BatteryHistory
import dev.qtremors.osyster.core.battery.domain.BatteryHistorySettings
import dev.qtremors.osyster.core.battery.domain.BatteryHistorySettingsStore
import dev.qtremors.osyster.core.battery.domain.BatterySample
import dev.qtremors.osyster.core.battery.domain.BatterySampling
import dev.qtremors.osyster.core.battery.domain.HOUR_MS
import dev.qtremors.osyster.core.navigation.BatteryHistoryRoute
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How far back the page looks. */
enum class BatteryRange(val millis: Long) { DAY(24 * HOUR_MS), WEEK(7 * 24 * HOUR_MS), MONTH(30 * 24 * HOUR_MS) }

@HiltViewModel
class BatteryHistoryViewModel @Inject constructor(
    private val history: BatteryHistory,
    private val settingsStore: BatteryHistorySettingsStore,
    private val sampling: BatterySampling
) : ViewModel() {
    val settings: StateFlow<BatteryHistorySettings?> = settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _range = MutableStateFlow(BatteryRange.DAY)
    val range: StateFlow<BatteryRange> = _range.asStateFlow()

    /** Readings in the chosen range; null while the first page loads. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val samples: StateFlow<List<BatterySample>?> = _range
        .flatMapLatest { history.samples(System.currentTimeMillis() - it.millis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _now = MutableStateFlow<BatterySample?>(null)
    val now: StateFlow<BatterySample?> = _now.asStateFlow()

    val facts: BatteryFacts = history.facts()

    /** Opening the page adds a fresh reading, so it is never out of date while looked at. */
    fun refresh() {
        viewModelScope.launch {
            val enabled = settings.value?.enabled ?: settingsStore.settings.first().enabled
            if (enabled) _now.value = history.recordNow()
        }
    }

    fun range(range: BatteryRange) {
        _range.value = range
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.update { it.copy(enabled = enabled) }
            sampling.schedule(enabled)
            if (enabled) _now.value = history.recordNow()
        }
    }

    fun clear() {
        viewModelScope.launch { history.clear() }
    }
}

fun NavGraphBuilder.registerBatteryHistoryRoute() {
    composable<BatteryHistoryRoute> { BatteryHistoryScreen(viewModel = hiltViewModel()) }
}
