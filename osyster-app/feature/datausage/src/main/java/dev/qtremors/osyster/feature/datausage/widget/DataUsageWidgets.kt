package dev.qtremors.osyster.feature.datausage.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import dev.qtremors.osyster.feature.datausage.R
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared behavior of the two data usage widgets: update on the system schedule and on the
 * refresh button, open Osyster's Data usage on tap, or Usage access settings until it is granted.
 * Widgets show device totals only, never per-app data. A tap on refresh turns the refresh icon
 * until the new totals are in, so the tap always gets a visible answer.
 */
abstract class DataUsageWidget : AppWidgetProvider() {
    @Inject lateinit var source: DataUsageSource

    protected abstract val refreshAction: String

    /** Builds the views for one widget. Called off the main thread. */
    protected abstract suspend fun render(context: Context, views: RemoteViews, widgetId: Int)

    /** The layout for one widget at its current size. */
    protected abstract fun layoutFor(manager: AppWidgetManager, widgetId: Int): Int

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAsync(context, manager, ids)
    }

    /** Resized, or moved to a launcher with another grid: redraw, maybe in the other layout. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, widgetId: Int, newOptions: Bundle) {
        updateAsync(context, manager, intArrayOf(widgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == refreshAction) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
            showSpinning(context, manager, ids)
            updateAsync(context, manager, ids, minSpinMs = MIN_SPIN_MS)
        }
    }

    /** Swaps each refresh icon for its spinning copy. The full update that follows puts the icon back. */
    private fun showSpinning(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, layoutFor(manager, id))
            refreshButtons.zip(refreshSpinners).forEach { (button, spinner) ->
                views.setViewVisibility(button, View.GONE)
                views.setViewVisibility(spinner, View.VISIBLE)
            }
            manager.partiallyUpdateAppWidget(id, views)
        }
    }

    private fun updateAsync(context: Context, manager: AppWidgetManager, ids: IntArray, minSpinMs: Long = 0) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        val started = SystemClock.elapsedRealtime()
        scope.launch {
            try {
                val built = ids.associateWith { id -> build(context, manager, id) }
                // Fast loads would flash the spinner; keep it up for one full turn.
                delay((minSpinMs - (SystemClock.elapsedRealtime() - started)).coerceAtLeast(0))
                built.forEach { (id, views) -> manager.updateAppWidget(id, views) }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun build(context: Context, manager: AppWidgetManager, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(manager, widgetId))
        val granted = source.accessState() == UsageAccessState.GRANTED
        if (granted) {
            runCatchingCancellable { render(context, views, widgetId) }.onFailure { showUnknown(context, views, widgetId) }
        } else {
            showSetup(context, views, widgetId)
        }
        views.setOnClickPendingIntent(R.id.widget_root, if (granted) openApp(context, widgetId) else openUsageAccess(context, widgetId))
        val refresh = PendingIntent.getBroadcast(
            context, widgetId,
            Intent(context, javaClass).setAction(refreshAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        refreshButtons.forEach { views.setOnClickPendingIntent(it, refresh) }
        // Launchers re-apply an update onto the views they already show, so the spinner that
        // showSpinning turned on stays on unless this update turns it off again.
        refreshButtons.zip(refreshSpinners).forEach { (button, spinner) ->
            views.setViewVisibility(button, View.VISIBLE)
            views.setViewVisibility(spinner, View.GONE)
        }
        return views
    }

    protected open val refreshButtons: List<Int> = listOf(R.id.widget_btn_refresh)

    /** The spinning icon that stands in for each of [refreshButtons], in the same order. */
    protected open val refreshSpinners: List<Int> = listOf(R.id.widget_refresh_spin)

    protected abstract fun showSetup(context: Context, views: RemoteViews, widgetId: Int)
    protected abstract fun showUnknown(context: Context, views: RemoteViews, widgetId: Int)

    private fun openApp(context: Context, widgetId: Int): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(DeepLinks.EXTRA_OPEN_TOOL, Tool.DATA_USAGE.id) ?: return null
        return PendingIntent.getActivity(context, widgetId, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openUsageAccess(context: Context, widgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context, widgetId,
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        const val MIN_SPIN_MS = 700L
    }
}

/**
 * One period of mobile and Wi-Fi, or download and upload, as picked in the setup screen
 * ([DataWidgetSetupActivity]). One cell tall it uses the compact two-row layout (label and refresh
 * beside the values); taller, the large one with a header.
 */
abstract class SinglePeriodDataUsageWidget : DataUsageWidget() {
    /** The period a widget shows until it is set up. */
    abstract val defaultPeriod: DataPeriod


    override fun layoutFor(manager: AppWidgetManager, widgetId: Int): Int {
        // Portrait height. Zero before the launcher reports a size: the widget starts 1 cell tall.
        val heightDp = manager.getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return if (heightDp < LARGE_MIN_HEIGHT_DP) R.layout.widget_data_usage_compact else R.layout.widget_data_usage
    }

    private companion object {
        /** The large layout's header and two value rows need about this much height. */
        const val LARGE_MIN_HEIGHT_DP = 110
    }

    private fun config(context: Context, widgetId: Int) = DataWidgetSettings.read(context, widgetId, defaultPeriod)

    override suspend fun render(context: Context, views: RemoteViews, widgetId: Int) {
        val config = config(context, widgetId)
        fillDataWidget(context, views, config, source.report(config.period, includeApps = false).totals)
    }

    override fun showSetup(context: Context, views: RemoteViews, widgetId: Int) {
        fillDataWidget(context, views, config(context, widgetId), totals = null, placeholder = context.getString(R.string.widget_setup))
    }

    override fun showUnknown(context: Context, views: RemoteViews, widgetId: Int) {
        fillDataWidget(context, views, config(context, widgetId), totals = null)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        DataWidgetSettings.delete(context, appWidgetIds)
    }
}

@AndroidEntryPoint
class DayDataUsageWidget : SinglePeriodDataUsageWidget() {
    override val defaultPeriod = DataPeriod.TODAY
    override val refreshAction = "dev.qtremors.osyster.action.REFRESH_DAY_DATA_WIDGET"
}

/** Today and this month side by side. */
@AndroidEntryPoint
class CombinedDataUsageWidget : DataUsageWidget() {
    override fun layoutFor(manager: AppWidgetManager, widgetId: Int): Int = R.layout.widget_data_usage_combined
    override val refreshAction = "dev.qtremors.osyster.action.REFRESH_COMBINED_DATA_WIDGET"
    override val refreshButtons = listOf(R.id.widget_btn_refresh, R.id.widget_btn_refresh_month)
    override val refreshSpinners = listOf(R.id.widget_refresh_spin, R.id.widget_refresh_spin_month)

    override suspend fun render(context: Context, views: RemoteViews, widgetId: Int) {
        val today = source.report(DataPeriod.TODAY, includeApps = false).totals
        val month = source.report(DataPeriod.MONTH, includeApps = false).totals
        views.setTextViewText(R.id.widget_today_mobile_text, UsageFormat.bytes(today.mobile))
        views.setTextViewText(R.id.widget_today_wifi_text, UsageFormat.bytes(today.wifi))
        views.setTextViewText(R.id.widget_month_mobile_text, UsageFormat.bytes(month.mobile))
        views.setTextViewText(R.id.widget_month_wifi_text, UsageFormat.bytes(month.wifi))
    }

    override fun showSetup(context: Context, views: RemoteViews, widgetId: Int) {
        views.setTextViewText(R.id.widget_today_mobile_text, context.getString(R.string.widget_setup))
        listOf(R.id.widget_today_wifi_text, R.id.widget_month_mobile_text, R.id.widget_month_wifi_text)
            .forEach { views.setTextViewText(it, context.getString(R.string.widget_unknown)) }
    }

    override fun showUnknown(context: Context, views: RemoteViews, widgetId: Int) {
        listOf(R.id.widget_today_mobile_text, R.id.widget_today_wifi_text, R.id.widget_month_mobile_text, R.id.widget_month_wifi_text)
            .forEach { views.setTextViewText(it, context.getString(R.string.widget_unknown)) }
    }
}
