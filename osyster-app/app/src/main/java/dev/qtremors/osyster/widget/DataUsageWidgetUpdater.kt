package dev.qtremors.osyster.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import dev.qtremors.osyster.MainActivity
import dev.qtremors.osyster.R
import dev.qtremors.osyster.monitor.NetworkInterfaceFilter
import dev.qtremors.osyster.monitor.NetworkInterval
import dev.qtremors.osyster.monitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DataUsageWidgetUpdater {

    const val ACTION_REFRESH_DAY = "dev.qtremors.osyster.action.REFRESH_DAY_WIDGET"
    const val ACTION_REFRESH_MONTH = "dev.qtremors.osyster.action.REFRESH_MONTH_WIDGET"
    const val ACTION_REFRESH_COMBINED = "dev.qtremors.osyster.action.REFRESH_COMBINED_WIDGET"
    const val EXTRA_TARGET_SCREEN = "target_screen"
    const val TARGET_NETWORK = "network"

    suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        interval: NetworkInterval
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_data_usage)

            val intervalTitle = if (interval == NetworkInterval.DAY) {
                context.getString(R.string.widget_day_title)
            } else {
                context.getString(R.string.widget_month_title)
            }
            views.setTextViewText(R.id.widget_interval_label, intervalTitle)

            val hasPermission = NetworkMonitor.hasUsageAccess(context)

            if (!hasPermission) {
                views.setTextViewText(R.id.widget_mobile_text, context.getString(R.string.widget_permission_required))
                views.setTextViewText(R.id.widget_wifi_text, "--")

                // Tapping background takes directly to Usage Access settings
                val permIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val permPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    permIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, permPendingIntent)
            } else {
                val summary = NetworkMonitor.queryNetworkUsage(
                    context = context,
                    interval = interval,
                    filter = NetworkInterfaceFilter.ALL,
                    targetDateMillis = System.currentTimeMillis(),
                    includeDetails = false
                )

                views.setTextViewText(R.id.widget_mobile_text, NetworkMonitor.formatBytes(summary.mobileBytes))
                views.setTextViewText(R.id.widget_wifi_text, NetworkMonitor.formatBytes(summary.wifiBytes))

                // Tapping background opens MainActivity at the Network screen
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_TARGET_SCREEN, TARGET_NETWORK)
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            }

            // Set up refresh button pending intent
            val refreshAction = if (interval == NetworkInterval.DAY) ACTION_REFRESH_DAY else ACTION_REFRESH_MONTH
            val receiverClass = if (interval == NetworkInterval.DAY) {
                DailyDataUsageWidgetReceiver::class.java
            } else {
                MonthlyDataUsageWidgetReceiver::class.java
            }

            val refreshIntent = Intent(context, receiverClass).apply {
                action = refreshAction
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (_: Throwable) {
            // Safe fallback so launcher never fails with unhandled crash
            try {
                val fallbackViews = RemoteViews(context.packageName, R.layout.widget_data_usage)
                val intervalTitle = if (interval == NetworkInterval.DAY) {
                    context.getString(R.string.widget_day_title)
                } else {
                    context.getString(R.string.widget_month_title)
                }
                fallbackViews.setTextViewText(R.id.widget_interval_label, intervalTitle)
                fallbackViews.setTextViewText(R.id.widget_mobile_text, "--")
                fallbackViews.setTextViewText(R.id.widget_wifi_text, "--")
                appWidgetManager.updateAppWidget(appWidgetId, fallbackViews)
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun updateCombinedWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_data_usage_combined)
            val hasPermission = NetworkMonitor.hasUsageAccess(context)

            if (!hasPermission) {
                views.setTextViewText(R.id.widget_today_mobile_text, context.getString(R.string.widget_permission_required))
                views.setTextViewText(R.id.widget_today_wifi_text, "--")
                views.setTextViewText(R.id.widget_month_mobile_text, "--")
                views.setTextViewText(R.id.widget_month_wifi_text, "--")

                val permIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val permPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    permIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, permPendingIntent)
            } else {
                val daySummary = NetworkMonitor.queryNetworkUsage(
                    context = context,
                    interval = NetworkInterval.DAY,
                    filter = NetworkInterfaceFilter.ALL,
                    targetDateMillis = System.currentTimeMillis(),
                    includeDetails = false
                )
                val monthSummary = NetworkMonitor.queryNetworkUsage(
                    context = context,
                    interval = NetworkInterval.MONTH,
                    filter = NetworkInterfaceFilter.ALL,
                    targetDateMillis = System.currentTimeMillis(),
                    includeDetails = false
                )

                views.setTextViewText(R.id.widget_today_mobile_text, NetworkMonitor.formatBytes(daySummary.mobileBytes))
                views.setTextViewText(R.id.widget_today_wifi_text, NetworkMonitor.formatBytes(daySummary.wifiBytes))
                views.setTextViewText(R.id.widget_month_mobile_text, NetworkMonitor.formatBytes(monthSummary.mobileBytes))
                views.setTextViewText(R.id.widget_month_wifi_text, NetworkMonitor.formatBytes(monthSummary.wifiBytes))

                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_TARGET_SCREEN, TARGET_NETWORK)
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            }

            val refreshIntent = Intent(context, CombinedDataUsageWidgetReceiver::class.java).apply {
                action = ACTION_REFRESH_COMBINED
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_refresh_month, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (_: Throwable) {
            try {
                val fallbackViews = RemoteViews(context.packageName, R.layout.widget_data_usage_combined)
                fallbackViews.setTextViewText(R.id.widget_today_mobile_text, "--")
                fallbackViews.setTextViewText(R.id.widget_today_wifi_text, "--")
                fallbackViews.setTextViewText(R.id.widget_month_mobile_text, "--")
                fallbackViews.setTextViewText(R.id.widget_month_wifi_text, "--")
                appWidgetManager.updateAppWidget(appWidgetId, fallbackViews)
            } catch (_: Throwable) {
            }
        }
    }

    fun updateAllWidgets(context: Context, interval: NetworkInterval) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val receiverClass = if (interval == NetworkInterval.DAY) {
            DailyDataUsageWidgetReceiver::class.java
        } else {
            MonthlyDataUsageWidgetReceiver::class.java
        }
        val componentName = ComponentName(context, receiverClass)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
        if (appWidgetIds.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            for (id in appWidgetIds) {
                updateWidget(context, appWidgetManager, id, interval)
            }
        }
    }

    fun updateAllCombinedWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val componentName = ComponentName(context, CombinedDataUsageWidgetReceiver::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
        if (appWidgetIds.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            for (id in appWidgetIds) {
                updateCombinedWidget(context, appWidgetManager, id)
            }
        }
    }

    fun updateAllActiveWidgets(context: Context) {
        updateAllWidgets(context, NetworkInterval.DAY)
        updateAllWidgets(context, NetworkInterval.MONTH)
        updateAllCombinedWidgets(context)
    }
}
