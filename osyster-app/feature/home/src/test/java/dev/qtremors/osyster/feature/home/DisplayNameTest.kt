package dev.qtremors.osyster.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameTest {
    @Test
    fun googleModelsDropTheBrand() {
        assertEquals("Pixel 7", HomeViewModel.displayName("Google", "Pixel 7"))
    }

    @Test
    fun brandIsCapitalizedAndPrefixed() {
        assertEquals("Samsung SM-S918B", HomeViewModel.displayName("samsung", "SM-S918B"))
    }

    @Test
    fun modelThatAlreadyNamesTheBrandIsNotDoubled() {
        assertEquals("OnePlus 12", HomeViewModel.displayName("OnePlus", "OnePlus 12"))
    }

    @Test
    fun blankBrandShowsModelOnly() {
        assertEquals("X1", HomeViewModel.displayName("  ", "X1"))
    }
}
