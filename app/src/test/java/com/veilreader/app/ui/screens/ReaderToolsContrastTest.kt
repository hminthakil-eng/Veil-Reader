package com.veilreader.app.ui.screens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Actual semantic palette emitted by VeilTheme, including both high-contrast variants. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderToolsContrastTest {
    @get:Rule val compose = createComposeRule()

    @Test fun enabledTextRolesMeetContrastOnOpaqueToolSurfaces() {
        val mode = mutableStateOf(AppThemeMode.LIGHT)
        val high = mutableStateOf(false)
        var actual: ColorScheme? = null
        compose.setContent { VeilTheme(mode.value, highContrastEnabled = high.value) {
            actual = MaterialTheme.colorScheme
        } }
        for (appearance in listOf(AppThemeMode.LIGHT, AppThemeMode.DARK)) {
            for (contrast in listOf(false, true)) {
                compose.runOnIdle { mode.value = appearance; high.value = contrast }
                compose.runOnIdle {
                    val colors = requireNotNull(actual)
                    for ((label, foreground) in listOf("text" to colors.onSurface,
                        "metadata" to colors.onSurfaceVariant, "action" to colors.primary)) {
                        for (background in listOf(colors.surface, colors.surfaceVariant)) {
                            val ratio = ratio(foreground, background)
                            assertTrue("$appearance high=$contrast $label ratio=$ratio", ratio >= 4.5)
                        }
                    }
                    assertTrue("$appearance primary button", ratio(colors.onPrimary, colors.primary) >= 4.5)
                }
            }
        }
    }

    private fun ratio(foreground: Color, background: Color): Double {
        val a = foreground.luminance().toDouble()
        val b = background.luminance().toDouble()
        return (maxOf(a, b) + .05) / (minOf(a, b) + .05)
    }
}
