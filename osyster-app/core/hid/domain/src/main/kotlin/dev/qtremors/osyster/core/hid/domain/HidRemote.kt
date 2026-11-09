package dev.qtremors.osyster.core.hid.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Why the remote can or cannot run right now, checked in this order. */
enum class HidSupport {
    /** Android 8, or no Bluetooth: the phone cannot act as a Bluetooth keyboard. */
    UNSUPPORTED,

    /** Android 12+ and Nearby devices is not granted yet. */
    NEEDS_PERMISSION,

    /** Bluetooth is off. */
    BLUETOOTH_OFF,

    READY
}

enum class Connection { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING }

/** What kind of device a TV, PC, or phone says it is, for its icon. */
enum class DeviceKind { TV, COMPUTER, PHONE, OTHER }

/**
 * A device the remote can control. [id] is its Bluetooth address: kept inside Osyster to connect
 * and reconnect, and never shown on screen.
 */
data class RemoteDevice(val id: String, val name: String, val kind: DeviceKind)

data class HidState(
    val support: HidSupport = HidSupport.UNSUPPORTED,
    /** True once Android accepted Osyster as a keyboard and mouse; only then can devices connect. */
    val registered: Boolean = false,
    val connection: Connection = Connection.DISCONNECTED,
    val device: RemoteDevice? = null
) {
    val connected: Boolean get() = connection == Connection.CONNECTED
}

/**
 * The phone as a Bluetooth keyboard, mouse, and remote. One shared instance: the remote screen,
 * its notification, and its widget all drive the same connection.
 */
interface HidRemote {
    val state: StateFlow<HidState>

    /** Re-reads permission and Bluetooth state, for example when the screen resumes. */
    fun refresh()

    /** Registers with Android so devices can connect, then reconnects to the last device if there is one. */
    fun start()

    /** Disconnects and unregisters. */
    fun stop()

    fun connect(device: RemoteDevice)
    fun disconnect()

    /** Devices already paired with this phone, TVs and computers first. */
    fun pairedDevices(): List<RemoteDevice>

    /** This phone's Bluetooth name, as a TV shows it when pairing from the TV. */
    fun localName(): String?

    /** Presses and holds [key] until [release]. */
    fun hold(key: RemoteKey)

    /** Presses and lets go of [key]. */
    suspend fun tap(key: RemoteKey)

    /** Presses and holds one keyboard key until [releaseKeyboard]. */
    fun holdKeyboard(stroke: KeyStroke)

    /** Types [strokes] one after another, each pressed and let go. */
    suspend fun type(strokes: List<KeyStroke>)

    fun release()
    fun releaseKeyboard()

    /** Sends one mouse report; see [HidReports.mouse]. */
    fun mouse(buttons: Int = 0, dx: Int = 0, dy: Int = 0, wheel: Int = 0)
}

/** Finds nearby Bluetooth devices that are not paired yet. */
interface DeviceScanner {
    /** Devices found since [start], updated as more are found. */
    val found: Flow<List<RemoteDevice>>
    val scanning: StateFlow<Boolean>

    /** Starts a scan, about 12 seconds long. False when Android refused (permission or Bluetooth off). */
    fun start(): Boolean
    fun stop()
}
