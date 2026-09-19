package dev.qtremors.osyster.widget.music

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import dev.qtremors.osyster.R

object MusicWidgetUpdater {

    const val ACTION_PLAY_PAUSE = "dev.qtremors.osyster.action.MUSIC_PLAY_PAUSE"
    const val ACTION_NEXT = "dev.qtremors.osyster.action.MUSIC_NEXT"
    const val ACTION_PREV = "dev.qtremors.osyster.action.MUSIC_PREV"
    const val ACTION_SHUFFLE = "dev.qtremors.osyster.action.MUSIC_SHUFFLE"
    const val ACTION_REPEAT = "dev.qtremors.osyster.action.MUSIC_REPEAT"

    fun pushImmediateUpdate(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

            val component3x3 = ComponentName(context, Music3x3WidgetReceiver::class.java)
            val ids3x3 = appWidgetManager.getAppWidgetIds(component3x3) ?: IntArray(0)
            for (id in ids3x3) {
                updateMusic3x3Widget(context, appWidgetManager, id)
            }

            val component5x1 = ComponentName(context, Music5x1WidgetReceiver::class.java)
            val ids5x1 = appWidgetManager.getAppWidgetIds(component5x1) ?: IntArray(0)
            for (id in ids5x1) {
                updateMusic5x1Widget(context, appWidgetManager, id)
            }
        } catch (_: Throwable) {
        }
    }

    fun updateAllMusicWidgets(context: Context) {
        pushImmediateUpdate(context)
    }

    fun updateMusic3x3Widget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_music_3x3)
            val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val hasAccess = MusicWidgetManager.hasNotificationAccess(context)
            val trackInfo = MusicWidgetManager.getTrackInfo(context)
            val artwork = MusicWidgetManager.getArtwork(context)
            val palette = MusicWidgetManager.extractPalette(artwork, isDarkMode)
            val density = context.resources.displayMetrics.density

            // App launch intent (clicking the central disc)
            val launchIntent = if (!hasAccess) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                trackInfo.packageName.takeIf { it.isNotBlank() }?.let { packageName ->
                    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            }

            if (launchIntent != null) {
                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + 1,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_music_art_disc, appPendingIntent)
            }

            // Center artwork disc: full-size pre-cropped circular bitmap
            val circleSize = resolveDiscSizePx(context, appWidgetManager, appWidgetId)
            val circularArt = artwork?.let { MusicWidgetManager.getCircularArtwork(it, circleSize) }

            if (circularArt != null) {
                views.setImageViewBitmap(R.id.widget_music_art_disc, circularArt)
            } else {
                val defaultDisc = MusicWidgetManager.createDefaultDiscBitmap(circleSize, isDarkMode, context)
                views.setImageViewBitmap(R.id.widget_music_art_disc, defaultDisc)
            }

            views.setViewVisibility(
                R.id.widget_btn_repeat,
                if (hasAccess && trackInfo.supportsRepeat) View.VISIBLE else View.GONE
            )
            if (hasAccess && trackInfo.supportsRepeat) {
                val isRepeating = trackInfo.isRepeat
                val repeatTint = if (isRepeating) {
                    palette.accentColor
                } else {
                    ColorUtils.setAlphaComponent(palette.accentColor, 180)
                }
                val repeatIconRes = if (trackInfo.isRepeatOne) R.drawable.ic_music_repeat_one else R.drawable.ic_music_repeat
                val repeatBitmap = MusicWidgetManager.getTintedBitmap(
                    context,
                    repeatIconRes,
                    repeatTint,
                    (24f * density).toInt()
                )
                views.setImageViewBitmap(R.id.widget_btn_repeat, repeatBitmap)
                views.setContentDescription(R.id.widget_btn_repeat, context.getString(trackInfo.repeatDescriptionRes()))

                val repeatIntent = Intent(context, MusicWidgetControlReceiver::class.java).apply {
                    action = ACTION_REPEAT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_repeat,
                    PendingIntent.getBroadcast(
                        context,
                        appWidgetId * 100 + 3,
                        repeatIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                views.setOnClickPendingIntent(R.id.widget_btn_repeat, null)
            }

            // Bottom-Left Play/Pause Squircle Button
            val playPauseIconRes = if (trackInfo.isPlaying) R.drawable.ic_music_pause else R.drawable.ic_music_play
            val playPauseBitmap = MusicWidgetManager.getTintedBitmap(
                context,
                playPauseIconRes,
                palette.accentColor,
                (24f * density).toInt()
            )
            views.setImageViewBitmap(R.id.widget_btn_play_pause, playPauseBitmap)
            views.setContentDescription(
                R.id.widget_btn_play_pause,
                context.getString(if (trackInfo.isPlaying) R.string.widget_music_pause_desc else R.string.widget_music_play_desc)
            )

            val playPauseIntent = Intent(context, MusicWidgetControlReceiver::class.java).apply {
                action = ACTION_PLAY_PAUSE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 2,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (_: Throwable) {
        }
    }

    fun updateMusic5x1Widget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_music_5x1)
            val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val hasAccess = MusicWidgetManager.hasNotificationAccess(context)
            val trackInfo = MusicWidgetManager.getTrackInfo(context)
            val artwork = MusicWidgetManager.getArtwork(context)
            val palette = MusicWidgetManager.extractPalette(artwork, isDarkMode)
            val density = context.resources.displayMetrics.density

            // Adaptive Pill Background
            val bgRes = if (isDarkMode) R.drawable.widget_music_pill_background_dark else R.drawable.widget_music_pill_background
            views.setImageViewResource(R.id.widget_music_pill_bg, bgRes)

            // App launch intent (artwork thumbnail and info container)
            val launchIntent = if (!hasAccess) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                trackInfo.packageName.takeIf { it.isNotBlank() }?.let { packageName ->
                    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            }

            if (launchIntent != null) {
                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + 4,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_music_art_thumb, appPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_music_info_container, appPendingIntent)
            }

            // Left artwork thumbnail: safely inset with anti-aliased 14dp rounded corners
            val thumbPx = (52f * density).toInt().coerceAtLeast(1)
            val squareArt = MusicWidgetManager.cachedSquareArtwork
                ?: artwork?.let { MusicWidgetManager.createRoundedBitmap(it, thumbPx, thumbPx, 14f * density) }

            if (squareArt != null) {
                views.setImageViewBitmap(R.id.widget_music_art_thumb, squareArt)
            } else {
                val defaultThumb = MusicWidgetManager.createDefaultThumbBitmap(thumbPx, thumbPx, 14f * density, isDarkMode, context)
                views.setImageViewBitmap(R.id.widget_music_art_thumb, defaultThumb)
            }

            // Explicitly set high-contrast text colors
            val primaryTextColor = if (isDarkMode) 0xFFFFFFFF.toInt() else 0xFF1C1B1F.toInt()
            val secondaryTextColor = if (isDarkMode) 0xFFB0B0B0.toInt() else 0xFF49454F.toInt()
            views.setTextColor(R.id.widget_music_title, primaryTextColor)
            views.setTextColor(R.id.widget_music_artist, secondaryTextColor)

            // Track details
            if (!hasAccess) {
                views.setTextViewText(R.id.widget_music_title, context.getString(R.string.widget_music_access_needed_title))
                views.setTextViewText(R.id.widget_music_artist, context.getString(R.string.widget_music_access_needed_desc))
            } else {
                views.setTextViewText(
                    R.id.widget_music_title,
                    trackInfo.title.ifBlank { context.getString(R.string.widget_music_default_title) }
                )
                views.setTextViewText(
                    R.id.widget_music_artist,
                    trackInfo.artist.ifBlank { context.getString(R.string.widget_music_default_artist) }
                )
            }

            // Button icon tints harmonized with album artwork palette
            val accentTint = palette.accentColor
            val inactiveTint = ColorUtils.setAlphaComponent(palette.accentColor, 180)

            // Shuffle
            views.setViewVisibility(
                R.id.widget_btn_shuffle,
                if (hasAccess && trackInfo.supportsShuffle) View.VISIBLE else View.GONE
            )
            if (hasAccess && trackInfo.supportsShuffle) {
                val shuffleTint = if (trackInfo.isShuffle) accentTint else inactiveTint
                val shuffleBitmap = MusicWidgetManager.getTintedBitmap(context, R.drawable.ic_music_shuffle, shuffleTint, (18f * density).toInt())
                views.setImageViewBitmap(R.id.widget_btn_shuffle, shuffleBitmap)
                views.setContentDescription(
                    R.id.widget_btn_shuffle,
                    context.getString(if (trackInfo.isShuffle) R.string.widget_music_shuffle_on_desc else R.string.widget_music_shuffle_off_desc)
                )
                val shuffleIntent = Intent(context, MusicWidgetControlReceiver::class.java).apply {
                    action = ACTION_SHUFFLE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_shuffle,
                    PendingIntent.getBroadcast(
                        context,
                        appWidgetId * 100 + 6,
                        shuffleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                views.setOnClickPendingIntent(R.id.widget_btn_shuffle, null)
            }

            // Previous
            val prevBitmap = MusicWidgetManager.getTintedBitmap(context, R.drawable.ic_music_skip_previous, accentTint, (18f * density).toInt())
            views.setImageViewBitmap(R.id.widget_btn_prev, prevBitmap)
            val prevIntent = Intent(context, MusicWidgetControlReceiver::class.java).apply {
                action = ACTION_PREV
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 7,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Play / Pause Circle Button
            val playPauseIconRes = if (trackInfo.isPlaying) R.drawable.ic_music_pause else R.drawable.ic_music_play
            val playPauseBitmap = MusicWidgetManager.getTintedBitmap(context, playPauseIconRes, accentTint, (20f * density).toInt())
            views.setImageViewBitmap(R.id.widget_btn_play_pause_5x1, playPauseBitmap)
            views.setContentDescription(
                R.id.widget_btn_play_pause_5x1,
                context.getString(if (trackInfo.isPlaying) R.string.widget_music_pause_desc else R.string.widget_music_play_desc)
            )
            val playPauseIntent = Intent(context, MusicWidgetControlReceiver::class.java).apply {
                action = ACTION_PLAY_PAUSE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause_5x1,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 8,
                    playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Next
            val nextBitmap = MusicWidgetManager.getTintedBitmap(context, R.drawable.ic_music_skip_next, accentTint, (18f * density).toInt())
            views.setImageViewBitmap(R.id.widget_btn_next, nextBitmap)
            val nextIntent = Intent(context, MusicWidgetControlReceiver::class.java).apply {
                action = ACTION_NEXT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 9,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Repeat
            views.setViewVisibility(
                R.id.widget_btn_repeat_5x1,
                if (hasAccess && trackInfo.supportsRepeat) View.VISIBLE else View.GONE
            )
            if (hasAccess && trackInfo.supportsRepeat) {
                val isRepeating = trackInfo.isRepeat
                val repeatIconRes = if (trackInfo.isRepeatOne) R.drawable.ic_music_repeat_one else R.drawable.ic_music_repeat
                val repeatTint = if (isRepeating) accentTint else inactiveTint
                val repeatBitmap = MusicWidgetManager.getTintedBitmap(context, repeatIconRes, repeatTint, (18f * density).toInt())
                views.setImageViewBitmap(R.id.widget_btn_repeat_5x1, repeatBitmap)
                views.setContentDescription(R.id.widget_btn_repeat_5x1, context.getString(trackInfo.repeatDescriptionRes()))
                val repeatIntent = Intent(context, MusicWidgetControlReceiver::class.java).apply {
                    action = ACTION_REPEAT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_repeat_5x1,
                    PendingIntent.getBroadcast(
                        context,
                        appWidgetId * 100 + 10,
                        repeatIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                views.setOnClickPendingIntent(R.id.widget_btn_repeat_5x1, null)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (_: Throwable) {
        }
    }
}

private fun MusicTrackInfo.repeatDescriptionRes(): Int = when (repeatMode) {
    MusicTrackInfo.REPEAT_ALL -> R.string.widget_music_repeat_all_desc
    MusicTrackInfo.REPEAT_ONE -> R.string.widget_music_repeat_one_desc
    else -> R.string.widget_music_repeat_off_desc
}

private fun resolveDiscSizePx(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
): Int {
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180).coerceAtLeast(120)
    val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180).coerceAtLeast(120)
    val density = context.resources.displayMetrics.density
    return (minOf(widthDp, heightDp) * density).toInt().coerceIn(128, 768)
}
