package dev.qtremors.osyster.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.preferences.domain.AppStartRepository
import dev.qtremors.osyster.core.preferences.domain.StartScreen
import dev.qtremors.osyster.core.preferences.domain.startScreenFor
import dev.qtremors.osyster.core.system.domain.FlashlightPulse
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Which optional access the user has already granted, re-read whenever the app resumes. */
data class AccessStatus(val usage: Boolean, val notificationAccess: Boolean)

@HiltViewModel
class StartGateViewModel @Inject constructor(
    private val appStart: AppStartRepository,
    private val dataUsage: DataUsageSource,
    private val pulse: FlashlightPulse
) : ViewModel() {
    /**
     * Decided once per launch from the stored state, then only ever moves to NONE. Resetting the
     * welcome from Settings therefore waits for the next start instead of covering Settings now.
     * Null until the stored state loads, so nothing flashes on a normal start.
     */
    private val _screen = MutableStateFlow<StartScreen?>(null)
    val screen: StateFlow<StartScreen?> = _screen.asStateFlow()

    private val _access = MutableStateFlow(readAccess())
    val access: StateFlow<AccessStatus> = _access.asStateFlow()

    init {
        viewModelScope.launch { _screen.value = startScreenFor(appStart.state.first()) }
    }

    fun refreshAccess() {
        _access.value = readAccess()
    }

    fun finishWelcome() {
        _screen.value = StartScreen.NONE
        viewModelScope.launch { appStart.finishOnboarding() }
    }


    private fun readAccess() = AccessStatus(
        usage = dataUsage.accessState() == UsageAccessState.GRANTED,
        notificationAccess = pulse.hasNotificationAccess()
    )
}
