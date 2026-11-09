package dev.qtremors.osyster.feature.musicwidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

class Music5x1WidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            MusicWidgetUpdater.updateMusic5x1Widget(context, appWidgetManager, appWidgetId)
        }
    }

    /** Redraws at the new size when the widget is resized or the launcher grid changes. */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        MusicWidgetUpdater.updateMusic5x1Widget(context, appWidgetManager, appWidgetId)
    }
}
