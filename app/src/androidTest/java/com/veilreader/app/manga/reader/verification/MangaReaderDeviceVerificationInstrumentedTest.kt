package com.veilreader.app.manga.reader.verification

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import com.veilreader.app.manga.reader.MangaReaderVerificationTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaReaderDeviceVerificationInstrumentedTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun offlinePagedReader_survivesActivityRecreation_andRestoresPage() {
        launch(startPage = 1).use { scenario ->
            waitForPage(1)
            scenario.recreate()
            waitForPage(1)
            compose.onNodeWithTag(MangaReaderVerificationTags.ROOT).assertExists()
        }
    }

    @Test
    fun rtlAndLtrSwipesAdvanceInOppositeDirections() {
        launch(startPage = 1).use {
            waitForPage(1)
            compose.onNodeWithTag(MangaReaderVerificationTags.page(1))
                .performTouchInput { swipeRight() }
            waitForPage(2)
        }

        launch(startPage = 1).use {
            waitForPage(1)
            openChrome(1)
            compose.onNodeWithTag(MangaReaderVerificationTags.DIRECTION).performClick()
            compose.onNodeWithTag(MangaReaderVerificationTags.DIRECTION)
                .assertTextContains("LTR")
            compose.onNodeWithTag(MangaReaderVerificationTags.page(1))
                .performTouchInput { swipeLeft() }
            waitForPage(2)
        }
    }

    @Test
    fun rtlAndLtrEdgeTapsAdvanceFromOppositeEdges() {
        launch(startPage = 1).use {
            waitForPage(1)
            compose.onNodeWithTag(MangaReaderVerificationTags.page(1))
                .performTouchInput { click(percentOffset(0.1f, 0.5f)) }
            waitForPage(2)
        }

        launch(startPage = 1).use {
            waitForPage(1)
            openChrome(1)
            compose.onNodeWithTag(MangaReaderVerificationTags.DIRECTION).performClick()
            compose.onNodeWithTag(MangaReaderVerificationTags.page(1))
                .performTouchInput { click(percentOffset(0.9f, 0.5f)) }
            waitForPage(2)
        }
    }

    @Test
    fun pagedToWebtoonMode_survivesActivityRecreation() {
        launch(startPage = 1).use { scenario ->
            waitForPage(1)
            openChrome(1)

            compose.onNodeWithTag(MangaReaderVerificationTags.MODE).performClick()
            compose.onNodeWithTag(MangaReaderVerificationTags.MODE)
                .assertTextContains("Paged")
            settleComposeAndMainThread()

            scenario.recreate()
            settleComposeAndMainThread()
            waitForTag(MangaReaderVerificationTags.ROOT)
            compose.onNodeWithTag(MangaReaderVerificationTags.MODE)
                .assertTextContains("Paged")

            compose.onNodeWithTag(MangaReaderVerificationTags.MODE).performClick()
            compose.onNodeWithTag(MangaReaderVerificationTags.MODE)
                .assertTextContains("Webtoon")
        }
    }

    @Test
    fun durableOfflineProgress_reopensAfterActivityIsClosed() {
        launch(startPage = 0, resetProgress = true).use {
            waitForPage(0)
            compose.onNodeWithTag(MangaReaderVerificationTags.page(0))
                .performTouchInput { swipeRight() }
            waitForPage(1)

            // Backgrounding triggers the production immediate flush path, independent of debounce.
            it.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(300L)
        }

        launch(startPage = 0, resetProgress = false).use {
            waitForPage(1)
        }
    }

    @Test
    fun partialOfflineBoundary_blocksChapterTransition() {
        launch(startPage = 0, partialOffline = true).use {
            waitForPage(0)
            waitForTag(MangaReaderVerificationTags.PARTIAL_OFFLINE)

            compose.onNodeWithTag(MangaReaderVerificationTags.page(0))
                .performTouchInput { swipeRight() }
            waitForPage(1)

            compose.onNodeWithTag(MangaReaderVerificationTags.page(1))
                .performTouchInput { swipeRight() }

            val boundaryMessage =
                "Reconnect to load the rest of this chapter before continuing."
            compose.waitUntil(timeoutMillis = 5_000L) {
                compose.onAllNodesWithText(boundaryMessage)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(boundaryMessage).assertExists()
        }
    }

    @Test
    fun extremeLocalPage_usesSubsampling_andDoesNotLeakSwipeToReader() {
        launch(extreme = true, startPage = 0).use { scenario ->
            waitForPage(0)
            waitForTag(MangaReaderVerificationTags.SUBSAMPLING)

            compose.onNodeWithTag(MangaReaderVerificationTags.SUBSAMPLING)
                .performTouchInput { swipeRight() }
            compose.waitForIdle()

            waitForPage(0)
            assertTrue(
                compose.onAllNodesWithTag(MangaReaderVerificationTags.page(1))
                    .fetchSemanticsNodes().isEmpty()
            )

            compose.onNodeWithTag(MangaReaderVerificationTags.SUBSAMPLING)
                .performTouchInput { click() }
            waitForTag(MangaReaderVerificationTags.CHROME)

            // Rotation/recreation must rebuild the large-image renderer without an OOM/crash.
            scenario.recreate()
            waitForTag(MangaReaderVerificationTags.SUBSAMPLING)
        }
    }

    private fun settleComposeAndMainThread() {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        compose.waitForIdle()
    }

    private fun openChrome(page: Int) {
        compose.onNodeWithTag(MangaReaderVerificationTags.page(page))
            .performTouchInput { click() }
        waitForTag(MangaReaderVerificationTags.CHROME)
    }

    private fun waitForPage(index: Int) {
        waitForTag(MangaReaderVerificationTags.page(index))
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 15_000L) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tag).assertExists()
    }

    private fun launch(
        extreme: Boolean = false,
        partialOffline: Boolean = false,
        resetProgress: Boolean = true,
        startPage: Int = 0
    ): ActivityScenario<MangaReaderVerificationActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MangaReaderVerificationActivity::class.java).apply {
            putExtra(MangaReaderVerificationActivity.EXTRA_EXTREME, extreme)
            putExtra(MangaReaderVerificationActivity.EXTRA_PARTIAL_OFFLINE, partialOffline)
            putExtra(MangaReaderVerificationActivity.EXTRA_RESET_PROGRESS, resetProgress)
            putExtra(MangaReaderVerificationActivity.EXTRA_START_PAGE, startPage)
        }
        return ActivityScenario.launch(intent)
    }
}
