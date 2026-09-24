package dev.qtremors.osyster.core.system.data

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.system.domain.SecureSettingsAccess
import javax.inject.Inject
import javax.inject.Singleton

/** Checks the WRITE_SECURE_SETTINGS grant and gives the exact ADB command for this build. */
@Singleton
internal class SecureSettingsAccessImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SecureSettingsAccess {
    override fun isGranted(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    override val adbCommand: String =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
}
