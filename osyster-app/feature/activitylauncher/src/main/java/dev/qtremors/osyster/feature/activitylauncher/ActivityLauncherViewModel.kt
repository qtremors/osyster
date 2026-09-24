package dev.qtremors.osyster.feature.activitylauncher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.system.domain.ActivityEntry
import dev.qtremors.osyster.core.system.domain.AppActivities
import dev.qtremors.osyster.core.system.domain.AppEntry
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LauncherState(
    /** Null while the app list loads. */
    val apps: List<AppEntry>? = null,
    val selected: AppEntry? = null,
    /** Null while the selected app's activities load. */
    val activities: List<ActivityEntry>? = null
)

/** One-off results for a snackbar. */
enum class LauncherMessage { OPEN_FAILED, PIN_UNSUPPORTED, PIN_REQUESTED }

@HiltViewModel
class ActivityLauncherViewModel @Inject constructor(
    private val activities: AppActivities
) : ViewModel() {
    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()
    private val _message = MutableStateFlow<LauncherMessage?>(null)
    val message: StateFlow<LauncherMessage?> = _message.asStateFlow()
    private var activitiesJob: Job? = null

    init {
        viewModelScope.launch {
            val apps = runCatchingCancellable { activities.apps() }.getOrDefault(emptyList())
            _state.update { it.copy(apps = apps) }
        }
    }

    fun select(app: AppEntry?) {
        _state.update { it.copy(selected = app, activities = null) }
        activitiesJob?.cancel()
        if (app == null) return
        activitiesJob = viewModelScope.launch {
            val list = runCatchingCancellable { activities.activities(app.packageName) }.getOrDefault(emptyList())
            _state.update { if (it.selected == app) it.copy(activities = list) else it }
        }
    }

    fun launch(activity: ActivityEntry) {
        if (!activities.launch(activity)) _message.value = LauncherMessage.OPEN_FAILED
    }

    fun pin(activity: ActivityEntry) {
        _message.value = if (activities.pinShortcut(activity)) LauncherMessage.PIN_REQUESTED else LauncherMessage.PIN_UNSUPPORTED
    }

    fun messageShown() {
        _message.value = null
    }
}
