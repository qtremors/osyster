package dev.qtremors.osyster.feature.btremote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.hid.domain.HidRemote
import dev.qtremors.osyster.core.hid.domain.HidState
import dev.qtremors.osyster.core.hid.domain.RemoteKey
import dev.qtremors.osyster.core.hid.domain.RemoteSettings
import dev.qtremors.osyster.core.hid.domain.RemoteSettingsStore
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Keeps Osyster registered as a Bluetooth keyboard and remote while it is in use. It starts when
 * the remote screen opens and stops when the screen closes with nothing connected, or when a
 * connected device goes away after the screen closed. Android shows its notification while it runs;
 * the notification can play, pause, and disconnect.
 */
@AndroidEntryPoint
class RemoteService : Service() {
    @Inject lateinit var hid: HidRemote
    @Inject lateinit var store: RemoteSettingsStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wasConnected = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(HidState(), RemoteSettings()), foregroundType())
        hid.start()
        scope.launch {
            combine(hid.state, store.settings) { state, settings -> state to settings }
                .distinctUntilChanged()
                .collect { (state, settings) ->
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(state, settings))
                    if (state.connected != wasConnected) RemoteWidget.updateAll(this@RemoteService)
                    if (wasConnected && !state.connected && !screenOpen) stopSelf()
                    wasConnected = state.connected
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> scope.launch { hid.tap(RemoteKey.PLAY_PAUSE) }
            ACTION_DISCONNECT -> {
                hid.disconnect()
                if (!screenOpen) stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        hid.stop()
        scope.cancel()
        RemoteWidget.updateAll(this)
        super.onDestroy()
    }

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.remote_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(state: HidState, settings: RemoteSettings): Notification {
        val connected = state.connected
        val name = state.device?.name?.takeIf { it.isNotBlank() } ?: getString(R.string.remote_unnamed)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_remote_notification)
            .setContentTitle(if (connected) getString(R.string.remote_connected_to, name) else getString(R.string.remote_notification_waiting))
            .setContentText(if (connected) null else getString(R.string.remote_notification_waiting_text))
            .setContentIntent(openRemote())
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply {
                if (connected && settings.notificationButtons) {
                    addAction(0, getString(R.string.remote_play_pause), serviceAction(ACTION_PLAY_PAUSE))
                }
                if (connected) addAction(0, getString(R.string.remote_disconnect), serviceAction(ACTION_DISCONNECT))
            }
            .build()
    }

    private fun serviceAction(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(), Intent(this, RemoteService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openRemote(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(DeepLinks.EXTRA_OPEN_TOOL, Tool.BT_REMOTE.id) ?: return null
        return PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val CHANNEL_ID = "bt_remote"
        private const val NOTIFICATION_ID = 0x5244
        private const val ACTION_PLAY_PAUSE = "dev.qtremors.osyster.action.REMOTE_PLAY_PAUSE"
        private const val ACTION_DISCONNECT = "dev.qtremors.osyster.action.REMOTE_DISCONNECT"

        /** True while the remote screen is showing, so a lost connection does not stop the service under it. */
        @Volatile
        internal var screenOpen = false

        internal fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, RemoteService::class.java)) }
        }

        internal fun stop(context: Context) {
            context.stopService(Intent(context, RemoteService::class.java))
        }
    }
}
