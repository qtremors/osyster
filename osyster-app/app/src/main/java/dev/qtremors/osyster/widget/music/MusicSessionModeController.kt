package dev.qtremors.osyster.widget.music

import android.content.Context
import android.media.session.MediaController
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

internal data class MusicSessionModes(
    val repeatMode: Int,
    val isShuffleEnabled: Boolean,
    val stateAvailable: Boolean,
    val supportsRepeat: Boolean,
    val supportsShuffle: Boolean
)

/**
 * Bridges repeat and shuffle through the legacy media-session protocol used by
 * many third-party players. Framework MediaController has no standard APIs for
 * these modes, so an advertised custom action remains the preferred route.
 */
internal object MusicSessionModeController {

    private val controllerLock = Any()
    private var cachedToken: android.media.session.MediaSession.Token? = null
    private var cachedController: MediaControllerCompat? = null

    fun read(context: Context, controller: MediaController): MusicSessionModes {
        val repeatAction = controller.findModeCustomAction(MusicModeControl.REPEAT)
        val shuffleAction = controller.findModeCustomAction(MusicModeControl.SHUFFLE)
        val compat = getCompatController(context, controller)

        val compatReady = try {
            compat?.isSessionReady == true
        } catch (_: Throwable) {
            false
        }
        val compatRepeat = try {
            compat?.repeatMode
        } catch (_: Throwable) {
            null
        }
        val compatShuffle = try {
            compat?.shuffleMode
        } catch (_: Throwable) {
            null
        }

        return MusicSessionModes(
            repeatMode = when (compatRepeat) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> MusicTrackInfo.REPEAT_ONE
                PlaybackStateCompat.REPEAT_MODE_ALL,
                PlaybackStateCompat.REPEAT_MODE_GROUP -> MusicTrackInfo.REPEAT_ALL
                else -> MusicTrackInfo.REPEAT_OFF
            },
            isShuffleEnabled = compatShuffle == PlaybackStateCompat.SHUFFLE_MODE_ALL ||
                compatShuffle == PlaybackStateCompat.SHUFFLE_MODE_GROUP,
            stateAvailable = compatReady,
            supportsRepeat = repeatAction != null || compatReady,
            supportsShuffle = shuffleAction != null || compatReady
        )
    }

    fun setRepeatMode(context: Context, controller: MediaController, repeatMode: Int): Boolean {
        val compatMode = when (repeatMode) {
            MusicTrackInfo.REPEAT_ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
            MusicTrackInfo.REPEAT_ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
            else -> PlaybackStateCompat.REPEAT_MODE_NONE
        }
        val compat = getCompatController(context, controller)
        if (compat != null && runCatching { compat.isSessionReady }.getOrDefault(false)) {
            try {
                compat.transportControls.setRepeatMode(compatMode)
                return true
            } catch (_: Throwable) {
            }
        }

        val action = controller.findModeCustomAction(MusicModeControl.REPEAT) ?: return false
        return runCatching {
            controller.transportControls.sendCustomAction(action.action, action.extras)
            true
        }.getOrDefault(false)
    }

    fun setShuffleEnabled(context: Context, controller: MediaController, enabled: Boolean): Boolean {
        val compat = getCompatController(context, controller)
        if (compat != null && runCatching { compat.isSessionReady }.getOrDefault(false)) {
            try {
                compat.transportControls.setShuffleMode(
                    if (enabled) PlaybackStateCompat.SHUFFLE_MODE_ALL else PlaybackStateCompat.SHUFFLE_MODE_NONE
                )
                return true
            } catch (_: Throwable) {
            }
        }

        val action = controller.findModeCustomAction(MusicModeControl.SHUFFLE) ?: return false
        return runCatching {
            controller.transportControls.sendCustomAction(action.action, action.extras)
            true
        }.getOrDefault(false)
    }

    fun release(controller: MediaController) {
        synchronized(controllerLock) {
            if (cachedToken == controller.sessionToken) {
                cachedToken = null
                cachedController = null
            }
        }
    }

    fun getCompatController(context: Context, controller: MediaController): MediaControllerCompat? {
        synchronized(controllerLock) {
            if (cachedToken == controller.sessionToken) return cachedController
            val compat = controller.createCompatController(context.applicationContext)
            cachedToken = controller.sessionToken
            cachedController = compat
            return compat
        }
    }
}

private fun MediaController.createCompatController(context: Context): MediaControllerCompat? {
    return try {
        val compatToken = MediaSessionCompat.Token.fromToken(sessionToken)
        MediaControllerCompat(context, compatToken)
    } catch (_: Throwable) {
        null
    }
}
