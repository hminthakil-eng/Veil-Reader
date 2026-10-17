package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual evidence fixture. These are genuine Compose-rendered pixels in Robolectric,
 * NOT screenshots from a physical Android device and NOT real user library content.
 * They must never be used to claim production visual acceptance.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogRebornRenderCaptureTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureEmptyArchiveDark() {
        compose.setContent {
            VeilTheme(AppThemeMode.DARK) {
                RebornThresholdScreen(
                    books = emptyList(),
                    onOpenBook = {},
                    onOpenLibrary = {},
                    onOpenCastle = {}
                )
            }
        }
        saveRoot("empty-archive-dark-390dp.png")
    }

    @Test fun captureReadingHeroDark() {
        val sample = Book(
            "fixture-hero",
            "The Mysterious Archive",
            "Example Author",
            sourceUri = "file:///test-fixture.epub",
            progress = 0.42f,
            currentChapter = "Chapter Twelve",
            lastOpenedAtEpochMs = 100L
        )
        compose.setContent {
            VeilTheme(AppThemeMode.DARK) {
                RebornThresholdScreen(
                    books = listOf(sample),
                    onOpenBook = {},
                    onOpenLibrary = {},
                    onOpenCastle = {},
                    showWorldEntry = false
                )
            }
        }
        saveRoot("reading-hero-dark-390dp.png")
    }

    @Test fun captureReadingHeroLightRtlLargeText() {
        val sample = Book(
            "fixture-rtl",
            "دفترچه رازهای مه آلود",
            "نویسنده آزمایشی",
            sourceUri = "file:///test-fixture-rtl.epub",
            progress = 0.31f,
            currentChapter = "فصل دوازدهم",
            lastOpenedAtEpochMs = 100L
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                VeilTheme(AppThemeMode.LIGHT) {
                    RebornThresholdScreen(
                        books = listOf(sample),
                        onOpenBook = {},
                        onOpenLibrary = {},
                        onOpenCastle = {},
                        showWorldEntry = false
                    )
                }
            }
        }
        saveRoot("reading-hero-light-rtl-200-percent-390dp.png")
    }

    private fun saveRoot(fileName: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File("build/reports/grayfog/compose-render-fixtures")
        assertTrue("Could not create screenshot directory", directory.isDirectory || directory.mkdirs())
        val output = File(directory, fileName)
        FileOutputStream(output).use { stream ->
            assertTrue("Bitmap encoding failed", bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertTrue("Screenshot is empty", output.length() > 1024L)
    }
}
