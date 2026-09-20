package dev.qtremors.osyster

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Android 11 and Android 12+ backup rules must exclude exactly the same paths. */
class BackupRulesTest {
    private val excludes = Regex("""<exclude\s+domain="([^"]+)"\s+path="([^"]+)"\s*/>""")

    private fun xml(name: String): File =
        generateSequence(File(System.getProperty("user.dir") ?: ".").absoluteFile) { it.parentFile }
            .map { File(it, "src/main/res/xml/$name") }
            .first { it.exists() }

    private fun pathsIn(text: String): Set<Pair<String, String>> =
        excludes.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toSet()

    @Test
    fun legacyAndModernRulesMatch() {
        val legacy = pathsIn(xml("backup_rules.xml").readText())
        val modern = xml("data_extraction_rules.xml").readText()
        val cloud = pathsIn(modern.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>"))
        val transfer = pathsIn(modern.substringAfter("<device-transfer>").substringBefore("</device-transfer>"))
        assertEquals(legacy, cloud)
        assertEquals(legacy, transfer)
    }

    @Test
    fun preferenceStoresAreExcluded() {
        assertTrue("datastore" in pathsIn(xml("backup_rules.xml").readText()).map { it.second })
    }
}
