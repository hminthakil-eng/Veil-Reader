package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRetrievalPolicyTest {
    @Test
    fun nonDefaultSort_marksRetrievalModifiedAndCountsAsSecondaryFilter() {
        assertTrue(libraryRetrievalModified("", "All", "", "", "Archive Depth"))
        assertEquals(1, librarySecondaryFilterCount("", "", "Archive Depth"))
    }

    @Test
    fun defaultRetrievalState_isNotModified() {
        assertFalse(libraryRetrievalModified("", "All", "", "", "Recent"))
    }
}
