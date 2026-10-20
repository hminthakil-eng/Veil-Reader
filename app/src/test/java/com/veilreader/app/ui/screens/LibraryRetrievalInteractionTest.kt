package com.veilreader.app.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises production retrieval controls, not a second test-only filter implementation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w390dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LibraryRetrievalInteractionTest {
    @get:Rule val compose = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()
    private fun book(id: String, title: String, series: String? = null) = Book(
        id = id, title = title, author = "Fixture author", seriesName = series,
        sourceUri = "veil-test://$id", coverCachePath = "", addedAtEpochMs = 100L
    )

    private fun show(books: () -> List<Book>, query: String = "") {
        compose.setContent {
            CompositionLocalProvider(LocalVeilReducedMotion provides true) {
                VeilTheme(AppThemeMode.DARK) {
                    LibraryArchiveContent(
                        books = books(), isImporting = false, onImportUri = {},
                        onOpenBook = {}, onFavorite = {}, onEditMetadata = {},
                        onDeleteBook = {}, onOpenSettings = {},
                        initialViewMode = LibraryViewMode.INDEX, initialQuery = query
                    )
                }
            }
        }
        compose.awaitGrayfogArtwork()
    }

    @Test fun effectiveSearchShowsRelevanceAndExplicitRecentRemainsRecent() {
        show({ listOf(book("one", "Veil"), book("two", "Veil Notes")) }, "veil")
        val relevanceDescription = context.getString(R.string.library_sort_books, "Relevance")
        compose.onNodeWithContentDescription(relevanceDescription)
            .performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.library_sort_recent)).performClick()
        compose.onNodeWithContentDescription(context.getString(
            R.string.library_sort_books, context.getString(R.string.library_sort_recent)
        )).assertExists()
        val search = compose.onNode(hasContentDescription(context.getString(R.string.library_search_hint)) and hasSetTextAction())
        search.performScrollTo().performTextClearance()
        search.performTextInput("notes")
        compose.onNodeWithContentDescription(context.getString(
            R.string.library_sort_books, context.getString(R.string.library_sort_recent)
        )).assertExists()
        compose.awaitGrayfogArtwork()
    }

    @Test fun metadataRemovalClearsStaleSeriesWithoutHidingRemainingBooks() {
        val books = mutableStateOf(listOf(book("one", "First volume", "Cycle")))
        show({ books.value })
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onAllNodes(hasText("Cycle") and hasClickAction()).onLast().performClick()
        val chip = context.getString(R.string.library_series_filter, "Cycle")
        compose.onNodeWithText(chip).assertExists()
        compose.awaitGrayfogArtwork()
        compose.runOnIdle { books.value = listOf(book("one", "First volume", "New cycle")) }
        compose.onNodeWithText(chip).assertDoesNotExist()
        compose.onNodeWithText("First volume").assertExists()
        compose.awaitGrayfogArtwork()
    }

    @Test fun trimmedSeriesGroupStillFindsItsVolume() {
        show({ listOf(book("one", "Only volume", " Cycle ")) })
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onAllNodes(hasText("Cycle") and hasClickAction()).onLast().performClick()
        compose.onNodeWithText("Only volume").assertExists()
        compose.awaitGrayfogArtwork()
    }
}
