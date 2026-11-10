package dev.qtremors.osyster.feature.musicwidgets

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.graphics.scale
import androidx.core.os.BundleCompat
import dev.qtremors.osyster.core.notifications.domain.CapturedMessage
import dev.qtremors.osyster.core.notifications.domain.CapturedNotification
import dev.qtremors.osyster.core.notifications.domain.PictureSource
import dev.qtremors.osyster.core.notifications.domain.RemovalReason

/**
 * Reads everything a posted notification shows into the history: its title and expanded text,
 * inbox lines, summary and info lines, progress, button labels, replies already sent from it,
 * its big picture, and for chats every message (the earlier ones Android keeps too) with sender
 * and time. App names are looked up once and remembered.
 */
internal object NotificationCapture {
    private val labels = object : LinkedHashMap<String, String>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 128
    }

    fun from(context: Context, sbn: StatusBarNotification, media: Boolean): CapturedNotification {
        val notification = sbn.notification
        val extras = notification.extras
        val chat = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification) }.getOrNull()
        return CapturedNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = label(context, sbn.packageName),
            postedAt = sbn.postTime,
            title = (extras.getCharSequence(Notification.EXTRA_TITLE_BIG) ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.map { it.toString() }.orEmpty(),
            conversationTitle = chat?.conversationTitle?.toString(),
            conversationId = notification.shortcutId?.takeIf { chat != null },
            messages = (chat?.historicMessages.orEmpty() + chat?.messages.orEmpty()).mapNotNull { message ->
                val text = message.text?.toString() ?: return@mapNotNull null
                CapturedMessage(sender = message.person?.name?.toString(), text = text, timestamp = message.timestamp)
            },
            summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString(),
            infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString(),
            replies = replies(extras),
            actions = notification.actions?.mapNotNull { it.title?.toString() }.orEmpty(),
            progress = progress(extras),
            picture = picture(context, extras),
            channelId = notification.channelId,
            category = notification.category,
            ongoing = sbn.isOngoing,
            groupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            media = media
        )
    }

    @Suppress("DEPRECATION")
    private fun replies(extras: Bundle): List<String> =
        extras.getCharSequenceArray(Notification.EXTRA_REMOTE_INPUT_HISTORY)?.map { it.toString() }.orEmpty()

    private fun progress(extras: Bundle): Int? {
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        if (max <= 0 || extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)) return null
        return extras.getInt(Notification.EXTRA_PROGRESS, 0) * 100 / max
    }

    /**
     * The big picture, if any. Only a reference is held here; the history writes it, scaled to
     * at most [PICTURE_PX] and as a JPEG, and only when it keeps the notification.
     */
    private fun picture(context: Context, extras: Bundle): PictureSource? {
        val bitmap = BundleCompat.getParcelable(extras, Notification.EXTRA_PICTURE, Bitmap::class.java)
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BundleCompat.getParcelable(extras, Notification.EXTRA_PICTURE_ICON, Icon::class.java)?.let { icon ->
                    runCatching { (icon.loadDrawable(context) as? BitmapDrawable)?.bitmap }.getOrNull()
                }
            } else {
                null
            }
        bitmap ?: return null
        return PictureSource { out ->
            val scale = minOf(1f, PICTURE_PX.toFloat() / maxOf(bitmap.width, bitmap.height))
            val sized = if (scale < 1f) bitmap.scale((bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1)) else bitmap
            sized.compress(Bitmap.CompressFormat.JPEG, PICTURE_QUALITY, out)
        }
    }

    private const val PICTURE_PX = 1080
    private const val PICTURE_QUALITY = 85

    /** Android's removal reasons, folded into the few the history shows. */
    fun reason(code: Int): RemovalReason = when (code) {
        NotificationListenerService.REASON_CLICK -> RemovalReason.OPENED
        NotificationListenerService.REASON_CANCEL, NotificationListenerService.REASON_CANCEL_ALL -> RemovalReason.DISMISSED
        NotificationListenerService.REASON_APP_CANCEL, NotificationListenerService.REASON_APP_CANCEL_ALL -> RemovalReason.APP_REMOVED
        else -> RemovalReason.OTHER
    }

    private fun label(context: Context, packageName: String): String = synchronized(labels) {
        labels.getOrPut(packageName) {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
        }
    }
}
