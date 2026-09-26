package dev.qtremors.osyster.core.privilege.data

import androidx.annotation.Keep
import kotlin.system.exitProcess

/**
 * Shizuku starts this class by name in its own process, running as the shell user, and Osyster
 * calls it through [IShellService]. [Keep] stops R8 from renaming or removing it, since nothing in
 * Osyster's own process ever constructs it.
 */
@Keep
class ShellUserService : IShellService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): Array<String> = ShellProcess.run(listOf("sh", "-c", command))
}
