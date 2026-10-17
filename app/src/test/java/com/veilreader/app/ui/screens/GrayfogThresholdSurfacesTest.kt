package com.veilreader.app.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogThresholdSurfacesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun archivalHeaderKeepsActualLabelsLegibleInRtlAtDoubleFontScale() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                VeilTheme(AppThemeMode.DARK) {
                    GrayfogThresholdHeader(
                        brand = "THE GRAYFOG ARCHIVE",
                        title = "Your reading sanctuary"
                    )
                }
            }
        }
        compose.onNodeWithText("THE GRAYFOG ARCHIVE").assertIsDisplayed()
        compose.onNodeWithText("Your reading sanctuary").assertIsDisplayed()
    }

    @Test fun sectionHeadingRemainsVisibleWithLightColors() {
        compose.setContent {
            VeilTheme(AppThemeMode.LIGHT) {
                GrayfogSectionHeading("Recent volumes")
            }
        }
        compose.onNodeWithText("Recent volumes").assertIsDisplayed()
    }
}
