package com.veilreader.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.navigation.LibraryShelfDestination
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

    private fun show(books: () -> List<Book>, query: String = "", fontScale: Float = 1f, restoration: StateRestorationTester? = null,
        requestedShelf: () -> LibraryShelfDestination? = { null },
        onShelfRequestConsumed: (LibraryShelfDestination) -> Unit = {}) {
        val content: @Composable () -> Unit = {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalVeilReducedMotion provides true,
                LocalDensity provides Density(density.density, fontScale)
            ) {
                VeilTheme(AppThemeMode.DARK) {
                    LibraryArchiveContent(
                        books = books(), isImporting = false, onImportUri = {},
                        onOpenBook = {}, onFavorite = {}, onEditMetadata = {},
                        onDeleteBook = {}, onOpenSettings = {},
                        initialViewMode = LibraryViewMode.INDEX, initialQuery = query,
                        requestedShelf = requestedShelf(), onShelfRequestConsumed = onShelfRequestConsumed
                    )
                }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
        compose.awaitGrayfogArtwork()
    }

    @Test fun homeShelfClearsConflictingSearchAndSeriesWithoutOverwritingLaterUserChoice() {
        val request = mutableStateOf<LibraryShelfDestination?>(null)
        val restoration = StateRestorationTester(compose)
        val ordinary = book("ordinary", "Ordinary volume", "Old cycle")
        val favorite = book("favorite", "Favorite volume", "New cycle").copy(favorite = true)
        show({ listOf(ordinary, favorite) }, restoration = restoration,
            requestedShelf = { request.value }, onShelfRequestConsumed = { request.value = null })
        compose.onNodeWithText(context.getString(R.string.library_group_series)).performScrollTo().performClick()
        compose.onAllNodes(hasText("Old cycle") and hasClickAction()).onLast().performClick()
        val search = compose.onNode(hasContentDescription(context.getString(R.string.library_search_hint)) and hasSetTextAction())
        search.performScrollTo().performTextInput("no-matching-volume")
        compose.onNodeWithText(favorite.title).assertDoesNotExist()
        compose.runOnIdle { request.value = LibraryShelfDestination.FAVORITES }
        compose.awaitGrayfogArtwork()
        compose.onNodeWithText(favorite.title).assertExists()
        compose.onNodeWithText(ordinary.title).assertDoesNotExist()
        compose.onNodeWithContentDescription(context.getString(R.string.library_reading_filter_selection,
            context.getString(R.string.library_shelf_favorites), 1)).performScrollTo().performClick()
        compose.onNode(hasText(context.getString(R.string.library_shelf_all)) and hasClickAction()).performClick()
        compose.awaitGrayfogArtwork()
        compose.onNodeWithText(ordinary.title).assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.awaitGrayfogArtwork()
        compose.onNodeWithText(ordinary.title).assertExists()
        compose.onNodeWithText(favorite.title).assertExists()
    }

    @Test fun pendingHomeShelfWinsOverRestoredRetrievalContext() {
        var restoringWithShelf = false
        val restoration = StateRestorationTester(compose)
        val ordinary = book("ordinary-restored", "Ordinary restored volume", "Old cycle").copy(collections = listOf("Old collection"))
        val favorite = book("favorite-restored", "Favorite restored volume", "New cycle").copy(favorite = true, collections = listOf("New collection"))
        show({ listOf(ordinary, favorite) }, restoration = restoration,
            requestedShelf = { if (restoringWithShelf) LibraryShelfDestination.FAVORITES else null },
            onShelfRequestConsumed = { restoringWithShelf = false })
        compose.onNode(hasText(context.getString(R.string.library_collection)) and hasClickAction())
            .performScrollTo().performClick()
        compose.onNode(hasText("Old collection") and hasClickAction()).performClick()
        compose.onNodeWithText(context.getString(R.string.library_group_series)).performScrollTo().performClick()
        compose.onAllNodes(hasText("Old cycle") and hasClickAction()).onLast().performClick()
        compose.onNode(hasContentDescription(context.getString(R.string.library_search_hint)) and hasSetTextAction())
            .performScrollTo().performTextInput("no-matching-volume")
        // Recreate with the old retrieval state and a not-yet-consumed Home destination.
        // A plain flag deliberately avoids processing the request before recreation.
        restoringWithShelf = true
        restoration.emulateSavedInstanceStateRestore()
        compose.awaitGrayfogArtwork()
        compose.onNode(hasText("Old collection") and hasClickAction()).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.library_series_filter, "Old cycle")).assertDoesNotExist()
        compose.onNodeWithText(favorite.title).assertExists()
        compose.onNodeWithText(ordinary.title).assertDoesNotExist()
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

    @Test @Config(qualifiers = "fa-rIR-w320dp-h900dp-mdpi")
    fun persianSearchSortRemainsReachableAtTwoHundredPercent() {
        show({ listOf(book("fa", "کتاب")) }, "كتاب", fontScale = 2f)
        compose.onNodeWithContentDescription(context.getString(R.string.library_sort_books, "مرتبط‌ترین"))
            .performScrollTo().assertHasClickAction().assertHeightIsAtLeast(48.dp)
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

    @Test fun validSeriesSurvivesFallingOutsideTheEightWingPreview() {
        val original = book("one", "First volume", "Cycle")
        val books = mutableStateOf(listOf(original))
        show({ books.value })
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onAllNodes(hasText("Cycle") and hasClickAction()).onLast().performClick()
        compose.awaitGrayfogArtwork()
        compose.runOnIdle {
            books.value = listOf(original) + (1..9).map {
                book("other-$it", "Other volume $it", "Other cycle $it")
                    .copy(lastOpenedAtEpochMs = 1_000L)
            }
        }
        compose.onNodeWithText(context.getString(R.string.library_series_filter, "Cycle")).assertExists()
        compose.onNodeWithText("First volume").assertExists()
        compose.onNodeWithText("Other volume 1").assertDoesNotExist()
        compose.awaitGrayfogArtwork()
    }

    @Test fun emptySearchResultsDoNotClearAValidSeries() {
        show({ listOf(book("one", "First volume", "Cycle")) })
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onAllNodes(hasText("Cycle") and hasClickAction()).onLast().performClick()
        val search = compose.onNode(hasContentDescription(context.getString(R.string.library_search_hint)) and hasSetTextAction())
        search.performScrollTo().performTextInput("no-matching-volume")
        compose.onNodeWithText(context.getString(R.string.library_series_filter, "Cycle")).assertExists()
        search.performTextClearance()
        compose.onNodeWithText("First volume").assertExists()
        compose.awaitGrayfogArtwork()
    }

    @Test fun restoredSeriesSurvivesEmptySnapshotUntilBooksArrive() {
        val original = book("one", "First volume", "Cycle")
        val books = mutableStateOf(listOf(original))
        var restoreWithEmpty = false
        val restoration = StateRestorationTester(compose)
        show({
            val snapshot = books.value
            if (restoreWithEmpty) emptyList() else snapshot
        }, restoration = restoration)
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onAllNodes(hasText("Cycle") and hasClickAction()).onLast().performClick()
        compose.awaitGrayfogArtwork()
        // Change the next restored input without recomposing away the saved selection first.
        compose.runOnIdle { restoreWithEmpty = true }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(context.getString(R.string.library_series_filter, "Cycle")).assertExists()
        compose.runOnIdle {
            restoreWithEmpty = false
            books.value = listOf(original.copy(title = "Restored volume"))
        }
        compose.onNodeWithText(context.getString(R.string.library_series_filter, "Cycle")).assertExists()
        compose.onNodeWithText("Restored volume").assertExists()
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

    @Test fun seriesOutsideEightPreviewCanBeSelected() {
        show({ (1..12).map { index ->
            book("book-$index", "Volume $index", "Cycle %02d".format(index))
                .copy(lastOpenedAtEpochMs = (100 - index).toLong())
        } })
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onNode(hasScrollAction() and hasAnyAncestor(isDialog()))
            .performScrollToNode(hasText("Cycle 12"))
        compose.onNode(hasText("Cycle 12") and hasClickAction() and hasAnyAncestor(isDialog()))
            .performClick()
        compose.onNodeWithText(context.getString(R.string.library_series_filter, "Cycle 12")).assertExists()
        compose.onNodeWithText("Volume 12").assertExists()
        compose.onNodeWithText("Volume 1").assertDoesNotExist()
        compose.awaitGrayfogArtwork()
    }

    @Test @Config(qualifiers = "fa-rIR-w320dp-h900dp-mdpi")
    fun seriesSearchAcceptsPersianKeyboardVariantsAtLargeFont() {
        show({ listOf(
            book("fa", "جلد اول", "کتاب‌های تاریخ"),
            book("other", "Other", "Other")
        ) }, fontScale = 2f)
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onNode(hasText("جست‌وجوی مجموعه‌ها") and hasSetTextAction())
            .performTextInput("كتاب هاي تاريخ")
        compose.onNode(hasText("کتاب‌های تاریخ") and hasClickAction() and hasAnyAncestor(isDialog()))
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("جلد اول").assertExists()
        compose.onNodeWithText("Other").assertDoesNotExist()
        compose.awaitGrayfogArtwork()
    }

    @Test fun seriesChooserShowsCountsSelectedStateAndClearSelection() {
        show({ listOf(book("one", "First volume", " Cycle "), book("two", "Second volume", "cycle")) })
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onNode(hasText("Cycle") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onNode(hasText("Cycle") and hasClickAction() and hasAnyAncestor(isDialog())).assertIsSelected()
        compose.onNodeWithText("2 books").assertExists()
        compose.onNodeWithText(context.getString(R.string.library_all_series)).performClick()
        compose.onNodeWithText(context.getString(R.string.library_series_filter, "Cycle")).assertDoesNotExist()
        compose.onNodeWithText("First volume").assertExists()
        compose.onNodeWithText("Second volume").assertExists()
        compose.awaitGrayfogArtwork()
    }

    @Test fun cancelledSeriesSearchRetainsLibraryQueryAndSelection() {
        show({ listOf(book("one", "Veil volume", "Cycle"), book("other", "Other", "Other")) }, query = "veil")
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onNode(hasText("Cycle") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        compose.onNode(hasText("Search series") and hasSetTextAction()).performTextInput("no-matching-series")
        compose.onNodeWithText("No matching series").assertExists()
        compose.onNodeWithText(context.getString(R.string.common_cancel)).performClick()
        compose.onNodeWithText(context.getString(R.string.library_series_filter, "Cycle")).assertExists()
        compose.onNodeWithText("Veil volume").assertExists()
        compose.onNode(hasContentDescription(context.getString(R.string.library_search_hint)) and hasSetTextAction())
            .assertTextContains("veil")
        compose.awaitGrayfogArtwork()
    }

    @Test fun seriesSearchImeActionReleasesDialogFocus() {
        show({ listOf(book("one", "First volume", "Cycle")) })
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        val search = compose.onNode(hasText("Search series") and hasSetTextAction())
        search.performClick().performTextInput("Cycle")
        search.assertIsFocused()
        search.performImeAction()
        search.assertIsNotFocused()
        compose.onNode(hasText("Cycle") and hasClickAction() and hasAnyAncestor(isDialog()) and !hasSetTextAction()).performClick()
        compose.onNodeWithText("First volume").assertExists()
        compose.awaitGrayfogArtwork()
    }

    @Test fun seriesDialogUsesParentAccessibilityFontScaleInActualTextLayout() {
        val name = "Records of the Observatory and the Grayfog Archive"
        show({ listOf(book("one", "First volume", name)) }, fontScale = 2f)
        compose.onNodeWithText(context.getString(R.string.library_group_series))
            .performScrollTo().performClick()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNode(
            hasText(name) and hasAnyAncestor(isDialog()) and
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true
        ).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            org.junit.Assert.assertTrue(action(layouts))
        }
        org.junit.Assert.assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0f)
        compose.onNodeWithText(context.getString(R.string.common_cancel)).performClick()
        compose.awaitGrayfogArtwork()
    }
}
