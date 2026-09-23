package dev.qtremors.osyster.feature.datausage.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import kotlinx.coroutines.launch

/**
 * Shared behavior of the three data usage widgets: update on the system schedule and on the
 * refresh button, open Osyster's Data usage on tap, or Usage access settings until it is granted.
 * Widgets show device totals only, never per-app data.
 */
abstract class DataUsageWidget : AppWidgetProvider() {
    @Inject lateinit var source: DataUsageSource

    protected abstract val refreshAction: String

    /** Builds the views for one widget. Called off the main thread. */
    protected abstract suspend fun render(context: Context, views: RemoteViews)

    protected abstract val layout: Int

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAsync(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == refreshAction) {
            val manager = AppWidgetManager.getInstance(context)
            updateAsync(context, manager, manager.getAppWidgetIds(ComponentName(context, javaClass)))
        }
    }

    private fun updateAsync(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        scope.launch {
            try {
                ids.forEach { id -> manager.updateAppWidget(id, build(context, id)) }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun build(context: Context, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, layout)
        val granted = source.accessState() == UsageAccessState.GRANTED
        if (granted) {
            runCatchingCancellable { render(context, views) }.onFailure { showUnknown(context, views) }
        } else {
            showSetup(context, views)
        }
        views.setOnClickPendingIntent(R.id.widget_root, if (granted) openApp(context, widgetId) else openUsageAccess(context, widgetId))
        val refresh = PendingIntent.getBroadcast(
            context, widgetId,
            Intent(context, javaClass).setAction(refreshAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        refreshButtons.forEach { views.setOnClickPendingIntent(it, refresh) }
        return views
    }

    protected open val refreshButtons: List<Int> = listOf(R.id.widget_btn_refresh)

    protected abstract fun showSetup(context: Context, views: RemoteViews)
    protected abstract fun showUnknown(context: Context, views: RemoteViews)

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
    }
}

/** One period: mobile and Wi-Fi totals with a title. */
abstract class SinglePeriodDataUsageWidget : DataUsageWidget() {
    protected abstract val period: DataPeriod
    protected abstract val title: Int
    override val layout: Int = R.layout.widget_data_usage

    override suspend fun render(context: Context, views: RemoteViews) {
        val report = source.report(period, includeApps = false)
        views.setTextViewText(R.id.widget_interval_label, context.getString(title))
        views.setTextViewText(R.id.widget_mobile_text, UsageFormat.bytes(report.totals.mobile))
        views.setTextViewText(R.id.widget_wifi_text, UsageFormat.bytes(report.totals.wifi))
    }

    override fun showSetup(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_interval_label, context.getString(title))
        views.setTextViewText(R.id.widget_mobile_text, context.getString(R.string.widget_setup))
        views.setTextViewText(R.id.widget_wifi_text, context.getString(R.string.widget_unknown))
    }

    override fun showUnknown(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_interval_label, context.getString(title))
        views.setTextViewText(R.id.widget_mobile_text, context.getString(R.string.widget_unknown))
        views.setTextViewText(R.id.widget_wifi_text, context.getString(R.string.widget_unknown))
    }
}

@AndroidEntryPoint
class DayDataUsageWidget : SinglePeriodDataUsageWidget() {
    override val period = DataPeriod.TODAY
    override val title = R.string.widget_day_title
    override val refreshAction = "dev.qtremors.osyster.action.REFRESH_DAY_DATA_WIDGET"
}

@AndroidEntryPoint
class MonthDataUsageWidget : SinglePeriodDataUsageWidget() {
    override val period = DataPeriod.MONTH
    override val title = R.string.widget_month_title
    override val refreshAction = "dev.qtremors.osyster.action.REFRESH_MONTH_DATA_WIDGET"
}

/** Today and this month side by side. */
@AndroidEntryPoint
class CombinedDataUsageWidget : DataUsageWidget() {
    override val layout: Int = R.layout.widget_data_usage_combined
    override val refreshAction = "dev.qtremors.osyster.action.REFRESH_COMBINED_DATA_WIDGET"
    override val refreshButtons = listOf(R.id.widget_btn_refresh, R.id.widget_btn_refresh_month)

    override suspend fun render(context: Context, views: RemoteViews) {
        val today = source.report(DataPeriod.TODAY, includeApps = false).totals
        val month = source.report(DataPeriod.MONTH, includeApps = false).totals
        views.setTextViewText(R.id.widget_today_mobile_text, UsageFormat.bytes(today.mobile))
        views.setTextViewText(R.id.widget_today_wifi_text, UsageFormat.bytes(today.wifi))
        views.setTextViewText(R.id.widget_month_mobile_text, UsageFormat.bytes(month.mobile))
        views.setTextViewText(R.id.widget_month_wifi_text, UsageFormat.bytes(month.wifi))
    }

    override fun showSetup(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_today_mobile_text, context.getString(R.string.widget_setup))
        listOf(R.id.widget_today_wifi_text, R.id.widget_month_mobile_text, R.id.widget_month_wifi_text)
            .forEach { views.setTextViewText(it, context.getString(R.string.widget_unknown)) }
    }

    override fun showUnknown(context: Context, views: RemoteViews) {
        listOf(R.id.widget_today_mobile_text, R.id.widget_today_wifi_text, R.id.widget_month_mobile_text, R.id.widget_month_wifi_text)
            .forEach { views.setTextViewText(it, context.getString(R.string.widget_unknown)) }
    }
}
