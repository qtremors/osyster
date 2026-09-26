package dev.qtremors.osyster.feature.powertools

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.privilege.domain.PackageNames
import dev.qtremors.osyster.core.privilege.domain.PowerApps
import dev.qtremors.osyster.core.privilege.domain.QuietLaunch
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The target of pinned Shut-Up! shortcuts: hides debugging, opens the app, and leaves a
 * notification to put debugging back. It has no UI and finishes straight away.
 */
@AndroidEntryPoint
class QuietLaunchActivity : ComponentActivity() {
    @Inject lateinit var quiet: QuietLaunch
    @Inject lateinit var apps: PowerApps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra(EXTRA_PACKAGE)?.takeIf(PackageNames::isValid)
        if (target == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            quiet.hide()
            QuietNotification.show(this@QuietLaunchActivity)
            apps.launch(target)
            finish()
        }
    }

    companion object {
        internal const val EXTRA_PACKAGE = "dev.qtremors.osyster.QUIET_PACKAGE"
    }
}

/** Puts debugging back from the notification, without opening Osyster. */
@AndroidEntryPoint
class RestoreDebuggingReceiver : BroadcastReceiver() {
    @Inject lateinit var quiet: QuietLaunch

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        // goAsync gives about ten seconds; three settings writes take a fraction of that.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (quiet.restore()) QuietNotification.cancel(context)
            } finally {
                pending.finish()
            }
        }
    }
}

internal object QuietNotification {
    private const val CHANNEL = "power_tools"
    private const val ID = 4310

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.shut_up_channel), NotificationManager.IMPORTANCE_LOW)
        )
        val restore = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, RestoreDebuggingReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_power_notification)
            .setContentTitle(context.getString(R.string.shut_up_notification_title))
            .setContentText(context.getString(R.string.shut_up_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.shut_up_restore), restore)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID, notification) }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
    }
}

/** Asks the launcher to pin a shortcut that opens [packageName] quietly. */
internal fun pinQuietShortcut(context: Context, packageName: String, label: String): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
    val intent = Intent(context, QuietLaunchActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .putExtra(QuietLaunchActivity.EXTRA_PACKAGE, packageName)
    val icon = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        IconCompat.createWithBitmap(drawable.toBitmapSafe())
    }.getOrElse { IconCompat.createWithResource(context, R.drawable.ic_power_notification) }
    val shortcut = ShortcutInfoCompat.Builder(context, "quiet_$packageName")
        .setShortLabel(label)
        .setIcon(icon)
        .setIntent(intent)
        .build()
    return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}

private fun android.graphics.drawable.Drawable.toBitmapSafe(): android.graphics.Bitmap {
    val size = 192
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, size, size)
    draw(canvas)
    return bitmap
}
