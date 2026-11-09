package dev.qtremors.osyster.core.hid.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.hid.domain.Connection
import dev.qtremors.osyster.core.hid.domain.DeviceKind
import dev.qtremors.osyster.core.hid.domain.HidRemote
import dev.qtremors.osyster.core.hid.domain.HidReports
import dev.qtremors.osyster.core.hid.domain.HidState
import dev.qtremors.osyster.core.hid.domain.HidSupport
import dev.qtremors.osyster.core.hid.domain.KeyStroke
import dev.qtremors.osyster.core.hid.domain.RemoteDevice
import dev.qtremors.osyster.core.hid.domain.RemoteKey
import dev.qtremors.osyster.core.hid.domain.RemoteSettingsStore
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Osyster registered with Android as a Bluetooth keyboard, mouse, and remote (Android 9+), adapted
 * from BT Remote by Atharok (GPLv3). Every Bluetooth call is behind [HidState.support]: nothing
 * runs before Nearby devices is granted on Android 12+, which is why MissingPermission is suppressed.
 */
@SuppressLint("MissingPermission")
@Singleton
internal class AndroidHidRemote @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: RemoteSettingsStore,
    dispatchers: AppDispatchers
) : HidRemote {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val _state = MutableStateFlow(HidState(support = readSupport()))
    override val state: StateFlow<HidState> = _state.asStateFlow()

    private var hid: BluetoothHidDevice? = null
    private var target: BluetoothDevice? = null
    private var reconnectOnRegister = false

    init {
        // Bluetooth turned off or on while the remote is open.
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = refresh()
            },
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun refresh() {
        val support = readSupport()
        _state.update { it.copy(support = support) }
        if (support != HidSupport.READY && hid != null) stop()
    }

    private fun readSupport(): HidSupport = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P || adapter == null -> HidSupport.UNSUPPORTED
        !hasConnectPermission() -> HidSupport.NEEDS_PERMISSION
        !adapter.isEnabled -> HidSupport.BLUETOOTH_OFF
        else -> HidSupport.READY
    }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun ready(): Boolean = readSupport() == HidSupport.READY

    override fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !ready() || hid != null) return
        reconnectOnRegister = true
        adapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    override fun stop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        hid?.let { proxy ->
            if (hasConnectPermission()) {
                target?.let { proxy.disconnect(it) }
                proxy.unregisterApp()
            }
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
        }
        hid = null
        target = null
        _state.update { HidState(support = it.support) }
    }

    override fun connect(device: RemoteDevice) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !ready()) return
        val proxy = hid ?: return
        val remote = runCatching { adapter?.getRemoteDevice(device.id) }.getOrNull() ?: return
        target = remote
        _state.update { it.copy(connection = Connection.CONNECTING, device = device) }
        if (!proxy.connect(remote)) _state.update { it.copy(connection = Connection.DISCONNECTED) }
    }

    override fun disconnect() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasConnectPermission()) return
        val proxy = hid ?: return
        val remote = target ?: return
        _state.update { it.copy(connection = Connection.DISCONNECTING) }
        // Twice: on some phones one call leaves the link stuck in DISCONNECTING (seen by BT Remote).
        proxy.disconnect(remote)
        proxy.disconnect(remote)
    }

    override fun pairedDevices(): List<RemoteDevice> {
        if (!ready()) return emptyList()
        return runCatching { adapter?.bondedDevices.orEmpty().map { it.toRemoteDevice() } }
            .getOrDefault(emptyList())
            .sortedWith(compareBy({ it.kind.ordinal }, { it.name.lowercase() }))
    }

    override fun localName(): String? = if (ready()) runCatching { adapter?.name }.getOrNull() else null

    // ---- Reports ----

    private fun send(id: Int, bytes: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !_state.value.connected) return false
        val proxy = hid ?: return false
        val remote = target ?: return false
        return runCatching { proxy.sendReport(remote, id, bytes) }.getOrDefault(false)
    }

    override fun hold(key: RemoteKey) {
        send(HidReports.REMOTE_ID, HidReports.remote(key))
    }

    override fun release() {
        send(HidReports.REMOTE_ID, HidReports.RELEASE)
    }

    override suspend fun tap(key: RemoteKey) {
        hold(key)
        delay(TAP_MILLIS)
        release()
    }

    override fun holdKeyboard(stroke: KeyStroke) {
        send(HidReports.KEYBOARD_ID, HidReports.keyboard(stroke))
    }

    override fun releaseKeyboard() {
        send(HidReports.KEYBOARD_ID, HidReports.RELEASE)
    }

    override suspend fun type(strokes: List<KeyStroke>) {
        strokes.forEach { stroke ->
            holdKeyboard(stroke)
            delay(KEY_GAP_MILLIS)
            releaseKeyboard()
            delay(KEY_GAP_MILLIS)
        }
    }

    override fun mouse(buttons: Int, dx: Int, dy: Int, wheel: Int) {
        send(HidReports.MOUSE_ID, HidReports.mouse(buttons, dx, dy, wheel))
    }

    // ---- Android callbacks ----

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || profile != BluetoothProfile.HID_DEVICE) return
            val device = proxy as? BluetoothHidDevice ?: return
            hid = device
            if (hasConnectPermission()) register(device)
        }

        override fun onServiceDisconnected(profile: Int) {
            hid = null
            target = null
            _state.update { HidState(support = it.support) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun register(device: BluetoothHidDevice) {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            context.applicationInfo.loadLabel(context.packageManager).toString(),
            SDP_DESCRIPTION,
            SDP_PROVIDER,
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidReports.descriptor
        )
        device.registerApp(sdp, null, null, { it.run() }, hidCallback())
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun hidCallback() = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _state.update { it.copy(registered = registered) }
            if (registered && reconnectOnRegister && !_state.value.connected) {
                reconnectOnRegister = false
                scope.launch { reconnectLast() }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val connection = when (state) {
                BluetoothProfile.STATE_CONNECTING -> Connection.CONNECTING
                BluetoothProfile.STATE_CONNECTED -> Connection.CONNECTED
                BluetoothProfile.STATE_DISCONNECTING -> Connection.DISCONNECTING
                else -> Connection.DISCONNECTED
            }
            val remote = device?.let { runCatching { it.toRemoteDevice() }.getOrNull() }
            if (connection == Connection.CONNECTED && device != null) {
                target = device
                scope.launch { store.setLastDeviceId(device.address) }
            }
            if (connection == Connection.DISCONNECTED) target = null
            _state.update { it.copy(connection = connection, device = remote ?: it.device) }
        }
    }

    /** Reconnects to the device that was connected last, if it is still paired. */
    private suspend fun reconnectLast() {
        val id = store.lastDeviceId() ?: return
        pairedDevices().firstOrNull { it.id == id }?.let(::connect)
    }

    private fun BluetoothDevice.toRemoteDevice(): RemoteDevice = RemoteDevice(
        id = address,
        name = name?.takeIf { it.isNotBlank() } ?: UNNAMED,
        kind = when (bluetoothClass?.majorDeviceClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceKind.TV
            BluetoothClass.Device.Major.COMPUTER -> DeviceKind.COMPUTER
            BluetoothClass.Device.Major.PHONE -> DeviceKind.PHONE
            else -> DeviceKind.OTHER
        }
    )

    private companion object {
        const val TAP_MILLIS = 40L
        const val KEY_GAP_MILLIS = 25L
        const val SDP_DESCRIPTION = "Keyboard, mouse, and remote"
        const val SDP_PROVIDER = "Osyster"

        /** A device that reports no name. The UI shows its own "Unnamed device" text for an empty name. */
        const val UNNAMED = ""
    }
}
