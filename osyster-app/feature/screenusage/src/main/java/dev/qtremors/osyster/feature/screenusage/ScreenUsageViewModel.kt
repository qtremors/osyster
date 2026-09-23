package dev.qtremors.osyster.feature.screenusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.usage.domain.ScreenDay
import dev.qtremors.osyster.core.usage.domain.ScreenDayTotal
import dev.qtremors.osyster.core.usage.domain.ScreenUsageSource
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScreenUsageState(
    val access: UsageAccessState? = null,
    /** The last seven days, oldest first; empty until loaded. */
    val history: List<ScreenDayTotal> = emptyList(),
    /** Index into [history]; defaults to today (the last entry). */
    val selectedIndex: Int = HISTORY_DAYS - 1,
    val day: ScreenDay? = null,
    val showAllApps: Boolean = false
)

internal const val HISTORY_DAYS = 7

@HiltViewModel
class ScreenUsageViewModel @Inject constructor(
    private val source: ScreenUsageSource
) : ViewModel() {
    private val _state = MutableStateFlow(ScreenUsageState())
    val state: StateFlow<ScreenUsageState> = _state.asStateFlow()
    private var dayJob: Job? = null

    /** Re-checks access and reloads history and the selected day; called on every resume. */
    fun refresh() {
        val access = source.accessState()
        _state.update { it.copy(access = access) }
        if (access != UsageAccessState.GRANTED) return
        viewModelScope.launch {
            val history = runCatchingCancellable { source.history(HISTORY_DAYS) }.getOrDefault(emptyList())
            _state.update { it.copy(history = history) }
            loadDay(_state.value.selectedIndex)
        }
    }

    fun select(index: Int) {
        if (index == _state.value.selectedIndex) return
        _state.update { it.copy(selectedIndex = index, day = null, showAllApps = false) }
        loadDay(index)
    }

    fun toggleShowAll() = _state.update { it.copy(showAllApps = !it.showAllApps) }

    private fun loadDay(index: Int) {
        val start = _state.value.history.getOrNull(index)?.dayStart ?: return
        dayJob?.cancel()
        dayJob = viewModelScope.launch {
            val day = runCatchingCancellable { source.day(start) }.getOrNull()
            _state.update { if (it.selectedIndex == index) it.copy(day = day) else it }
        }
    }
}
