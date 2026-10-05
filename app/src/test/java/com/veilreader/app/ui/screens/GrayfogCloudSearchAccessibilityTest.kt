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

/** Shared assertions; each locale has its own worker and native sandbox. */
abstract class GrayfogCloudSearchFixture {
    @get:Rule val compose = createComposeRule()

    protected fun checkSearchLabel(scale: Float) {
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
        compose.awaitGrayfogArtwork()
        val label = context.getString(R.string.library_search_hint)
        val search = compose.onNode(hasContentDescription(label) and hasSetTextAction())
        search.performScrollTo().assertHeightIsAtLeast(48.dp)
        // Scrolling can compose/re-size a prefetched cover and start another IO decode.
        // Settle that actual artwork before typing removes the book from the result set.
        compose.awaitGrayfogArtwork()
        search.performTextInput("Still")
        search.assertTextContains("Still")
        search.assertIsDisplayed()
        compose.awaitGrayfogArtwork()
    }

}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogCloudSearchAccessibilityTest : GrayfogCloudSearchFixture() {
    @Test
    fun searchPurposeSurvivesEnteredText() = checkSearchLabel(1f)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogCloudPersianSearchAccessibilityTest : GrayfogCloudSearchFixture() {
    @Test
    fun persianLargeTextSearchRetainsItsAccessibleLabel() = checkSearchLabel(2f)
}
