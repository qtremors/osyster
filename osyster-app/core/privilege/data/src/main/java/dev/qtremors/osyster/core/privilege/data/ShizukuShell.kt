package dev.qtremors.osyster.core.privilege.data

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import dev.qtremors.osyster.core.privilege.domain.ShellResult
import dev.qtremors.osyster.core.privilege.domain.ShizukuState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/**
 * Binds [ShellUserService] through Shizuku and keeps the connection while it stays alive. The
 * service is not a daemon: Shizuku stops it when Osyster's process ends.
 */
internal class ShizukuShell(packageName: String) {
    private val args = Shizuku.UserServiceArgs(ComponentName(packageName, ShellUserService::class.java.name))
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(false)
        .version(SERVICE_VERSION)

    private val lock = Mutex()

    @Volatile
    private var service: IShellService? = null

    @Volatile
    private var pending: CompletableDeferred<IShellService?>? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val connected = binder?.takeIf { it.pingBinder() }?.let(IShellService.Stub::asInterface)
            service = connected
            pending?.complete(connected)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun state(): ShizukuState = try {
        when {
            !Shizuku.pingBinder() -> ShizukuState.NOT_RUNNING
            Shizuku.isPreV11() -> ShizukuState.UNSUPPORTED
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuState.READY
            else -> ShizukuState.NEEDS_PERMISSION
        }
    } catch (_: RuntimeException) {
        // Shizuku throws IllegalStateException when its binder vanishes mid-check.
        ShizukuState.NOT_RUNNING
    }

    /** Must be called with Shizuku READY. Binding happens on first use and is reused afterwards. */
    suspend fun exec(command: String): ShellResult {
        val shell = connect() ?: return ShellResult.Unavailable
        return try {
            shell.exec(command).toShellResult()
        } catch (error: RemoteException) {
            service = null
            ShellResult(-1, "", error.message.orEmpty())
        }
    }

    fun release() {
        runCatching { Shizuku.unbindUserService(args, connection, true) }
        service = null
    }

    private suspend fun connect(): IShellService? = lock.withLock {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return@withLock it }
        val waiting = CompletableDeferred<IShellService?>()
        pending = waiting
        val bound = runCatching { Shizuku.bindUserService(args, connection) }.isSuccess
        if (!bound) return@withLock null
        withTimeoutOrNull(CONNECT_TIMEOUT_MS) { waiting.await() }.also { pending = null }
    }

    private companion object {
        /** Bump when [ShellUserService] changes, so Shizuku replaces a running old copy. */
        const val SERVICE_VERSION = 1
        const val CONNECT_TIMEOUT_MS = 8_000L
    }
}

/** Root through `su`. Each command is its own `su -c`, so no root shell stays open in the background. */
internal class RootShell {
    fun exec(command: String): ShellResult = ShellProcess.run(listOf("su", "-c", command)).toShellResult()
}
