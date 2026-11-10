package dev.qtremors.osyster.core.barcode.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeContentTest {

    @Test
    fun `links phones and email`() {
        assertEquals("https://example.com/a", (parseContent("https://example.com/a", CodeFormat.QR_CODE) as CodeContent.Link).url)
        assertEquals("+15551234567", (parseContent("tel:+15551234567", CodeFormat.QR_CODE) as CodeContent.Phone).number)
        val mail = parseContent("mailto:me@example.com?subject=Hi", CodeFormat.QR_CODE) as CodeContent.Email
        assertEquals("me@example.com", mail.to)
        assertEquals("Hi", mail.subject)
    }

    @Test
    fun `contacts from vCard and MeCard`() {
        val vcard = parseContent(
            "BEGIN:VCARD\nVERSION:3.0\nN:Doe;Jane\nFN:Jane Doe\nTEL:+15550001111\nEMAIL:jane@example.com\nORG:Acme\nEND:VCARD",
            CodeFormat.QR_CODE
        ) as CodeContent.Contact
        assertEquals("Jane Doe", vcard.name)
        assertEquals(listOf("+15550001111"), vcard.phones)
        assertEquals("Acme", vcard.organization)
        val mecard = parseContent("MECARD:N:Roe,Sam;TEL:5552223333;EMAIL:sam@example.com;;", CodeFormat.QR_CODE) as CodeContent.Contact
        assertEquals(listOf("5552223333"), mecard.phones)
    }

    @Test
    fun `wifi products and plain text`() {
        val wifi = parseContent("WIFI:T:WPA;S:Home;P:secret123;;", CodeFormat.QR_CODE) as CodeContent.Wifi
        assertEquals("Home", wifi.ssid)
        assertEquals("secret123", wifi.password)
        val product = parseContent("4006381333931", CodeFormat.EAN_13) as CodeContent.Product
        assertEquals("4006381333931", product.code)
        assertTrue(parseContent("just a note", CodeFormat.QR_CODE) is CodeContent.Text)
    }

    @Test
    fun `actions end with copy and share and offer search sites for products`() {
        val site = SearchSite(1, "Books", "https://example.com/search?q=%s")
        val actions = actionsFor(parseContent("9780306406157", CodeFormat.EAN_13), listOf(site))
        assertEquals(CodeAction.Search(site, "https://example.com/search?q=9780306406157"), actions.first())
        assertTrue(actions[actions.size - 2] is CodeAction.Copy)
        assertTrue(actions.last() is CodeAction.Share)
        assertTrue(actionsFor(parseContent("https://a.b", CodeFormat.QR_CODE), emptyList()).first() is CodeAction.OpenLink)
    }

    @Test
    fun `search templates need a web address with one placeholder`() {
        assertTrue(SearchSite.isValidTemplate("https://example.com/s?k=%s"))
        assertFalse(SearchSite.isValidTemplate("example.com/s?k=%s"))
        assertFalse(SearchSite.isValidTemplate("https://example.com/s"))
        assertFalse(SearchSite.isValidTemplate("https://example.com/%s/%s"))
        assertEquals("https://x.y/?q=a+b%26c", SearchSite(0, "x", "https://x.y/?q=%s").urlFor("a b&c"))
    }
}
