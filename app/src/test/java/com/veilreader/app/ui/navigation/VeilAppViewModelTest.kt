package com.veilreader.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
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
        val readerSessionId = requireNotNull(first.route.value.readerSessionInstanceId)
        assertEquals("{\"href\":\"chapter.xhtml\"}", first.route.value.locatorOverrideJson)
        assertEquals("{\"href\":\"chapter.xhtml\"}", first.route.value.readerLocatorCheckpointJson)
        assertFalse(first.route.value.showArchive)

        val recreated = VeilAppViewModel(handle)
        assertEquals(VeilTab.CASTLE, recreated.route.value.selectedTab)
        assertEquals("book-42", recreated.route.value.activeBookId)
        assertEquals(readerSessionId, recreated.route.value.readerSessionInstanceId)
        assertEquals("{\"href\":\"chapter.xhtml\"}", recreated.route.value.locatorOverrideJson)
        assertEquals(
            "{\"href\":\"chapter.xhtml\"}",
            recreated.route.value.readerLocatorCheckpointJson
        )

        recreated.readerOpened("book-42", readerSessionId)
        assertNull(recreated.route.value.locatorOverrideJson)
        assertNull(recreated.route.value.readerLocatorCheckpointJson)
        assertEquals("book-42", recreated.route.value.activeBookId)

        recreated.closeReader(readerSessionId)
        assertEquals(VeilTab.LIBRARY, recreated.route.value.selectedTab)
        assertNull(recreated.route.value.activeBookId)
        assertNull(recreated.route.value.readerSessionInstanceId)
        assertNull(recreated.route.value.activeChamber)
        assertFalse(recreated.route.value.showArchive)
    }

    @Test
    fun readerSessionIdentity_isStableForRecreation_butFreshForANewOpenRequest() {
        val handle = SavedStateHandle()
        val first = VeilAppViewModel(handle)

        first.requestBook("book-session")
        val firstSessionId = requireNotNull(first.route.value.readerSessionInstanceId)

        val recreated = VeilAppViewModel(handle)
        assertEquals(firstSessionId, recreated.route.value.readerSessionInstanceId)

        recreated.requestBook("book-session")
        val secondSessionId = requireNotNull(recreated.route.value.readerSessionInstanceId)
        assertNotEquals(firstSessionId, secondSessionId)
    }

    @Test
    fun committedCheckpoint_survivesProcessRecreation_andStaleAckCannotClearNewerState() {
        val beforeKill = VeilAppViewModel(SavedStateHandle())
        beforeKill.requestBook("book-process")
        val processSessionId = requireNotNull(beforeKill.route.value.readerSessionInstanceId)
        beforeKill.checkpointReaderLocator("book-process", processSessionId, "locator-1")

        val restoredHandle = SavedStateHandle(
            mapOf(
                "veil.route.tab" to VeilTab.READING.name,
                "veil.route.book" to "book-process",
                "veil.route.reader_session" to processSessionId,
                "veil.route.reader_checkpoint" to "locator-1"
            )
        )
        val restored = VeilAppViewModel(restoredHandle)

        assertEquals("book-process", restored.route.value.activeBookId)
        assertEquals(processSessionId, restored.route.value.readerSessionInstanceId)
        assertEquals("locator-1", restored.route.value.readerLocatorCheckpointJson)

        restored.checkpointReaderLocator("book-process", processSessionId, "locator-2")
        restored.readerCheckpointPersisted("book-process", processSessionId, "locator-1")
        assertEquals("locator-2", restored.route.value.readerLocatorCheckpointJson)

        restored.readerCheckpointPersisted("book-process", processSessionId, "locator-2")
        assertNull(restored.route.value.readerLocatorCheckpointJson)
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
        val settingsSessionId = requireNotNull(recreated.route.value.readerSessionInstanceId)
        assertFalse(recreated.route.value.showSettings)
        assertEquals("book-settings", recreated.route.value.activeBookId)

        recreated.closeReader(settingsSessionId)
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
                "veil.route.locator" to "orphan-locator",
                "veil.route.reader_checkpoint" to "orphan-checkpoint"
            )
        )
        val model = VeilAppViewModel(handle)

        assertEquals(VeilTab.READING, model.route.value.selectedTab)
        assertTrue(model.route.value.showArchive)
        assertNull(model.route.value.activeChamber)
        assertNull(model.route.value.activeBookId)
        assertNull(model.route.value.locatorOverrideJson)
        assertNull(model.route.value.readerLocatorCheckpointJson)

        model.requestBook("book-a", "locator")
        model.selectTab(VeilTab.PROFILE)
        assertEquals(VeilTab.PROFILE, model.route.value.selectedTab)
        assertNull(model.route.value.activeBookId)
        assertNull(model.route.value.locatorOverrideJson)
        assertNull(model.route.value.readerLocatorCheckpointJson)

        model.openChamber("unknown-room")
        assertNull(model.route.value.activeChamber)
        model.openChamber("treasury")
        assertEquals("treasury", model.route.value.activeChamber)
        assertFalse(model.route.value.showArchive)
    }
    @Test
    fun explicitFlushAck_preservesNewerReaderCheckpoint() {
        val handle = SavedStateHandle()
        val model = VeilAppViewModel(handle)

        model.requestBook("book-race", "locator-explicit")
        val sessionId = requireNotNull(model.route.value.readerSessionInstanceId)
        model.checkpointReaderLocator("book-race", sessionId, "locator-newer")

        model.readerOpened("book-race", sessionId)

        assertNull(model.route.value.locatorOverrideJson)
        assertEquals("locator-newer", model.route.value.readerLocatorCheckpointJson)

        val recreated = VeilAppViewModel(handle)
        assertEquals("book-race", recreated.route.value.activeBookId)
        assertNull(recreated.route.value.locatorOverrideJson)
        assertEquals("locator-newer", recreated.route.value.readerLocatorCheckpointJson)
    }


    @Test
    fun checkpointUpdates_ignoreBlankAndWrongBook() {
        val model = VeilAppViewModel(SavedStateHandle())
        model.requestBook("book-a")
        val sessionId = requireNotNull(model.route.value.readerSessionInstanceId)
        model.checkpointReaderLocator("book-a", sessionId, "locator-a")

        model.checkpointReaderLocator("book-a", sessionId, "   ")
        model.checkpointReaderLocator("book-b", sessionId, "locator-b")

        assertEquals("locator-a", model.route.value.readerLocatorCheckpointJson)
    }



    @Test
    fun closeReader_clearsPendingCheckpointFromSavedState() {
        val handle = SavedStateHandle()
        val model = VeilAppViewModel(handle)

        model.requestBook("book-close")
        val sessionId = requireNotNull(model.route.value.readerSessionInstanceId)
        model.checkpointReaderLocator("book-close", sessionId, "locator-pending")
        model.closeReader(sessionId)

        assertNull(model.route.value.activeBookId)
        assertNull(model.route.value.locatorOverrideJson)
        assertNull(model.route.value.readerLocatorCheckpointJson)

        val recreated = VeilAppViewModel(handle)
        assertNull(recreated.route.value.activeBookId)
        assertNull(recreated.route.value.readerLocatorCheckpointJson)
        assertEquals(VeilTab.LIBRARY, recreated.route.value.selectedTab)
    }



    @Test
    fun currentBookOpenFailure_clearsOwnedRequestAndSavedRecoveryState() {
        val handle = SavedStateHandle()
        val model = VeilAppViewModel(handle)

        model.requestBook("missing-book", "explicit-locator")
        val sessionId = requireNotNull(model.route.value.readerSessionInstanceId)
        model.checkpointReaderLocator("missing-book", sessionId, "checkpoint-locator")

        model.bookOpenFailed("missing-book", sessionId)

        assertNull(model.route.value.activeBookId)
        assertNull(model.route.value.readerSessionInstanceId)
        assertNull(model.route.value.locatorOverrideJson)
        assertNull(model.route.value.readerLocatorCheckpointJson)

        val recreated = VeilAppViewModel(handle)
        assertNull(recreated.route.value.activeBookId)
        assertNull(recreated.route.value.readerSessionInstanceId)
        assertNull(recreated.route.value.locatorOverrideJson)
        assertNull(recreated.route.value.readerLocatorCheckpointJson)
    }


    @Test
    fun staleSameBookCallbacks_cannotMutateFreshOpenRequest() {
        val model = VeilAppViewModel(SavedStateHandle())

        model.requestBook("book-race", "old-explicit")
        val oldSessionId = requireNotNull(model.route.value.readerSessionInstanceId)
        model.checkpointReaderLocator("book-race", oldSessionId, "old-checkpoint")

        model.requestBook("book-race", "fresh-explicit")
        val freshSessionId = requireNotNull(model.route.value.readerSessionInstanceId)
        assertNotEquals(oldSessionId, freshSessionId)

        model.checkpointReaderLocator("book-race", oldSessionId, "stale-checkpoint")
        model.readerCheckpointPersisted("book-race", oldSessionId, "fresh-explicit")
        model.readerOpened("book-race", oldSessionId)
        model.bookOpenFailed("book-race", oldSessionId)
        model.closeReader(oldSessionId)

        assertEquals("book-race", model.route.value.activeBookId)
        assertEquals(freshSessionId, model.route.value.readerSessionInstanceId)
        assertEquals("fresh-explicit", model.route.value.locatorOverrideJson)
        assertEquals("fresh-explicit", model.route.value.readerLocatorCheckpointJson)

        model.checkpointReaderLocator("book-race", freshSessionId, "fresh-checkpoint")
        assertEquals("fresh-checkpoint", model.route.value.readerLocatorCheckpointJson)
    }


    @Test
    fun canonicalChambers_areRestorableAndExclusive() {
        listOf(
            "observatory",
            "treasury",
            "sanctum",
            "mirror",
            "manga"
        ).forEachIndexed { index, chamber ->
            val handle = SavedStateHandle()
            val model = VeilAppViewModel(handle)
            if (index % 2 == 0) {
                model.openArchive()
            } else {
                model.openSettings()
            }

            model.openChamber(chamber)

            assertEquals(chamber, model.route.value.activeChamber)
            assertFalse(model.route.value.showArchive)
            assertFalse(model.route.value.showSettings)
            assertEquals(
                chamber,
                VeilAppViewModel(handle).route.value.activeChamber
            )
        }
    }

}
