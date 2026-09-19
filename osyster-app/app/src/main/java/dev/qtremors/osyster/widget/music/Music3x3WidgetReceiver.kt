package dev.qtremors.osyster.widget.music

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

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

}
