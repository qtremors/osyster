package dev.qtremors.osyster.feature.musicwidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

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

}
