package dev.qtremors.osyster.core.device.data.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceMathTest {
    @Test
    fun aspectRatioReducesAndIgnoresOrientation() {
        assertEquals("20:9", DeviceMath.aspectRatio(1080, 2400))
        assertEquals("20:9", DeviceMath.aspectRatio(2400, 1080))
        assertEquals("16:9", DeviceMath.aspectRatio(1920, 1080))
        assertNull(DeviceMath.aspectRatio(0, 1080))
    }

    @Test
    fun unwieldyRatiosFallBackToDecimal() {
        assertEquals("2.22:1", DeviceMath.aspectRatio(1080, 2400 - 1, Locale.US))
    }

    @Test
    fun densityBucketsRoundUp() {
        assertEquals("mdpi", DeviceMath.densityBucket(160))
        assertEquals("xxhdpi", DeviceMath.densityBucket(420))
        assertEquals("xxxhdpi", DeviceMath.densityBucket(560))
    }

    @Test
    fun wifiBandsAndStandards() {
        assertEquals("2.4 GHz", DeviceMath.wifiBand(2437))
        assertEquals("5 GHz", DeviceMath.wifiBand(5180))
        assertEquals("6 GHz", DeviceMath.wifiBand(5955))
        assertEquals("900 MHz", DeviceMath.wifiBand(900))
        assertEquals("Wi-Fi 6 (802.11ax)", DeviceMath.wifiStandard(6))
        assertNull(DeviceMath.wifiStandard(0))
    }
}

class DistinguishedNameTest {
    @Test
    fun readsSimpleFields() {
        val dn = "CN=ISRG Root X1,O=Internet Security Research Group,C=US"
        assertEquals("ISRG Root X1", DistinguishedName.field(dn, "CN"))
        assertEquals("Internet Security Research Group", DistinguishedName.field(dn, "O"))
        assertEquals("ISRG Root X1", DistinguishedName.displayName(dn))
    }

    @Test
    fun keepsQuotedAndEscapedCommas() {
        assertEquals("Acme, Inc.", DistinguishedName.field("CN=Root,O=\"Acme, Inc.\",C=US", "O"))
        assertEquals("Acme, Inc.", DistinguishedName.field("CN=Root,O=Acme\\, Inc.,C=US", "O"))
    }

    @Test
    fun displayNameFallsBackThroughFields() {
        assertEquals("Security Unit", DistinguishedName.displayName("OU=Security Unit,O=Org"))
        assertEquals("Org", DistinguishedName.displayName("O=Org,C=DE"))
        assertEquals("C=DE", DistinguishedName.displayName("C=DE"))
    }
}
