package dev.qtremors.osyster.feature.appmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.apps.domain.AppDetails
import dev.qtremors.osyster.core.apps.domain.SpecialAccess
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything about one app: what it is, what it may do, what it is made of, and who signed it. */
@Composable
internal fun AppDetailsScreen(viewModel: AppDetailsViewModel, onNavigate: (AppManagerDestination) -> Unit) {
    val state by viewModel.details.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val loaded = (state as? DetailsState.Loaded)?.details
    OsysterScaffold(title = loaded?.summary?.label ?: stringResource(R.string.apps_title)) { padding ->
        when (val current = state) {
            DetailsState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            DetailsState.Missing -> Box(modifier = Modifier.padding(padding)) {
                EmptyState(Icons.Rounded.Block, stringResource(R.string.apps_title), stringResource(R.string.apps_not_found))
            }
            is DetailsState.Loaded -> LazyColumn(contentPadding = padding) {
                item(key = "header") { Header(current.details, onNavigate) }
                item(key = "usage") {
                    ConnectedGroup(modifier = Modifier.padding(top = 12.dp)) {
                        NavigationRow(
                            stringResource(R.string.apps_usage),
                            Icons.Rounded.Insights,
                            "apps-usage",
                            { onNavigate(AppManagerDestination.Usage(current.details.summary.packageName)) },
                            supporting = stringResource(R.string.apps_usage_detail)
                        )
                    }
                }
                item(key = "details") { Facts(current.details) }
                item(key = "permissions") { Permissions(current.details) }
                item(key = "components") { Components(current.details) }
                current.details.signerSha256?.let { sha -> item(key = "certificate") { Certificate(sha) } }
            }
        }
    }
}

@Composable
private fun Header(details: AppDetails, onNavigate: (AppManagerDestination) -> Unit) {
    val context = LocalContext.current
    val app = details.summary
    val scope = rememberCoroutineScope()
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { copyApk(context, details.apkPaths.first(), uri) }
            Toast.makeText(context, if (ok) R.string.apps_saved else R.string.apps_save_failed, Toast.LENGTH_SHORT).show()
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        AppIcon(app.packageName, size = 80.dp)
        Text(app.label, style = MaterialTheme.typography.headlineSmall)
        app.versionName?.let { Text(stringResource(R.string.apps_version, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        SelectionContainer { Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        RiskTag(app.risk)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            if (app.launchable) Action(Icons.AutoMirrored.Rounded.OpenInNew, R.string.apps_open) { onNavigate(AppManagerDestination.Open(app.packageName)) }
            Action(Icons.Rounded.Info, R.string.apps_info) { onNavigate(AppManagerDestination.AppInfo(app.packageName)) }
            if (!app.system) Action(Icons.Rounded.Shop, R.string.apps_store) { onNavigate(AppManagerDestination.Store(app.packageName)) }
            if (!app.system) Action(Icons.Rounded.Delete, R.string.apps_uninstall) {
                runCatching { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))) }
            }
            if (details.apkPaths.size == 1) Action(Icons.Rounded.Download, R.string.apps_save_apk) { save.launch("${app.label}-${app.versionName ?: app.versionCode}.apk".replace(Regex("[^A-Za-z0-9._-]+"), "_")) }
        }
        if (details.apkPaths.size > 1) {
            Text(
                stringResource(R.string.apps_save_split, details.apkPaths.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun Action(icon: ImageVector, label: Int, onClick: () -> Unit) {
    FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = onClick) {
        Icon(icon, contentDescription = null)
        Text(stringResource(label), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Facts(details: AppDetails) {
    val context = LocalContext.current
    val app = details.summary
    val date = { millis: Long -> DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME) }
    SectionHeader(stringResource(R.string.apps_details))
    ConnectedGroup {
        Fact(R.string.apps_installer, app.installer ?: stringResource(R.string.apps_installer_unknown))
        Fact(R.string.apps_installed_on, date(app.installedAt))
        if (app.updatedAt > app.installedAt) Fact(R.string.apps_updated_on, date(app.updatedAt))
        Fact(R.string.apps_target_sdk, stringResource(R.string.apps_sdk_value, app.targetSdk, androidVersion(app.targetSdk)))
        if (app.minSdk > 0) Fact(R.string.apps_min_sdk, stringResource(R.string.apps_sdk_value, app.minSdk, androidVersion(app.minSdk)))
        Fact(R.string.apps_abis, details.abis.joinToString(", ").ifEmpty { stringResource(R.string.apps_abis_none) })
        Fact(R.string.apps_apk_size, size(context, app.apkBytes))
        Fact(
            R.string.apps_storage,
            details.storage?.let { stringResource(R.string.apps_storage_value, size(context, it.appBytes), size(context, it.dataBytes), size(context, it.cacheBytes)) }
                ?: stringResource(R.string.apps_storage_none)
        )
        Fact(
            R.string.apps_battery,
            stringResource(if (SpecialAccess.UNRESTRICTED_BATTERY in app.special) R.string.apps_battery_unrestricted else R.string.apps_battery_optimized)
        )
    }
}

@Composable
private fun Fact(title: Int, value: String) {
    ConnectedRow(title = stringResource(title), supporting = value)
}

/** Allowed first, then special access, then what was refused, then install-time permissions. */
@Composable
private fun Permissions(details: AppDetails) {
    val app = details.summary
    val runtime = app.permissions.filter { it.area != null }
    val allowed = runtime.filter { it.granted }
    val denied = runtime.filterNot { it.granted }
    val other = app.permissions.filter { it.area == null }
    val label = { name: String -> details.permissionLabels[name] ?: name }
    SectionHeader(stringResource(R.string.apps_permissions_title))
    if (app.permissions.isEmpty() && app.special.isEmpty()) {
        Text(stringResource(R.string.apps_permissions_none), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (allowed.isNotEmpty()) PermissionGroupBlock(R.string.apps_permissions_allowed) {
            allowed.forEach { p -> PermissionRow(label(p.name), stringResource(p.area!!.label), Icons.Rounded.Check, p.area!!.risk.color()) }
        }
        if (app.special.isNotEmpty()) PermissionGroupBlock(R.string.apps_permissions_special) {
            app.special.sortedByDescending { it.risk }.forEach { access -> PermissionRow(stringResource(access.label), null, Icons.Rounded.Star, access.risk.color()) }
        }
        if (denied.isNotEmpty()) PermissionGroupBlock(R.string.apps_permissions_denied) {
            denied.forEach { p -> PermissionRow(label(p.name), stringResource(p.area!!.label), Icons.Rounded.Close, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (other.isNotEmpty()) PermissionGroupBlock(R.string.apps_permissions_other) {
            other.forEach { p -> PermissionRow(label(p.name), p.name.substringAfterLast('.'), Icons.Rounded.Check, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun PermissionGroupBlock(title: Int, rows: @Composable () -> Unit) {
    Column {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        ConnectedGroup { rows() }
    }
}

@Composable
private fun PermissionRow(title: String, supporting: String?, icon: ImageVector, tint: androidx.compose.ui.graphics.Color) {
    ConnectedRow(title = title, supporting = supporting, leading = { Icon(icon, contentDescription = null, tint = tint) })
}

/** Counts of each kind of component; tap one to list its class names. */
@Composable
private fun Components(details: AppDetails) {
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    val c = details.components
    SectionHeader(stringResource(R.string.apps_components))
    ConnectedGroup {
        listOf(
            R.string.apps_activities to c.activities,
            R.string.apps_services to c.services,
            R.string.apps_receivers to c.receivers,
            R.string.apps_providers to c.providers,
            R.string.apps_libraries to details.nativeLibraries
        ).forEach { (title, names) ->
            Column {
                ConnectedRow(
                    title = stringResource(title),
                    leading = { IconBadge(Icons.Rounded.Widgets, key = "component-$title") },
                    trailing = { Text(names.size.toString(), style = MaterialTheme.typography.titleMedium) },
                    onClick = if (names.isEmpty()) null else ({ open = if (open == title) null else title })
                )
                AnimatedVisibility(visible = open == title) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        SelectionContainer {
                            Text(
                                names.joinToString("\n") { it.removePrefix(details.summary.packageName) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Certificate(sha: String) {
    val context = LocalContext.current
    SectionHeader(stringResource(R.string.apps_certificate))
    ConnectedGroup {
        ConnectedRow(
            title = sha,
            supporting = stringResource(R.string.apps_certificate_detail),
            leading = { IconBadge(Icons.Rounded.Verified, key = "certificate") },
            onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText(null, sha))
                Toast.makeText(context, R.string.apps_copied, Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/** Copies an app's single APK to a file the person picked; APK files are readable by every app. */
private fun copyApk(context: Context, path: String, target: Uri): Boolean = runCatching {
    File(path).inputStream().use { input ->
        context.contentResolver.openOutputStream(target, "wt")?.use { input.copyTo(it) } != null
    }
}.getOrDefault(false)
