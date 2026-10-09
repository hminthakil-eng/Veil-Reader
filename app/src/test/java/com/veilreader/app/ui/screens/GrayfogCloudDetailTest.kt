package com.veilreader.app.ui.screens

import android.content.res.Configuration
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.review.GrayfogReviewContent
import com.veilreader.app.ui.review.GrayfogReviewSurface
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real production headings and missing-cover composition, without publication owners. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogCloudDetailTest {
    @get:Rule val compose = createComposeRule()

    private fun captureDetail(name: String, dialog: Boolean = false) {
        compose.awaitGrayfogArtwork()
        val roots = compose.onAllNodes(isRoot())
        val root = if (dialog) compose.onNode(isDialog()) else roots[roots.fetchSemanticsNodes().lastIndex]
        val bitmap = root.captureToImage().asAndroidBitmap()
        val locale = RuntimeEnvironment.getApplication().resources.configuration.locales[0].language
        val file = File("build/outputs/grayfog-detail/$locale/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        file.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private fun checkHeadings(scale: Float) {
        val surface = mutableStateOf(GrayfogReviewSurface.THRESHOLD_ACTIVE)
        compose.setContent {
            val density = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale), LocalConfiguration provides config) {
                key(surface.value) { GrayfogReviewContent(surface.value) }
            }
        }
        for (target in listOf(GrayfogReviewSurface.THRESHOLD_ACTIVE, GrayfogReviewSurface.LIBRARY_GALLERY,
            GrayfogReviewSurface.BOOK_DETAIL_PERSIAN, GrayfogReviewSurface.APPEARANCE_QUICK,
            GrayfogReviewSurface.NOTES, GrayfogReviewSurface.SETTINGS, GrayfogReviewSurface.CASTLE_ADVANCED,
            GrayfogReviewSurface.OBSERVATORY_DENSE, GrayfogReviewSurface.PATH,
            GrayfogReviewSurface.SANCTUM_POPULATED, GrayfogReviewSurface.PROFILE)) {
            compose.runOnIdle { surface.value = target }
            compose.awaitGrayfogArtwork()
            assertTrue("Missing navigable heading: $target", compose.onAllNodes(isHeading()).fetchSemanticsNodes().isNotEmpty())
        }
    }

    private fun checkCaptionReachability() {
        val height = mutableStateOf(240)
        val title = "فهرست اتاق‌های خاموش و یادداشت‌های آخرین نگهبان رصدخانه"
        compose.setContent {
            VeilTheme(AppThemeMode.DARK) {
                Box(Modifier.size(140.dp, height.value.dp)) {
                    GeneratedBookCover(title, "نویسندهٔ دفترهای فراموش‌شده")
                }
            }
        }
        compose.onNodeWithText(title).assertIsDisplayed()
        // A wide but short cover must not squeeze a duplicate title into its ornament field.
        compose.runOnIdle { height.value = 100 }
        compose.onNodeWithText(title).assertDoesNotExist()
    }

    @Test @Config(qualifiers = "en-w412dp-h900dp-mdpi")
    fun editorialHierarchyIsAvailableToHeadingNavigation() = checkHeadings(1f)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persianLargeTextRetainsHeadingNavigation() = checkHeadings(2f)

    @Test @Config(qualifiers = "en-w320dp-h720dp-mdpi")
    fun persianFallbackCaptionInEnglishShellRespectsAvailableHeight() = checkCaptionReachability()

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persianFallbackCaptionRespectsAvailableHeight() = checkCaptionReachability()
    private fun checkMissingTitle(scale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale), LocalConfiguration provides config) {
                GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_MISSING_METADATA)
            }
        }
        val context = RuntimeEnvironment.getApplication()
        val title = context.getString(R.string.common_untitled_book)
        compose.onAllNodesWithText(title).assertCountEquals(1)
        compose.onAllNodesWithText(title)[0].performScrollTo().assertIsDisplayed()
        captureDetail("missing-metadata-gallery")
        // The completed fixture belongs to two collections: Shelves intentionally presents three registers.
        for ((mode, count) in listOf(R.string.library_view_index to 1, R.string.library_view_shelves to 3)) {
            // The header may be virtualized after a deep capture: navigate its actual lazy owner first.
            compose.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(0)
            compose.onNodeWithText(context.getString(mode), ignoreCase = true).performScrollTo().performClick()
            compose.onAllNodesWithText(title).assertCountEquals(count)
            compose.onAllNodesWithText(title)[0].performScrollTo().assertIsDisplayed()
            captureDetail(if (mode == R.string.library_view_index) "missing-metadata-index" else "missing-metadata-shelves")
        }
    }

    @Test @Config(qualifiers = "en-w412dp-h900dp-mdpi")
    fun missingTitleHasTruthfulIdentityInAllArchiveModes() = checkMissingTitle(1f)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persianLargeTextMissingTitleHasTruthfulIdentityInAllArchiveModes() = checkMissingTitle(2f)

    private fun checkUnknownDate() {
        compose.setContent { GrayfogReviewContent(GrayfogReviewSurface.BOOK_DETAIL) }
        compose.awaitGrayfogArtwork()
        val context = RuntimeEnvironment.getApplication()
        val unknown = context.getString(R.string.capsule_date_unknown)
        assertTrue("Unknown history must remain explicit", compose.onAllNodesWithText(unknown, ignoreCase = true)
            .fetchSemanticsNodes().isNotEmpty())
        val epochDate = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM,
            context.resources.configuration.locales[0]).format(java.util.Date(0L))
        compose.onAllNodesWithText(epochDate, ignoreCase = true).assertCountEquals(0)
        compose.onAllNodesWithText(unknown, ignoreCase = true)[0].performScrollTo().assertIsDisplayed()
        // Keep this contract semantic-only in Robolectric. Native Graphics + Compose
        // captureToImage on a dialog can abort the whole Gradle test worker inside
        // Android's JNI graphics bridge even after assertions have passed.
        // Pixel evidence belongs to the emulator-backed Grayfog capture lane.
    }

    @Test
    @Config(qualifiers = "en-w412dp-h900dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun unknownAnnotationDatesDoNotInventEpochHistory() = checkUnknownDate()

    @Test
    @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun persianUnknownAnnotationDatesRemainExplicit() = checkUnknownDate()

}
