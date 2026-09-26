package dev.qtremors.osyster.core.privilege.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.privilege.domain.PowerCommands
import dev.qtremors.osyster.core.privilege.domain.PrivilegeStatus
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.ShellCommand
import dev.qtremors.osyster.core.privilege.domain.ShellResult
import dev.qtremors.osyster.core.privilege.domain.ShizukuState
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Chooses how a command runs: Shizuku first (no root needed, shell user), then root if the user
 * turned it on. Nothing runs when neither is available; callers show what to set up instead.
 */
@Singleton
internal class PrivilegedShellImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : PrivilegedShell {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val shizuku = ShizukuShell(context.packageName)
    private val root = RootShell()

    private val _status = MutableStateFlow(readStatus())
    override val status: StateFlow<PrivilegeStatus> = _status.asStateFlow()

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        shizuku.release()
        refresh()
    }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { code, _ ->
        if (code == PERMISSION_REQUEST) refresh()
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
    }

    override fun refresh() {
        _status.value = readStatus()
    }

    override fun requestShizukuPermission() {
        if (shizuku.state() == ShizukuState.NEEDS_PERMISSION) {
            runCatching { Shizuku.requestPermission(PERMISSION_REQUEST) }
        }
        refresh()
    }

    override suspend fun enableRoot(): Boolean {
        val works = withContext(dispatchers.io) {
            val result = root.exec(PowerCommands.whoAmI.text)
            result.ok && result.output.trim() == "0"
        }
        prefs.edit().putBoolean(KEY_ROOT, works).apply()
        refresh()
        return works
    }

    override fun disableRoot() {
        prefs.edit().putBoolean(KEY_ROOT, false).apply()
        refresh()
    }

    override suspend fun run(command: ShellCommand): ShellResult = withContext(dispatchers.io) {
        val current = readStatus()
        when {
            current.shizuku == ShizukuState.READY -> shizuku.exec(command.text)
            current.root -> root.exec(command.text)
            else -> ShellResult.Unavailable
        }
    }

    override suspend fun grantSecureSettings(): Boolean {
        run(PowerCommands.grantSecureSettings(context.packageName))
        refresh()
        return status.value.secureSettings
    }

    private fun readStatus() = PrivilegeStatus(
        secureSettings = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED,
        shizuku = shizuku.state(),
        root = prefs.getBoolean(KEY_ROOT, false)
    )

    private companion object {
        const val PREFS = "privilege"
        const val KEY_ROOT = "root_enabled"
        const val PERMISSION_REQUEST = 4207
    }
}
