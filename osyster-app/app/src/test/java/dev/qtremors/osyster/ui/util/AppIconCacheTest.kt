package dev.qtremors.osyster.ui.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import dev.qtremors.osyster.settings.DiagnosticsInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppIconCacheTest {

    private class TestImageBitmap : ImageBitmap {
        override val width: Int = 48
        override val height: Int = 48
        override val hasAlpha: Boolean = true
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888
        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int
        ) {}
        override fun prepareToDraw() {}
    }

    @Before
    fun setUp() {
        AppIconCache.clear()
    }

    @Test
    fun putAndGet_storesAndRetrievesBitmap() {
        val bitmap = TestImageBitmap()
        AppIconCache.put("com.example.app", bitmap)

        assertTrue(AppIconCache.contains("com.example.app"))
        val retrieved = AppIconCache.get("com.example.app")
        assertNotNull(retrieved)
        assertEquals(bitmap, retrieved)
        assertEquals(1, AppIconCache.size())
    }

    @Test
    fun get_returnsNullForMissingPackage() {
        assertNull(AppIconCache.get("com.nonexistent.app"))
        assertFalse(AppIconCache.contains("com.nonexistent.app"))
    }

    @Test
    fun clear_evictsAllEntries() {
        AppIconCache.put("app.one", TestImageBitmap())
        AppIconCache.put("app.two", TestImageBitmap())
        assertEquals(2, AppIconCache.size())

        AppIconCache.clear()
        assertEquals(0, AppIconCache.size())
        assertNull(AppIconCache.get("app.one"))
        assertNull(AppIconCache.get("app.two"))
    }

    @Test
    fun diagnosticsInterval_supports5000msEcoInterval() {
        assertEquals(5000L, DiagnosticsInterval.INTERVAL_5000MS.millis)
        assertEquals("interval_eco", DiagnosticsInterval.INTERVAL_5000MS.labelResName)
        assertEquals(5, DiagnosticsInterval.entries.size)
    }
}
