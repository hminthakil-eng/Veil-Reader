package com.veilreader.app.ui.navigation

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VeilAppViewModelProcessDeathTest {
    @Test
    fun activeReaderRoute_survivesSavedStateRegistryProcessDeathSimulation_thenClosesToLibrary() {
        viewModelScenario {
            VeilAppViewModel(createSavedStateHandle())
        }.use { scenario ->
            val first = scenario.viewModel

            first.selectTab(VeilTab.CASTLE)
            first.openArchive()
            first.requestBook("book-42", "{\"href\":\"chapter.xhtml\"}")

            assertEquals("book-42", first.route.value.activeBookId)
            val readerSessionId = requireNotNull(first.route.value.readerSessionInstanceId)
            assertEquals("{\"href\":\"chapter.xhtml\"}", first.route.value.locatorOverrideJson)
            assertFalse(first.route.value.showArchive)

            scenario.recreate()
            val recreated = scenario.viewModel

            assertEquals(VeilTab.CASTLE, recreated.route.value.selectedTab)
            assertEquals("book-42", recreated.route.value.activeBookId)
            assertEquals(readerSessionId, recreated.route.value.readerSessionInstanceId)
            assertEquals("{\"href\":\"chapter.xhtml\"}", recreated.route.value.locatorOverrideJson)

            recreated.readerOpened("book-42", readerSessionId)
            assertNull(recreated.route.value.locatorOverrideJson)
            assertEquals("book-42", recreated.route.value.activeBookId)

            recreated.closeReader(readerSessionId)
            assertEquals(VeilTab.LIBRARY, recreated.route.value.selectedTab)
            assertNull(recreated.route.value.activeBookId)
            assertNull(recreated.route.value.readerSessionInstanceId)
            assertNull(recreated.route.value.activeChamber)
            assertFalse(recreated.route.value.showArchive)
        }
    }
}
