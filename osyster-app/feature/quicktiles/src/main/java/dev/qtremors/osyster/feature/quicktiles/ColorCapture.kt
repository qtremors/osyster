package dev.qtremors.osyster.feature.quicktiles

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import java.io.File

/**
 * Captures exactly one frame of the screen after the user agreed in Android's own prompt, saves
 * it to Osyster's cache, and stops. Android 14 needs a media projection foreground service for
 * this, so it runs as one for the second or so the capture takes.
 */
class ColorCaptureService : Service() {
    private var projection: MediaProjection? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val data = intent?.let { readData(it) }
        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        val manager = getSystemService(MediaProjectionManager::class.java)
        projection = runCatching { manager.getMediaProjection(code, data) }.getOrNull()
        val active = projection ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        // Let the consent dialog fade out so it is not in the picture.
        main.postDelayed({ capture(active) }, SETTLE_MS)
        return START_NOT_STICKY
    }

    private fun capture(active: MediaProjection) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        active.registerCallback(object : MediaProjection.Callback() {}, main)
        val display = active.createVirtualDisplay(
            "osyster-color", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.surface, null, main
        )
        reader.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            source.setOnImageAvailableListener(null, null)
            val saved = runCatching {
                val plane = image.planes[0]
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val padded = Bitmap.createBitmap(width + rowPadding / plane.pixelStride, height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(plane.buffer)
                val bitmap = Bitmap.createBitmap(padded, 0, 0, width, height)
                captureFile(this).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }.isSuccess
            image.close()
            display?.release()
            reader.close()
            active.stop()
            if (saved) openPicker()
            stopSelf()
        }, main)
    }

    private fun openPicker() {
        startActivity(
            Intent(this, ColorPickerActivity::class.java)
                .putExtra(ColorPickerActivity.EXTRA_READY, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.color_channel), NotificationManager.IMPORTANCE_LOW))
        val notification: Notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_qt_color)
            .setContentTitle(getString(R.string.color_capturing))
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        projection?.stop()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun readData(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else intent.getParcelableExtra(EXTRA_DATA)

    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        private const val CHANNEL = "color_picker"
        private const val NOTIFICATION_ID = 4520
        private const val SETTLE_MS = 450L

        fun captureFile(context: Context): File = File(context.cacheDir, "color-capture.png")
    }
}

/** Quick Settings tile: tap, agree to the capture prompt, pick a color. */
class ColorPickerTile : TileService() {
    @Suppress("DEPRECATION")
    override fun onClick() {
        val intent = Intent(this, ColorPickerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
