package dev.qtremors.osyster.widget.music

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.media.session.MediaControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class MusicNotificationListenerService : NotificationListenerService() {

    private companion object {
        const val ARTWORK_DECODE_TARGET_PX = 1024
    }

    private var sessionManager: MediaSessionManager? = null
    private val selectionLock = Any()
    @Volatile
    private var selectedController: MediaController? = null
    private var selectedCallback: MediaController.Callback? = null
    private var selectedCompatController: MediaControllerCompat? = null
    private var selectedCompatCallback: MediaControllerCompat.Callback? = null
    private val artworkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val artworkRequestId = AtomicLong()
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        handleSessionsChanged(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(this, MusicNotificationListenerService::class.java)
            sessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)

            val initialSessions = sessionManager?.getActiveSessions(componentName)
            handleSessionsChanged(initialSessions)

            val activeNotifs = activeNotifications
            if (!activeNotifs.isNullOrEmpty()) {
                val selectedPackage = selectedController?.packageName
                val musicNotif = activeNotifs.firstOrNull { it.packageName == selectedPackage }
                    ?: activeNotifs.firstOrNull { isMusicNotification(it) }
                if (musicNotif != null) {
                    processNotification(musicNotif)
                }
            }
        } catch (_: Throwable) {
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Throwable) {
        }
        clearSelectedController(markStopped = true)
    }

    override fun onDestroy() {
        clearSelectedController(markStopped = false)
        artworkScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        if (isMusicNotification(sbn)) {
            processNotification(sbn)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        if (sbn.packageName == selectedController?.packageName) {
            refreshActiveSessions()
        }
    }

    private fun isMusicNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val extras = notification.extras ?: return false
        val category = notification.category
        val hasMediaSession = extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        val isTransport = category == Notification.CATEGORY_TRANSPORT
        val hasMediaActions = notification.actions?.any { action ->
            val title = action.title?.toString()?.lowercase() ?: ""
            title.contains("play") || title.contains("pause") || title.contains("skip") || title.contains("next")
        } == true

        return hasMediaSession || isTransport || hasMediaActions
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
        val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()

        val token = androidx.core.os.BundleCompat.getParcelable(
            extras,
            Notification.EXTRA_MEDIA_SESSION,
            android.media.session.MediaSession.Token::class.java
        )
        if (token != null) {
            try {
                val controller = MediaController(this, token)
                val current = selectedController
                val incomingIsPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
                val currentIsPlaying = current?.playbackState?.state == PlaybackState.STATE_PLAYING
                if (current == null || current.sessionToken == token || incomingIsPlaying && !currentIsPlaying) {
                    selectController(controller)
                }
                if (selectedController?.sessionToken == token) {
                    extractArtworkFromNotification(sbn)?.let { MusicWidgetManager.saveArtwork(this, it) }
                    updateFromController(controller, fallbackTitle = title, fallbackArtist = artist)
                }
                return
            } catch (_: Throwable) {
            }
        }

        var isPlaying = false
        notification.actions?.forEach { action ->
            val actionTitle = action.title?.toString()?.lowercase() ?: ""
            if (actionTitle.contains("pause")) {
                isPlaying = true
            }
        }

        val cached = MusicWidgetManager.getTrackInfo(this)
        if (selectedController != null || cached.packageName.isNotBlank() && cached.packageName != sbn.packageName && !isPlaying) {
            return
        }
        val newTrackInfo = MusicTrackInfo(
            title = title ?: cached.title,
            artist = artist ?: cached.artist,
            packageName = sbn.packageName,
            isPlaying = isPlaying,
            repeatMode = cached.repeatMode,
            isShuffle = cached.isShuffle,
            supportsRepeat = false,
            supportsShuffle = false
        )
        MusicWidgetManager.saveTrackInfo(this, newTrackInfo)

        val art = extractArtworkFromNotification(sbn)
        if (art != null) {
            MusicWidgetManager.saveArtwork(this, art)
        }

        MusicWidgetUpdater.updateAllMusicWidgets(this)
    }

    private fun handleSessionsChanged(controllers: List<MediaController>?) {
        val available = controllers.orEmpty()
        val currentToken = selectedController?.sessionToken
        val selectedToken = selectMediaSession(
            candidates = available.map { controller ->
                SessionCandidate(
                    key = controller.sessionToken,
                    activity = controller.playbackState.toSessionActivity(),
                    lastUpdateTime = controller.playbackState?.lastPositionUpdateTime ?: 0L
                )
            },
            currentKey = currentToken
        )
        selectController(available.firstOrNull { it.sessionToken == selectedToken })
    }

    private fun selectController(controller: MediaController?) {
        val existing = selectedController
        if (existing != null && controller != null && existing.sessionToken == controller.sessionToken) {
            selectedController = controller
            MusicWidgetManager.activeMediaController = controller
            updateFromController(controller)
            return
        }

        clearSelectedController(markStopped = controller == null)
        if (controller == null) return

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateIfSelected(controller, refreshArtwork = true)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateIfSelected(controller)
                refreshActiveSessions()
            }

            override fun onSessionEvent(event: String, extras: Bundle?) {
                updateIfSelected(controller)
            }

            override fun onExtrasChanged(extras: Bundle?) {
                updateIfSelected(controller)
            }

            override fun onSessionDestroyed() {
                if (selectedController?.sessionToken == controller.sessionToken) {
                    clearSelectedController(markStopped = true)
                    refreshActiveSessions()
                }
            }
        }
        val compatController = MusicSessionModeController.getCompatController(this, controller)
        val compatCallback = object : MediaControllerCompat.Callback() {
            override fun onSessionReady() {
                updateIfSelected(controller)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                updateIfSelected(controller)
            }

            override fun onShuffleModeChanged(shuffleMode: Int) {
                updateIfSelected(controller)
            }
        }
        synchronized(selectionLock) {
            selectedController = controller
            selectedCallback = callback
            selectedCompatController = compatController
            selectedCompatCallback = compatCallback
            MusicWidgetManager.activeMediaController = controller
        }
        try {
            controller.registerCallback(callback)
        } catch (_: Throwable) {
        }
        try {
            compatController?.registerCallback(compatCallback)
        } catch (_: Throwable) {
        }
        updateFromController(controller, refreshArtwork = true, clearStaleArtwork = true)
    }

    private fun updateIfSelected(controller: MediaController, refreshArtwork: Boolean = false) {
        if (selectedController?.sessionToken == controller.sessionToken) {
            updateFromController(controller, refreshArtwork = refreshArtwork)
        }
    }

    private fun updateFromController(
        controller: MediaController,
        fallbackTitle: String? = null,
        fallbackArtist: String? = null,
        refreshArtwork: Boolean = false,
        clearStaleArtwork: Boolean = false
    ) {
        if (selectedController?.sessionToken != controller.sessionToken) return
        val metadata = controller.metadata
        val state = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: fallbackTitle
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: fallbackArtist

        val isPlaying = state?.state == PlaybackState.STATE_PLAYING

        val cached = MusicWidgetManager.getTrackInfo(this)
        val samePackage = cached.packageName == controller.packageName
        if (clearStaleArtwork) {
            MusicWidgetManager.clearArtwork(this)
        }
        val sessionModes = MusicSessionModeController.read(this, controller)
        var repeatMode = if (sessionModes.stateAvailable) {
            sessionModes.repeatMode
        } else if (samePackage) {
            cached.repeatMode
        } else {
            MusicTrackInfo.REPEAT_OFF
        }
        var isShuffle = if (sessionModes.stateAvailable) sessionModes.isShuffleEnabled else samePackage && cached.isShuffle
        val supportsRepeat = sessionModes.supportsRepeat
        val supportsShuffle = sessionModes.supportsShuffle

        val extras = state?.extras ?: controller.extras
        if (extras != null) {
            extras.readInt(
                "android.support.v4.media.session.command.ARGUMENT_REPEAT_MODE",
                "android.media.extra.REPEAT_MODE",
                "repeat_mode"
            )?.takeIf { it in 0..2 }?.let { repeatMode = it }
            extras.readBooleanOrInt(
                "android.support.v4.media.session.command.ARGUMENT_SHUFFLE_MODE",
                "android.media.extra.SHUFFLE_MODE",
                "shuffle_mode"
            )?.let { isShuffle = it }
        }

        val newTrackInfo = MusicTrackInfo(
            title = title ?: cached.title.takeIf { samePackage }.orEmpty(),
            artist = artist ?: cached.artist.takeIf { samePackage }.orEmpty(),
            packageName = controller.packageName,
            isPlaying = isPlaying,
            repeatMode = repeatMode,
            isShuffle = isShuffle,
            supportsRepeat = supportsRepeat,
            supportsShuffle = supportsShuffle
        )
        MusicWidgetManager.saveTrackInfo(this, newTrackInfo)
        MusicWidgetUpdater.updateAllMusicWidgets(this)

        if (refreshArtwork && metadata != null) {
            queueArtworkUpdate(controller, metadata)
        }
    }

    private fun queueArtworkUpdate(controller: MediaController, metadata: MediaMetadata) {
        val requestId = artworkRequestId.incrementAndGet()
        val token = controller.sessionToken
        artworkScope.launch {
            val artUriString = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            val artworkBitmap = artUriString
                ?.takeIf { it.isNotBlank() }
                ?.let(::decodeArtworkUri)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

            val resolvedArtwork = artworkBitmap
            if (
                resolvedArtwork != null &&
                requestId == artworkRequestId.get() &&
                selectedController?.sessionToken == token
            ) {
                MusicWidgetManager.saveArtwork(this@MusicNotificationListenerService, resolvedArtwork)
                MusicWidgetUpdater.updateAllMusicWidgets(this@MusicNotificationListenerService)
            }
        }
    }

    private fun decodeArtworkUri(uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            val largestDimension = maxOf(bounds.outWidth, bounds.outHeight)
            while (largestDimension / (sampleSize * 2) >= ARTWORK_DECODE_TARGET_PX) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (_: Throwable) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun extractArtworkFromNotification(sbn: StatusBarNotification): Bitmap? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        val largeIcon = notification.getLargeIcon()
        if (largeIcon != null) {
            try {
                val drawable = largeIcon.loadDrawable(this)
                if (drawable is BitmapDrawable) {
                    return drawable.bitmap
                } else if (drawable != null) {
                    val w = drawable.intrinsicWidth.coerceAtLeast(1)
                    val h = drawable.intrinsicHeight.coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    return bitmap
                }
            } catch (_: Throwable) {
            }
        }

        try {
            val bmp = androidx.core.os.BundleCompat.getParcelable(extras, Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            if (bmp != null) return bmp
        } catch (_: Throwable) {}

        try {
            val bmp = androidx.core.os.BundleCompat.getParcelable(extras, Notification.EXTRA_PICTURE, Bitmap::class.java)
            if (bmp != null) return bmp
        } catch (_: Throwable) {}

        return null
    }

    private fun refreshActiveSessions() {
        try {
            val componentName = ComponentName(this, MusicNotificationListenerService::class.java)
            handleSessionsChanged(sessionManager?.getActiveSessions(componentName))
        } catch (_: Throwable) {
        }
    }

    private fun clearSelectedController(markStopped: Boolean) {
        synchronized(selectionLock) {
            val controller = selectedController
            val callback = selectedCallback
            if (controller != null && callback != null) {
                try {
                    controller.unregisterCallback(callback)
                } catch (_: Throwable) {
                }
            }
            val compatController = selectedCompatController
            val compatCallback = selectedCompatCallback
            if (compatController != null && compatCallback != null) {
                try {
                    compatController.unregisterCallback(compatCallback)
                } catch (_: Throwable) {
                }
            }
            selectedController = null
            selectedCallback = null
            selectedCompatController = null
            selectedCompatCallback = null
            MusicWidgetManager.activeMediaController = null
            artworkRequestId.incrementAndGet()
            controller?.let(MusicSessionModeController::release)
        }
        if (markStopped) {
            val cached = MusicWidgetManager.getTrackInfo(this)
            MusicWidgetManager.saveTrackInfo(
                this,
                cached.copy(isPlaying = false, supportsRepeat = false, supportsShuffle = false)
            )
            MusicWidgetUpdater.updateAllMusicWidgets(this)
        }
    }
}

@Suppress("DEPRECATION")
private fun Bundle.readInt(vararg keys: String): Int? {
    for (key in keys) {
        if (!containsKey(key)) continue
        when (val value = get(key)) {
            is Int -> return value
            is Number -> return value.toInt()
        }
    }
    return null
}

@Suppress("DEPRECATION")
private fun Bundle.readBooleanOrInt(vararg keys: String): Boolean? {
    for (key in keys) {
        if (!containsKey(key)) continue
        when (val value = get(key)) {
            is Boolean -> return value
            is Number -> return value.toInt() != 0
        }
    }
    return null
}
