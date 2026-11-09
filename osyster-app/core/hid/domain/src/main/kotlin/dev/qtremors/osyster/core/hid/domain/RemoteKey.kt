package dev.qtremors.osyster.core.hid.domain

/**
 * Remote buttons, each a Consumer page usage that Android TV and most TVs and PCs understand.
 * Hold-to-repeat works because a key stays down until [HidReports.RELEASE] is sent.
 */
enum class RemoteKey(val usage: Int) {
    POWER(0x30),
    MENU(0x40),
    SELECT(0x41),
    UP(0x42),
    DOWN(0x43),
    LEFT(0x44),
    RIGHT(0x45),
    CAPTIONS(0x61),
    RED(0x69),
    GREEN(0x6A),
    BLUE(0x6B),
    YELLOW(0x6C),
    BRIGHTNESS_UP(0x6F),
    BRIGHTNESS_DOWN(0x70),
    CHANNEL_UP(0x9C),
    CHANNEL_DOWN(0x9D),
    PLAY(0xB0),
    PAUSE(0xB1),
    FAST_FORWARD(0xB3),
    REWIND(0xB4),
    NEXT(0xB5),
    PREVIOUS(0xB6),
    STOP(0xB7),
    REPEAT(0xBC),
    PLAY_PAUSE(0xCD),
    MUTE(0xE2),
    VOLUME_UP(0xE9),
    VOLUME_DOWN(0xEA),
    EMAIL(0x18A),
    CALCULATOR(0x192),
    FILES(0x194),
    BROWSER(0x196),
    SEARCH(0x221),
    HOME(0x223),
    BACK(0x224)
}

/** The power keys of the System control page; Windows and Linux follow them, macOS mostly does not. */
enum class SystemKey(val bit: Int) {
    POWER_DOWN(0x01),
    SLEEP(0x02),
    WAKE(0x04)
}

/** One keyboard press: the held modifier bits ([Modifiers]) and one key code ([KeyCodes]). */
data class KeyStroke(val key: Int, val modifiers: Int = 0)

/** Modifier bits of a keyboard report. */
object Modifiers {
    const val CTRL = 0x01
    const val SHIFT = 0x02
    const val ALT = 0x04
    const val META = 0x08
}

/** Keyboard page key codes for the keys that are not characters. */
object KeyCodes {
    const val ENTER = 0x28
    const val ESCAPE = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val F1 = 0x3A
    const val PRINT_SCREEN = 0x46
    const val INSERT = 0x49
    const val HOME = 0x4A
    const val PAGE_UP = 0x4B
    const val DELETE = 0x4C
    const val END = 0x4D
    const val PAGE_DOWN = 0x4E
    const val RIGHT = 0x4F
    const val LEFT = 0x50
    const val DOWN = 0x51
    const val UP = 0x52

    /** F1 to F12 in order. */
    fun function(number: Int): Int {
        require(number in 1..12) { "F$number does not exist" }
        return F1 + number - 1
    }

    /** The number row: 1 to 9, then 0. */
    fun digit(digit: Int): Int {
        require(digit in 0..9) { "$digit is not a digit" }
        return if (digit == 0) 0x27 else 0x1E + digit - 1
    }
}
