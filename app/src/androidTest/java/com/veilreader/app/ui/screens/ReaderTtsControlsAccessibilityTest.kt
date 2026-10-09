package com.veilreader.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderTtsSettings
import com.veilreader.app.ui.reader.tts.ReaderTtsPhase
import com.veilreader.app.ui.reader.tts.ReaderTtsVoice
import com.veilreader.app.ui.reader.tts.ReaderTtsState
import com.veilreader.app.ui.theme.VeilTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReaderTtsControlsAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun englishAtLargeText_keepsStopAndCloseReachable() = verify(Locale.ENGLISH, LayoutDirection.Ltr)
    @Test fun persianAtLargeText_keepsStopAndCloseReachable() = verify(Locale.forLanguageTag("fa"), LayoutDirection.Rtl)

    @Test fun automaticPreviewUsesPlaybackDialectRanking() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var previewed: String? = null
        compose.setContent {
            VeilTheme {
                ReaderTtsControls(
                    state = ReaderTtsState(), supported = true, settings = ReaderTtsSettings(),
                    startPending = false, startFailed = false, publicationLanguage = "en-US",
                    voiceCatalogSupported = true,
                    voices = listOf(
                        ReaderTtsVoice("gb", "en-GB", 500, false, true),
                        ReaderTtsVoice("us", "en-US", 300, false, true)
                    ),
                    onStart = {}, onResume = {}, onPause = {}, onPrevious = {}, onNext = {},
                    onStop = {}, onSettingsChange = {}, onDone = {},
                    onPreviewVoice = { _, voice, _ -> previewed = voice }
                )
            }
        }
        compose.onNodeWithText(context.getString(R.string.tts_preview_voice))
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals("us", previewed) }
    }

    private fun verify(locale: Locale, direction: LayoutDirection) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
        val localized = context.createConfigurationContext(configuration)
        var stops = 0
        var closes = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalResources provides localized.resources,
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(density.density, 2f)
            ) {
                VeilTheme {
                    Box(Modifier.width(240.dp).height(400.dp)) {
                        ReaderTtsControls(
                            state = ReaderTtsState(ReaderTtsPhase.PLAYING), supported = true,
                            settings = ReaderTtsSettings(), startPending = false, startFailed = false,
                            onStart = {}, onResume = {}, onPause = {},
                            onPrevious = {}, onNext = {}, onStop = { stops++ },
                            onSettingsChange = {}, onDone = { closes++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(localized.getString(R.string.tts_stop)).performScrollTo().performClick()
        compose.onNodeWithText(localized.getString(R.string.reader_image_viewer_close)).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, stops)
            assertEquals(1, closes)
        }
    }
}
