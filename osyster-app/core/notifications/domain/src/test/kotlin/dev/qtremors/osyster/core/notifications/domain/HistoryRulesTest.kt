package dev.qtremors.osyster.core.notifications.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRulesTest {
    private val on = HistorySettings(enabled = true)
    private fun note(
        title: String? = "Title",
        text: String? = "Text",
        packageName: String = "com.example",
        ongoing: Boolean = false,
        summary: Boolean = false,
        media: Boolean = false
    ) = CapturedNotification(
        key = "k",
        packageName = packageName,
        appLabel = "Example",
        postedAt = 1_000,
        title = title,
        text = text,
        ongoing = ongoing,
        groupSummary = summary,
        media = media
    )

    @Test
    fun `only what is worth keeping is recorded`() {
        assertTrue(shouldRecord(note(), on, "dev.self"))
        assertFalse(shouldRecord(note(), HistorySettings(), "dev.self"))
        assertFalse(shouldRecord(note(packageName = "dev.self"), on, "dev.self"))
        assertFalse(shouldRecord(note(), on.copy(excluded = setOf("com.example")), "dev.self"))
        assertFalse(shouldRecord(note(media = true), on, "dev.self"))
        assertFalse(shouldRecord(note(summary = true), on, "dev.self"))
        assertFalse(shouldRecord(note(ongoing = true), on, "dev.self"))
        assertTrue(shouldRecord(note(ongoing = true), on.copy(keepOngoing = true), "dev.self"))
        assertFalse(shouldRecord(note(title = " ", text = null), on, "dev.self"))
    }

    @Test
    fun `a plain notification keeps its fullest text`() {
        val entry = entriesFor(note().copy(bigText = "The whole long text", lines = listOf("Line one", "Line two"))).single()
        assertEquals("The whole long text\nLine one\nLine two", entry.text)
        assertNull(entry.sender)
        val inbox = entriesFor(note(text = "2 new emails").copy(lines = listOf("Ana: Lunch?", "Bo: Report"))).single()
        assertEquals("2 new emails\nAna: Lunch?\nBo: Report", inbox.text)
    }

    @Test
    fun `the rest of the notification comes along`() {
        val full = note(title = "Download", text = "photo.jpg").copy(
            summaryText = "From Files",
            infoText = "2 MB",
            replies = listOf("Thanks!"),
            actions = listOf("Open", "Share", "Open"),
            progress = 140
        )
        val entry = entriesFor(full).single()
        assertEquals("From Files\n2 MB", entry.details)
        assertEquals(listOf("Thanks!"), entry.replies)
        assertEquals(listOf("Open", "Share"), entry.actions)
        assertEquals(100, entry.progress)
        // A progress update alone is the same entry.
        assertEquals(entry.fingerprint, entriesFor(full.copy(progress = 60)).single().fingerprint)
        assertTrue(shouldRecord(note(title = null, text = null).copy(picture = PictureSource { true }), on, "dev.self"))
    }

    @Test
    fun `a chat becomes one entry per message`() {
        val chat = note(title = "Ana").copy(
            messages = listOf(CapturedMessage("Ana", "Hi", 10), CapturedMessage(null, "Hey!", 20), CapturedMessage("Ana", " ", 30))
        )
        val entries = entriesFor(chat)
        assertEquals(listOf("Hi", "Hey!"), entries.map { it.text })
        assertEquals(listOf("Ana", null), entries.map { it.sender })
        assertEquals(listOf(10L, 20L), entries.map { it.postedAt })
        assertTrue(entries.all { it.conversation == "k" && it.title == "Ana" })
        // The app reposting the chat with one more message gives the same rows for the old ones.
        val again = entriesFor(chat.copy(postedAt = 5_000, messages = chat.messages + CapturedMessage("Ana", "Lunch?", 40)))
        assertEquals(entries.map { it.fingerprint to it.postedAt }, again.take(2).map { it.fingerprint to it.postedAt })
        val group = note(title = "Team: Ana").copy(conversationTitle = "Team", messages = listOf(CapturedMessage("Bo", "Done", 50)))
        assertEquals("Team", entriesFor(group).single().conversation)
        // A new post titled after its latest sender is still the same chat, so old messages match.
        val reposted = group.copy(title = "Team: Bo")
        assertEquals(entriesFor(group).single().fingerprint, entriesFor(reposted).single().fingerprint)
        assertEquals("chat-1", entriesFor(note(title = "Ana").copy(conversationId = "chat-1", messages = chat.messages)).first().conversation)
    }

    @Test
    fun `fingerprints and retention`() {
        assertEquals(fingerprint("a", "b"), fingerprint("a", "b"))
        assertNotEquals(fingerprint("ab", "c"), fingerprint("a", "bc"))
        assertEquals(1_000L * DAY_MS - 30 * DAY_MS, retentionCutoff(1_000L * DAY_MS, Retention.MONTH))
    }
}
