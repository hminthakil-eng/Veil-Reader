package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.review.GrayfogReviewFixtures
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Actual production controls with fictional records; native Skia, not phone acceptance. */
abstract class AppleReadingFlowFixture {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String) {
        compose.awaitGrayfogArtwork()
        val bitmap = compose.onNode(isRoot()).captureToImage().asAndroidBitmap()
        val file = File("build/outputs/grayfog-cloud/apple-reading-flow/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        file.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
    }

    protected fun checkResume(scale: Float, name: String) {
        val context = RuntimeEnvironment.getApplication()
        val books = GrayfogReviewFixtures.manyBooks
        val current = books.first()
        val mode = mutableStateOf(LibraryViewMode.GALLERY)
        var opened: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(AppThemeMode.LIGHT) {
                    key(mode.value) {
                        LibraryArchiveContent(books = books, isImporting = false,
                            onImportUri = {}, onOpenBook = { opened = it.id }, onFavorite = {},
                            onEditMetadata = {}, onDeleteBook = {}, onOpenSettings = {},
                            initialViewMode = mode.value)
                    }
                }
            }
        }
        for (view in LibraryViewMode.entries) {
            compose.runOnIdle { mode.value = view; opened = null }
            compose.awaitGrayfogArtwork()
            // A 48-book collection must not push Resume after its final entry.
            val resume = compose.onNodeWithContentDescription(
                context.getString(R.string.library_continue_book_semantics, current.title))
            resume.assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertHasClickAction().performClick()
            compose.runOnIdle { assertEquals(current.id, opened) }
            capture("$name-library-${view.name.lowercase()}")
            val search = compose.onNode(hasContentDescription(context.getString(R.string.library_search_hint))
                and hasSetTextAction())
            search.performScrollTo().performTextInput("Ledger")
            compose.awaitGrayfogArtwork()
            resume.assertDoesNotExist()
        }
    }

    protected fun checkCompactAppearance(scale: Float, name: String) {
        val context = RuntimeEnvironment.getApplication()
        val appearance = mutableStateOf(ReaderAppearance())
        var finished: ReaderAppearance? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(AppThemeMode.LIGHT) {
                    Box(Modifier.height(520.dp)) {
                        EpubAppearancePanel(appearance.value, false, ReaderFixedLayoutSpread.AUTO, null,
                            onSpreadChange = {}, onChange = { appearance.value = it },
                            onDone = { finished = it }, compactSheet = true,
                            modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        val finish = compose.onNodeWithText(context.getString(R.string.reader_back_to_reading))
        finish.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        capture("$name-appearance-quick")
        compose.onNodeWithText(context.getString(R.string.settings_reader_oled))
            .performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText(context.getString(R.string.reader_advanced)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.reader_reset_appearance)).performScrollTo()
        // Even the end of a long advanced form leaves the return action in place.
        finish.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        capture("$name-appearance-advanced-end")
        finish.performClick()
        compose.runOnIdle { assertEquals(ReaderTheme.OLED, finished?.theme) }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppleReadingFlowTest : AppleReadingFlowFixture() {
    @Test fun lightResumeIsFirstAndOpensTheSameBookInEveryView() = checkResume(1f, "en-light-100")
    @Test fun compactSettingsKeepReturnVisibleAndPreserveTheChosenTheme() = checkCompactAppearance(1f, "en-light-100")
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ApplePersianReadingFlowTest : AppleReadingFlowFixture() {
    @Test fun persianLargeResumeRemainsFirstInEveryView() = checkResume(2f, "fa-light-200")
    @Test fun persianLargeSettingsKeepReturnVisibleAfterScrolling() = checkCompactAppearance(2f, "fa-light-200")
}
