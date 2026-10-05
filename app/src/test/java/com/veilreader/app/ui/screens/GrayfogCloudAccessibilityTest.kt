package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.review.GrayfogReviewContent
import com.veilreader.app.ui.review.GrayfogReviewSurface
import com.veilreader.app.ui.review.GrayfogReviewFixtures
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Check real narrow Gallery utilities: independent 48dp targets must not open the volume. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogCloudAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    private fun checkGallery(scale: Float, width: Int) {
        val context = RuntimeEnvironment.getApplication()
        val book = GrayfogReviewFixtures.books[1]
        var opens = 0
        var details = 0
        var favorites = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(AppThemeMode.DARK) {
                    Box(Modifier.width(width.dp)) {
                        BookLibraryTile(book, null, null, onOpen = { opens++ },
                            onFavorite = { favorites++ }, onDetails = { details++ })
                    }
                }
            }
        }
        val detail = compose.onNodeWithContentDescription(context.getString(R.string.library_book_details_semantics, book.title))
        detail.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        val favorite = compose.onNodeWithContentDescription(context.getString(R.string.library_remove_favorite_semantics, book.title))
        favorite.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle {
            assertEquals(1, details)
            assertEquals(1, favorites)
            assertEquals(0, opens)
        }
    }

    @Test fun narrowGalleryUtilities() = checkGallery(1f, 140)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h900dp-mdpi")
    fun persianLargeGalleryUtilities() = checkGallery(2f, 224)
    private fun checkSearchLabel(scale: Float) {
        val context = RuntimeEnvironment.getApplication()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(AppThemeMode.DARK) {
                    GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_GALLERY)
                }
            }
        }
        // Compose idle does not include IO. Await real artwork before filtering disposes
        // a cover, and before Robolectric tears down the native rendering sandbox.
        compose.waitForIdle()
        awaitCoverArtwork()
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        val label = context.getString(R.string.library_search_hint)
        val search = compose.onNode(hasContentDescription(label) and hasSetTextAction())
        search.performScrollTo().assertHeightIsAtLeast(48.dp).performTextInput("Still")
        search.assertTextContains("Still")
        search.assertIsDisplayed()
        compose.waitForIdle()
        awaitCoverArtwork()
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
    }

    private fun awaitCoverArtwork() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(SemanticsMatcher.keyIsDefined(BookCoverArtworkReady), useUnmergedTree = true)
                .fetchSemanticsNodes().all { it.config[BookCoverArtworkReady] }
        }
    }

    @Test @Config(qualifiers = "en-w320dp-h720dp-mdpi")
    fun searchPurposeSurvivesEnteredText() = checkSearchLabel(1f)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persianLargeTextSearchRetainsItsAccessibleLabel() = checkSearchLabel(2f)
    private fun checkReadingAccess(scale: Float) {
        val context = RuntimeEnvironment.getApplication()
        var menus = 0
        var settings = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(AppThemeMode.DARK) {
                    Box(Modifier.width(288.dp)) {
                        ReaderAccessDock(context.getString(R.string.reader_chrome_appearance),
                            androidx.compose.ui.graphics.Color.Black,
                            androidx.compose.ui.graphics.Color.White,
                            androidx.compose.ui.graphics.Color.Yellow,
                            onMenu = { menus++ }, onSettings = { settings++ })
                    }
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.reader_reading_menu))
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.reader_chrome_appearance))
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, menus); assertEquals(1, settings) }
    }

    @Test fun readingMenuAndAppearanceHaveSeparateOrdinaryTapTargets() = checkReadingAccess(1f)

    @Test @Config(qualifiers = "fa-rIR-w320dp-h800dp-mdpi")
    fun persianLargeReadingMenuRemainsReachable() = checkReadingAccess(2f)

    private fun checkReadingFilter(scale: Float) {
        var selected: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(AppThemeMode.DARK) {
                    Box(Modifier.width(288.dp)) {
                        LibraryReadingFilter("All", listOf(
                            LibraryReadingFilterOption("All", "All volumes", 48),
                            LibraryReadingFilterOption("Unread", "Unread volumes", 12)),
                            onSelect = { selected = it })
                    }
                }
            }
        }
        compose.onNodeWithText("All volumes").assertIsDisplayed().performClick()
        compose.onNodeWithText("Unread volumes").assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals("Unread", selected) }
        compose.onNodeWithText("Unread volumes").assertDoesNotExist()
    }

    @Test fun readingStateDisclosureSelectsAndDismisses() = checkReadingFilter(1f)

    @Test @Config(qualifiers = "fa-rIR-w320dp-h800dp-mdpi")
    fun largeTextReadingStateDisclosureRemainsReachable() = checkReadingFilter(2f)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h900dp-mdpi")
    fun castleFloorRegistrationsUseTheInterfaceNumerals() {
        val context = RuntimeEnvironment.getApplication()
        compose.setContent { GrayfogReviewContent(GrayfogReviewSurface.CASTLE_ADVANCED) }
        compose.onNodeWithText(context.getString(R.string.castle_floor, "۰۶"))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.castle_floor, "06")).assertDoesNotExist()
    }

}
