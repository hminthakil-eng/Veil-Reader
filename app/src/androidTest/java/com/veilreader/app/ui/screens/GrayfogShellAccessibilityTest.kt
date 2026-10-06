package com.veilreader.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivityResultRegistryOwner
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
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.review.GrayfogReviewContent
import com.veilreader.app.ui.review.GrayfogReviewFixtures
import com.veilreader.app.ui.review.GrayfogReviewSurface
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
            // Keep the real host's launcher ownership while localizing its resources.
            val activityResults = checkNotNull(LocalActivityResultRegistryOwner.current)
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides activityResults,
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
    fun liveSearchPreservesItsPurposeWhileOriginalArtworkLeavesTheResults() {
        val localized = localizedContext()
        val original = GrayfogReviewFixtures.booksWithOriginalCover(localized).first()
        assertTrue(File(checkNotNull(original.coverCachePath)).isFile)
        present {
            GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_GALLERY, highContrast = highContrast)
        }
        val label = localized.getString(R.string.library_search_hint)
        val search = compose.onNode(hasContentDescription(label) and hasSetTextAction())
        search.performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.waitUntil(20_000) {
            val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(BookCoverArtworkReady), useUnmergedTree = true)
                .fetchSemanticsNodes()
            nodes.isNotEmpty() && nodes.all { it.config[BookCoverArtworkReady] }
        }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        search.performTextInput("Still")
        search.assertTextContains("Still").assertIsDisplayed()
        compose.onNodeWithText(original.title).assertDoesNotExist()
        val remaining = GrayfogReviewFixtures.books.first { it.title.contains("Still") }
        // The editable query contains the same words as the matching book title.
        // Assert the publication record, not the text field that initiated retrieval.
        compose.onNode(hasText(remaining.title) and !hasSetTextAction()).assertExists()
        capture("archive-search")
    }

    @Test
    fun thresholdCopyDoesNotOverlapAndResumeRemainsReachable() {
        val localized = localizedContext()
        val book = Book(id = "grayfog-review", title = "The Unwritten Observatory", author = "Archive fixture",
            progress = 0.42f, sourceUri = "veil-review://fictional/grayfog-review")
        var opens = 0
        present {
            ReadingNowScreen(
                books = listOf(book), profile = SampleData.profile, quests = emptyList(),
                onOpenBook = { opens++ }, onOpenPassage = { _, _ -> },
                onOpenLibrary = {}, onOpenCastle = {}
            )
        }
        compose.onNodeWithText(localized.getString(R.string.threshold_title_first_volume))
            .assertExists()
            .assertIsDisplayed()
        // Returning compact users should not pay the recurring cinematic prose cost.
        // Large text follows the same rule without shrinking any user-visible text.
        compose.onNodeWithText(localized.getString(R.string.threshold_body_first_volume))
            .assertDoesNotExist()
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
        compose.onNodeWithText(localized.getString(R.string.threshold_body_unwritten))
            .assertExists()
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

    @Test
    fun hiddenArchivePopulatedStateHasDirectVisualEvidence() {
        val localized = localizedContext()
        val book = Book(
            id = "hidden-archive-review",
            title = if (language == "fa") "رصدخانهٔ مه‌آلود" else "The Veiled Observatory",
            author = if (language == "fa") "بایگان خاکستری" else "Grayfog archivist"
        )
        val note = if (language == "fa") {
            "یادداشتی که باید در بایگانی پنهان بدون ازدحام دیده شود."
        } else {
            "A note that must remain visible in the Hidden Archive without duplicate chrome."
        }
        val quote = if (language == "fa") {
            "مه روی رصدخانه آرام گرفت."
        } else {
            "The fog settled quietly over the observatory."
        }
        present {
            ArchiveRecordContent(
                books = listOf(book),
                highlights = listOf(
                    Highlight(
                        id = "hidden-archive-highlight",
                        bookId = book.id,
                        quote = quote,
                        locatorJson = "{\"href\":\"chapter-1\"}",
                        note = note
                    )
                ),
                bookmarks = listOf(
                    Bookmark(
                        id = "hidden-archive-bookmark",
                        bookId = book.id,
                        label = if (language == "fa") "نشان رصدخانه" else "Observatory mark",
                        locatorJson = "{\"href\":\"chapter-1\"}"
                    )
                ),
                readingSessions = emptyList(),
                readingCycles = emptyList(),
                passageVisits = emptyList(),
                onClose = {},
                onOpenPassage = { _, _ -> },
                onSaveNote = { _, _ -> },
                onDeleteHighlight = {},
                onDeleteBookmark = {}
            )
        }
        compose.onNodeWithText(localized.getString(R.string.archive_title))
            .assertExists()
            .assertIsDisplayed()
        compose.onNodeWithText(note)
            .performScrollTo()
            .assertExists()
            .assertIsDisplayed()
        capture("hidden-archive-populated")
    }

    @Test
    fun hiddenArchiveEmptyStateHasDirectVisualEvidence() {
        val localized = localizedContext()
        present {
            ArchiveRecordContent(
                books = emptyList(),
                highlights = emptyList(),
                bookmarks = emptyList(),
                readingSessions = emptyList(),
                readingCycles = emptyList(),
                passageVisits = emptyList(),
                onClose = {},
                onOpenPassage = { _, _ -> },
                onSaveNote = { _, _ -> },
                onDeleteHighlight = {},
                onDeleteBookmark = {}
            )
        }
        compose.onNodeWithText(localized.getString(R.string.archive_title))
            .assertExists()
            .assertIsDisplayed()
        capture("hidden-archive-empty")
    }

    private fun capture(surface: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.filesDir, "grayfog-review").apply { mkdirs() }
        val name = "$surface-$language-${(scale * 100).toInt()}-${if (highContrast) "contrast" else "standard"}.png"
        val file = File(directory, name)
        file.outputStream().use { output ->
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        com.veilreader.app.exportGrayfogCapture(file)
    }
}
