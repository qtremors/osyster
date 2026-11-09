package dev.qtremors.osyster.core.hid.domain

/** Which remote the screen shows, matched to what the connected device understands. */
enum class RemoteLayout {
    /** The full TV remote: power, menu, channels, media, and color keys. */
    TV,

    /** Arrows, Enter, Esc, and the Windows or Super key, plus media and volume. */
    COMPUTER,

    /** Android's own keys, without power, channels, or color keys. */
    TABLET
}

/** The "Remote for" setting: follow the device, or always use one layout. */
enum class LayoutChoice { AUTO, TV, COMPUTER, TABLET }

/**
 * The layout for a connected device of [kind]. Automatic follows the Bluetooth device class each
 * device reports; an unknown kind gets the TV remote, which has every key.
 */
fun layoutFor(choice: LayoutChoice, kind: DeviceKind?): RemoteLayout = when (choice) {
    LayoutChoice.TV -> RemoteLayout.TV
    LayoutChoice.COMPUTER -> RemoteLayout.COMPUTER
    LayoutChoice.TABLET -> RemoteLayout.TABLET
    LayoutChoice.AUTO -> when (kind) {
        DeviceKind.COMPUTER -> RemoteLayout.COMPUTER
        DeviceKind.PHONE -> RemoteLayout.TABLET
        DeviceKind.TV, DeviceKind.OTHER, null -> RemoteLayout.TV
    }
}

/**
 * The keyboard key to send instead of remote [key], or null to send the remote key itself.
 * Computers ignore the TV's menu navigation keys but follow keyboard arrows, Enter, and Esc;
 * [enterSelects] makes OK send Enter on any layout.
 */
fun keyboardFor(layout: RemoteLayout, key: RemoteKey, enterSelects: Boolean): KeyStroke? {
    if (key == RemoteKey.SELECT && (enterSelects || layout == RemoteLayout.COMPUTER)) return KeyStroke(KeyCodes.ENTER)
    if (layout != RemoteLayout.COMPUTER) return null
    return when (key) {
        RemoteKey.UP -> KeyStroke(KeyCodes.UP)
        RemoteKey.DOWN -> KeyStroke(KeyCodes.DOWN)
        RemoteKey.LEFT -> KeyStroke(KeyCodes.LEFT)
        RemoteKey.RIGHT -> KeyStroke(KeyCodes.RIGHT)
        RemoteKey.BACK -> KeyStroke(KeyCodes.ESCAPE)
        // The Windows key alone opens Start; on Linux and ChromeOS it opens the launcher.
        RemoteKey.HOME -> KeyStroke(key = 0, modifiers = Modifiers.META)
        else -> null
    }
}

/** Keys a layout leaves out because that kind of device has no use for them. */
fun RemoteLayout.shows(key: RemoteKey): Boolean = when (this) {
    RemoteLayout.TV -> true
    RemoteLayout.COMPUTER -> key !in COMPUTER_HIDDEN
    RemoteLayout.TABLET -> key !in TABLET_HIDDEN
}

private val TV_ONLY = setOf(
    RemoteKey.POWER, RemoteKey.CHANNEL_UP, RemoteKey.CHANNEL_DOWN,
    RemoteKey.RED, RemoteKey.GREEN, RemoteKey.YELLOW, RemoteKey.BLUE, RemoteKey.CAPTIONS
)
private val COMPUTER_HIDDEN = TV_ONLY + RemoteKey.MENU
private val TABLET_HIDDEN = TV_ONLY
