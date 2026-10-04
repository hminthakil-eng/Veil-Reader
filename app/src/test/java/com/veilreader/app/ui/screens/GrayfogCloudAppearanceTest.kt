package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogCloudAppearanceTest {
    @get:Rule val compose = createComposeRule()

    private fun checkPreviewDisclosure() {
        var changes = 0
        var commits = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                VeilTheme(AppThemeMode.DARK) {
                    EpubAppearancePanel(ReaderAppearance(), false, ReaderFixedLayoutSpread.AUTO, null,
                        onSpreadChange = {}, onChange = { changes++ }, onDone = { commits++ },
                        modifier = Modifier.fillMaxSize())
                }
            }
        }
        val context = RuntimeEnvironment.getApplication()
        compose.onNodeWithText(context.getString(R.string.reader_show_reading_preview))
            .performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText(context.getString(R.string.reader_hide_reading_preview))
            .performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.reader_show_reading_preview)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, changes); assertEquals(0, commits) }
    }

    @Test @Config(qualifiers = "en-w320dp-h720dp-mdpi")
    fun largeTextPreviewDisclosureDoesNotMutateReadingPreferences() = checkPreviewDisclosure()

    @Test @Config(qualifiers = "fa-rIR-w900dp-h420dp-mdpi")
    fun shortPersianWindowKeepsPreviewOptionalAndReachable() = checkPreviewDisclosure()
}
