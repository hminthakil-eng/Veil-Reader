package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Measures the real production ReaderControl composable, not a mock dimension formula.
 * Uses the same four weighted actions and spacing as ReaderScreen's bottom chrome.
 * This is a Robolectric layout/semantics regression; visual clipping and TalkBack
 * speech still require emulator/physical evidence before merging.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderChromeHitTargetTest {
    @get:Rule val compose = createComposeRule()

    private fun verifyControls(
        widthDp: Int,
        fontScale: Float,
        rtl: Boolean,
        disableBookmark: Boolean = false
    ) {
        val actions = listOf(
            ReaderAction.NOTEBOOK,
            ReaderAction.BOOKMARK,
            ReaderAction.FOCUS,
            ReaderAction.APPEARANCE
        )
        val labelIds = listOf(
            R.string.reader_chrome_notebook,
            R.string.reader_chrome_bookmark,
            R.string.reader_focus,
            R.string.reader_chrome_appearance
        )
        val clicks = IntArray(actions.size)

        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides
                    (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
            ) {
                VeilTheme(AppThemeMode.DARK) {
                    Row(
                        modifier = Modifier
                            .width(widthDp.dp)
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                            .testTag("reader-bottom-row"),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions.forEachIndexed { index, action ->
                            val button = Modifier.weight(1f).testTag("reader-control-$index")
                            val modifier = if (action == ReaderAction.FOCUS) {
                                button.semantics { stateDescription = "Off" }
                            } else button
                            ReaderControl(
                                action = action,
                                label = stringResource(labelIds[index]),
                                modifier = modifier,
                                enabled = !disableBookmark || action != ReaderAction.BOOKMARK,
                                onClick = { clicks[index]++ }
                            )
                        }
                    }
                }
            }
        }

        val rowBounds = compose.onNodeWithTag("reader-bottom-row")
            .fetchSemanticsNode().boundsInRoot
        val measured = actions.indices.map { index ->
            val node = compose.onNodeWithTag("reader-control-$index")
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(56.dp)
            if (disableBookmark && index == 1) node.assertIsNotEnabled()
            else node.assertIsEnabled().assertHasClickAction()
            node.fetchSemanticsNode().boundsInRoot
        }

        // Regardless of reading direction, the four measured targets stay distinct
        // and entirely within the padded Row's actual rendered bounds.
        val spatialOrder = measured.sortedBy { it.left }
        spatialOrder.forEach { bounds ->
            assertTrue("target escapes row start", bounds.left >= rowBounds.left - 1f)
            assertTrue("target escapes row end", bounds.right <= rowBounds.right + 1f)
        }
        spatialOrder.zipWithNext().forEach { (left, right) ->
            assertTrue("reader buttons overlap", left.right <= right.left + 1f)
        }

        // Save rendered pixels for independent visual review; not device approval.
        val screenshot = compose.onNodeWithTag("reader-bottom-row")
            .captureToImage().asAndroidBitmap()
        val caseName = (if (rtl) "fa-rtl" else "en-ltr") +
            "-$widthDp-${(fontScale * 100).toInt()}" +
            (if (disableBookmark) "-disabled" else "")
        val output = File("build/outputs/reader-chrome-hit-target/$caseName.png")
        requireNotNull(output.parentFile).mkdirs()
        output.outputStream().use {
            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        actions.indices.forEach { index ->
            if (!disableBookmark || index != 1) {
                compose.onNodeWithTag("reader-control-$index").performClick()
            }
        }
        compose.runOnIdle {
            clicks.forEachIndexed { index, count ->
                assertEquals("unexpected click for reader action $index",
                    if (disableBookmark && index == 1) 0 else 1, count)
            }
        }
    }

    @Test fun english320ControlsDoNotOverlap() =
        verifyControls(widthDp = 320, fontScale = 1f, rtl = false)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persian360LargeTextControlsDoNotOverlap() =
        verifyControls(widthDp = 360, fontScale = 2f, rtl = true)

    @Test @Config(qualifiers = "fa-rIR-w320dp-h800dp-mdpi")
    fun persian320LargeTextKeepsDistinctActions() =
        verifyControls(widthDp = 320, fontScale = 2f, rtl = true)

    @Test fun bookmarkUnavailableDoesNotTriggerClick() =
        verifyControls(widthDp = 320, fontScale = 1.5f, rtl = false,
            disableBookmark = true)
}