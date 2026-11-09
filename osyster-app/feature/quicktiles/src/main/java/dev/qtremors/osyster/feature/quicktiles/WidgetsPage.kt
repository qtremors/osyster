package dev.qtremors.osyster.feature.quicktiles

import androidx.compose.material3.ButtonDefaults
import dev.qtremors.osyster.core.ui.components.ConnectedItem
import androidx.compose.foundation.lazy.itemsIndexed
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.navigation.WidgetsRoute
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

/** Every Osyster widget, by receiver class name, so this page needs no other feature module. */
private data class WidgetEntry(val className: String, @param:StringRes val title: Int, @param:StringRes val summary: Int, val icon: ImageVector)

private val widgets = listOf(
    WidgetEntry("dev.qtremors.osyster.feature.quicktiles.GlanceStackWidget", R.string.widgets_glance, R.string.widgets_glance_summary, Icons.Rounded.ViewCarousel),
    WidgetEntry("dev.qtremors.osyster.feature.musicwidgets.Music3x3WidgetReceiver", R.string.widgets_music_disc, R.string.widgets_music_disc_summary, Icons.Rounded.Album),
    WidgetEntry("dev.qtremors.osyster.feature.musicwidgets.Music5x1WidgetReceiver", R.string.widgets_music_capsule, R.string.widgets_music_capsule_summary, Icons.Rounded.MusicNote),
    WidgetEntry("dev.qtremors.osyster.feature.datausage.widget.DayDataUsageWidget", R.string.widgets_data_today, R.string.widgets_data_today_summary, Icons.Rounded.DataUsage),
    WidgetEntry("dev.qtremors.osyster.feature.datausage.widget.CombinedDataUsageWidget", R.string.widgets_data_combined, R.string.widgets_data_combined_summary, Icons.Rounded.DataUsage),
    WidgetEntry("dev.qtremors.osyster.feature.btremote.RemoteWidget", R.string.widgets_remote, R.string.widgets_remote_summary, Icons.Rounded.SettingsRemote),
    WidgetEntry("dev.qtremors.osyster.feature.powertools.PrivateDnsWidget", R.string.widgets_private_dns, R.string.widgets_private_dns_summary, Icons.Rounded.Dns)
)

fun NavGraphBuilder.registerWidgetsRoute() {
    composable<WidgetsRoute> { WidgetsScreen() }
}

@Composable
private fun WidgetsScreen() {
    val context = LocalContext.current
    val canPin = canPinWidgets(context)
    OsysterScaffold(title = stringResource(Tool.WIDGETS.label.title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "how") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    ConnectedGroup {
                        ConnectedRow(
                            title = stringResource(R.string.widgets_how_title),
                            supporting = stringResource(if (canPin) R.string.widgets_how_pin else R.string.widgets_how_manual),
                            leading = { IconBadge(Icons.Rounded.Info, key = "widgets_how") }
                        )
                    }
                }
            }
            item(key = "header") { SectionHeader(stringResource(R.string.widgets_available)) }
            itemsIndexed(widgets, key = { _, it -> it.className }) { index, widget ->
                ConnectedItem(index, widgets.size) {
                    ConnectedRow(
                        title = stringResource(widget.title),
                        supporting = stringResource(widget.summary),
                        leading = { IconBadge(widget.icon, key = widget.className) },
                        trailing = {
                            if (canPin) {
                                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { requestPin(context, widget.className) }) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Text(stringResource(R.string.qt_add), modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun canPinWidgets(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

/** The launcher shows its own prompt with a preview; nothing is added unless the user agrees. */
private fun requestPin(context: Context, className: String) {
    runCatching { AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context.packageName, className), null, null) }
}
