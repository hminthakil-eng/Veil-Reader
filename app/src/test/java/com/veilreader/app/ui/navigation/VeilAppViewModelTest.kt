package com.veilreader.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilAppViewModelTest {
    @Test
    fun activeReaderRoute_reusesSavedHandleAcrossViewModelRecreation_thenClosesToLibrary() {
        val handle = SavedStateHandle()
        val first = VeilAppViewModel(handle)

        first.selectTab(VeilTab.CASTLE)
        first.openArchive()
        first.requestBook("book-42", "{\"href\":\"chapter.xhtml\"}")

        assertEquals("book-42", first.route.value.activeBookId)
        assertEquals("{\"href\":\"chapter.xhtml\"}", first.route.value.locatorOverrideJson)
        assertFalse(first.route.value.showArchive)

        val recreated = VeilAppViewModel(handle)
        assertEquals(VeilTab.CASTLE, recreated.route.value.selectedTab)
        assertEquals("book-42", recreated.route.value.activeBookId)
        assertEquals("{\"href\":\"chapter.xhtml\"}", recreated.route.value.locatorOverrideJson)

        recreated.readerOpened("book-42")
        assertNull(recreated.route.value.locatorOverrideJson)
        assertEquals("book-42", recreated.route.value.activeBookId)

        recreated.closeReader()
        assertEquals(VeilTab.LIBRARY, recreated.route.value.selectedTab)
        assertNull(recreated.route.value.activeBookId)
        assertNull(recreated.route.value.activeChamber)
        assertFalse(recreated.route.value.showArchive)
    }

    @Test
    fun settingsRoute_isRestorableAndExclusiveWithArchiveAndReader() {
        val handle = SavedStateHandle()
        val first = VeilAppViewModel(handle)

        first.selectTab(VeilTab.PROFILE)
        first.openSettings()

        assertTrue(first.route.value.showSettings)
        assertFalse(first.route.value.showArchive)
        assertNull(first.route.value.activeBookId)

        val recreated = VeilAppViewModel(handle)
        assertEquals(VeilTab.PROFILE, recreated.route.value.selectedTab)
        assertTrue(recreated.route.value.showSettings)

        recreated.openArchive()
        assertFalse(recreated.route.value.showSettings)
        assertTrue(recreated.route.value.showArchive)

        recreated.openSettings()
        recreated.requestBook("book-settings")
        assertFalse(recreated.route.value.showSettings)
        assertEquals("book-settings", recreated.route.value.activeBookId)

        recreated.closeReader()
        assertFalse(recreated.route.value.showSettings)
        assertEquals(VeilTab.LIBRARY, recreated.route.value.selectedTab)
    }

    @Test
    fun tabsAndOverlays_cancelPendingReader_andNormalizeInvalidSavedState() {
        val handle = SavedStateHandle(
            mapOf(
                "veil.route.tab" to "NOT_A_TAB",
                "veil.route.archive" to true,
                "veil.route.chamber" to "unknown-room",
                "veil.route.book" to "   ",
                "veil.route.locator" to "orphan-locator"
            )
        )
        val model = VeilAppViewModel(handle)

        assertEquals(VeilTab.READING, model.route.value.selectedTab)
        assertTrue(model.route.value.showArchive)
        assertNull(model.route.value.activeChamber)
        assertNull(model.route.value.activeBookId)
        assertNull(model.route.value.locatorOverrideJson)

        model.requestBook("book-a", "locator")
        model.selectTab(VeilTab.PROFILE)
        assertEquals(VeilTab.PROFILE, model.route.value.selectedTab)
        assertNull(model.route.value.activeBookId)
        assertNull(model.route.value.locatorOverrideJson)

        model.openChamber("unknown-room")
        assertNull(model.route.value.activeChamber)
        model.openChamber("treasury")
        assertEquals("treasury", model.route.value.activeChamber)
        assertFalse(model.route.value.showArchive)
    }
}
