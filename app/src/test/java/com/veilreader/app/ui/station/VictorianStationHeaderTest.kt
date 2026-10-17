package com.veilreader.app.ui.station

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VictorianStationHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun hangingSignOpensActualLibraryCallback() {
        var opens = 0
        compose.setContent {
            VeilTheme(AppThemeMode.DARK) {
                VictorianStationHeader(
                    brand = "Veil Reader",
                    title = "Your reading sanctuary",
                    onOpenLibrary = { opens++ }
                )
            }
        }
        compose.onNodeWithText("Veil Reader").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun rtlAtTwoHundredPercentKeepsRealUnwarpedTextAndClick() {
        var opens = 0
        compose.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(current.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                VeilTheme(AppThemeMode.LIGHT, highContrastEnabled = true) {
                    VictorianStationHeader(
                        brand = "Veil Reader",
                        title = "به ایستگاه کتاب‌ها خوش آمدید",
                        onOpenLibrary = { opens++ }
                    )
                }
            }
        }
        compose.onNodeWithText("Veil Reader").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }
}
