package dev.qtremors.osyster.core.system.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.system.domain.StayAwakeTimer
import javax.inject.Inject

/**
 * Holds a screen wake lock for the chosen time and shows a notification with a countdown and a
 * Stop button. The lock is released when the timer ends, Stop is tapped, or the service dies.
 */
@AndroidEntryPoint
class StayAwakeService : Service() {
    @Inject internal lateinit var controller: StayAwakeControllerImpl

    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val stopTask = Runnable { stopSelfCleanly() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelfCleanly()
            else -> begin(intent?.getIntExtra(EXTRA_MINUTES, UNTIL_STOPPED) ?: UNTIL_STOPPED)
        }
        return START_NOT_STICKY
    }

    private fun begin(minutesExtra: Int) {
        val minutes = minutesExtra.takeIf { it > 0 }
        val endsAt = minutes?.let { System.currentTimeMillis() + it * 60_000L }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(endsAt), type)
        acquire(minutes)
        handler.removeCallbacks(stopTask)
        if (minutes != null) handler.postDelayed(stopTask, minutes * 60_000L)
        controller.report(StayAwakeTimer.Running(endsAt, minutes))
    }

    @Suppress("DEPRECATION")
    private fun acquire(minutes: Int?) {
        wakeLock?.takeIf { it.isHeld }?.release()
        val power = getSystemService(POWER_SERVICE) as PowerManager
        // SCREEN_BRIGHT_WAKE_LOCK is deprecated for activities, which should use keepScreenOn; a
        // service that keeps the screen on with Osyster closed has no window to put that flag on.
        wakeLock = power.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            if (minutes != null) acquire(minutes * 60_000L + 1_000L) else acquire()
        }
    }

    private fun stopSelfCleanly() {
        handler.removeCallbacks(stopTask)
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        controller.report(StayAwakeTimer.Off)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopTask)
        wakeLock?.takeIf { it.isHeld }?.release()
        controller.report(StayAwakeTimer.Off)
        super.onDestroy()
    }

    private fun notification(endsAt: Long?): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.stay_awake_channel), NotificationManager.IMPORTANCE_LOW).apply {
                    description = getString(R.string.stay_awake_channel_description)
                    setShowBadge(false)
                }
            )
        }
        val stop = PendingIntent.getService(
            this, 0, Intent(this, StayAwakeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = packageManager.getLaunchIntentForPackage(packageName)
            ?.putExtra(DeepLinks.EXTRA_OPEN_TOOL, Tool.STAY_AWAKE.id)
            ?.let { PendingIntent.getActivity(this, 1, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stay_awake)
            .setContentTitle(getString(R.string.stay_awake_notification_title))
            .setContentText(getString(if (endsAt == null) R.string.stay_awake_until_stopped else R.string.stay_awake_counting_down))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(0, getString(R.string.stay_awake_stop), stop)
            .apply {
                if (endsAt != null) {
                    setWhen(endsAt)
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                    setShowWhen(true)
                }
            }
            .build()
    }

    companion object {
        internal const val ACTION_START = "dev.qtremors.osyster.action.STAY_AWAKE_START"
        internal const val ACTION_STOP = "dev.qtremors.osyster.action.STAY_AWAKE_STOP"
        internal const val EXTRA_MINUTES = "minutes"
        internal const val UNTIL_STOPPED = -1
        private const val CHANNEL_ID = "stay_awake"
        private const val NOTIFICATION_ID = 2001
        private const val WAKE_LOCK_TAG = "Osyster:StayAwake"
    }
}
