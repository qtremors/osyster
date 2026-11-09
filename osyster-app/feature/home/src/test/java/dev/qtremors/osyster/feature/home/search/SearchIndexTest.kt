package dev.qtremors.osyster.feature.home.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIndexTest {
    private val battery = SearchEntry("page:battery", SearchKind.PAGE, "Battery", "Health, charge, temperature", SearchTarget.Page("battery"))
    private val stayAwake = SearchEntry("tool:stay_awake", SearchKind.TOOL, "Stay awake", "Keep the screen on", SearchTarget.Tool("stay_awake"))
    private val batteryFact = SearchEntry("fact:battery:tech", SearchKind.FACT, "Battery technology", "Li-ion", SearchTarget.Page("battery"))
    private val android = SearchEntry("fact:system:release", SearchKind.FACT, "Android version", "16", SearchTarget.Page("system"))
    private val theme = SearchEntry("setting:theme", SearchKind.SETTING, "Thème", "Light, dark, or system", SearchTarget.Setting(SettingsPlace.SETTINGS))
    private val all = listOf(batteryFact, stayAwake, battery, android, theme)

    @Test
    fun blankQueryFindsNothing() {
        assertTrue(searchEntries("  ", all).isEmpty())
    }

    @Test
    fun pageRanksAheadOfFactWithSameTitlePrefix() {
        assertEquals(listOf(battery, batteryFact), searchEntries("batt", all))
    }

    @Test
    fun everyWordMustMatchTitleOrDetail() {
        assertEquals(listOf(android), searchEntries("android 16", all))
        assertTrue(searchEntries("android 15", all).isEmpty())
    }

    @Test
    fun detailOnlyMatchesRankLast() {
        val results = searchEntries("screen", all)
        assertEquals(listOf(stayAwake), results)
    }

    @Test
    fun accentsAndCaseAreIgnored() {
        assertEquals(listOf(theme), searchEntries("THEME", all))
    }

    @Test
    fun foldCollapsesPunctuationButKeepsDots() {
        assertEquals("li ion 1.2 ghz", fold("Li-ion, 1.2  GHz!"))
    }

    @Test
    fun limitCapsResults() {
        val many = (1..100).map { SearchEntry("k$it", SearchKind.FACT, "Item $it", "", SearchTarget.Page("system")) }
        assertEquals(10, searchEntries("item", many, limit = 10).size)
    }
}
