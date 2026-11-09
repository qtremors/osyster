package dev.qtremors.osyster.feature.powertools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.qtremors.osyster.core.privilege.domain.PrivilegeStatus
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SettingKey
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What every power tool page shares: the access status, one-off messages, and writes that say why
 * they failed. Pages built on settings re-read their state whenever one of their keys changes, so
 * a change made in the system Settings app shows up immediately.
 */
abstract class PowerViewModel(
    protected val shell: PrivilegedShell,
    protected val settings: SystemSettings
) : ViewModel() {
    val access: StateFlow<PrivilegeStatus> = shell.status

    private val _messages = MutableSharedFlow<PowerMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<PowerMessage> = _messages.asSharedFlow()

    fun refresh() = shell.refresh()

    protected fun say(message: PowerMessage) {
        _messages.tryEmit(message)
    }

    /** Writes and reports a missing-access or failure message when it does not stick. */
    protected fun write(key: SettingKey, value: String?) {
        viewModelScope.launch { writeNow(key, value) }
    }

    protected suspend fun writeNow(key: SettingKey, value: String?): Boolean {
        val ok = settings.write(key, value)
        if (!ok) say(if (access.value.canWriteSettings) PowerMessage.FAILED else PowerMessage.NO_ACCESS)
        return ok
    }

    /**
     * State read from [keys], re-read on every change and whenever access changes (a value that
     * was unreadable may become readable once Shizuku connects).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> settingsState(keys: Set<SettingKey>, initial: T, read: suspend () -> T): StateFlow<T> =
        combine(settings.changes(keys), shell.status) { _, _ -> }
            .mapLatest { read() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    protected fun <T> Flow<T>.stateInScope(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
