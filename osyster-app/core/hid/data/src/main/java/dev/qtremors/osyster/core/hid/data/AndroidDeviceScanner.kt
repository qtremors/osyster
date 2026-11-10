package dev.qtremors.osyster.core.hid.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.hid.domain.DeviceKind
import dev.qtremors.osyster.core.hid.domain.DeviceScanner
import dev.qtremors.osyster.core.hid.domain.RemoteDevice
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Bluetooth discovery for devices not paired yet. Needs Nearby devices (Android 12+) or location
 * (Android 11 and older); without it [start] returns false and the screen asks for it.
 * Discovery only lists names, so it is declared neverForLocation on Android 12+.
 */
@SuppressLint("MissingPermission")
@Singleton
internal class AndroidDeviceScanner @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DeviceScanner {
    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val devices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    private val _scanning = MutableStateFlow(false)
    override val found: Flow<List<RemoteDevice>> = devices.asStateFlow()
    override val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
                    val entry = runCatching { device.toRemoteDevice() }.getOrNull() ?: return
                    if (runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)) return
                    devices.update { list -> if (list.any { it.id == entry.id }) list else list + entry }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _scanning.value = false
            }
        }
    }

    override fun start(): Boolean {
        val bt = adapter ?: return false
        if (!registered) {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                },
                ContextCompat.RECEIVER_EXPORTED
            )
            registered = true
        }
        devices.value = emptyList()
        val started = runCatching {
            if (bt.isDiscovering) bt.cancelDiscovery()
            bt.startDiscovery()
        }.getOrDefault(false)
        _scanning.value = started
        return started
    }

    override fun stop() {
        runCatching { adapter?.cancelDiscovery() }
        _scanning.value = false
        if (registered) {
            runCatching { context.unregisterReceiver(receiver) }
            registered = false
        }
    }

    private fun BluetoothDevice.toRemoteDevice(): RemoteDevice = RemoteDevice(
        id = address,
        name = name.orEmpty(),
        kind = when (bluetoothClass?.majorDeviceClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceKind.TV
            BluetoothClass.Device.Major.COMPUTER -> DeviceKind.COMPUTER
            BluetoothClass.Device.Major.PHONE -> DeviceKind.PHONE
            else -> DeviceKind.OTHER
        }
    )
}
