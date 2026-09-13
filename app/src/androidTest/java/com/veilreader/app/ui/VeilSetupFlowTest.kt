package com.veilreader.app.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.MainActivity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.domain.AppPreferences
import com.veilreader.app.domain.AppTheme
import com.veilreader.app.domain.ReaderAppearance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise the real activity/repositories, in addition to isolated component render checks. */
@RunWith(AndroidJUnit4::class)
class VeilSetupFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun firstSetupPersistsQuietModeAndSettingsSurviveActivityRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)
        val original = store.settings.first()
        val database = VeilDatabase.get(context)
        database.clearAllTables()
        store.markLegacyLibraryImported()
        store.saveReaderAppearance(ReaderAppearance(reduceMotion = true))
        store.saveAppPreferences(AppPreferences(AppTheme.LIGHT))
        try {
            ActivityScenario.launch(MainActivity::class.java).use { activity ->
                compose.waitUntil(10_000) { compose.onAllNodesWithText("Skip setup").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Quiet reading").performScrollTo().performClick()
                compose.onNodeWithText("Open my library").performScrollTo().performClick()
                awaitSaved(store, "quiet setup") { it.onboardingCompleted && !it.gameVisible }
                compose.onNodeWithText("Castle").assertDoesNotExist()
                compose.onNodeWithText("Path").assertDoesNotExist()
                compose.onNodeWithText("Profile").performClick()
                compose.onNodeWithText("Settings & reading comfort").performScrollTo().performClick()
                compose.onNodeWithText("Quiet mode").performScrollTo().assertIsOn()
                compose.onNodeWithText("Dark").performScrollTo().performClick()
                awaitSaved(store, "dark theme") { it.appTheme == AppTheme.DARK }
                activity.recreate()
                compose.waitUntil(10_000) { compose.onAllNodesWithText("Quiet mode").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Quiet mode").performScrollTo().assertIsOn()
                compose.onNodeWithText("Dark").performScrollTo().assertIsSelected()
                compose.onNodeWithText("Quiet mode").performScrollTo().performClick()
                awaitSaved(store, "world re-enabled") { it.gameVisible }
                compose.onNodeWithText("Back").performClick()
                compose.onNodeWithText("Castle").assertIsDisplayed()
                compose.onNodeWithText("Path").assertIsDisplayed()
                compose.onNodeWithText("Skip setup").assertDoesNotExist()
            }
        } finally {
            store.restorePreferences(original.readerAppearance, original.appPreferences)
            database.clearAllTables()
        }
    }

    private suspend fun awaitSaved(store: SettingsStore, stage: String, predicate: (AppSettings) -> Boolean) {
        // A pointer action can return before Compose dispatches its click coroutine. Advance the
        // UI clock before waiting on IO, then require the durable DataStore value, not UI optimism.
        compose.waitForIdle()
        try {
            withTimeout(5000) { store.settings.first(predicate) }
        } catch (error: kotlinx.coroutines.TimeoutCancellationException) {
            throw AssertionError("Preference save timed out after $stage. Saved: ${store.settings.first().appPreferences}", error)
        }
    }
}
