package com.veilreader.app.ui.screens

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the real fullscreen image dialog at 200% text size.
 *
 * These assertions verify accessible names, visible controls and touch geometry.
 * They DO NOT establish actual TalkBack speech or pixel-level text legibility.
 * Dialog screenshots can crash Robolectric's native graphics bridge, so the
 * pixel/speech evidence must come from an emulator or a physical device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderImageViewerAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    private fun verify200PercentLayout(rtl: Boolean) {
        val app = RuntimeEnvironment.getApplication()
        val imageLabel =
            if (rtl) "نقشهٔ مصور فصل و جزئیات آن" else "Illustrated chapter map and its details"
        val title = app.getString(R.string.reader_image_viewer_title)
        val close = app.getString(R.string.reader_image_viewer_close)
        val hint = app.getString(R.string.reader_image_viewer_hint)
        val fit = app.getString(R.string.reader_image_zoom_reset)
        val zoomIn = app.getString(R.string.pdf_zoom_in)
        val zoomOut = app.getString(R.string.pdf_zoom_out)
        val content = ReaderImageContent(
            Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888),
            caption = imageLabel
        )
        var dismissCount = 0

        compose.setContent {
            val density = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = 2f }
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalConfiguration provides config,
                LocalLayoutDirection provides
                    (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
            ) {
                MaterialTheme {
                    ReaderImageViewer(
                        content = content,
                        title = title,
                        closeLabel = close,
                        zoomHint = hint,
                        onDismiss = { dismissCount++ }
                    )
                }
            }
        }

        // The illustration remains discoverable with its complete caption.
        compose.onNodeWithContentDescription(imageLabel).assertIsDisplayed()
        // The hint and Close action must remain reachable with enlarged text.
        compose.onNodeWithText(hint).assertIsDisplayed()
        val closeNode = compose.onNodeWithText(close)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        val fitNode = compose.onNodeWithContentDescription(fit)
        val inNode = compose.onNodeWithContentDescription(zoomIn)
        val outNode = compose.onNodeWithContentDescription(zoomOut)
        listOf(fitNode, inNode, outNode).forEach {
            it.assertIsDisplayed()
                .assertHasClickAction()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }

        // The accessible non-gesture alternatives still act on the real dialog.
        inNode.performClick()
        compose.waitForIdle()
        outNode.assertHasClickAction().assertIsDisplayed()
        closeNode.performClick()
        compose.runOnIdle { assertEquals(1, dismissCount) }
    }

    @Test
    @Config(qualifiers = "en-w320dp-h720dp-mdpi")
    fun english320At200Percent() = verify200PercentLayout(rtl = false)

    @Test
    @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persianRtl360At200Percent() = verify200PercentLayout(rtl = true)
}
