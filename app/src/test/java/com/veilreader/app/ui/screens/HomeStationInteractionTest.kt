package com.veilreader.app.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.veilreader.app.R
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeStationInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = RuntimeEnvironment.getApplication()
    private fun book(id: String, progress: Float = 0f) = Book(
        id = id, title = "Volume $id", author = "Fixture", progress = progress,
        sourceUri = "veil-test://$id", coverCachePath = "", addedAtEpochMs = 10L
    )
    private fun show(books: List<Book>, scale: Float = 1f, onLibrary: () -> Unit = {}, onBook: (Book) -> Unit = {}) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalVeilReducedMotion provides true,
                LocalDensity provides Density(density.density, scale)) {
                VeilTheme(AppThemeMode.DARK) {
                    ReadingNowScreen(books, SampleData.profile, emptyList(),
                        onOpenBook = onBook, onOpenPassage = { _, _ -> },
                        onOpenLibrary = onLibrary, onOpenCastle = {})
                }
            }
        }
        compose.awaitGrayfogArtwork()
    }
    @Test fun activeHomeHasDirectLibraryEntranceEvenWithOnlyOneBook() {
        var opened = 0
        show(listOf(book("current", .42f)), onLibrary = { opened++ })
        compose.onNodeWithText(context.getString(R.string.threshold_enter_library))
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
    @Test fun fourLibraryEntrancesShowCatalogCountsAndRemainActionable() {
        var opened = 0
        show(listOf(book("current", .42f).copy(lastOpenedAtEpochMs = 100L),
            book("unread").copy(favorite = true), book("done", 1f).copy(finished = true),
            book("unavailable", .5f).copy(sourceUri = null, favorite = true)), onLibrary = { opened++ })
        listOf(R.string.library_shelf_all to 4, R.string.library_shelf_reading to 2,
            R.string.library_shelf_completed to 1, R.string.library_shelf_favorites to 2).forEach { (label, count) ->
            compose.onNode(hasText(context.getString(label)) and hasText(context.resources.getQuantityString(
                R.plurals.library_series_books, count, count)) and hasClickAction())
                .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        }
        compose.runOnIdle { assertEquals(4, opened) }
    }
    @Test @Config(qualifiers = "fa-rIR-w320dp-h720dp-mdpi")
    fun persianLargeTextLibraryAlcovesRemainWideAndActionable() {
        var opened = 0
        show(listOf(book("current", .42f).copy(lastOpenedAtEpochMs = 100L),
            book("favorite").copy(favorite = true)), scale = 2f, onLibrary = { opened++ })
        val title = context.getString(R.string.library_shelf_favorites)
        val count = context.resources.getQuantityString(R.plurals.library_series_books, 1, 1)
        compose.onNode(hasText(title) and hasText(count) and hasClickAction())
            .performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(240.dp)
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun largeTextResumeWithMissingCoverOpensTheCurrentBook() {
        val current = book("current", .42f).copy(lastOpenedAtEpochMs = 100L,
            title = "The Cartographer of Quiet Rooms and the Observatory Register")
        val opened = mutableListOf<String>()
        show(listOf(book("other"), current), scale = 2f, onBook = { opened += it.id })
        compose.onNodeWithText(context.getString(R.string.threshold_return_volume))
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(listOf(current.id), opened) }
    }
}
