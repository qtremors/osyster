package dev.qtremors.osyster.widget.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicWidgetTest {

    @Test
    fun widgetActionConstants_areConfiguredCorrectly() {
        assertEquals("dev.qtremors.osyster.action.MUSIC_PLAY_PAUSE", MusicWidgetUpdater.ACTION_PLAY_PAUSE)
        assertEquals("dev.qtremors.osyster.action.MUSIC_NEXT", MusicWidgetUpdater.ACTION_NEXT)
        assertEquals("dev.qtremors.osyster.action.MUSIC_PREV", MusicWidgetUpdater.ACTION_PREV)
        assertEquals("dev.qtremors.osyster.action.MUSIC_SHUFFLE", MusicWidgetUpdater.ACTION_SHUFFLE)
        assertEquals("dev.qtremors.osyster.action.MUSIC_REPEAT", MusicWidgetUpdater.ACTION_REPEAT)
    }

    @Test
    fun musicTrackInfo_propertiesAndCopy_workCorrectly() {
        val track = MusicTrackInfo(
            title = "Try Me",
            artist = "The Weeknd",
            packageName = "com.apple.android.music",
            isPlaying = true,
            isRepeat = false,
            isShuffle = true
        )

        assertEquals("Try Me", track.title)
        assertEquals("The Weeknd", track.artist)
        assertEquals("com.apple.android.music", track.packageName)
        assertTrue(track.isPlaying)
        assertFalse(track.isRepeat)
        assertFalse(track.isRepeatOne)
        assertEquals(MusicTrackInfo.REPEAT_OFF, track.repeatMode)
        assertTrue(track.isShuffle)

        val repeatAllTrack = track.copy(repeatMode = MusicTrackInfo.REPEAT_ALL)
        assertTrue(repeatAllTrack.isRepeat)
        assertFalse(repeatAllTrack.isRepeatOne)
        assertEquals(MusicTrackInfo.REPEAT_ALL, repeatAllTrack.repeatMode)

        val repeatOneTrack = track.copy(repeatMode = MusicTrackInfo.REPEAT_ONE)
        assertTrue(repeatOneTrack.isRepeat)
        assertTrue(repeatOneTrack.isRepeatOne)
        assertEquals(MusicTrackInfo.REPEAT_ONE, repeatOneTrack.repeatMode)

        val pausedTrack = track.copy(isPlaying = false)
        assertFalse(pausedTrack.isPlaying)
        assertEquals("Try Me", pausedTrack.title)
    }

    @Test
    fun repeatMode_cycling_cyclesCorrectly() {
        var mode = MusicTrackInfo.REPEAT_OFF

        // Next from OFF is ALL
        mode = when (mode) {
            MusicTrackInfo.REPEAT_OFF -> MusicTrackInfo.REPEAT_ALL
            MusicTrackInfo.REPEAT_ALL -> MusicTrackInfo.REPEAT_ONE
            else -> MusicTrackInfo.REPEAT_OFF
        }
        assertEquals(MusicTrackInfo.REPEAT_ALL, mode)

        // Next from ALL is ONE (single repeat)
        mode = when (mode) {
            MusicTrackInfo.REPEAT_OFF -> MusicTrackInfo.REPEAT_ALL
            MusicTrackInfo.REPEAT_ALL -> MusicTrackInfo.REPEAT_ONE
            else -> MusicTrackInfo.REPEAT_OFF
        }
        assertEquals(MusicTrackInfo.REPEAT_ONE, mode)

        // Next from ONE is OFF
        mode = when (mode) {
            MusicTrackInfo.REPEAT_OFF -> MusicTrackInfo.REPEAT_ALL
            MusicTrackInfo.REPEAT_ALL -> MusicTrackInfo.REPEAT_ONE
            else -> MusicTrackInfo.REPEAT_OFF
        }
        assertEquals(MusicTrackInfo.REPEAT_OFF, mode)
    }

    @Test
    fun sessionSelection_keepsCurrentPlayingSession() {
        val candidates = listOf(
            SessionCandidate("spotify", SessionActivity.PLAYING, lastUpdateTime = 10L),
            SessionCandidate("apple", SessionActivity.PLAYING, lastUpdateTime = 20L)
        )

        assertEquals("spotify", selectMediaSession(candidates, currentKey = "spotify"))
    }

    @Test
    fun sessionSelection_switchesOnlyWhenAnotherSessionStartsPlaying() {
        val candidates = listOf(
            SessionCandidate("spotify", SessionActivity.PAUSED, lastUpdateTime = 20L),
            SessionCandidate("youtube", SessionActivity.PLAYING, lastUpdateTime = 10L)
        )

        assertEquals("youtube", selectMediaSession(candidates, currentKey = "spotify"))
    }

    @Test
    fun sessionSelection_doesNotLetPausedNotificationStealCurrentPlayer() {
        val candidates = listOf(
            SessionCandidate("spotify", SessionActivity.PAUSED, lastUpdateTime = 10L),
            SessionCandidate("apple", SessionActivity.PAUSED, lastUpdateTime = 20L)
        )

        assertEquals("spotify", selectMediaSession(candidates, currentKey = "spotify"))
    }

    @Test
    fun modeActions_onlyUseActionsAdvertisedByThePlayer() {
        val actions = listOf(
            SessionAction("com.player.LIKE", "Like"),
            SessionAction("com.player.SET_SHUFFLE", "Shuffle"),
            SessionAction("com.player.LOOP_MODE", "Loop")
        )

        assertEquals(
            "com.player.SET_SHUFFLE",
            findModeAction(actions, MusicModeControl.SHUFFLE)
        )
        assertEquals(
            "com.player.LOOP_MODE",
            findModeAction(actions, MusicModeControl.REPEAT)
        )
    }

    @Test
    fun modeActions_returnNullWhenPlayerDoesNotExposeControl() {
        val actions = listOf(SessionAction("com.player.LIKE", "Like"))

        assertEquals(null, findModeAction(actions, MusicModeControl.SHUFFLE))
        assertEquals(null, findModeAction(actions, MusicModeControl.REPEAT))
    }

    @Test
    fun extractedPalette_nullBitmap_respectsSystemTheme() {
        val lightPalette = MusicWidgetManager.extractPalette(null, isSystemDark = false)
        assertTrue(lightPalette.isLight)
        assertEquals(0xFFF1F5ED.toInt(), lightPalette.dominantColor)
        assertEquals(MusicWidgetManager.DEFAULT_ACCENT_COLOR, lightPalette.accentColor)

        val darkPalette = MusicWidgetManager.extractPalette(null, isSystemDark = true)
        assertFalse(darkPalette.isLight)
        assertEquals(0xFF1C1C1E.toInt(), darkPalette.dominantColor)
        assertEquals(MusicWidgetManager.DEFAULT_ACCENT_COLOR, darkPalette.accentColor)
    }
}
