package dev.qtremors.osyster.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dev.qtremors.osyster.monitor.NetworkInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MonthlyDataUsageWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    DataUsageWidgetUpdater.updateWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        interval = NetworkInterval.MONTH
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == DataUsageWidgetUpdater.ACTION_REFRESH_MONTH) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val targetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (targetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        DataUsageWidgetUpdater.updateWidget(
                            context = context,
                            appWidgetManager = appWidgetManager,
                            appWidgetId = targetId,
                            interval = NetworkInterval.MONTH
                        )
                    } else {
                        DataUsageWidgetUpdater.updateAllWidgets(context, NetworkInterval.MONTH)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
