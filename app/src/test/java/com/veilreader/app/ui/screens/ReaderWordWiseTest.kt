package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderWordWiseTest {
    @Test fun englishWordIsMatchedCaseInsensitively() {
        assertEquals("crimson", ReaderWordWise.find("  “Crimson!”  ")?.lemma)
    }

    @Test fun unknownWordDoesNotFabricateAnExplanation() {
        assertNull(ReaderWordWise.find("supercalifragilistic"))
    }

    @Test fun passagesAndNonEnglishTextHaveNoAutomaticLookup() {
        assertNull(ReaderWordWise.find("Klein Moretti"))
        assertNull(ReaderWordWise.find("قرمز"))
    }

    @Test fun doesNotUseUntrustedRegexFromSelection() {
        assertNull(ReaderWordWise.find("crimson.*"))
        assertEquals("parchment", ReaderWordWise.find("(parchment)")?.lemma)
    }
}
