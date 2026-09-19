package dev.qtremors.osyster.widget.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicWidgetControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MusicWidgetUpdater.ACTION_PLAY_PAUSE -> MusicWidgetManager.togglePlayPause(context)
            MusicWidgetUpdater.ACTION_NEXT -> MusicWidgetManager.skipNext(context)
            MusicWidgetUpdater.ACTION_PREV -> MusicWidgetManager.skipPrevious(context)
            MusicWidgetUpdater.ACTION_SHUFFLE -> MusicWidgetManager.toggleShuffle(context)
            MusicWidgetUpdater.ACTION_REPEAT -> MusicWidgetManager.toggleRepeat(context)
            else -> return
        }
        MusicWidgetUpdater.updateAllMusicWidgets(context)
    }
}
