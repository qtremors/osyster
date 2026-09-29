package dev.qtremors.osyster.feature.musicwidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

class Music3x3WidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            MusicWidgetUpdater.updateMusic3x3Widget(context, appWidgetManager, appWidgetId)
        }
    }

    /** Redraws at the new size when the widget is resized or the launcher grid changes. */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        MusicWidgetUpdater.updateMusic3x3Widget(context, appWidgetManager, appWidgetId)
    }
}
