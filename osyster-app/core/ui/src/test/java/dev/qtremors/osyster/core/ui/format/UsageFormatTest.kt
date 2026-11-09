package dev.qtremors.osyster.core.ui.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageFormatTest {
    @Test
    fun bytesAndRates() {
        assertEquals("0 B", UsageFormat.bytes(-5, Locale.US))
        assertEquals("1.5 MB", UsageFormat.bytes(1_572_864, Locale.US))
        assertEquals("820 KB/s", UsageFormat.rate(839_680, Locale.US))
    }

    @Test
    fun durations() {
        assertEquals("0m", UsageFormat.duration(0))
        assertEquals("<1m", UsageFormat.duration(30_000))
        assertEquals("45m", UsageFormat.duration(45 * 60_000L))
        assertEquals("3h 12m", UsageFormat.duration((3 * 60 + 12) * 60_000L))
    }
}
