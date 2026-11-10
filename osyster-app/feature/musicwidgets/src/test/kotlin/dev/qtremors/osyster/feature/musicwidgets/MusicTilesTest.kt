package dev.qtremors.osyster.feature.musicwidgets

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicTilesTest {
    @Test
    fun `auto follows the widget's height`() {
        val auto = MusicTileConfig()
        assertEquals(MusicTileKind.GRID, auto.kind(GRID_MIN_HEIGHT_DP))
        assertEquals(MusicTileKind.GRID, auto.kind(260))
        assertEquals(MusicTileKind.TILES, auto.kind(GRID_MIN_HEIGHT_DP - 1))
        assertEquals(MusicTileKind.TILES, auto.kind(110))
        // An unknown size (some launchers report none) gets the full grid.
        assertEquals(MusicTileKind.GRID, auto.kind(null))
    }

    @Test
    fun `a picked size stays at any height`() {
        assertEquals(MusicTileKind.GRID, MusicTileConfig(size = TileSize.LARGE).kind(100))
        assertEquals(MusicTileKind.TILES, MusicTileConfig(size = TileSize.SMALL).kind(400))
    }

    @Test
    fun `play sits beside shuffle and repeat when asked`() {
        assertEquals("PLN", MusicTileConfig().rowOrder())
        assertEquals("LPN", MusicTileConfig(playBySide = true, sideLeft = true).rowOrder())
        assertEquals("PNL", MusicTileConfig(playBySide = true, sideLeft = false).rowOrder())
    }

    @Test
    fun `the grid always has shuffle and repeat beside the art`() {
        val playRepeat = MusicTileConfig(buttons = SideButtons.PLAY_REPEAT)
        assertEquals(SideButtons.SHUFFLE_REPEAT, playRepeat.sideButtons(MusicTileKind.GRID))
        assertEquals(SideButtons.PLAY_REPEAT, playRepeat.sideButtons(MusicTileKind.TILES))
    }

    @Test
    fun `each arrangement has its own layout`() {
        assertEquals(R.layout.widget_music_grid_top_left, MusicTileConfig().layout(MusicTileKind.GRID))
        assertEquals(R.layout.widget_music_grid_bottom_right, MusicTileConfig(controlsTop = false, sideLeft = false).layout(MusicTileKind.GRID))
        assertEquals(R.layout.widget_music_tiles_left, MusicTileConfig().layout(MusicTileKind.TILES))
        assertEquals(R.layout.widget_music_tiles_right, MusicTileConfig(sideLeft = false).layout(MusicTileKind.TILES))
    }
}
