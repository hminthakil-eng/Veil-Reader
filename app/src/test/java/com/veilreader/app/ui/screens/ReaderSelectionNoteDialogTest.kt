package com.veilreader.app.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
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

/** Actual native Dialog with fictional drafts; this is not IME or device acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderSelectionNoteDialogTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun lightNewDraftRequiresTextAndPreservesExactUnicode() =
        checkDialog(isNewNote = true, mode = AppThemeMode.LIGHT,
            scale = 1f, captureName = "selection-note-en-light")

    @Test fun darkExistingAnnotationCanBeCleared() =
        checkDialog(isNewNote = false, mode = AppThemeMode.DARK,
            scale = 1f, captureName = "selection-note-en-dark")

    // A 260dp native window leaves at most 220dp after the production 20dp gutters,
    // before insets and content padding. No keyboard simulation is claimed.
    @Test @Config(qualifiers = "fa-rIR-w720dp-h260dp-mdpi")
    fun shortPersianWindowAtTwoHundredPercentKeepsActionsReachable() =
        checkDialog(isNewNote = true, mode = AppThemeMode.LIGHT,
            scale = 2f, captureName = "selection-note-fa-short-200")

    private fun checkDialog(isNewNote: Boolean, mode: AppThemeMode,
        scale: Float, captureName: String) {
        val quote = "A fictional selected passage · ردّی از نور در حاشیهٔ دفتر. ".repeat(12)
        val note = mutableStateOf("")
        val saving = mutableStateOf(false)
        val error = mutableStateOf<Int?>(null)
        var saved: String? = null
        var cancels = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(mode) {
                    ReaderSelectionNoteDialog(
                        quote = quote, note = note.value,
                        onNoteChange = { note.value = it; error.value = null },
                        saving = saving.value,
                        saveEnabled = canSavePendingSelectionNote(isNewNote, note.value),
                        errorRes = error.value,
                        onCancel = { cancels++ },
                        onSave = { saved = note.value; saving.value = true }
                    )
                }
            }
        }
        compose.onNode(isDialog()).assertExists().assertIsDisplayed()
        val save = compose.onNodeWithText(context.getString(R.string.common_save))
        val cancel = compose.onNodeWithText(context.getString(R.string.common_cancel))
        val input = compose.onNodeWithTag("reader_note_editor_input")
        input.assert(hasSetTextAction())
        save.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        cancel.assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(48.dp)
        if (isNewNote) {
            save.assertIsNotEnabled()
            input.performScrollTo().performTextReplacement("   ")
            save.assertIsNotEnabled()
        } else {
            save.assertIsEnabled().performClick()
            compose.runOnIdle { assertEquals("", saved); saving.value = false }
        }

        input.performScrollTo().performTextReplacement("A long margin note · یادداشت بلند ".repeat(30))
        save.assertIsDisplayed().assertIsEnabled()
        cancel.assertIsDisplayed()
        capture(captureName)

        val exactNote = "  Exact note\nمتن دقیق؛ نیم\u200cفاصله و 👩🏽‍💻  "
        input.performScrollTo().performTextReplacement(exactNote)
        save.assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(exactNote, saved) }
        input.assertIsNotEnabled()
        cancel.assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.reader_notebook_saving))
            .assertIsDisplayed().assertIsNotEnabled()

        // A failed write leaves the draft in this same dialog and exposes a live error.
        compose.runOnIdle {
            saving.value = false
            error.value = R.string.reader_note_save_failed
        }
        compose.onNodeWithText(context.getString(R.string.reader_note_save_failed))
            .performScrollTo().assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNode(isDialog()).assertExists()
        save.assertIsDisplayed().assertIsEnabled()
        cancel.assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle { assertEquals(exactNote, note.value) }
        input.performScrollTo().performTextReplacement("Retry · تلاش دوباره")
        compose.onNodeWithText(context.getString(R.string.reader_note_save_failed)).assertDoesNotExist()
        cancel.performClick()
        compose.runOnIdle { assertEquals(1, cancels) }
    }

    private fun capture(name: String) {
        val bitmap = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        val file = File("build/outputs/grayfog-cloud/apple-reading-tools/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        file.outputStream().use {
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
