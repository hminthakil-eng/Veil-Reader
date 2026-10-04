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
        val label = context.getString(R.string.library_search_hint)
        val search = compose.onNode(hasContentDescription(label) and hasSetTextAction())
        search.performScrollTo().assertHeightIsAtLeast(48.dp).performTextInput("Still")
        search.assertTextContains("Still")
        search.assertIsDisplayed()
    }

    @Test @Config(qualifiers = "en-w320dp-h720dp-mdpi")
    fun searchPurposeSurvivesEnteredText() = checkSearchLabel(1f)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persianLargeTextSearchRetainsItsAccessibleLabel() = checkSearchLabel(2f)
}
