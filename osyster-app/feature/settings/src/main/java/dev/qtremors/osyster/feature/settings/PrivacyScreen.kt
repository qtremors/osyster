package dev.qtremors.osyster.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

private data class Promise(val title: Int, val detail: Int, val icon: ImageVector, val key: String)

private val promises = listOf(
    Promise(R.string.privacy_offline, R.string.privacy_offline_detail, Icons.Rounded.CloudOff, "offline"),
    Promise(R.string.privacy_local, R.string.privacy_local_detail, Icons.Rounded.Storage, "local"),
    Promise(R.string.privacy_identifiers, R.string.privacy_identifiers_detail, Icons.Rounded.VisibilityOff, "identifiers")
)

private val permissions = listOf(
    R.string.privacy_perm_usage to R.string.privacy_perm_usage_detail,
    R.string.privacy_perm_location to R.string.privacy_perm_location_detail,
    R.string.privacy_perm_phone to R.string.privacy_perm_phone_detail,
    R.string.privacy_perm_camera to R.string.privacy_perm_camera_detail,
    R.string.privacy_perm_microphone to R.string.privacy_perm_microphone_detail,
    R.string.privacy_perm_bluetooth to R.string.privacy_perm_bluetooth_detail,
    R.string.privacy_perm_notifications to R.string.privacy_perm_notifications_detail,
    R.string.privacy_perm_notification_access to R.string.privacy_perm_notification_access_detail,
    R.string.privacy_perm_secure_settings to R.string.privacy_perm_secure_settings_detail
)

/** What Osyster does and does not do with data, and why each optional permission exists. */
@Composable
internal fun PrivacyScreen() {
    OsysterScaffold(title = stringResource(R.string.about_privacy)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "promises") {
                Column {
                    SectionHeader(stringResource(R.string.privacy_promises))
                    ConnectedGroup {
                        promises.forEach { promise ->
                            ConnectedRow(
                                title = stringResource(promise.title),
                                supporting = stringResource(promise.detail),
                                leading = { IconBadge(promise.icon, key = promise.key) }
                            )
                        }
                    }
                }
            }
            item(key = "permissions_header") { SectionHeader(stringResource(R.string.privacy_permissions)) }
            items(permissions, key = { it.first }) { (title, detail) ->
                ConnectedGroup {
                    ConnectedRow(
                        title = stringResource(title),
                        supporting = stringResource(detail),
                        leading = { IconBadge(Icons.Rounded.Key, key = "perm_$title") }
                    )
                }
            }
        }
    }
}
