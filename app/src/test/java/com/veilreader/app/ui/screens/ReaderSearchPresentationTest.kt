package com.veilreader.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSearchPresentationTest {
    @Test
    fun typingOrEditingAQueryNeverClaimsACompletedEmptySearch() {
        assertFalse(empty("Klein", null))
        assertFalse(empty("Klein", "Amon"))
        assertFalse(empty("K", "K"))
    }

    @Test
    fun onlyTheCompletedCurrentQueryMayShowNoMatches() {
        assertTrue(empty("  Klein  ", "Klein"))
        assertFalse(empty("Klein", "Klein", searching = true))
        assertFalse(empty("Klein", "Klein", failed = true))
        assertFalse(empty("Klein", "Klein", hasResults = true))
    }

    private fun empty(query: String, submitted: String?, searching: Boolean = false,
        failed: Boolean = false, hasResults: Boolean = false) =
        shouldShowReaderSearchNoMatches(query, submitted, searching, failed, hasResults)
}
