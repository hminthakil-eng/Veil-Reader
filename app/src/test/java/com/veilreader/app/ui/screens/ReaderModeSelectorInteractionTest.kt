package com.veilreader.app.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.PageTurnStyle
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
@Config(sdk = [35], qualifiers = "fa-rIR-w320dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderModeSelectorInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unavailablePaperDoesNotDispatchButEnabledSlideDoes() {
        val selected = mutableListOf<PageTurnStyle>()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                VeilTheme(AppThemeMode.DARK) {
                    ReaderPageTurnSelector(
                        selected = PageTurnStyle.SLIDE,
                        paperEnabled = false,
                        onSelect = { selected += it }
                    )
                }
            }
        }
        val context = RuntimeEnvironment.getApplication()
        compose.runOnIdle { assertEquals(emptyList<PageTurnStyle>(), selected) }
        compose.onNodeWithText(context.getString(R.string.settings_mode_curl))
            .assertIsNotEnabled()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(emptyList<PageTurnStyle>(), selected) }
        compose.onNodeWithText(context.getString(R.string.settings_mode_slide))
            .assertIsSelected()
            .performClick()
        compose.runOnIdle { assertEquals(listOf(PageTurnStyle.SLIDE), selected) }
    }
}
