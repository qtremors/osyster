package dev.qtremors.osyster.core.privilege.domain

import kotlinx.coroutines.flow.StateFlow

/** Where Shizuku stands right now. Osyster never installs or starts it; it only asks to use it. */
enum class ShizukuState {
    /** Shizuku is not installed, or installed but its service is not running. */
    NOT_RUNNING,

    /** Running, but a version too old for the user service Osyster uses (before v11). */
    UNSUPPORTED,

    /** Running; the user has not allowed Osyster yet. */
    NEEDS_PERMISSION,

    /** Running and allowed: shell commands work. */
    READY
}

/**
 * What the power tools can do on this phone. [secureSettings] means WRITE_SECURE_SETTINGS was
 * granted, by ADB once or by Shizuku or root; it survives reboots and needs neither afterwards.
 * Shell commands (freezing, standby buckets, display density) need Shizuku or root while in use.
 */
data class PrivilegeStatus(
    val secureSettings: Boolean = false,
    val shizuku: ShizukuState = ShizukuState.NOT_RUNNING,
    /** The user turned root on in Power setup and the last `su` check succeeded. */
    val root: Boolean = false
) {
    val shell: Boolean get() = shizuku == ShizukuState.READY || root

    /** Secure and global settings can be written, directly or through the shell. */
    val canWriteSettings: Boolean get() = secureSettings || shell

    /** Which route a shell command takes, for the status line in Power setup. */
    val shellRoute: ShellRoute
        get() = when {
            shizuku == ShizukuState.READY -> ShellRoute.SHIZUKU
            root -> ShellRoute.ROOT
            else -> ShellRoute.NONE
        }
}

enum class ShellRoute { NONE, SHIZUKU, ROOT }

/** The outcome of one shell command. [exitCode] is -1 when no shell was available to run it. */
data class ShellResult(val exitCode: Int, val output: String, val error: String) {
    val ok: Boolean get() = exitCode == 0

    companion object {
        val Unavailable = ShellResult(-1, "", "No shell access")
    }
}

/**
 * Runs commands as the shell user (Shizuku) or as root (su). Every command is built with
 * [ShellCommand], which quotes arguments, so values from the UI can never become extra commands.
 */
interface PrivilegedShell {
    val status: StateFlow<PrivilegeStatus>

    /** Re-reads permission, Shizuku, and root state, for example when the app resumes. */
    fun refresh()

    /** Shows Shizuku's own permission prompt. The result arrives through [status]. */
    fun requestShizukuPermission()

    /** Asks for root once (the su manager shows its prompt) and remembers the choice if it works. */
    suspend fun enableRoot(): Boolean

    fun disableRoot()

    suspend fun run(command: ShellCommand): ShellResult

    /** Uses the shell to grant WRITE_SECURE_SETTINGS to Osyster, so settings work without it later. */
    suspend fun grantSecureSettings(): Boolean
}
