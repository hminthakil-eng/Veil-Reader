package com.veilreader.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryShelfNavigationTest {
    @Test fun shelfRequestSurvivesRecreationAndIsConsumedOnlyOnce() {
        val handle = SavedStateHandle()
        val first = VeilAppViewModel(handle)
        first.openSettings()
        first.openLibraryShelf(LibraryShelfDestination.FAVORITES)
        val restored = VeilAppViewModel(handle)
        assertEquals(VeilTab.LIBRARY, restored.route.value.selectedTab)
        assertFalse(restored.route.value.showSettings)
        assertEquals(LibraryShelfDestination.FAVORITES, restored.route.value.libraryShelfRequest)
        restored.consumeLibraryShelfRequest(LibraryShelfDestination.FAVORITES)
        assertNull(restored.route.value.libraryShelfRequest)
        assertNull(VeilAppViewModel(handle).route.value.libraryShelfRequest)
    }

    @Test fun staleAcknowledgementCannotEraseADifferentShelfRequest() {
        val model = VeilAppViewModel(SavedStateHandle())
        model.openLibraryShelf(LibraryShelfDestination.READING)
        model.openLibraryShelf(LibraryShelfDestination.COMPLETED)
        model.consumeLibraryShelfRequest(LibraryShelfDestination.READING)
        assertEquals(LibraryShelfDestination.COMPLETED, model.route.value.libraryShelfRequest)
        model.selectTab(VeilTab.PROFILE)
        assertNull(model.route.value.libraryShelfRequest)
        model.selectTab(VeilTab.LIBRARY)
        assertNull(model.route.value.libraryShelfRequest)
    }

    @Test fun overlaysAndReaderDoNotRestoreAnUnconsumedShelfIntent() {
        listOf<(VeilAppViewModel) -> Unit>(
            { it.openSettings() }, { it.openArchive() }, { it.openChamber("mirror") },
            { it.requestBook("book") }
        ).forEach { leave ->
            val handle = SavedStateHandle()
            val model = VeilAppViewModel(handle)
            model.openLibraryShelf(LibraryShelfDestination.READING)
            leave(model)
            assertNull(model.route.value.libraryShelfRequest)
            assertNull(VeilAppViewModel(handle).route.value.libraryShelfRequest)
        }
        val invalid = VeilAppViewModel(SavedStateHandle(mapOf(
            "veil.route.tab" to "LIBRARY", "veil.route.library_shelf" to "REMOVED_SHELF"
        )))
        assertNull(invalid.route.value.libraryShelfRequest)
    }
}
