package dev.qtremors.osyster.feature.notificationhistory

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.navigation.NotificationHistoryRoute
import dev.qtremors.osyster.core.notifications.domain.HistoryApp
import dev.qtremors.osyster.core.notifications.domain.HistoryEntry
import dev.qtremors.osyster.core.notifications.domain.HistoryQuery
import dev.qtremors.osyster.core.notifications.domain.HistorySettings
import dev.qtremors.osyster.core.notifications.domain.HistorySettingsStore
import dev.qtremors.osyster.core.notifications.domain.NotificationHistory
import dev.qtremors.osyster.core.notifications.domain.PAGE
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(
    private val history: NotificationHistory,
    private val settingsStore: HistorySettingsStore
) : ViewModel() {
    /** Null until the stored settings load, so the intro never flashes for people who use it. */
    val settings: StateFlow<HistorySettings?> = settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _access = MutableStateFlow(history.hasAccess())
    val access: StateFlow<Boolean> = _access.asStateFlow()

    private val _query = MutableStateFlow(HistoryQuery())
    val query: StateFlow<HistoryQuery> = _query.asStateFlow()

    /** The list for [query]; null while the first page loads. Typing waits a moment before searching. */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<HistoryEntry>?> = _query
        .debounce(SEARCH_DELAY_MS)
        .flatMapLatest(history::entries)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val apps: StateFlow<List<HistoryApp>> = history.apps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _open = MutableStateFlow<HistoryEntry?>(null)
    val open: StateFlow<HistoryEntry?> = _open.asStateFlow()

    /** The whole chat behind the open entry, when it is a message. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val thread: StateFlow<List<HistoryEntry>> = _open
        .flatMapLatest { entry ->
            val conversation = entry?.conversation
            if (entry == null || conversation == null) flowOf(emptyList()) else history.conversation(entry.packageName, conversation)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshAccess() {
        _access.value = history.hasAccess()
    }

    fun search(text: String) {
        _query.value = _query.value.copy(search = text, limit = PAGE)
    }

    fun filter(packageName: String?) {
        _query.value = _query.value.copy(packageName = packageName, limit = PAGE)
    }

    fun loadMore() {
        _query.value = _query.value.copy(limit = _query.value.limit + PAGE)
    }

    fun open(entry: HistoryEntry?) {
        _open.value = entry
    }

    fun delete(entry: HistoryEntry) {
        _open.value = null
        viewModelScope.launch { history.delete(entry.id) }
    }

    fun deleteApp(packageName: String) {
        viewModelScope.launch { history.deleteApp(packageName) }
        if (_query.value.packageName == packageName) filter(null)
    }

    fun clear() {
        viewModelScope.launch { history.clear() }
    }

    fun update(transform: (HistorySettings) -> HistorySettings) {
        viewModelScope.launch { settingsStore.update(transform) }
    }

    fun exclude(packageName: String) = update { it.copy(excluded = it.excluded + packageName) }

    fun include(packageName: String) = update { it.copy(excluded = it.excluded - packageName) }

    private companion object {
        const val SEARCH_DELAY_MS = 200L
    }
}

sealed interface NotificationHistoryDestination {
    /** Android's notification access page. */
    data object NotificationAccess : NotificationHistoryDestination

    data class OpenApp(val packageName: String) : NotificationHistoryDestination
}

fun NavGraphBuilder.registerNotificationHistoryRoute(onNavigate: (NotificationHistoryDestination) -> Unit) {
    composable<NotificationHistoryRoute> {
        NotificationHistoryScreen(viewModel = hiltViewModel(), onNavigate = onNavigate)
    }
}
