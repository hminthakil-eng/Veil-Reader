package com.veilreader.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilAppViewModelTest {
    @Test
    fun settingsRestoresItsSectionAndBackReturnsToTheOriginalTab() {
        val handle = SavedStateHandle()
        val model = VeilAppViewModel(handle)
        model.selectTab(VeilTab.PROFILE)
        model.openSettings("reading")
        val recreated = VeilAppViewModel(handle)
        assertEquals("reading", recreated.route.value.settingsSection)
        recreated.closeSettings()
        assertNull(recreated.route.value.settingsSection)
        assertEquals(VeilTab.PROFILE, recreated.route.value.selectedTab)
        recreated.openSettings("invalid")
        assertEquals("general", recreated.route.value.settingsSection)
        recreated.openArchive()
        assertNull(recreated.route.value.settingsSection)
        assertTrue(recreated.route.value.showArchive)
    }

    @Test
    fun quietModeRemovesWorldRoutesAndKeepsActiveReaderAndSettings() {
        val model = VeilAppViewModel(SavedStateHandle())
        model.selectTab(VeilTab.CASTLE)
        model.openChamber("treasury")
        model.applyGameVisibility(false)
        assertEquals(VeilTab.READING, model.route.value.selectedTab)
        assertNull(model.route.value.activeChamber)
        assertEquals(listOf(VeilTab.READING, VeilTab.LIBRARY, VeilTab.PROFILE), visibleTabs(false))
        model.requestBook("book", "locator")
        model.applyGameVisibility(false)
        assertEquals("book", model.route.value.activeBookId)
        assertEquals("locator", model.route.value.locatorOverrideJson)
        model.closeReader()
        model.openSettings("data")
        model.applyGameVisibility(false)
        assertEquals("data", model.route.value.settingsSection)
        model.applyGameVisibility(true)
        assertEquals(5, visibleTabs(true).size)
    }

    @Test
    fun activeReaderRoute_survivesViewModelRecreation_thenClosesToLibrary() {
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
