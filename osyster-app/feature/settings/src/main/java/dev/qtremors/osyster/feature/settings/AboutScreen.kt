package dev.qtremors.osyster.feature.settings

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import kotlinx.coroutines.launch

internal const val REPO_URL = "https://github.com/qtremors/osyster"
private const val DEVELOPER_URL = "https://github.com/qtremors"

/** Who made Osyster, where its code and releases live, and what this phone is, for bug reports. */
@Composable
internal fun AboutScreen(appVersion: String, onOpenUrl: (String) -> Unit, onPrivacy: () -> Unit, onLicenses: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val deviceLine = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    OsysterScaffold(title = stringResource(R.string.about_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppIcon(context.packageName, size = 88.dp)
                    Text(stringResource(R.string.about_app_name), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.about_version, appVersion), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.about_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "project") {
                Column {
                    SectionHeader(stringResource(R.string.about_project))
                    ConnectedGroup {
                        NavigationRow(stringResource(R.string.about_developer), Icons.Rounded.Person, "developer", { onOpenUrl(DEVELOPER_URL) }, supporting = stringResource(R.string.about_developer_name))
                        NavigationRow(stringResource(R.string.settings_source), Icons.Rounded.Code, "source", { onOpenUrl(REPO_URL) }, supporting = stringResource(R.string.settings_source_summary))
                        NavigationRow(stringResource(R.string.about_releases), Icons.Rounded.NewReleases, "releases", { onOpenUrl("$REPO_URL/releases") }, supporting = stringResource(R.string.about_releases_summary))
                        NavigationRow(stringResource(R.string.about_issues), Icons.Rounded.BugReport, "issues", { onOpenUrl("$REPO_URL/issues") }, supporting = stringResource(R.string.about_issues_summary))
                    }
                }
            }
            item(key = "legal") {
                Column {
                    SectionHeader(stringResource(R.string.about_legal))
                    ConnectedGroup {
                        NavigationRow(stringResource(R.string.about_privacy), Icons.Rounded.PrivacyTip, "privacy", onPrivacy, supporting = stringResource(R.string.about_privacy_summary))
                        ConnectedRow(
                            title = stringResource(R.string.settings_license),
                            supporting = stringResource(R.string.settings_license_summary),
                            leading = { IconBadge(Icons.Rounded.Gavel, key = "license") }
                        )
                        NavigationRow(stringResource(R.string.about_licenses), Icons.Rounded.Favorite, "licenses", onLicenses, supporting = stringResource(R.string.about_licenses_summary))
                    }
                }
            }
            item(key = "device") {
                Column {
                    SectionHeader(stringResource(R.string.about_this_device))
                    ConnectedGroup {
                        ConnectedRow(
                            title = deviceLine,
                            supporting = stringResource(R.string.about_device_hint),
                            leading = { IconBadge(Icons.Rounded.Smartphone, key = "this_device") },
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("device", "Osyster $appVersion · $deviceLine")))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
