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

/** Full-scene search uses its own native sandbox process, independent of chamber fixtures. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogCloudSearchAccessibilityTest {
    @get:Rule val compose = createComposeRule()

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
}
