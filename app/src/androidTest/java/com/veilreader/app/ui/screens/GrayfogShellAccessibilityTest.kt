package com.veilreader.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
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
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Device evidence harness. Compilation does not establish that these tests have run. */
@RunWith(Parameterized::class)
class GrayfogShellAccessibilityTest(
    private val language: String,
    private val scale: Float,
    private val highContrast: Boolean
) {
    @get:Rule val compose = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} text={1} contrast={2}")
        fun cases(): List<Array<Any>> = listOf("en", "fa").flatMap { language ->
            listOf(1f, 1.3f, 1.5f, 2f).flatMap { scale ->
                listOf(false, true).map { contrast -> arrayOf<Any>(language, scale, contrast) }
            }
        }
    }

    private fun localizedContext(): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
            fontScale = scale
        })
    }

    private fun present(content: @androidx.compose.runtime.Composable () -> Unit) {
        val localized = localizedContext()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalResources provides localized.resources,
                LocalConfiguration provides localized.resources.configuration,
                LocalLayoutDirection provides if (language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(density.density, scale)
            ) {
                VeilTheme(themeMode = AppThemeMode.DARK, highContrastEnabled = highContrast) {
                    CompositionLocalProvider(LocalVeilReducedMotion provides true) {
                        Box(Modifier.width(320.dp).height(640.dp).background(VeilPalette.Ink)) {
                            content()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun thresholdCopyDoesNotOverlapAndResumeRemainsReachable() {
        val localized = localizedContext()
        val book = Book(id = "grayfog-review", title = "The Unwritten Observatory", author = "Archive fixture", progress = 0.42f)
        var opens = 0
        present {
            ReadingNowScreen(
                books = listOf(book), profile = SampleData.profile, quests = emptyList(),
                onOpenBook = { opens++ }, onOpenPassage = { _, _ -> },
                onOpenLibrary = {}, onOpenCastle = {}
            )
        }
        val title = compose.onNodeWithText(localized.getString(R.string.threshold_title_first_volume)).fetchSemanticsNode()
        val body = compose.onNodeWithText(localized.getString(R.string.threshold_body_first_volume)).fetchSemanticsNode()
        assertTrue("Editorial copy must occupy separate vertical space", title.boundsInRoot.bottom <= body.boundsInRoot.top)
        capture("threshold-entrance")
        compose.onNodeWithText(localized.getString(R.string.threshold_return_volume))
            .performScrollTo().assertIsDisplayed().assertHasClickAction()
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, opens) }
        capture("threshold-resume")
    }

    @Test
    fun emptyThresholdKeepsLibraryActionReachable() {
        val localized = localizedContext()
        var libraries = 0
        present {
            ReadingNowScreen(
                books = emptyList(), profile = SampleData.profile, quests = emptyList(),
                onOpenBook = {}, onOpenPassage = { _, _ -> },
                onOpenLibrary = { libraries++ }, onOpenCastle = {}
            )
        }
        compose.onNodeWithText(localized.getString(R.string.threshold_enter_library))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, libraries) }
        capture("threshold-empty")
    }

    @Test
    fun indexActionsKeepTheirTargetsAndDoNotOpenTheBook() {
        val localized = localizedContext()
        val book = Book(
            id = "index-review",
            title = if (language == "fa") "یادداشت‌های رصدخانه در کتابخانهٔ خاکستری" else "Records of the Observatory in the Grayfog Archive",
            author = if (language == "fa") "پژوهشگر بایگانی" else "The archive researcher",
            progress = 0.42f, seriesName = "A long archival series"
        )
        var opens = 0
        var favorites = 0
        var records = 0
        present {
            Box(Modifier.fillMaxSize()) {
                BookLibraryRow(
                    book, archiveMemory = null, artifactMemory = null, showMemorySummary = true,
                    onOpen = { opens++ }, onFavorite = { favorites++ }, onDetails = { records++ }
                )
            }
        }
        compose.onNodeWithText(book.title).assertIsDisplayed()
        compose.onNodeWithContentDescription(localized.getString(R.string.library_add_favorite_semantics, book.title))
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription(localized.getString(R.string.library_archive_record_semantics, book.title))
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle {
            assertEquals(0, opens)
            assertEquals(1, favorites)
            assertEquals(1, records)
        }
        capture("archive-index")
    }

    @Test
    fun galleryUtilitiesRemainReachableWithoutOpeningTheVolume() {
        val localized = localizedContext()
        val book = Book(
            id = "gallery-review",
            title = if (language == "fa") "دفتر رصدخانه و خاطره‌های بایگانی" else "The Observatory and Its Preserved Records",
            author = if (language == "fa") "پژوهشگر بایگانی" else "Archive researcher", progress = 0.42f
        )
        var opens = 0
        var favorites = 0
        var records = 0
        present {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                BookLibraryTile(book, null, null, onOpen = { opens++ },
                    onFavorite = { favorites++ }, onDetails = { records++ })
            }
        }
        compose.onNodeWithContentDescription(localized.getString(R.string.library_book_details_semantics, book.title))
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription(localized.getString(R.string.library_add_favorite_semantics, book.title))
            .performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle {
            assertEquals(0, opens)
            assertEquals(1, records)
            assertEquals(1, favorites)
        }
        capture("archive-gallery")
    }

    private fun capture(surface: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.filesDir, "grayfog-review").apply { mkdirs() }
        val name = "$surface-$language-${(scale * 100).toInt()}-${if (highContrast) "contrast" else "standard"}.png"
        File(directory, name).outputStream().use { output ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
