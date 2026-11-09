package dev.qtremors.osyster.feature.quicktiles

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorHexTest {
    @Test
    fun dropsAlphaAndPadsToSixDigits() {
        assertEquals("#00687A", colorHex(0xFF00687A.toInt()))
        assertEquals("#000000", colorHex(0xFF000000.toInt()))
        assertEquals("#FFFFFF", colorHex(0x80FFFFFF.toInt()))
    }
}
