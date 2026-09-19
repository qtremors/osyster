package dev.qtremors.osyster.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CombinedDataUsageWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    DataUsageWidgetUpdater.updateCombinedWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == DataUsageWidgetUpdater.ACTION_REFRESH_COMBINED) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val targetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (targetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        DataUsageWidgetUpdater.updateCombinedWidget(
                            context = context,
                            appWidgetManager = appWidgetManager,
                            appWidgetId = targetId
                        )
                    } else {
                        DataUsageWidgetUpdater.updateAllCombinedWidgets(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
