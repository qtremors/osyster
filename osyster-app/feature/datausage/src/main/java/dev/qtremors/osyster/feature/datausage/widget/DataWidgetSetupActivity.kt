package dev.qtremors.osyster.feature.datausage.widget

import androidx.compose.material3.ButtonDefaults
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.theme.OsysterTheme
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import dev.qtremors.osyster.core.ui.theme.spacing
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.TrafficTotals
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import dev.qtremors.osyster.feature.datausage.R
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opens when a data widget is placed (and, on Android 12 and newer, from the widget's settings):
 * pick the period and whether the two lines are mobile and Wi-Fi or download and upload, with a
 * live preview on top. Backing out cancels placing the widget.
 */
@AndroidEntryPoint
class DataWidgetSetupActivity : ComponentActivity() {
    @Inject lateinit var themeSettings: ThemeSettings
    @Inject lateinit var source: DataUsageSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider
        val initial = DataWidgetSettings.read(this, widgetId, DataPeriod.TODAY)
        val doneLabel = if (DataWidgetSettings.has(this, widgetId)) R.string.widget_setup_save else R.string.widget_setup_add
        setContent {
            val options by themeSettings.options.collectAsStateWithLifecycle(initialValue = null)
            val loaded = options ?: return@setContent
            OsysterTheme(options = loaded) {
                DataWidgetSetupScreen(
                    initial = initial,
                    doneLabel = stringResource(doneLabel),
                    loadTotals = ::loadTotals,
                    onDone = { config -> save(widgetId, provider, config) }
                )
            }
        }
    }

    /** Null without usage access or when the totals cannot be read; the preview then shows "--". */
    private suspend fun loadTotals(period: DataPeriod): TrafficTotals? = withContext(Dispatchers.IO) {
        if (source.accessState() != UsageAccessState.GRANTED) return@withContext null
        runCatchingCancellable { source.report(period, includeApps = false).totals }.getOrNull()
    }

    private fun save(widgetId: Int, provider: ComponentName?, config: DataWidgetConfig) {
        DataWidgetSettings.write(this, widgetId, config)
        if (provider != null) {
            sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
            )
        }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}

@Composable
private fun DataWidgetSetupScreen(
    initial: DataWidgetConfig,
    doneLabel: String,
    loadTotals: suspend (DataPeriod) -> TrafficTotals?,
    onDone: (DataWidgetConfig) -> Unit
) {
    var period by rememberSaveable { mutableStateOf(initial.period) }
    var traffic by rememberSaveable { mutableStateOf(initial.traffic) }
    val totals by produceState<TrafficTotals?>(initialValue = null, period) { value = loadTotals(period) }
    val layoutDirection = LocalLayoutDirection.current
    val spacing = MaterialTheme.spacing

    OsysterScaffold(title = stringResource(R.string.widget_setup_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection),
                    top = padding.calculateTopPadding()
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                WidgetPreview(DataWidgetConfig(period, traffic), totals)
                ConnectedGroup {
                    ChoiceRow(
                        title = stringResource(R.string.widget_setup_period),
                        options = DataPeriod.entries,
                        selected = period,
                        label = { stringResource(it.choiceLabel()) },
                        onSelect = { period = it }
                    )
                    ChoiceRow(
                        title = stringResource(R.string.widget_setup_traffic),
                        options = WidgetTraffic.entries,
                        selected = traffic,
                        label = { stringResource(it.choiceLabel()) },
                        onSelect = { traffic = it }
                    )
                }
            }
            Button(shapes = ButtonDefaults.shapes(), 
                onClick = { onDone(DataWidgetConfig(period, traffic)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = spacing.medium)
                    .height(56.dp)
            ) {
                Text(doneLabel, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * The real widget layout, filled the same way the widget is, so the preview is exactly what lands
 * on the home screen. Sized like a 2x1 widget, on a panel standing in for the wallpaper.
 */
@Composable
private fun WidgetPreview(config: DataWidgetConfig, totals: TrafficTotals?) {
    val context = LocalContext.current
    val description = stringResource(R.string.widget_setup_preview)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(vertical = MaterialTheme.spacing.extraLarge)
                .semantics { contentDescription = description }
        ) {
            AndroidView(
                factory = { FrameLayout(it) },
                modifier = Modifier.size(width = 190.dp, height = 76.dp),
                update = { frame ->
                    val views = RemoteViews(context.packageName, R.layout.widget_data_usage_compact)
                    fillDataWidget(context, views, config, totals)
                    frame.removeAllViews()
                    frame.addView(views.apply(context, frame))
                }
            )
        }
    }
}
