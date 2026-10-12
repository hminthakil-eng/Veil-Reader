package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReaderNavigationModeSelectorTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun fourLargeTextChoicesRemainDistinctReachableAndSelected() {
        val selected = mutableStateOf(ReaderNavigationMode.PAGED)
        compose.setContent {
            val density = LocalDensity.current
            VeilTheme {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    Column(Modifier.width(280.dp).verticalScroll(rememberScrollState())) {
                        ReaderNavigationModeSelector(selected.value, scrollEnabled = true,
                            paperEnabled = true, onSelect = { selected.value = it })
                    }
                }
            }
        }
        for ((mode, label) in listOf(
            ReaderNavigationMode.PAPER_CURL to R.string.settings_mode_curl,
            ReaderNavigationMode.SLIDE to R.string.settings_mode_slide,
            ReaderNavigationMode.SCROLL to R.string.settings_mode_scroll,
            ReaderNavigationMode.PAGED to R.string.settings_mode_paged
        )) {
            compose.onNodeWithText(context.getString(label)).performScrollTo().performClick().assertIsSelected()
            compose.runOnIdle { assertEquals(mode, selected.value) }
        }
    }

    @Test fun unavailableModesCannotBeSelectedAndReducedMotionIsExplained() {
        var requests = 0
        compose.setContent {
            VeilTheme {
                CompositionLocalProvider(LocalVeilReducedMotion provides true) {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        ReaderNavigationModeSelector(ReaderNavigationMode.PAGED, scrollEnabled = false,
                            paperEnabled = false, slideEnabled = false, onSelect = { requests++ })
                    }
                }
            }
        }
        for (label in listOf(R.string.settings_mode_curl, R.string.settings_mode_slide, R.string.settings_mode_scroll)) {
            compose.onNodeWithText(context.getString(label)).performScrollTo().assertIsNotEnabled()
        }
        compose.onNodeWithText(context.getString(R.string.settings_mode_curl_reduced_motion_description))
            .assertExists()
        compose.runOnIdle { assertEquals(0, requests) }
    }
}
