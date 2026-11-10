package dev.qtremors.osyster.feature.btremote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.hid.domain.HidRemote
import dev.qtremors.osyster.core.hid.domain.RemoteKey
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The remote on the home screen: previous, volume, play or pause, and next. While a device is
 * connected the keys go straight to it; otherwise every key opens the remote, which reconnects to
 * the last device. The service refreshes it whenever the connection changes.
 */
@AndroidEntryPoint
class RemoteWidget : AppWidgetProvider() {
    @Inject lateinit var hid: HidRemote

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_KEY) return
        val key = intent.getStringExtra(EXTRA_KEY)?.let { name -> RemoteKey.entries.firstOrNull { it.name == name } } ?: return
        if (!hid.state.value.connected) return
        val pending = goAsync()
        scope.launch {
            try {
                hid.tap(key)
            } finally {
                pending.finish()
            }
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val state = hid.state.value
        val name = state.device?.name?.ifBlank { null } ?: context.getString(R.string.remote_unnamed)
        val views = RemoteViews(context.packageName, R.layout.widget_remote).apply {
            setTextViewText(
                R.id.remote_widget_name,
                if (state.connected) context.getString(R.string.remote_connected_to, name) else context.getString(R.string.remote_widget_not_connected)
            )
            setOnClickPendingIntent(R.id.remote_widget_root, openRemote(context))
            KEYS.forEach { (view, key) ->
                setOnClickPendingIntent(view, if (state.connected) keyIntent(context, key) else openRemote(context))
            }
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun keyIntent(context: Context, key: RemoteKey): PendingIntent = PendingIntent.getBroadcast(
        context, key.ordinal,
        Intent(context, RemoteWidget::class.java).setAction(ACTION_KEY).putExtra(EXTRA_KEY, key.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openRemote(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(DeepLinks.EXTRA_OPEN_TOOL, Tool.BT_REMOTE.id) ?: return null
        return PendingIntent.getActivity(context, OPEN_REQUEST, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val ACTION_KEY = "dev.qtremors.osyster.action.REMOTE_WIDGET_KEY"
        private const val EXTRA_KEY = "key"
        private const val OPEN_REQUEST = 0x5257
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val KEYS = listOf(
            R.id.remote_widget_previous to RemoteKey.PREVIOUS,
            R.id.remote_widget_volume_down to RemoteKey.VOLUME_DOWN,
            R.id.remote_widget_play_pause to RemoteKey.PLAY_PAUSE,
            R.id.remote_widget_volume_up to RemoteKey.VOLUME_UP,
            R.id.remote_widget_next to RemoteKey.NEXT
        )

        /** Redraws every remote widget, after the connection changed. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, RemoteWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(ComponentName(context, RemoteWidget::class.java))
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }
}
