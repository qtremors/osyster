package dev.qtremors.osyster.widget.music

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.palette.graphics.Palette
import dev.qtremors.osyster.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong

data class MusicTrackInfo(
    val title: String,
    val artist: String,
    val packageName: String,
    val isPlaying: Boolean,
    val repeatMode: Int = REPEAT_OFF,
    val isShuffle: Boolean = false,
    val supportsRepeat: Boolean = false,
    val supportsShuffle: Boolean = false
) {
    val isRepeat: Boolean get() = repeatMode != REPEAT_OFF
    val isRepeatOne: Boolean get() = repeatMode == REPEAT_ONE

    constructor(
        title: String,
        artist: String,
        packageName: String,
        isPlaying: Boolean,
        isRepeat: Boolean,
        isShuffle: Boolean
    ) : this(
        title = title,
        artist = artist,
        packageName = packageName,
        isPlaying = isPlaying,
        repeatMode = if (isRepeat) REPEAT_ALL else REPEAT_OFF,
        isShuffle = isShuffle
    )

    companion object {
        const val REPEAT_OFF = 0
        const val REPEAT_ONE = 1
        const val REPEAT_ALL = 2
    }
}

data class ExtractedPalette(
    val dominantColor: Int,
    val accentColor: Int,
    val isLight: Boolean
)

object MusicWidgetManager {

    const val DEFAULT_ACCENT_COLOR = 0xFFFA243C.toInt()
    private const val PREFS_NAME = "osyster_music_widget_prefs"
    private const val KEY_TITLE = "music_last_title"
    private const val KEY_ARTIST = "music_last_artist"
    private const val KEY_PACKAGE = "music_last_package"
    private const val KEY_IS_PLAYING = "music_is_playing"
    private const val KEY_IS_REPEAT = "music_is_repeat"
    private const val KEY_REPEAT_MODE = "music_repeat_mode"
    private const val KEY_IS_SHUFFLE = "music_is_shuffle"
    private const val KEY_SUPPORTS_REPEAT = "music_supports_repeat"
    private const val KEY_SUPPORTS_SHUFFLE = "music_supports_shuffle"
    private const val KEY_ACCENT_COLOR = "music_accent_color"
    private const val ARTWORK_FILENAME = "music_widget_artwork.png"

    private const val MAX_SOURCE_ART_SIZE = 1024
    private const val MAX_THUMB_ART_SIZE = 256

    @Volatile
    var activeMediaController: MediaController? = null

    @Volatile
    var cachedArtwork: Bitmap? = null

    @Volatile
    var cachedCircularArtwork: Bitmap? = null

    @Volatile
    private var cachedCircularArtworkSize: Int = 0

    @Volatile
    var cachedSquareArtwork: Bitmap? = null

    @Volatile
    var cachedPalette: ExtractedPalette? = null

    @Volatile
    private var cachedPaletteIsDark: Boolean? = null

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val artworkIoScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private val artworkGeneration = AtomicLong()

    fun hasNotificationAccess(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    fun saveTrackInfo(context: Context, trackInfo: MusicTrackInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TITLE, trackInfo.title)
            .putString(KEY_ARTIST, trackInfo.artist)
            .putString(KEY_PACKAGE, trackInfo.packageName)
            .putBoolean(KEY_IS_PLAYING, trackInfo.isPlaying)
            .putInt(KEY_REPEAT_MODE, trackInfo.repeatMode)
            .putBoolean(KEY_IS_REPEAT, trackInfo.isRepeat)
            .putBoolean(KEY_IS_SHUFFLE, trackInfo.isShuffle)
            .putBoolean(KEY_SUPPORTS_REPEAT, trackInfo.supportsRepeat)
            .putBoolean(KEY_SUPPORTS_SHUFFLE, trackInfo.supportsShuffle)
            .apply()
    }

    fun getTrackInfo(context: Context): MusicTrackInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultMode = if (prefs.getBoolean(KEY_IS_REPEAT, false)) MusicTrackInfo.REPEAT_ALL else MusicTrackInfo.REPEAT_OFF
        val repeatMode = prefs.getInt(KEY_REPEAT_MODE, defaultMode)
        return MusicTrackInfo(
            title = prefs.getString(KEY_TITLE, null).orEmpty(),
            artist = prefs.getString(KEY_ARTIST, null).orEmpty(),
            packageName = prefs.getString(KEY_PACKAGE, null).orEmpty(),
            isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false),
            repeatMode = repeatMode,
            isShuffle = prefs.getBoolean(KEY_IS_SHUFFLE, false),
            supportsRepeat = prefs.getBoolean(KEY_SUPPORTS_REPEAT, false),
            supportsShuffle = prefs.getBoolean(KEY_SUPPORTS_SHUFFLE, false)
        )
    }

    fun saveArtwork(context: Context, bitmap: Bitmap) {
        try {
            val minDim = Math.min(bitmap.width, bitmap.height)
            val square = if (bitmap.width == minDim && bitmap.height == minDim) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, (bitmap.width - minDim) / 2, (bitmap.height - minDim) / 2, minDim, minDim)
            }

            val safeSquare = if (square.width > MAX_SOURCE_ART_SIZE) {
                Bitmap.createScaledBitmap(square, MAX_SOURCE_ART_SIZE, MAX_SOURCE_ART_SIZE, true)
            } else {
                square
            }

            cachedArtwork = safeSquare

            // Pre-render artwork crops immediately
            val density = context.resources.displayMetrics.density
            val thumbPx = (52f * density).toInt().coerceIn(120, MAX_THUMB_ART_SIZE)
            val thumbRadiusPx = 14f * density

            cachedCircularArtwork = null
            cachedCircularArtworkSize = 0
            cachedSquareArtwork = createRoundedBitmap(safeSquare, thumbPx, thumbPx, thumbRadiusPx)
            cachedPalette = null
            cachedPaletteIsDark = null

            // Persist to file asynchronously to avoid blocking the main or notification thread
            val generation = artworkGeneration.incrementAndGet()
            artworkIoScope.launch {
                if (generation != artworkGeneration.get()) return@launch
                try {
                    val file = File(context.cacheDir, ARTWORK_FILENAME)
                    FileOutputStream(file).use { out ->
                        safeSquare.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun getArtwork(context: Context): Bitmap? {
        cachedArtwork?.let { return it }
        val file = File(context.cacheDir, ARTWORK_FILENAME)
        if (!file.exists()) return null
        return try {
            val decoded = BitmapFactory.decodeFile(file.absolutePath)
            if (decoded != null) {
                val minDim = Math.min(decoded.width, decoded.height)
                val square = if (decoded.width == minDim && decoded.height == minDim) {
                    decoded
                } else {
                    Bitmap.createBitmap(
                        decoded,
                        (decoded.width - minDim) / 2,
                        (decoded.height - minDim) / 2,
                        minDim,
                        minDim
                    )
                }
                val safeSquare = if (square.width > MAX_SOURCE_ART_SIZE) {
                    Bitmap.createScaledBitmap(square, MAX_SOURCE_ART_SIZE, MAX_SOURCE_ART_SIZE, true)
                } else square
                cachedArtwork = safeSquare
                val density = context.resources.displayMetrics.density
                val thumbPx = (52f * density).toInt().coerceIn(120, MAX_THUMB_ART_SIZE)
                val thumbRadiusPx = 14f * density
                cachedCircularArtwork = null
                cachedCircularArtworkSize = 0
                cachedSquareArtwork = createRoundedBitmap(safeSquare, thumbPx, thumbPx, thumbRadiusPx)
                return safeSquare
            }
            decoded
        } catch (_: Throwable) {
            null
        }
    }

    fun clearArtwork(context: Context) {
        cachedArtwork = null
        cachedCircularArtwork = null
        cachedCircularArtworkSize = 0
        cachedSquareArtwork = null
        cachedPalette = null
        cachedPaletteIsDark = null
        artworkGeneration.incrementAndGet()
        artworkIoScope.launch {
            try {
                File(context.cacheDir, ARTWORK_FILENAME).delete()
            } catch (_: Throwable) {
            }
        }
    }

    fun getActiveMediaController(context: Context): MediaController? {
        if (!hasNotificationAccess(context)) {
            activeMediaController = null
            return null
        }
        return try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return null
            val componentName = ComponentName(context, MusicNotificationListenerService::class.java)
            val controllers = sessionManager.getActiveSessions(componentName)
            if (controllers.isNullOrEmpty()) {
                activeMediaController = null
                return null
            }

            val currentToken = activeMediaController?.sessionToken
            val selectedToken = selectMediaSession(
                candidates = controllers.map { controller ->
                    SessionCandidate(
                        key = controller.sessionToken,
                        activity = controller.playbackState.toSessionActivity(),
                        lastUpdateTime = controller.playbackState?.lastPositionUpdateTime ?: 0L
                    )
                },
                currentKey = currentToken
            )
            val chosen = controllers.firstOrNull { it.sessionToken == selectedToken }

            if (chosen != null) {
                activeMediaController = chosen
            }
            chosen
        } catch (_: Throwable) {
            null
        }
    }

    fun togglePlayPause(context: Context) {
        val current = getTrackInfo(context)
        val controller = activeMediaController ?: getActiveMediaController(context)
        if (controller != null) {
            try {
                val state = controller.playbackState?.state
                if (state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
                saveTrackInfo(context, current.copy(isPlaying = state != PlaybackState.STATE_PLAYING))
            } catch (_: Throwable) {
                dispatchFallbackPlayPause(context)
            }
        } else {
            dispatchFallbackPlayPause(context)
        }
    }

    private fun dispatchFallbackPlayPause(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
    }

    fun skipNext(context: Context) {
        val controller = activeMediaController ?: getActiveMediaController(context)
        if (controller != null) {
            try {
                controller.transportControls.skipToNext()
            } catch (_: Throwable) {
                dispatchFallbackKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            }
        } else {
            dispatchFallbackKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun skipPrevious(context: Context) {
        val controller = activeMediaController ?: getActiveMediaController(context)
        if (controller != null) {
            try {
                controller.transportControls.skipToPrevious()
            } catch (_: Throwable) {
                dispatchFallbackKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
        } else {
            dispatchFallbackKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    private fun dispatchFallbackKey(context: Context, keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun toggleShuffle(context: Context) {
        val cached = getTrackInfo(context)
        val controller = activeMediaController ?: getActiveMediaController(context)
        if (controller == null) return
        val newShuffle = !cached.isShuffle
        if (MusicSessionModeController.setShuffleEnabled(context, controller, newShuffle)) {
            saveTrackInfo(context, cached.copy(isShuffle = newShuffle, supportsShuffle = true))
            scheduleModeReconciliation(context, controller)
        }
    }

    fun toggleRepeat(context: Context) {
        val cached = getTrackInfo(context)
        val nextMode = when (cached.repeatMode) {
            MusicTrackInfo.REPEAT_OFF -> MusicTrackInfo.REPEAT_ALL
            MusicTrackInfo.REPEAT_ALL -> MusicTrackInfo.REPEAT_ONE
            else -> MusicTrackInfo.REPEAT_OFF
        }
        val controller = activeMediaController ?: getActiveMediaController(context)
        if (controller == null) return
        if (MusicSessionModeController.setRepeatMode(context, controller, nextMode)) {
            saveTrackInfo(context, cached.copy(repeatMode = nextMode, supportsRepeat = true))
            scheduleModeReconciliation(context, controller)
        }
    }

    private fun scheduleModeReconciliation(context: Context, controller: MediaController) {
        val appContext = context.applicationContext
        val token = controller.sessionToken
        ioScope.launch {
            delay(750L)
            if (activeMediaController?.sessionToken != token) return@launch
            val modes = MusicSessionModeController.read(appContext, controller)
            if (!modes.stateAvailable) return@launch
            val current = getTrackInfo(appContext)
            saveTrackInfo(
                appContext,
                current.copy(
                    repeatMode = modes.repeatMode,
                    isShuffle = modes.isShuffleEnabled,
                    supportsRepeat = modes.supportsRepeat,
                    supportsShuffle = modes.supportsShuffle
                )
            )
            MusicWidgetUpdater.updateAllMusicWidgets(appContext)
        }
    }

    fun extractPalette(
        bitmap: Bitmap?,
        isSystemDark: Boolean
    ): ExtractedPalette {
        if (cachedPalette != null && cachedPaletteIsDark == isSystemDark) {
            return cachedPalette!!
        }

        val backgroundColor = if (isSystemDark) 0xFF1C1C1E.toInt() else 0xFFF1F5ED.toInt()

        if (bitmap == null) {
            val fallback = ExtractedPalette(backgroundColor, DEFAULT_ACCENT_COLOR, !isSystemDark)
            cachedPalette = fallback
            cachedPaletteIsDark = isSystemDark
            return fallback
        }

        val fallbackAccent = if (isSystemDark) 0xFFB0B0B0.toInt() else 0xFF636366.toInt()

        return try {
            val palette = Palette.from(bitmap).clearFilters().generate()

            val rawAccent = palette.getVibrantColor(
                palette.getLightVibrantColor(
                    palette.getDarkVibrantColor(
                        palette.getMutedColor(
                            palette.getLightMutedColor(
                                palette.getDarkMutedColor(
                                    palette.swatches.maxByOrNull { it.population }?.rgb ?: fallbackAccent
                                )
                            )
                        )
                    )
                )
            )

            // Ensure readable contrast against widget surface
            var chosenAccent = rawAccent
            if (ColorUtils.calculateContrast(chosenAccent, backgroundColor) < 2.0) {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(chosenAccent, hsl)
                if (isSystemDark) {
                    while (ColorUtils.calculateContrast(chosenAccent, backgroundColor) < 2.5 && hsl[2] < 0.95f) {
                        hsl[2] = (hsl[2] + 0.05f).coerceAtMost(1.0f)
                        chosenAccent = ColorUtils.HSLToColor(hsl)
                    }
                } else {
                    while (ColorUtils.calculateContrast(chosenAccent, backgroundColor) < 2.5 && hsl[2] > 0.05f) {
                        hsl[2] = (hsl[2] - 0.05f).coerceAtLeast(0.0f)
                        chosenAccent = ColorUtils.HSLToColor(hsl)
                    }
                }
            }

            val dominant = palette.getDominantColor(backgroundColor)
            val isLight = ColorUtils.calculateLuminance(dominant) > 0.5

            val result = ExtractedPalette(dominant, chosenAccent, isLight)
            cachedPalette = result
            cachedPaletteIsDark = isSystemDark
            result
        } catch (_: Throwable) {
            val fallback = ExtractedPalette(backgroundColor, fallbackAccent, !isSystemDark)
            cachedPalette = fallback
            cachedPaletteIsDark = isSystemDark
            fallback
        }
    }

    fun getCircularBitmap(source: Bitmap, targetSizePx: Int = 0): Bitmap {
        val minDim = Math.min(source.width, source.height)
        val safeSize = if (targetSizePx > 0) targetSizePx else minDim.coerceAtLeast(1)
        val square = if (source.width == minDim && source.height == minDim) {
            source
        } else {
            Bitmap.createBitmap(source, (source.width - minDim) / 2, (source.height - minDim) / 2, minDim, minDim)
        }

        val scaled = if (square.width == safeSize && square.height == safeSize) {
            square
        } else {
            Bitmap.createScaledBitmap(square, safeSize, safeSize, true)
        }

        val output = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val radius = safeSize / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        return output
    }

    fun getCircularArtwork(source: Bitmap, targetSizePx: Int): Bitmap {
        val safeSize = targetSizePx.coerceIn(1, MAX_SOURCE_ART_SIZE)
        cachedCircularArtwork?.let { cached ->
            if (cachedCircularArtworkSize == safeSize) return cached
        }
        return getCircularBitmap(source, safeSize).also { rendered ->
            cachedCircularArtwork = rendered
            cachedCircularArtworkSize = safeSize
        }
    }

    fun createRoundedBitmap(
        source: Bitmap,
        widthPx: Int,
        heightPx: Int,
        cornerRadiusPx: Float
    ): Bitmap {
        val safeW = widthPx.coerceAtLeast(1)
        val safeH = heightPx.coerceAtLeast(1)

        val minDim = Math.min(source.width, source.height)
        val square = if (source.width == minDim && source.height == minDim) {
            source
        } else {
            Bitmap.createBitmap(source, (source.width - minDim) / 2, (source.height - minDim) / 2, minDim, minDim)
        }

        val scaled = Bitmap.createScaledBitmap(square, safeW, safeH, true)

        val output = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val rect = RectF(0f, 0f, safeW.toFloat(), safeH.toFloat())
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        return output
    }

    fun createDefaultDiscBitmap(sizePx: Int, isDarkMode: Boolean, context: Context): Bitmap {
        val safeSize = sizePx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = if (isDarkMode) 0xFF2C2C2E.toInt() else 0xFFE5E5EA.toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
        }
        val radius = safeSize / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        val iconSize = (safeSize * 0.4f).toInt()
        val iconColor = if (isDarkMode) 0xFF8E8E93.toInt() else 0xFF636366.toInt()
        val iconBitmap = getTintedBitmap(context, R.drawable.ic_music_note, iconColor, iconSize)
        val iconLeft = (safeSize - iconSize) / 2f
        val iconTop = (safeSize - iconSize) / 2f
        canvas.drawBitmap(iconBitmap, iconLeft, iconTop, null)

        return bitmap
    }

    fun createDefaultThumbBitmap(widthPx: Int, heightPx: Int, cornerRadiusPx: Float, isDarkMode: Boolean, context: Context): Bitmap {
        val safeW = widthPx.coerceAtLeast(1)
        val safeH = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = if (isDarkMode) 0xFF2C2C2E.toInt() else 0xFFE5E5EA.toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
        }
        val rect = RectF(0f, 0f, safeW.toFloat(), safeH.toFloat())
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)

        val iconSize = (Math.min(safeW, safeH) * 0.48f).toInt()
        val iconColor = if (isDarkMode) 0xFF8E8E93.toInt() else 0xFF636366.toInt()
        val iconBitmap = getTintedBitmap(context, R.drawable.ic_music_note, iconColor, iconSize)
        val iconLeft = (safeW - iconSize) / 2f
        val iconTop = (safeH - iconSize) / 2f
        canvas.drawBitmap(iconBitmap, iconLeft, iconTop, null)

        return bitmap
    }

    fun getTintedBitmap(
        context: Context,
        @DrawableRes drawableRes: Int,
        @ColorInt color: Int,
        sizePx: Int = 0
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val targetSize = if (sizePx > 0) sizePx else (24f * density).toInt().coerceAtLeast(1)
        val drawable = ContextCompat.getDrawable(context, drawableRes)?.mutate()
            ?: return Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)

        DrawableCompat.setTint(drawable, color)
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)
        return bitmap
    }
}

internal fun PlaybackState?.toSessionActivity(): SessionActivity = when (this?.state) {
    PlaybackState.STATE_PLAYING -> SessionActivity.PLAYING
    PlaybackState.STATE_BUFFERING,
    PlaybackState.STATE_CONNECTING,
    PlaybackState.STATE_FAST_FORWARDING,
    PlaybackState.STATE_REWINDING,
    PlaybackState.STATE_SKIPPING_TO_NEXT,
    PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
    PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> SessionActivity.TRANSITIONAL
    PlaybackState.STATE_PAUSED -> SessionActivity.PAUSED
    else -> SessionActivity.INACTIVE
}

internal fun MediaController.findModeCustomAction(control: MusicModeControl): PlaybackState.CustomAction? {
    val actions = playbackState?.customActions.orEmpty()
    val actionId = findModeAction(
        actions = actions.map { SessionAction(it.action, it.name?.toString().orEmpty()) },
        control = control
    ) ?: return null
    return actions.firstOrNull { it.action == actionId }
}
