package com.veilreader.app.manga.hub.verification

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.manga.hub.MangaHubVerificationTags
import com.veilreader.app.manga.reader.MangaReaderVerificationTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaHubVerticalSliceInstrumentedTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun discoverAddLibraryAndRecreationKeepCanonicalWork() {
        launch(resetLibrary = true).use { scenario ->
            waitForTag(
                MangaHubVerificationTags.catalog(
                    "fixture.local",
                    "ink-dragon"
                )
            )

            compose.onNodeWithTag(
                MangaHubVerificationTags.catalog(
                    "fixture.local",
                    "ink-dragon"
                )
            ).performClick()

            waitForTag(MangaHubVerificationTags.DETAILS)
            compose.onNodeWithTag(MangaHubVerificationTags.ADD).performClick()
            waitForTag(MangaHubVerificationTags.REMOVE)

            scenario.recreate()

            waitForTag(MangaHubVerificationTags.ROOT)
            compose.onNodeWithTag(MangaHubVerificationTags.LIBRARY).performClick()
            waitForText("Ink Dragon")
            compose.onNodeWithText("Ink Dragon").assertExists()
        }
    }

    @Test
    fun searchUsesSourceSdkAndOpensDetails() {
        launch(resetLibrary = true).use {
            waitForTag(MangaHubVerificationTags.ROOT)
            compose.onNodeWithTag(MangaHubVerificationTags.SEARCH).performClick()
            waitForTag(MangaHubVerificationTags.SEARCH_FIELD)

            compose.onNodeWithTag(MangaHubVerificationTags.SEARCH_FIELD)
                .performTextInput("pharmacy")
            compose.onNodeWithTag(MangaHubVerificationTags.SEARCH_ACTION)
                .performClick()

            val resultTag = MangaHubVerificationTags.catalog(
                "fixture.local",
                "midnight-pharmacy"
            )
            waitForTag(resultTag)
            compose.onNodeWithTag(resultTag).performClick()

            waitForTag(MangaHubVerificationTags.DETAILS)
            compose.onNodeWithText("Midnight Pharmacy").assertExists()
            compose.onNodeWithText("4 chapters").assertExists()
        }
    }

    @Test
    fun chapterTapLaunchesReaderAtSelectedChapter() {
        launch(resetLibrary = true).use {
            val resultTag = MangaHubVerificationTags.catalog(
                "fixture.local",
                "ink-dragon"
            )
            waitForTag(resultTag)
            compose.onNodeWithTag(resultTag).performClick()

            val chapterTag = MangaHubVerificationTags.chapter(
                "fixture.local",
                "ink-dragon-chapter-2"
            )
            waitForTag(chapterTag)
            compose.onNodeWithTag(chapterTag).performClick()

            waitForTag(
                MangaReaderVerificationTags.chapter(
                    "fixture.local",
                    "ink-dragon-chapter-2"
                )
            )
        }
    }

    @Test
    fun readAndSaveLaunchesIntegratedReaderAndBackReturnsToDetails() {
        launch(resetLibrary = true).use { scenario ->
            val resultTag = MangaHubVerificationTags.catalog(
                "fixture.local",
                "ink-dragon"
            )
            waitForTag(resultTag)
            compose.onNodeWithTag(resultTag).performClick()
            waitForTag(MangaHubVerificationTags.READ)

            compose.onNodeWithTag(MangaHubVerificationTags.READ).performClick()

            waitForTag(MangaReaderVerificationTags.page(0))

            scenario.onActivity {
                it.onBackPressedDispatcher.onBackPressed()
            }

            waitForTag(MangaHubVerificationTags.DETAILS)
            compose.onNodeWithTag(MangaHubVerificationTags.REMOVE).assertExists()
        }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 15_000L) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 15_000L) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tag).assertExists()
    }

    private fun launch(
        resetLibrary: Boolean
    ): ActivityScenario<MangaHubVerificationActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MangaHubVerificationActivity::class.java).apply {
            putExtra(
                MangaHubVerificationActivity.EXTRA_RESET_LIBRARY,
                resetLibrary
            )
        }
        return ActivityScenario.launch(intent)
    }
}
