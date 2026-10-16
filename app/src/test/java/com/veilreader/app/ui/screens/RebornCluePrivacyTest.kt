package com.veilreader.app.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A sealed quote must not restore itself as revealed after activity recreation
 * or after a background transition. This is UI privacy, not encryption.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RebornCluePrivacyTest {
    @get:Rule val compose = createComposeRule()
    private val book = Book("private-book", "Private Archive", "Author", sourceUri = "file:///private.epub")
    private val clue = Highlight("private-highlight", book.id, "HIDDEN USER QUOTE",
        """{"href":"chapter.xhtml","type":"application/xhtml+xml"}""", "HIDDEN USER NOTE")
    private val groups = listOf(InvestigationDeskGroup(book, listOf(clue)))

    private fun openAndReveal() {
        compose.onNodeWithTag("reborn-investigation-scroll").performScrollToIndex(3)
        compose.onNodeWithText("Reveal").performScrollTo().performClick()
        compose.onNodeWithText(clue.quote).assertExists()
        compose.onNodeWithText(clue.note).assertExists()
    }

    private fun assertSealed() {
        compose.onNodeWithText(clue.quote, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(clue.note, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription(clue.quote, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Reveal").assertExists()
    }

    @Test fun revealDoesNotSurviveSavedInstanceStateRestore() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            VeilTheme(AppThemeMode.DARK) {
                RebornInvestigationDesk(groups, { _, _ -> }, {})
            }
        }
        assertSealed()
        openAndReveal()
        restoration.emulateSavedInstanceStateRestore()
        assertSealed()
    }

    @Test fun backgroundEventSealsPreviouslyRevealedPrivateText() {
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }
        compose.runOnIdle { owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                VeilTheme(AppThemeMode.DARK) {
                    RebornInvestigationDesk(groups, { _, _ -> }, {})
                }
            }
        }
        openAndReveal()
        compose.runOnIdle { owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }
        assertSealed()
    }
}
