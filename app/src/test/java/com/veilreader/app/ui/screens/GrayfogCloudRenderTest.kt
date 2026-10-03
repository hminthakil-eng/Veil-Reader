package com.veilreader.app.ui.screens

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.veilreader.app.R
import org.robolectric.RuntimeEnvironment
import com.veilreader.app.ui.review.GrayfogReviewContent
import com.veilreader.app.ui.review.GrayfogReviewSurface
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Actual production Composables rendered with native Skia. Never device acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogCloudRenderTest {
    @get:Rule val compose = createComposeRule()

    private fun captureMatrix(name: String, scale: Float = 1f, contrast: Boolean = false) {
        val current = mutableStateOf(GrayfogReviewSurface.THRESHOLD_ACTIVE)
        compose.setContent {
            val density = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale), LocalConfiguration provides config) {
                key(current.value) { GrayfogReviewContent(current.value, contrast) }
            }
        }
        for (surface in GrayfogReviewSurface.entries) {
            compose.runOnIdle { current.value = surface }
            compose.waitForIdle()
            if (surface == GrayfogReviewSurface.THRESHOLD_ACTIVE) {
                compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.threshold_return_volume))
                    .assertIsDisplayed().assertHasClickAction()
            }
            if (surface == GrayfogReviewSurface.PROFILE) {
                compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.profile_settings), ignoreCase = true)
                    .assertIsDisplayed().assertHasClickAction()
            }
            val roots = compose.onAllNodes(isRoot())
            val root = if (surface.name.startsWith("BOOK_DETAIL") || surface == GrayfogReviewSurface.RITUAL || surface == GrayfogReviewSurface.ERROR) compose.onNode(isDialog())
                else roots[roots.fetchSemanticsNodes().lastIndex]
            val image = root.captureToImage().asAndroidBitmap()
            check(image.width > 0 && image.height > 0)
            val file = File("build/outputs/grayfog-cloud/$name/${surface.name.lowercase()}.png")
            requireNotNull(file.parentFile).mkdirs()
            file.outputStream().use { check(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
        }
    }

    @Test @Config(qualifiers = "en-w320dp-h720dp-mdpi")
    fun compact320() = captureMatrix("en-100-320")

    @Test @Config(qualifiers = "en-w412dp-h900dp-mdpi")
    fun largePhone412() = captureMatrix("en-100-412")

    @Test @Config(qualifiers = "fa-rIR-w412dp-h900dp-mdpi")
    fun persian130() = captureMatrix("fa-130-412", 1.3f)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persian200() = captureMatrix("fa-200-360", 2f)

    @Test @Config(qualifiers = "en-w900dp-h420dp-mdpi")
    fun landscape() = captureMatrix("en-100-900-landscape")

    @Test @Config(qualifiers = "en-w1280dp-h900dp-mdpi")
    fun tablet() = captureMatrix("en-100-1280-tablet")

    @Test @Config(qualifiers = "en-w720dp-h720dp-mdpi")
    fun foldable() = captureMatrix("en-100-720-foldable")

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persian100() = captureMatrix("fa-100-360")

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persian150() = captureMatrix("fa-150-360", 1.5f)

    @Test @Config(qualifiers = "en-w600dp-h900dp-mdpi")
    fun expanded600() = captureMatrix("en-100-600")

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun highContrast200() = captureMatrix("fa-200-360-contrast", 2f, true)
}
