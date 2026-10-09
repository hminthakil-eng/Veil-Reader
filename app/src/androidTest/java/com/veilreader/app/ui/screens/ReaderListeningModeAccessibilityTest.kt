package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.ReaderTtsSettings
import com.veilreader.app.ui.reader.tts.ReaderTtsState
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Rule
import org.junit.Test

class ReaderListeningModeAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun advancedSettingsUseAModalWindowAndCloseBackToListeningAtLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                VeilTheme {
                    Box(Modifier.width(320.dp).height(480.dp)) {
                        ReaderListeningMode(
                            book = Book("test", "Test volume", "Author"),
                            state = ReaderTtsState(), supported = true,
                            settings = ReaderTtsSettings(), startPending = false, startFailed = false,
                            onStart = {}, onResume = {}, onPause = {}, onPrevious = {},
                            onNext = {}, onStop = {}, onSettingsChange = {}, onDone = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.tts_listening_mode_settings))
            .performScrollTo().performClick()
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithText(context.getString(R.string.reader_image_viewer_close))
            .performScrollTo().performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.tts_listening_mode_title)).assertExists()
    }
}
