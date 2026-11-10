package dev.qtremors.osyster.feature.musicwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils

/** The two arrangements of the music tiles widget: the 3x3 grid and the 3x2 tiles. */
enum class MusicTileKind { GRID, TILES }

/**
 * The widget's size: [AUTO] picks the grid when it is about three cells tall and the tiles when
 * it is two, so resizing on the home screen switches between them; the others keep one.
 */
enum class TileSize { AUTO, LARGE, SMALL }

/** What the two side tiles hold. */
enum class SideButtons { SHUFFLE_REPEAT, PLAY_REPEAT }

/**
 * Where the tiles go. [controlsTop] places the previous, play, and next row above the art
 * (grid only); [sideLeft] puts the two side tiles left of the art; [buttons] picks what they hold
 * (the 3x2 tiles only; the grid always has shuffle and repeat beside the art). [playBySide] moves
 * play or pause into the row's corner tile, the one touching shuffle and repeat (grid only).
 */
data class MusicTileConfig(
    val controlsTop: Boolean = true,
    val sideLeft: Boolean = true,
    val buttons: SideButtons = SideButtons.SHUFFLE_REPEAT,
    val playBySide: Boolean = false,
    val size: TileSize = TileSize.AUTO
) {
    /** The arrangement for a widget [heightDp] tall (its portrait height, null when unknown). */
    fun kind(heightDp: Int?): MusicTileKind = when (size) {
        TileSize.LARGE -> MusicTileKind.GRID
        TileSize.SMALL -> MusicTileKind.TILES
        TileSize.AUTO -> if ((heightDp ?: GRID_MIN_HEIGHT_DP) >= GRID_MIN_HEIGHT_DP) MusicTileKind.GRID else MusicTileKind.TILES
    }

    fun layout(kind: MusicTileKind): Int = when (kind) {
        MusicTileKind.GRID -> when {
            controlsTop && sideLeft -> R.layout.widget_music_grid_top_left
            controlsTop -> R.layout.widget_music_grid_top_right
            sideLeft -> R.layout.widget_music_grid_bottom_left
            else -> R.layout.widget_music_grid_bottom_right
        }
        MusicTileKind.TILES -> if (sideLeft) R.layout.widget_music_tiles_left else R.layout.widget_music_tiles_right
    }

    /**
     * The row's three tiles from left to right, as previous (P), play (L), and next (N). Play sits
     * in the middle, or with [playBySide] in the corner tile above or below the side buttons.
     */
    fun rowOrder(): String = when {
        !playBySide -> "PLN"
        sideLeft -> "LPN"
        else -> "PNL"
    }

    /** The side tiles' content; the grid's row already has play, so its side is always shuffle and repeat. */
    fun sideButtons(kind: MusicTileKind): SideButtons = if (kind == MusicTileKind.GRID) SideButtons.SHUFFLE_REPEAT else buttons
}

/** Each placed tile widget's layout, kept by widget ID. */
internal object MusicTileSettings {
    private const val FILE = "music_tiles"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun has(context: Context, id: Int): Boolean = prefs(context).contains("top_$id")

    fun read(context: Context, id: Int): MusicTileConfig {
        val prefs = prefs(context)
        return MusicTileConfig(
            controlsTop = prefs.getBoolean("top_$id", true),
            sideLeft = prefs.getBoolean("left_$id", true),
            buttons = prefs.getString("buttons_$id", null)?.let { saved -> SideButtons.entries.firstOrNull { it.name == saved } }
                ?: SideButtons.SHUFFLE_REPEAT,
            playBySide = prefs.getBoolean("play_side_$id", false),
            size = prefs.getString("size_$id", null)?.let { saved -> TileSize.entries.firstOrNull { it.name == saved } } ?: TileSize.AUTO
        )
    }

    fun write(context: Context, id: Int, config: MusicTileConfig) {
        prefs(context).edit()
            .putBoolean("top_$id", config.controlsTop)
            .putBoolean("left_$id", config.sideLeft)
            .putString("buttons_$id", config.buttons.name)
            .putBoolean("play_side_$id", config.playBySide)
            .putString("size_$id", config.size.name)
            .apply()
    }

    fun delete(context: Context, ids: IntArray) {
        val editor = prefs(context).edit()
        ids.forEach { editor.remove("top_$it").remove("left_$it").remove("buttons_$it").remove("play_side_$it").remove("size_$it") }
        editor.apply()
    }
}

/**
 * Fills a music tile layout: square album art with rounded corners, and each button tinted from
 * the art's colors. Buttons go to [MusicWidgetControlReceiver] like the disc and capsule, so they
 * behave the same. Without notification access every tile opens the access settings.
 */
internal object MusicTileRenderer {

    fun update(context: Context, manager: AppWidgetManager, id: Int) {
        runCatching {
            val config = MusicTileSettings.read(context, id)
            val kind = config.kind(heightDp(manager, id))
            val views = RemoteViews(context.packageName, config.layout(kind))
            fill(context, views, kind, config, id)
            manager.updateAppWidget(id, views)
        }
    }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        (manager.getAppWidgetIds(ComponentName(context, MusicGridWidgetReceiver::class.java)) ?: IntArray(0)).forEach { update(context, manager, it) }
    }

    /** The widget's height in portrait, where launchers report it as the largest height. */
    fun heightDp(manager: AppWidgetManager, id: Int): Int? =
        manager.getAppWidgetOptions(id)?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)?.takeIf { it > 0 }

    /** Fills [views], built from [config]'s layout for [kind]; also used by the setup screen's preview. */
    fun fill(context: Context, views: RemoteViews, kind: MusicTileKind, config: MusicTileConfig, id: Int) {
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val access = MusicWidgetManager.hasNotificationAccess(context)
        val track = MusicWidgetManager.getTrackInfo(context)
        val artwork = MusicWidgetManager.getArtwork(context)
        val tile = context.getColor(R.color.widget_music_button_bg)
        val onTile = context.getColor(R.color.widget_music_primary_text)
        val accent = readable(MusicWidgetManager.extractPalette(artwork, dark).accentColor, tile, dark)
        val density = context.resources.displayMetrics.density
        val iconPx = (26f * density).toInt()
        val setup = if (access) null else accessSettings(context, id)

        // Square art with rounded corners, the same radius as the tiles, drawn large so it stays sharp.
        val artPx = ART_PX
        val radius = artPx * ART_CORNER_SHARE
        val art = artwork?.let { MusicWidgetManager.createRoundedBitmap(it, artPx, artPx, radius) }
            ?: MusicWidgetManager.createDefaultThumbBitmap(artPx, artPx, radius, dark, context)
        views.setImageViewBitmap(R.id.music_tile_art, art)
        views.setOnClickPendingIntent(R.id.music_tile_art, setup ?: openPlayer(context, track.packageName, id))

        // Plain buttons use the widget's text color; the art's color marks play and modes that are on.
        fun button(view: Int, iconRes: Int, color: Int, description: Int, action: String?, slot: Int) {
            views.setImageViewBitmap(view, MusicWidgetManager.getTintedBitmap(context, iconRes, color, iconPx))
            views.setContentDescription(view, context.getString(description))
            views.setOnClickPendingIntent(view, setup ?: action?.let { control(context, it, id, slot) })
        }
        val playIcon = if (track.isPlaying) R.drawable.ic_music_pause else R.drawable.ic_music_play
        val playDescription = if (track.isPlaying) R.string.widget_music_pause_desc else R.string.widget_music_play_desc

        if (kind == MusicTileKind.GRID) {
            // The row's tiles are left, middle, and right; what each holds follows the chosen order.
            listOf(R.id.music_tile_prev, R.id.music_tile_play, R.id.music_tile_next).zip(config.rowOrder().toList()).forEachIndexed { slot, (view, part) ->
                when (part) {
                    'P' -> button(view, R.drawable.ic_music_skip_previous, onTile, R.string.widget_music_prev_desc, MusicWidgetUpdater.ACTION_PREV, slot + 1)
                    'L' -> button(view, playIcon, accent, playDescription, MusicWidgetUpdater.ACTION_PLAY_PAUSE, slot + 1)
                    else -> button(view, R.drawable.ic_music_skip_next, onTile, R.string.widget_music_next_desc, MusicWidgetUpdater.ACTION_NEXT, slot + 1)
                }
            }
        }

        // On: the art's color. Off: the text color. A mode the player cannot change stays in its
        // place, faded, so the tiles never shift.
        fun mode(supported: Boolean, on: Boolean) = when {
            !supported -> ColorUtils.setAlphaComponent(onTile, DISABLED)
            on -> accent
            else -> ColorUtils.setAlphaComponent(onTile, INACTIVE)
        }
        val repeatColor = mode(track.supportsRepeat, track.isRepeat)
        val repeatIcon = if (track.isRepeatOne) R.drawable.ic_music_repeat_one else R.drawable.ic_music_repeat
        val repeatDescription = when (track.repeatMode) {
            MusicTrackInfo.REPEAT_ONE -> R.string.widget_music_repeat_one_desc
            MusicTrackInfo.REPEAT_ALL -> R.string.widget_music_repeat_all_desc
            else -> R.string.widget_music_repeat_off_desc
        }
        when (config.sideButtons(kind)) {
            SideButtons.SHUFFLE_REPEAT -> {
                button(
                    R.id.music_tile_side1, R.drawable.ic_music_shuffle, mode(track.supportsShuffle, track.isShuffle),
                    if (track.isShuffle) R.string.widget_music_shuffle_on_desc else R.string.widget_music_shuffle_off_desc,
                    MusicWidgetUpdater.ACTION_SHUFFLE.takeIf { track.supportsShuffle }, 4
                )
            }
            SideButtons.PLAY_REPEAT -> button(R.id.music_tile_side1, playIcon, accent, playDescription, MusicWidgetUpdater.ACTION_PLAY_PAUSE, 4)
        }
        button(R.id.music_tile_side2, repeatIcon, repeatColor, repeatDescription, MusicWidgetUpdater.ACTION_REPEAT.takeIf { track.supportsRepeat }, 5)
    }

    /**
     * The art's color, lightened (dark theme) or darkened (light theme) until it reads clearly on
     * the tile: dark album art often gives a color that nearly vanishes on a dark tile.
     */
    private fun readable(color: Int, tile: Int, dark: Boolean): Int {
        val toward = if (dark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        var result = ColorUtils.setAlphaComponent(color, 255)
        var step = 0
        while (ColorUtils.calculateContrast(result, tile) < MIN_CONTRAST && step < 10) {
            step++
            result = ColorUtils.blendARGB(color, toward, step / 10f)
        }
        return result
    }

    private fun control(context: Context, action: String, id: Int, slot: Int): PendingIntent = PendingIntent.getBroadcast(
        context, id * 100 + 40 + slot,
        Intent(context, MusicWidgetControlReceiver::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openPlayer(context: Context, packageName: String, id: Int): PendingIntent? {
        val launch = packageName.takeIf { it.isNotBlank() }?.let { context.packageManager.getLaunchIntentForPackage(it) }
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return null
        return PendingIntent.getActivity(context, id * 100 + 40, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun accessSettings(context: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        context, id * 100 + 49,
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Art is drawn at this size and scaled down by the launcher, so it stays sharp on big widgets. */
    private const val ART_PX = 720

    /** Corner radius as a share of the art's side, close to the tiles' corners at common sizes. */
    private const val ART_CORNER_SHARE = 0.09f
    private const val INACTIVE = 170
    private const val DISABLED = 90
    private const val MIN_CONTRAST = 3.0
}

/**
 * The music tiles widget. At 3x3: the art (2x2), previous, play, and next in a row, and shuffle
 * and repeat in a column. At 3x2: the art and two buttons beside it. Resizing redraws it in the
 * arrangement that fits, unless a size is picked in its setup.
 */
class MusicGridWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { MusicTileRenderer.update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        MusicTileRenderer.update(context, manager, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        MusicTileSettings.delete(context, ids)
    }
}

/** About three home-screen cells; shorter widgets get the 3x2 arrangement. */
internal const val GRID_MIN_HEIGHT_DP = 170
