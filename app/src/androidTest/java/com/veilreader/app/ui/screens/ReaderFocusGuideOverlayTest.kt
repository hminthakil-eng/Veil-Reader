package com.veilreader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.ReaderFocusGuideMode
import com.veilreader.app.domain.ReaderFocusGuideSettings
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReaderFocusGuideOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun guideNeverOwnsTouchesAndLeavesFocusBandUndimmedAcrossThemes() {
        val theme = mutableStateOf(ReaderTheme.PAPER)
        val mode = mutableStateOf(ReaderFocusGuideMode.WINDOW)
        val highContrast = mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            VeilTheme(highContrastEnabled = highContrast.value) {
                Box(Modifier.size(200.dp).testTag("viewport")) {
                    Box(Modifier.fillMaxSize().background(Color.White).clickable { clicks += 1 })
                    ReaderFocusGuideOverlay(
                        ReaderFocusGuideSettings(mode = mode.value), theme.value, Modifier.fillMaxSize()
                    )
                }
            }
        }
        val viewport = compose.onNodeWithTag("viewport")
        for (contrast in listOf(false, true)) {
            for (readerTheme in ReaderTheme.entries) {
                for (guideMode in ReaderFocusGuideMode.entries) {
                    compose.runOnIdle {
                        theme.value = readerTheme
                        mode.value = guideMode
                        highContrast.value = contrast
                    }
                    viewport.performTouchInput { click(Offset(width / 2f, height * 0.1f)) }
                    viewport.performTouchInput { click(center) }
                    val pixels = viewport.captureToImage().toPixelMap()
                    val centerColor = pixels[pixels.width / 2, pixels.height / 2]
                    assertEquals(1f, centerColor.red, 0.01f)
                    val outside = pixels[pixels.width / 2, pixels.height / 10]
                    if (guideMode == ReaderFocusGuideMode.OFF) {
                        assertEquals(1f, outside.red, 0.01f)
                    } else {
                        assertTrue("Outside band must dim for $readerTheme", outside.red < 0.9f)
                    }
                }
            }
        }
        compose.runOnIdle { assertEquals(48, clicks) }
    }
}
