package dev.qtremors.osyster.feature.appstopper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.usage.domain.AppStopperRepository
import dev.qtremors.osyster.core.usage.domain.InstalledApp
import dev.qtremors.osyster.core.usage.domain.ManagedApp
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The installed-apps picker. Null [apps] means still loading. */
data class PickerState(val includeSystem: Boolean = false, val apps: List<InstalledApp>? = null)

@HiltViewModel
class AppStopperViewModel @Inject constructor(
    private val repository: AppStopperRepository
) : ViewModel() {
    /** Null until the saved list loads, so the empty state never flashes. */
    val apps: StateFlow<List<ManagedApp>?> = repository.apps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _picker = MutableStateFlow(PickerState())
    val picker: StateFlow<PickerState> = _picker.asStateFlow()

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun loadPicker(includeSystem: Boolean = _picker.value.includeSystem) {
        _picker.value = PickerState(includeSystem, apps = null)
        viewModelScope.launch {
            val installed = runCatchingCancellable { repository.installedApps(includeSystem) }.getOrDefault(emptyList())
            if (_picker.value.includeSystem == includeSystem) _picker.value = PickerState(includeSystem, installed)
        }
    }

    fun add(packages: Collection<String>) {
        if (packages.isEmpty()) return
        viewModelScope.launch { repository.add(packages) }
    }

    fun remove(packageName: String) {
        viewModelScope.launch { repository.remove(packageName) }
    }
}
