package dev.qtremors.osyster.core.hid.domain

import kotlinx.coroutines.flow.Flow

/** How the remote moves the TV's selection. */
enum class RemoteNavigation {
    /** Four arrow buttons around OK. */
    DPAD,

    /** Swipe in a direction to move, tap to select. */
    SWIPE
}

data class RemoteSettings(
    val navigation: RemoteNavigation = RemoteNavigation.DPAD,
    /** Only the D-pad, Back, Home, and volume. */
    val minimal: Boolean = false,
    /** OK sends the keyboard's Enter key, for PCs and TVs that ignore the remote's Select. */
    val enterSelects: Boolean = false,
    /** Play and pause buttons in the notification while connected. */
    val notificationButtons: Boolean = true,
    /** Pointer speed, 0.5 to 3. */
    val mouseSpeed: Float = 1f,
    /** Scroll speed, 0.5 to 3. */
    val scrollSpeed: Float = 1f,
    val invertScroll: Boolean = false,
    /** Point the phone to move the pointer, instead of the touchpad. */
    val airMouse: Boolean = false,
    /** While the remote is open, the phone's volume keys change the TV's volume. */
    val volumeKeys: Boolean = false
)

/** The remote's settings and the last connected device, kept on the phone only. */
interface RemoteSettingsStore {
    val settings: Flow<RemoteSettings>
    suspend fun update(transform: (RemoteSettings) -> RemoteSettings)

    /**
     * The remote layout chosen for each device, by Bluetooth address. A device with no entry is
     * [LayoutChoice.AUTO]: it gets the layout its device kind suggests.
     */
    val deviceLayouts: Flow<Map<String, LayoutChoice>>
    suspend fun setDeviceLayout(id: String, choice: LayoutChoice)

    /** The last device that connected, by its Bluetooth address; null when none is saved. */
    suspend fun lastDeviceId(): String?
    suspend fun setLastDeviceId(id: String?)
}
