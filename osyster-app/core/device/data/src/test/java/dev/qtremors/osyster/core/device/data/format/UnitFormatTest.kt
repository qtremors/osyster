package dev.qtremors.osyster.core.device.data.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitFormatTest {
    private val en = Locale.US

    @Test
    fun bytesUseBinaryStepsAndOneDecimal() {
        assertEquals("512 B", UnitFormat.bytes(512, en))
        assertEquals("1.5 KB", UnitFormat.bytes(1536, en))
        assertEquals("7.5 GB", UnitFormat.bytes(8_053_063_680, en))
        assertEquals("238 GB", UnitFormat.bytes(255_550_554_112, en))
    }

    @Test
    fun usedOfTotalShowsPercent() {
        assertEquals("1.0 GB of 4.0 GB (25%)", UnitFormat.usedOfTotal(1L shl 30, 4L shl 30, en))
    }

    @Test
    fun frequencySwitchesToGigahertz() {
        assertEquals("787 MHz", UnitFormat.frequencyKhz(787_200, en))
        assertEquals("2.50 GHz", UnitFormat.frequencyKhz(2_496_000, en))
    }

    @Test
    fun durationDropsSeconds() {
        assertEquals("12m", UnitFormat.duration(12 * 60_000L + 59_000L))
        assertEquals("4h 0m", UnitFormat.duration(4 * 3_600_000L))
        assertEquals("1d 2h 3m", UnitFormat.duration(((24 + 2) * 60 + 3) * 60_000L))
    }

    @Test
    fun patchDateFallsBackToRawText() {
        assertEquals("August 1, 2026", UnitFormat.patchDate("2026-08-01", en))
        assertEquals("not-a-date", UnitFormat.patchDate("not-a-date", en))
    }
}
