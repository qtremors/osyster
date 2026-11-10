package dev.qtremors.osyster.core.hid.domain

/**
 * Turns typed text into key presses on a US English keyboard layout, the layout Android TV and
 * most computers assume for an unknown Bluetooth keyboard. Characters a US keyboard cannot type
 * (accents, emoji) are skipped rather than sent as a wrong key.
 */
object UsKeyboard {
    private val unshifted: Map<Char, Int> = buildMap {
        ('a'..'z').forEachIndexed { index, c -> put(c, 0x04 + index) }
        "1234567890".forEachIndexed { index, c -> put(c, 0x1E + index) }
        put('\n', KeyCodes.ENTER)
        put('\t', KeyCodes.TAB)
        put(' ', KeyCodes.SPACE)
        put('-', 0x2D); put('=', 0x2E); put('[', 0x2F); put(']', 0x30); put('\\', 0x31)
        put(';', 0x33); put('\'', 0x34); put('`', 0x35); put(',', 0x36); put('.', 0x37); put('/', 0x38)
    }

    private val shifted: Map<Char, Int> = buildMap {
        ('A'..'Z').forEachIndexed { index, c -> put(c, 0x04 + index) }
        "!@#$%^&*()".forEachIndexed { index, c -> put(c, 0x1E + index) }
        put('_', 0x2D); put('+', 0x2E); put('{', 0x2F); put('}', 0x30); put('|', 0x31)
        put(':', 0x33); put('"', 0x34); put('~', 0x35); put('<', 0x36); put('>', 0x37); put('?', 0x38)
    }

    /** The press for [c], or null when a US keyboard has no key for it. */
    fun strokeFor(c: Char): KeyStroke? =
        unshifted[c]?.let { KeyStroke(it) } ?: shifted[c]?.let { KeyStroke(it, Modifiers.SHIFT) }

    /** Every press needed to type [text], in order, skipping characters with no key. */
    fun strokesFor(text: String): List<KeyStroke> = text.mapNotNull(::strokeFor)
}
