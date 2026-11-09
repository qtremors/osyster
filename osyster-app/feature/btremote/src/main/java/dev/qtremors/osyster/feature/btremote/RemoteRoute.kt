package dev.qtremors.osyster.feature.btremote

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.hid.domain.DeviceScanner
import dev.qtremors.osyster.core.hid.domain.HidRemote
import dev.qtremors.osyster.core.hid.domain.HidState
import dev.qtremors.osyster.core.hid.domain.HidSupport
import dev.qtremors.osyster.core.hid.domain.KeyStroke
import dev.qtremors.osyster.core.hid.domain.LayoutChoice
import dev.qtremors.osyster.core.hid.domain.RemoteDevice
import dev.qtremors.osyster.core.hid.domain.RemoteKey
import dev.qtremors.osyster.core.hid.domain.RemoteLayout
import dev.qtremors.osyster.core.hid.domain.keyboardFor
import dev.qtremors.osyster.core.hid.domain.layoutFor
import dev.qtremors.osyster.core.hid.domain.RemoteSettings
import dev.qtremors.osyster.core.hid.domain.RemoteSettingsStore
import dev.qtremors.osyster.core.navigation.BtRemoteRoute
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val hid: HidRemote,
    private val scanner: DeviceScanner,
    private val store: RemoteSettingsStore
) : ViewModel() {
    val state: StateFlow<HidState> = hid.state
    val settings: StateFlow<RemoteSettings> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, RemoteSettings())
    val found: StateFlow<List<RemoteDevice>> = scanner.found.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val scanning: StateFlow<Boolean> = scanner.scanning

    private val _paired = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val paired: StateFlow<List<RemoteDevice>> = _paired.asStateFlow()
    private val _lastId = MutableStateFlow<String?>(null)
    val lastId: StateFlow<String?> = _lastId.asStateFlow()

    fun refresh() {
        hid.refresh()
        _paired.value = hid.pairedDevices()
        viewModelScope.launch { _lastId.value = store.lastDeviceId() }
    }

    fun localName(): String? = hid.localName()

    fun connect(device: RemoteDevice) {
        scanner.stop()
        hid.connect(device)
    }

    fun disconnect() = hid.disconnect()

    /** Each device's own layout choice, by Bluetooth address; missing means Automatic. */
    val deviceLayouts: StateFlow<Map<String, LayoutChoice>> = store.deviceLayouts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** The connected device's layout choice. */
    val layoutChoice: StateFlow<LayoutChoice> = combine(hid.state, deviceLayouts) { state, layouts ->
        state.device?.id?.let { layouts[it] } ?: LayoutChoice.AUTO
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LayoutChoice.AUTO)

    /** The remote for the connected device: its own choice, or on Automatic what its kind suggests. */
    val layout: StateFlow<RemoteLayout> = combine(hid.state, layoutChoice) { state, choice -> layoutFor(choice, state.device?.kind) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RemoteLayout.TV)

    /** Saves [choice] as [device]'s layout, used every time it connects. */
    fun setLayout(device: RemoteDevice, choice: LayoutChoice) {
        viewModelScope.launch { store.setDeviceLayout(device.id, choice) }
    }

    /** Whether the key now held went out as a keyboard key, so [release] lets go of the same one. */
    private var keyboardHeld = false

    /** Presses [key], or the keyboard key the layout uses for it (arrows, Enter, Esc on a computer). */
    fun hold(key: RemoteKey) {
        val stroke = keyboardFor(layout.value, key, settings.value.enterSelects)
        keyboardHeld = stroke != null
        if (stroke != null) hid.holdKeyboard(stroke) else hid.hold(key)
    }

    fun release() {
        if (keyboardHeld) hid.releaseKeyboard() else hid.release()
        keyboardHeld = false
    }

    fun tap(key: RemoteKey) {
        viewModelScope.launch { hid.tap(key) }
    }

    /** One digit or Enter on the channel pad, sent as keyboard keys like a remote's number pad. */
    fun typeKey(code: Int) {
        viewModelScope.launch { hid.type(listOf(KeyStroke(code))) }
    }

    fun startScan(): Boolean = scanner.start()
    fun stopScan() = scanner.stop()

    fun update(transform: (RemoteSettings) -> RemoteSettings) {
        viewModelScope.launch { store.update(transform) }
    }

    override fun onCleared() {
        scanner.stop()
    }
}

sealed interface BtRemoteDestination {
    /** Osyster's App info page, where a permission refused twice can still be turned on. */
    data class AppSettings(val packageName: String) : BtRemoteDestination
}

fun NavGraphBuilder.registerBtRemoteRoute(onNavigate: (BtRemoteDestination) -> Unit) {
    composable<BtRemoteRoute> {
        val viewModel: RemoteViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Registered only while the remote is in use: from opening this screen until it closes
        // with nothing connected (or, if connected, until that device disconnects).
        LaunchedEffect(state.support) {
            if (state.support == HidSupport.READY) RemoteService.start(context)
        }
        LaunchedEffect(state.registered, state.connection) { viewModel.refresh() }
        DisposableEffect(Unit) {
            RemoteService.screenOpen = true
            onDispose {
                RemoteService.screenOpen = false
                viewModel.stopScan()
                if (!viewModel.state.value.connected) RemoteService.stop(context)
            }
        }

        RemoteScreen(
            viewModel = viewModel,
            onAppSettings = { onNavigate(BtRemoteDestination.AppSettings(context.packageName)) }
        )
    }
}
