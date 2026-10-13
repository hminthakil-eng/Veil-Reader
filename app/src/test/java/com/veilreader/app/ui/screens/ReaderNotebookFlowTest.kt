@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.domain.*
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
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.search.*
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/** Fictional native production compositions; neither book rendering nor phone acceptance. */
abstract class ReaderNotebookFlowFixture {
    @get:Rule val compose = createComposeRule()
    private val context get() = RuntimeEnvironment.getApplication()

    private fun capture(name: String) {
        val bitmap = compose.onNode(isRoot()).captureToImage().asAndroidBitmap()
        val file = File("build/outputs/grayfog-cloud/apple-reading-tools/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        file.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private fun chapterTitle(index: Int): String =
        if (context.resources.configuration.locales[0].language == "fa") "فصل $index · دفتر اتاق‌های خاموش"
        else "Chapter $index · The quiet register"

    private class FictionalSearch : SearchService {
        override val options = SearchService.Options()
        var closes = 0
        override suspend fun search(query: String, options: SearchService.Options?): SearchIterator = object : SearchIterator {
            var emitted = false
            override suspend fun next(): SearchTry<LocatorCollection?> {
                if (emitted) return Try.success(null)
                emitted = true
                return Try.success(LocatorCollection(locators = List(83) { index -> Locator(
                    href = Url("chapter80.xhtml")!!, mediaType = MediaType.XHTML,
                    title = "Fixture match $index", locations = Locator.Locations(position = index + 1),
                    text = Locator.Text(before = "The archive kept ", highlight = query, after = " beside the western register.")
                ) }))
            }
            override fun close() { closes++ }
        }
    }

    protected fun checkChapterAndSearch(scale: Float, height: Int, mode: AppThemeMode, name: String, highContrast: Boolean = false) {
        val search = FictionalSearch()
        val links = (0 until 100).map { Link(Url("chapter$it.xhtml")!!, MediaType.XHTML, chapterTitle(it)) }
        val publication = Publication(Manifest(metadata = Metadata(identifier = "fixture"),
            readingOrder = links, tableOfContents = links), servicesBuilder = Publication.ServicesBuilder(search = { search }))
        val opened = OpenedPublication(Book("fixture", "دفتر مه · The fog register", "Fictional author"), publication, BookFormat.EPUB, null)
        var chosen: Link? = null
        var result: Locator? = null
        var savedPassageCalls = 0
        var dismissed = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(mode, highContrastEnabled = highContrast) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        ReaderNotebookPanel(opened, "fixture-session", "chapter80.xhtml#anchor",
                            emptyList(), emptyList(), emptyList(), onDismiss = { dismissed++ },
                            onGo = { savedPassageCalls++ }, onChapter = { chosen = it }, onSearchResult = { result = it },
                            onSaveNote = { _, _ -> }, onDeleteHighlight = {}, onDeleteBookmark = {},
                            modifier = Modifier.fillMaxWidth().height(height.dp))
                    }
                }
            }
        }
        val exit = compose.onNodeWithContentDescription(context.getString(R.string.reader_back_to_reading))
        exit.assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText(chapterTitle(80)).assertIsDisplayed().assertIsSelected()
        capture("$name-contents-current")
        val filter = compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.reader_notebook_search_chapters)))
        filter.performTextReplacement(chapterTitle(99))
        // The editable query also contains this title. Target the actual chapter action.
        compose.onNode(hasText(chapterTitle(99)) and hasClickAction() and !hasSetTextAction())
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(links[99], chosen) }
        filter.performTextReplacement("no such chapter")
        compose.onNodeWithText(context.getString(R.string.reader_notebook_no_matching_chapters)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.reader_notebook_show_current)).performClick()
        compose.onNodeWithText(chapterTitle(80)).assertIsDisplayed()
        // In short windows all the same sections remain available in the native menu.
        if (compose.onAllNodesWithText(context.getString(R.string.reader_notebook_tab_search)).fetchSemanticsNodes().isEmpty()) compose.onNodeWithText(context.getString(R.string.reader_notebook_tab_contents)).performClick()
        compose.onNodeWithText(context.getString(R.string.reader_notebook_tab_search)).performClick()
        compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.reader_search_query))).performTextInput("a")
        compose.onNodeWithText(context.getString(R.string.reader_search_submit)).performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Fixture match 0").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Fixture match 0").performScrollTo().assertIsDisplayed()
        capture("$name-search")
        exit.assertIsDisplayed()
        compose.onNodeWithText("Fixture match 0").performClick()
        compose.runOnIdle {
            assertEquals(1, result?.locations?.position)
            assertEquals(0, savedPassageCalls)
            assertEquals(1, search.closes) // Closing a partially consumed 83-result cursor.
        }
        exit.performClick()
        compose.runOnIdle { assertEquals(1, dismissed) }
        publication.close()
    }

    protected fun checkNoteFooter(scale: Float, mode: AppThemeMode, name: String, highContrast: Boolean = false, height: Int = 340) {
        val quote = "ردّی از نور در حاشیهٔ دفتر باقی مانده بود. ".repeat(12)
        val highlight = Highlight("note", "fixture", quote, "{}", createdAtEpochMs = 0)
        val note = mutableStateOf("A long note · یادداشت بلند ".repeat(30))
        val saving = mutableStateOf(false)
        var saved: String? = null
        var cancels = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(mode, highContrastEnabled = highContrast) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        ReaderNoteEditorContent(highlight, note.value, onNoteChange = { note.value = it },
                            saving = saving.value, errorRes = R.string.reader_notebook_note_save_failed,
                            onCancel = { cancels++ }, onSave = { saved = note.value },
                            modifier = Modifier.fillMaxWidth().height(height.dp))
                    }
                }
            }
        }
        val save = compose.onNodeWithText(context.getString(R.string.common_save))
        val cancel = compose.onNodeWithText(context.getString(R.string.common_cancel))
        save.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        cancel.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Exact note · متن دقیق")
        save.assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Exact note · متن دقیق", saved) }
        cancel.performClick()
        compose.runOnIdle { assertEquals(1, cancels); saving.value = true }
        cancel.assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.reader_notebook_saving)).assertIsNotEnabled()
        compose.runOnIdle { saving.value = false }
        capture("$name-note-editor")
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderNotebookFlowTest : ReaderNotebookFlowFixture() {
    @Test fun lightChaptersAndCanonicalSearch() = checkChapterAndSearch(1f, 680, AppThemeMode.LIGHT, "en-light-100")
    @Test fun lightNoteActionsStayVisible() = checkNoteFooter(1f, AppThemeMode.LIGHT, "en-light-100")
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderNotebookPersianFlowTest : ReaderNotebookFlowFixture() {
    @Test fun persianLargeChaptersAndSearch() = checkChapterAndSearch(2f, 680, AppThemeMode.LIGHT, "fa-light-200")
    @Test fun persianLargeNoteActionsStayVisible() = checkNoteFooter(2f, AppThemeMode.LIGHT, "fa-light-200")
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fa-rIR-w720dp-h360dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderNotebookShortWindowTest : ReaderNotebookFlowFixture() {
    @Test fun shortWindowKeepsAllSectionsAndCurrentChapter() = checkChapterAndSearch(2f, 340, AppThemeMode.LIGHT, "fa-short-200")
    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun mediumHeightLargeTextKeepsTheChapterViewport() = checkChapterAndSearch(2f, 520, AppThemeMode.LIGHT, "fa-medium-200")
    @Test fun shortLandscapeNoteInputAndActionsRemainReachable() = checkNoteFooter(2f, AppThemeMode.LIGHT, "fa-short-note-200", height = 220)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w800dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderNotebookDarkFlowTest : ReaderNotebookFlowFixture() {
    @Test fun darkExpandedChaptersAndSearch() = checkChapterAndSearch(1f, 680, AppThemeMode.DARK, "en-dark-wide")
    @Test fun highContrastNotesStayLegible() = checkNoteFooter(2f, AppThemeMode.DARK, "en-dark-high-200", highContrast = true)
}
