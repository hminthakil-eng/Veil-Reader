package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.veilreader.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Real Compose pointer input on the production ReaderImageViewer dialog.
 * This test deliberately checks UI state rather than invoking geometry helpers.
 * It is a Robolectric gesture contract, not proof of physical device behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderImageViewerGestureTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun doubleTapPinchPanAndDoubleTapResetPreserveZoomControls() {
        val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
        val imageLabel = "Test illustrated page"
        val zoomOutLabel = RuntimeEnvironment.getApplication().getString(R.string.pdf_zoom_out)
        val zoomInLabel = RuntimeEnvironment.getApplication().getString(R.string.pdf_zoom_in)
        val fitLabel = RuntimeEnvironment.getApplication().getString(R.string.reader_image_zoom_reset)

        compose.setContent {
            MaterialTheme {
                ReaderImageViewer(
                    content = ReaderImageContent(bitmap, caption = imageLabel),
                    title = "Illustration",
                    closeLabel = "Close image",
                    zoomHint = "Double-tap or pinch to zoom",
                    onDismiss = {}
                )
            }
        }

        val image = compose.onNodeWithContentDescription(imageLabel)
        val zoomOut = compose.onNodeWithContentDescription(zoomOutLabel)
        val zoomIn = compose.onNodeWithContentDescription(zoomInLabel)

        image.assertIsDisplayed()
        zoomOut.assertIsNotEnabled()
        zoomIn.assertIsEnabled()
        compose.onNodeWithContentDescription(fitLabel).assertIsDisplayed()

        image.performTouchInput { doubleClick() }
        compose.waitForIdle()
        zoomOut.assertIsEnabled()
        zoomIn.assertIsEnabled()

        // Spread two fingers far enough to reach the production 5x scale cap.
        // If transform gestures are consumed by the tap detector, Zoom In
        // stays enabled and this assertion fails.
        image.performTouchInput {
            pinch(
                start0 = center + Offset(-20f, 0f),
                end0 = center + Offset(-125f, 0f),
                start1 = center + Offset(20f, 0f),
                end1 = center + Offset(125f, 0f),
                durationMillis = 450L
            )
        }
        compose.waitForIdle()
        zoomOut.assertIsEnabled()
        zoomIn.assertIsNotEnabled()

        image.performTouchInput {
            swipe(start = center, end = center + Offset(30f, 20f), durationMillis = 320L)
        }
        compose.waitForIdle()
        zoomOut.assertIsEnabled()

        image.performTouchInput { doubleClick() }
        compose.waitForIdle()
        zoomOut.assertIsNotEnabled()
        zoomIn.assertIsEnabled()
    }
}
