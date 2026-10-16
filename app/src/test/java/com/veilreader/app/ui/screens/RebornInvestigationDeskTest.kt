package com.veilreader.app.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RebornInvestigationDeskTest {
    @get:Rule val compose = createComposeRule()
    private val book = Book("book", "Archive", "Author", sourceUri = "file:///book")
    private val saved = Highlight("mark", book.id, "Secret quote", " {\"href\":\"chapter.xhtml\",\"type\":\"application/xhtml+xml\"} ", "Secret note")

    @Test fun revealHideReturnAndBackAtLargeFontInRtl() {
        var opened: Pair<Book, String>? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl) {
                VeilTheme(AppThemeMode.DARK) {
                    RebornThresholdScreen(listOf(book), {}, {}, {}, highlights = listOf(saved),
                        onOpenPassage = { b, locator -> opened = b to locator })
                }
            }
        }
        compose.onNodeWithText("Investigation Desk").performScrollTo().performClick()
        compose.onNodeWithText(saved.quote, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(saved.note, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription(saved.quote, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Reveal").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText(saved.quote).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(saved.note).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide").performScrollTo().performClick()
        compose.onNodeWithText(saved.quote, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(saved.note, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Return to passage").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(book to saved.locatorJson, opened) }
        compose.onNodeWithText("Back to Home").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("reborn-threshold-scroll").assertExists()
        compose.onNodeWithText(saved.quote, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun invalidRecordsDoNotOfferDeskNavigation() {
        compose.setContent {
            VeilTheme(AppThemeMode.LIGHT) {
                RebornThresholdScreen(listOf(book), {}, {}, {},
                    highlights = listOf(saved.copy(locatorJson = "invalid")))
            }
        }
        compose.onNodeWithText("Investigation Desk").assertDoesNotExist()
    }

    @Test fun emptyDeskHasExplanationAndBack() {
        var backs = 0
        compose.setContent {
            VeilTheme(AppThemeMode.LIGHT) {
                RebornInvestigationDesk(emptyList(), { _, _ -> }, { backs++ })
            }
        }
        compose.onNodeWithText("No saved passages available").assertIsDisplayed()
        compose.onNodeWithText("Reveal").assertDoesNotExist()
        compose.onNodeWithText("Back to Home").performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }
}
