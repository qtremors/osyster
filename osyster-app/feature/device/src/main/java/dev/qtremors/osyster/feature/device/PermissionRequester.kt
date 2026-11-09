package dev.qtremors.osyster.feature.device

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import dev.qtremors.osyster.core.device.domain.PermissionKind

/**
 * Asks Android for the runtime permissions behind a [PermissionKind]. Live pages refresh on their
 * own, so a grant shows up on the next tick without reopening the page.
 */
@Composable
internal fun rememberPermissionRequester(onResult: (granted: Boolean) -> Unit): (PermissionKind) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        onResult(results.isNotEmpty() && results.values.any { it })
    }
    return { kind ->
        val permissions = runtimePermissions(kind)
        if (permissions.isEmpty()) onResult(false) else launcher.launch(permissions)
    }
}

/** Kinds that are special access (usage, notification access, secure settings) have no runtime prompt. */
internal fun runtimePermissions(kind: PermissionKind): Array<String> = when (kind) {
    PermissionKind.LOCATION -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    PermissionKind.PHONE_STATE -> arrayOf(Manifest.permission.READ_PHONE_STATE)
    PermissionKind.BLUETOOTH ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyArray()
    PermissionKind.NOTIFICATIONS ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
    PermissionKind.USAGE_ACCESS, PermissionKind.NOTIFICATION_ACCESS, PermissionKind.WRITE_SECURE_SETTINGS -> emptyArray()
}
