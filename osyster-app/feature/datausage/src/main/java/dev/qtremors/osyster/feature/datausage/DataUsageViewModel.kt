package dev.qtremors.osyster.feature.datausage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageReport
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.LiveRate
import dev.qtremors.osyster.core.usage.domain.NetworkFilter
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataUsageState(
    val access: UsageAccessState? = null,
    val period: DataPeriod = DataPeriod.TODAY,
    val filter: NetworkFilter = NetworkFilter.ALL,
    /** Null while the first report for this period loads. */
    val report: DataUsageReport? = null,
    val loading: Boolean = false,
    val showAllApps: Boolean = false
)

@HiltViewModel
class DataUsageViewModel @Inject constructor(
    private val source: DataUsageSource
) : ViewModel() {
    private val _state = MutableStateFlow(DataUsageState())
    val state: StateFlow<DataUsageState> = _state.asStateFlow()

    /** Live rate only while the screen is on display. */
    val liveRate: StateFlow<LiveRate?> = source.liveRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), null)

    private var loadJob: Job? = null

    /** Re-checks access and reloads; called each time the screen resumes. */
    fun refresh() {
        val access = source.accessState()
        _state.update { it.copy(access = access) }
        if (access == UsageAccessState.GRANTED) load(_state.value.period)
    }

    fun setPeriod(period: DataPeriod) {
        if (period == _state.value.period) return
        _state.update { it.copy(period = period, report = null, showAllApps = false) }
        load(period)
    }

    fun setFilter(filter: NetworkFilter) = _state.update { it.copy(filter = filter, showAllApps = false) }

    fun toggleShowAll() = _state.update { it.copy(showAllApps = !it.showAllApps) }

    /** Latest request wins: changing the period cancels a slower query still running. */
    private fun load(period: DataPeriod) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val report = runCatchingCancellable { source.report(period) }.getOrNull()
            _state.update { current ->
                if (current.period != period) current
                else current.copy(report = report ?: current.report, loading = false)
            }
        }
    }
}
