package dev.qtremors.osyster.feature.measure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasureMathTest {
    @Test
    fun `flat on a table is level`() {
        val flat = tiltFrom(0f, 0f, 9.81f)
        assertTrue(flat.level)
        assertEquals(0f, flat.roll, 0.01f)
        val leaning = tiltFrom(9.81f * 0.5f, 0f, 9.81f * 0.866f)
        assertEquals(30f, leaning.roll, 0.1f)
        assertFalse(leaning.level)
    }

    @Test
    fun `headings wrap the short way round north`() {
        assertEquals(350f, normalizeHeading(-10f), 0.001f)
        assertEquals(10f, normalizeHeading(370f), 0.001f)
        // From 350 toward 10, the needle passes north, not back through south.
        assertEquals(353f, smoothHeading(350f, 10f, 0.15f), 0.01f)
        assertEquals(0, cardinalIndex(355f))
        assertEquals(4, cardinalIndex(90f))
        assertEquals(8, cardinalIndex(180f))
    }

    @Test
    fun `loudness from samples`() {
        assertNull(decibels(ShortArray(0), 0))
        assertEquals(0.0, decibels(ShortArray(100), 100)!!, 0.001)
        val full = ShortArray(100) { if (it % 2 == 0) Short.MAX_VALUE else (-Short.MAX_VALUE).toShort() }
        assertEquals(90.0, decibels(full, 100)!!, 0.01)
        assertEquals(2, loudnessBand(60.0))
    }

    @Test
    fun `screen length`() {
        assertEquals(2.54f, pixelsToCm(400f, 400f), 0.001f)
        assertEquals(400f, cmToPixels(2.54f, 400f), 0.001f)
    }
}
