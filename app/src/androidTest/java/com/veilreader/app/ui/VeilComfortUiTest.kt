package com.veilreader.app.ui

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.domain.*
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.navigation.visibleTabs
import com.veilreader.app.ui.screens.SettingsScreen
import com.veilreader.app.ui.screens.WelcomeScreen
import com.veilreader.app.ui.theme.VeilTheme
import java.io.ByteArrayOutputStream
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VeilComfortUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun welcomeLetsAReaderChooseQuietAndProceedWithoutAnAccount() {
        var choice: Boolean? = null
        compose.setContent {
            VeilTheme(AppTheme.DARK, reduceMotion = true) {
                WelcomeScreen(true, onStart = { choice = it }, onSkip = {}, onRestore = {})
            }
        }
        compose.onNodeWithText("Skip setup").assertIsDisplayed()
        capture("welcome-dark")
        compose.onNodeWithText("Quiet reading").performScrollTo().performClick()
        compose.onNodeWithText("Quiet reading").assertIsSelected()
        compose.onNodeWithText("Open my library").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(false, choice) }
    }

    @Test fun settingsChangesThemeQuietModeAndEpubStyleAndConfirmsRestore() {
        var prefs by mutableStateOf(AppPreferences(AppTheme.LIGHT, onboardingCompleted = true))
        var appearance by mutableStateOf(ReaderAppearance(reduceMotion = true))
        var section by mutableStateOf("general")
        compose.setContent {
            VeilTheme(prefs.theme, appearance.reduceMotion) {
                SettingsScreen(prefs, appearance, section, 20, false, false,
                    onSection = { section = it }, onPreferences = { prefs = it }, onAppearance = { appearance = it },
                    onDailyGoal = {}, onExportBackup = {}, onRestoreBackup = {}, onExportNotes = {}, onShowWelcome = {}, onClose = {})
            }
        }
        compose.onNodeWithText("Quiet mode").performScrollTo().performClick().assertIsOn()
        compose.runOnIdle { assertFalse(prefs.gameVisible) }
        capture("settings-light")
        compose.onNodeWithText("Dark").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(AppTheme.DARK, prefs.theme) }
        compose.onNodeWithText("General").performClick()
        capture("settings-dark")
        compose.onNodeWithText("Reading").performClick()
        compose.onNodeWithText("Make the page yours").assertIsDisplayed()
        capture("reading-preview")
        compose.onNodeWithText("Scroll").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertTrue(appearance.scroll) }
        compose.onNodeWithText("3D curl").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertFalse(appearance.scroll); assertEquals(PageTurnStyle.CURL, appearance.pageTurnStyle) }
        compose.onNodeWithContentDescription("Text size").performScrollTo().assertExists()
        compose.onNodeWithText("Backups & data").performClick()
        compose.onNodeWithText("Restore library backup").performScrollTo().performClick()
        compose.onNodeWithText("Replace this library?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Replace this library?").assertDoesNotExist()
    }

    @Test fun largeTextSettingsAndQuietDockKeepEssentialActionsReachable() {
        var settings by mutableStateOf(true)
        var selected by mutableStateOf(VeilTab.READING)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                VeilTheme(AppTheme.LIGHT, reduceMotion = true) {
                    if (settings) {
                        SettingsScreen(AppPreferences(), ReaderAppearance(), "general", 20, false, false,
                            onSection = {}, onPreferences = {}, onAppearance = {}, onDailyGoal = {},
                            onExportBackup = {}, onRestoreBackup = {}, onExportNotes = {}, onShowWelcome = {},
                            onClose = { settings = false })
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Spacer(Modifier.weight(1f))
                            VeilBottomDock(selected, { selected = it }, tabs = visibleTabs(false))
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Quiet mode").performScrollTo().assertIsDisplayed()
        capture("settings-large-text")
        compose.onNodeWithText("Revisit the welcome guide").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Castle").assertDoesNotExist()
        compose.onNodeWithText("Path").assertDoesNotExist()
        compose.onNodeWithText("Profile").assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithText("Library").assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithText("Reading").assertIsDisplayed()
    }

    private fun capture(name: String) {
        if (Build.VERSION.SDK_INT < 31) return // Older devices still run every interaction assertion.
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Device screenshot unavailable: $name" }
        val bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        bitmap.recycle()
        fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)
        ).bufferedReader().use { it.readText() }
        // Preserve only synthetic test captures outside the app's uninstall lifetime. The API's
        // stdin/stdout descriptors transfer exact bytes without log truncation or shell quoting.
        shell("mkdir -p /data/local/tmp/veil-ui-previews")
        val destination = "/data/local/tmp/veil-ui-previews/$name.png"
        val pipes = instrumentation.uiAutomation.executeShellCommandRw("dd of=$destination")
        ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use { it.write(bytes) }
        ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
        val copiedBytes = shell("wc -c $destination").trim().substringBefore(' ').toLong()
        assertEquals("Screenshot copy must be complete", bytes.size.toLong(), copiedBytes)
    }
}
