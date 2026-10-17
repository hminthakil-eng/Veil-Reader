package com.veilreader.app.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
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
class RebornThresholdScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun continueOpensOriginalBookAtLargeTextInRtl() {
        val book = Book("hero", "A long title for a quiet evening", "Author", progress = .4f,
            sourceUri = "file:///private/book.epub", lastOpenedAtEpochMs = 100L)
        var opened: Book? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                VeilTheme(AppThemeMode.DARK) {
                    RebornThresholdScreen(listOf(book), { opened = it }, {}, {})
                }
            }
        }
        compose.onNodeWithText("Continue Reading").performScrollTo()
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(book, opened) }
    }

    @Test fun emptyLibraryRoutesToExistingLibrary() {
        var libraryOpens = 0
        compose.setContent {
            VeilTheme(AppThemeMode.LIGHT) {
                RebornThresholdScreen(emptyList(), {}, { libraryOpens++ }, {})
            }
        }
        compose.onNodeWithText("Open Library").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, libraryOpens) }
        compose.onNodeWithText("Continue Reading").assertDoesNotExist()
        compose.onNodeWithText("Visit Castle").assertDoesNotExist()
        compose.onNodeWithText("Latest Clue").assertDoesNotExist()
        compose.onNodeWithText("Trace in the Archive").assertDoesNotExist()
    }

    @Test fun bookUpdatesRefreshActionAndCallbackWithoutKeepingStaleData() {
        val book = Book("volume", "A new volume", "Author", sourceUri = "file:///volume")
        val books = mutableStateOf(listOf(book))
        var opened: Book? = null
        compose.setContent {
            VeilTheme(AppThemeMode.LIGHT) {
                RebornThresholdScreen(books.value, { opened = it }, {}, {})
            }
        }
        compose.onNodeWithText("Start Reading").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(book, opened) }
        val finishedBook = book.copy(finished = true, progress = 1f)
        compose.runOnIdle { books.value = listOf(finishedBook) }
        compose.onNodeWithText("Read Again").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(finishedBook, opened) }
        compose.onNodeWithText("Start Reading").assertDoesNotExist()
    }

    @Test fun recentShelfAndOptionalCastleUseIndependentCallbacks() {
        val hero = Book("hero", "Current volume", "Author", sourceUri = "file:///hero", lastOpenedAtEpochMs = 200L)
        val recent = Book("recent", "Earlier volume", "Author", sourceUri = "file:///recent", lastOpenedAtEpochMs = 100L)
        var opened: Book? = null
        var castleOpens = 0
        compose.setContent {
            VeilTheme(AppThemeMode.DARK) {
                RebornThresholdScreen(listOf(hero, recent), { opened = it }, {}, { castleOpens++ })
            }
        }
        compose.onNodeWithText("Earlier volume").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(recent, opened); assertEquals(0, castleOpens) }
        compose.onNodeWithTag("reborn-threshold-scroll").performScrollToIndex(4)
        compose.onNodeWithText("Show Castle note").performScrollTo().performClick()
        compose.onNodeWithText("Visit Castle").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, castleOpens) }
    }
    @Test fun latestClueOpensExactPassageAtLargeTextInRtlAndDisappearsWhenBookRemoved() {
        val book = Book("clue-book", "کتاب / Archive", "Author", sourceUri = "file:///clue")
        val saved = Highlight("clue", book.id, "متن ذخیره‌شده / ".repeat(80), " {original locator} ")
        val books = mutableStateOf(listOf(book))
        var opened: Pair<Book, String>? = null
        var bookOpens = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl) {
                VeilTheme(AppThemeMode.DARK) {
                    RebornThresholdScreen(books.value, { bookOpens++ }, {}, {},
                        highlights = listOf(saved), onOpenPassage = { b, locator -> opened = b to locator })
                }
            }
        }
        compose.onNodeWithText(saved.quote).assertDoesNotExist()
        compose.onNodeWithText("Trace in the Archive").performScrollTo()
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(book to saved.locatorJson, opened); assertEquals(0, bookOpens) }
        compose.runOnIdle { books.value = emptyList() }
        compose.onNodeWithText("Latest Clue").assertDoesNotExist()
        compose.onNodeWithText("Trace in the Archive").assertDoesNotExist()
    }

    @Test fun invalidSavedPassagesOmitEntireClueCard() {
        val book = Book("book", "Volume", "Author", sourceUri = "file:///book")
        compose.setContent {
            VeilTheme(AppThemeMode.LIGHT) {
                RebornThresholdScreen(listOf(book), {}, {}, {},
                    highlights = listOf(Highlight("bad", book.id, "Saved", " ")),
                    onOpenPassage = { _, _ -> error("Invalid passage opened") })
            }
        }
        compose.onNodeWithText("Latest Clue").assertDoesNotExist()
        compose.onNodeWithText("Trace in the Archive").assertDoesNotExist()
    }
    @Test fun unopenedBookDoesNotClaimZeroPercentWasMeasured() {
        val book = Book("new", "Unopened volume", "Writer", sourceUri = "file:///new.epub")
        compose.setContent {
            VeilTheme(AppThemeMode.LIGHT) {
                RebornThresholdScreen(listOf(book), {}, {}, {})
            }
        }
        compose.onNodeWithText("Start Reading").assertExists()
        compose.onNodeWithText("0%", substring = true).assertDoesNotExist()
    }

    @Test fun optionalWorldDoesNotLeakWhenDisabled() {
        val book = Book("book", "Quiet Volume", "Author", sourceUri = "file:///volume.epub")
        compose.setContent {
            VeilTheme(AppThemeMode.DARK) {
                RebornThresholdScreen(
                    books = listOf(book),
                    onOpenBook = {},
                    onOpenLibrary = {},
                    onOpenCastle = { error("Hidden world route must not be called") },
                    showWorldEntry = false
                )
            }
        }
        compose.onNodeWithText("Show Castle note").assertDoesNotExist()
        compose.onNodeWithText("Visit Castle").assertDoesNotExist()
        compose.onNodeWithText("Start Reading").assertExists()
    }

}
