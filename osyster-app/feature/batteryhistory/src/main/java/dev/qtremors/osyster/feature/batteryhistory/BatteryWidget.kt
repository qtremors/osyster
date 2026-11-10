package dev.qtremors.osyster.feature.batteryhistory

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.battery.domain.ACTION_BATTERY_HISTORY_UPDATED
import dev.qtremors.osyster.core.battery.domain.BatteryHistory
import dev.qtremors.osyster.core.battery.domain.BatterySample
import dev.qtremors.osyster.core.battery.domain.HOUR_MS
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The battery widget: level, what it is doing, and the last 24 hours as a small chart. It is
 * redrawn when battery history saves a reading (every half hour, or when the page is open), so it
 * needs no schedule of its own.
 */
@AndroidEntryPoint
class BatteryWidget : AppWidgetProvider() {
    @Inject lateinit var history: BatteryHistory

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        launch { render(context, manager, ids) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        launch { render(context, manager, intArrayOf(id)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_BATTERY_HISTORY_UPDATED) return
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java)) ?: return
        if (ids.isNotEmpty()) launch { render(context, manager, ids) }
    }

    private fun launch(block: suspend () -> Unit) {
        val pending = goAsync()
        scope.launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val samples = runCatching { history.samples(System.currentTimeMillis() - DAY_MS).first() }.getOrDefault(emptyList())
        val latest = samples.lastOrNull()
        ids.forEach { id ->
            runCatching {
                val views = RemoteViews(context.packageName, R.layout.widget_battery)
                views.setTextViewText(R.id.battery_widget_level, latest?.let { "${it.level}%" } ?: context.getString(R.string.battery_widget_placeholder))
                views.setTextViewText(R.id.battery_widget_status, status(context, latest))
                val options = manager.getAppWidgetOptions(id)
                val density = context.resources.displayMetrics.density
                val width = ((options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0).takeIf { it > 0 } ?: 150) * density
                val height = ((options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0).takeIf { it > 0 } ?: 150) * density
                // The chart fills what is left under the two lines of text.
                val chartHeight = (height - CHART_TOP_DP * density).coerceAtLeast(MIN_CHART_DP * density)
                if (samples.size >= 2 && height >= MIN_HEIGHT_FOR_CHART_DP * density) {
                    views.setImageViewBitmap(R.id.battery_widget_chart, chart(context, samples, (width - 32 * density).toInt(), chartHeight.toInt()))
                } else {
                    views.setImageViewBitmap(R.id.battery_widget_chart, null)
                }
                views.setOnClickPendingIntent(R.id.battery_widget_root, open(context, id))
                manager.updateAppWidget(id, views)
            }
        }
    }

    private fun status(context: Context, sample: BatterySample?): String {
        sample ?: return context.getString(R.string.battery_widget_empty)
        val state = when {
            sample.charging && sample.level >= 100 -> context.getString(R.string.battery_now_full)
            sample.charging -> context.getString(R.string.battery_now_charging)
            else -> context.getString(R.string.battery_now_draining)
        }
        val temperature = sample.temperatureC?.let { context.getString(R.string.battery_temperature, String.format(Locale.getDefault(), "%.1f", it)) }
        return listOfNotNull(state, temperature).joinToString(" · ")
    }

    /** The last day's level as a line over a faint fill, in the widget's accent color. */
    private fun chart(context: Context, samples: List<BatterySample>, width: Int, height: Int): Bitmap {
        val w = width.coerceAtLeast(MIN_CHART_PX)
        val h = height.coerceAtLeast(MIN_CHART_PX)
        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        val accent = context.getColor(R.color.battery_widget_accent)
        val end = samples.last().at
        val start = end - DAY_MS
        val stroke = 3f * context.resources.displayMetrics.density
        val x = { at: Long -> ((at - start).toFloat() / DAY_MS) * (w - stroke) + stroke / 2 }
        val y = { level: Int -> (h - stroke / 2) - level / 100f * (h - stroke) }
        val line = Path()
        val fill = Path()
        samples.forEachIndexed { i, sample ->
            if (i == 0) {
                line.moveTo(x(sample.at), y(sample.level))
                fill.moveTo(x(sample.at), h.toFloat())
            } else {
                line.lineTo(x(sample.at), y(sample.level))
            }
            fill.lineTo(x(sample.at), y(sample.level))
        }
        fill.lineTo(x(samples.last().at), h.toFloat())
        fill.close()
        canvas.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtils.setAlphaComponent(accent, FILL_ALPHA) })
        canvas.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
        return bitmap
    }

    private fun open(context: Context, id: Int): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.putExtra(DeepLinks.EXTRA_OPEN_TOOL, Tool.BATTERY_HISTORY.id)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) ?: return null
        return PendingIntent.getActivity(context, id, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val DAY_MS = 24 * HOUR_MS
        const val CHART_TOP_DP = 88
        const val MIN_CHART_DP = 24
        const val MIN_HEIGHT_FOR_CHART_DP = 100
        const val MIN_CHART_PX = 16
        const val FILL_ALPHA = 48
    }
}
