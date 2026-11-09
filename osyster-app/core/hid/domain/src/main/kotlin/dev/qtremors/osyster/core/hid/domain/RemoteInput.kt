package dev.qtremors.osyster.core.hid.domain

/** What the remote screen shows for a device. Saved per device, so each one opens how it was left. */
enum class RemoteInput { REMOTE, TOUCHPAD, KEYBOARD, GAMEPAD }

/** The input a device opens on before one is picked: computers the touchpad, the rest the remote. */
fun defaultInput(layout: RemoteLayout): RemoteInput =
    if (layout == RemoteLayout.COMPUTER) RemoteInput.TOUCHPAD else RemoteInput.REMOTE

/** How a typing field changed: delete [backspaces] characters, then type [added]. */
data class TypingChange(val backspaces: Int, val added: String)

/**
 * The keys that turn [old] into [new] when only the end of the text can change, which is how
 * the remote keyboard field is typed: anything after the first difference is deleted and typed
 * again. Swipe typing adds a word at once; autocorrect replaces the last word.
 */
fun typingChange(old: String, new: String): TypingChange {
    val common = old.commonPrefixWith(new).length
    return TypingChange(backspaces = old.length - common, added = new.substring(common))
}
