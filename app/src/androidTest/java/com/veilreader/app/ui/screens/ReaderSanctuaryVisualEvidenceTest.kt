package com.veilreader.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
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
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.exportGrayfogCapture
import com.veilreader.app.ui.reader.tts.ReaderTtsPhase
import com.veilreader.app.ui.reader.tts.ReaderTtsState
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test

/**
 * QA-only visual evidence for Reader Sanctuary presentation components.
 *
 * These fixtures never load a publication or write user history. They only exercise the same
 * production composables used by Reader chrome/appearance so CI can export device screenshots.
 */
class ReaderSanctuaryVisualEvidenceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun appearanceQuickEnglishStandard() {
        val localized = localizedContext("en", 1f)
        present("en", 1f, highContrast = false) {
            EpubAppearancePanel(
                appearance = ReaderAppearance(),
                fixedLayout = false,
                fixedLayoutSpread = ReaderFixedLayoutSpread.AUTO,
                publicationLanguage = null,
                onSpreadChange = {},
                onChange = {},
                onDone = {},
                modifier = Modifier.fillMaxSize(),
                initiallyAdvanced = false
            )
        }

        compose.onNodeWithText(localized.getString(R.string.reader_quick)).assertIsDisplayed()
        capture("sanctuary-appearance-quick-en-100-standard")
    }

    @Test
    fun appearanceAdvancedPersianLargeContrast() {
        val localized = localizedContext("fa", 2f)
        present("fa", 2f, highContrast = true) {
            EpubAppearancePanel(
                appearance = ReaderAppearance(),
                fixedLayout = false,
                fixedLayoutSpread = ReaderFixedLayoutSpread.AUTO,
                publicationLanguage = null,
                onSpreadChange = {},
                onChange = {},
                onDone = {},
                modifier = Modifier.fillMaxSize(),
                initiallyAdvanced = true
            )
        }

        compose.onNodeWithText(localized.getString(R.string.reader_advanced)).assertIsDisplayed()
        capture("sanctuary-appearance-advanced-fa-200-contrast")
    }

    @Test
    fun ttsMiniEnglishStandard() {
        val localized = localizedContext("en", 1f)
        present("en", 1f, highContrast = false) {
            val theme = ReaderTheme.DUSK
            val colors = readerAccessColors(theme)
            Box(Modifier.fillMaxSize().background(readerCanvasColor(theme))) {
                ReaderTtsMiniPlayer(
                    state = ReaderTtsState(ReaderTtsPhase.PLAYING),
                    activeText = "A narrow line of light remained across the register.",
                    speed = 1.2,
                    background = colors.background,
                    foreground = colors.foreground,
                    accent = colors.accent,
                    onPrevious = {},
                    onPause = {},
                    onResume = {},
                    onNext = {},
                    onExpand = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }

        compose.onNodeWithText(localized.getString(R.string.tts_title)).assertIsDisplayed()
        compose.onNode(hasContentDescription(localized.getString(R.string.tts_pause)))
            .assertHeightIsAtLeast(48.dp)
        capture("sanctuary-tts-mini-en-100-standard")
    }

    @Test
    fun ttsMiniPersianLargeContrast() {
        val localized = localizedContext("fa", 2f)
        present("fa", 2f, highContrast = true) {
            val theme = ReaderTheme.DUSK
            val colors = readerAccessColors(theme)
            Box(Modifier.fillMaxSize().background(readerCanvasColor(theme))) {
                ReaderTtsMiniPlayer(
                    state = ReaderTtsState(ReaderTtsPhase.PLAYING),
                    activeText = "خط باریکی از نور روی حاشیهٔ دفتر باقی مانده بود.",
                    speed = 1.2,
                    background = colors.background,
                    foreground = colors.foreground,
                    accent = colors.accent,
                    onPrevious = {},
                    onPause = {},
                    onResume = {},
                    onNext = {},
                    onExpand = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }

        compose.onNodeWithText(localized.getString(R.string.tts_title)).assertIsDisplayed()
        compose.onNode(hasContentDescription(localized.getString(R.string.tts_pause)))
            .assertHeightIsAtLeast(48.dp)
        capture("sanctuary-tts-mini-fa-200-contrast")
    }

    @Test
    fun previousLocationEnglishStandard() {
        previousLocationEvidence("en", 1f, highContrast = false)
        capture("sanctuary-previous-location-en-100-standard")
    }

    @Test
    fun previousLocationPersianLargeContrast() {
        previousLocationEvidence("fa", 2f, highContrast = true)
        capture("sanctuary-previous-location-fa-200-contrast")
    }

    private fun previousLocationEvidence(language: String, scale: Float, highContrast: Boolean) {
        val localized = localizedContext(language, scale)
        present(language, scale, highContrast) {
            val theme = ReaderTheme.DUSK
            val colors = readerAccessColors(theme)
            Box(Modifier.fillMaxSize().background(readerCanvasColor(theme))) {
                PreviousLocationChip(
                    returnLabel = localized.getString(R.string.reader_return),
                    locationLabel = localized.getString(R.string.reader_previous_location),
                    background = colors.background,
                    foreground = colors.foreground,
                    accent = colors.accent,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    onClick = {}
                )
            }
        }

        compose.onNodeWithText(localized.getString(R.string.reader_return)).assertIsDisplayed()
        compose.onNodeWithText(localized.getString(R.string.reader_previous_location)).assertIsDisplayed()
    }

    private fun localizedContext(language: String, scale: Float): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
            fontScale = scale
        })
    }

    private fun present(
        language: String,
        scale: Float,
        highContrast: Boolean,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        val localized = localizedContext(language, scale)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalResources provides localized.resources,
                LocalLayoutDirection provides if (language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(density.density, scale)
            ) {
                VeilTheme(
                    themeMode = AppThemeMode.DARK,
                    highContrastEnabled = highContrast
                ) {
                    Box(
                        Modifier
                            .width(320.dp)
                            .height(640.dp)
                            .background(VeilPalette.Ink)
                    ) {
                        content()
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.filesDir, "grayfog-review").apply { mkdirs() }
        val file = File(directory, "$name.png")
        file.outputStream().use { output ->
            check(
                compose.onRoot().captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, output)
            )
        }
        exportGrayfogCapture(file)
    }
}
