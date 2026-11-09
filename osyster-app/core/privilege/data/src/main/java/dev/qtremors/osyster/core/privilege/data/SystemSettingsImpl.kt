package dev.qtremors.osyster.core.privilege.data

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.privilege.domain.PowerCommands
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SettingKey
import dev.qtremors.osyster.core.privilege.domain.SettingsTable
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

/**
 * Settings through the public provider API first. When Android refuses (reading keys that are not
 * marked readable since Android 12, or writing without WRITE_SECURE_SETTINGS), the same operation
 * goes through `settings` in the shell if Shizuku or root is set up.
 */
@Singleton
internal class SystemSettingsImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val shell: PrivilegedShell,
    private val dispatchers: AppDispatchers
) : SystemSettings {
    private val resolver: ContentResolver = context.contentResolver

    override suspend fun read(key: SettingKey): String? = withContext(dispatchers.io) {
        try {
            direct(key)
        } catch (_: SecurityException) {
            readThroughShell(key)
        }
    }

    override fun changes(keys: Set<SettingKey>): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        keys.forEach { key -> resolver.registerContentObserver(uriOf(key), false, observer) }
        trySend(Unit)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate()

    override suspend fun write(key: SettingKey, value: String?): Boolean = withContext(dispatchers.io) {
        if (shell.status.value.secureSettings && writeDirect(key, value)) return@withContext true
        if (!shell.status.value.shell) return@withContext false
        val command = if (value == null) PowerCommands.deleteSetting(key.table, key.name) else PowerCommands.putSetting(key.table, key.name, value)
        shell.run(command).ok
    }

    private fun direct(key: SettingKey): String? = when (key.table) {
        SettingsTable.SYSTEM -> Settings.System.getString(resolver, key.name)
        SettingsTable.SECURE -> Settings.Secure.getString(resolver, key.name)
        SettingsTable.GLOBAL -> Settings.Global.getString(resolver, key.name)
    }

    private suspend fun readThroughShell(key: SettingKey): String? {
        if (!shell.status.value.shell) return null
        val result = shell.run(PowerCommands.getSetting(key.table, key.name))
        return result.output.trim().takeIf { result.ok && it.isNotEmpty() && it != "null" }
    }

    private fun writeDirect(key: SettingKey, value: String?): Boolean = try {
        when (key.table) {
            SettingsTable.SYSTEM -> Settings.System.putString(resolver, key.name, value)
            SettingsTable.SECURE -> Settings.Secure.putString(resolver, key.name, value)
            SettingsTable.GLOBAL -> Settings.Global.putString(resolver, key.name, value)
        }
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        // System settings reject keys outside the public set unless written by the shell.
        false
    }

    private fun uriOf(key: SettingKey): Uri = when (key.table) {
        SettingsTable.SYSTEM -> Settings.System.getUriFor(key.name)
        SettingsTable.SECURE -> Settings.Secure.getUriFor(key.name)
        SettingsTable.GLOBAL -> Settings.Global.getUriFor(key.name)
    }
}
