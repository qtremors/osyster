package dev.qtremors.osyster.feature.device

import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoRow
import dev.qtremors.osyster.core.device.domain.InfoSection
import dev.qtremors.osyster.core.device.domain.PermissionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class InventorySearchTest {
    private fun row(key: String, label: String, value: String, advanced: Boolean = false) =
        InfoRow(key, label, Availability.Available(value, "test", 0L), advanced = advanced)

    private val decoders = InfoSection(
        key = "video_decoders",
        title = "Video decoders (3)",
        rows = listOf(
            row("a", "video/avc", "c2.qti.avc.decoder · hardware"),
            row("b", "video/hevc", "c2.qti.hevc.decoder · hardware"),
            row("c", "video/av01", "c2.android.av1.decoder · software", advanced = true)
        )
    )
    private val encoders = InfoSection(
        key = "audio_encoders",
        title = "Audio encoders (1)",
        rows = listOf(row("d", "audio/opus", "c2.android.opus.encoder · software")),
        permission = PermissionKind.LOCATION
    )

    @Test
    fun blankQueryChangesNothing() {
        val sections = listOf(decoders, encoders)
        assertSame(sections, filterSections(sections, "   "))
    }

    @Test
    fun matchesLabelsAndValuesIgnoringCase() {
        val result = filterSections(listOf(decoders, encoders), "HEVC")
        assertEquals(1, result.size)
        assertEquals(listOf("b"), result.single().rows.map(InfoRow::key))
    }

    @Test
    fun everyWordMustMatchAndFoldedRowsUnfold() {
        val result = filterSections(listOf(decoders, encoders), "av1 software")
        val rows = result.single().rows
        assertEquals(listOf("c"), rows.map(InfoRow::key))
        assertFalse(rows.single().advanced)
    }

    @Test
    fun sectionTitleCountsAsContextAndPermissionPromptsHide() {
        val result = filterSections(listOf(decoders, encoders), "audio opus")
        assertEquals("audio_encoders", result.single().key)
        assertTrue(result.single().permission == null)
    }

    @Test
    fun noMatchesLeavesNoSections() {
        assertTrue(filterSections(listOf(decoders, encoders), "vp9").isEmpty())
    }
}
