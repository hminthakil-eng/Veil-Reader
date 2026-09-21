package com.veilreader.app.manga.reader.verification

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.veilreader.app.manga.reader.MangaReaderVerificationTags
import java.io.FileInputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaReaderProcessDeathInstrumentedTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun durableOfflineProgress_survivesRealProcessDeath() {
        val scenario = launch(resetProgress = true, startPage = 0)
        waitForPage(0)

        compose.onNodeWithTag(MangaReaderVerificationTags.page(0))
            .performTouchInput { swipeRight() }
        waitForPage(1)

        scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1_000L)

        val pidBeforeKill = shell("pidof com.veilreader.app").trim()
        assertTrue("target app process should exist before OS kill", pidBeforeKill.isNotEmpty())

        shell("am kill com.veilreader.app")
        waitForTargetProcessToExit()

        launch(resetProgress = false, startPage = 0).use {
            waitForPage(1)
            compose.onNodeWithTag(MangaReaderVerificationTags.ROOT).assertExists()
        }
    }

    private fun waitForPage(index: Int) {
        val tag = MangaReaderVerificationTags.page(index)
        compose.waitUntil(timeoutMillis = 20_000L) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tag).assertExists()
    }

    private fun waitForTargetProcessToExit() {
        repeat(50) {
            if (shell("pidof com.veilreader.app").trim().isEmpty()) return
            Thread.sleep(100L)
        }
        throw AssertionError("target app process did not exit after am kill")
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return descriptor.use { pfd ->
            FileInputStream(pfd.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }

    private fun launch(
        resetProgress: Boolean,
        startPage: Int
    ): ActivityScenario<MangaReaderVerificationActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MangaReaderVerificationActivity::class.java).apply {
            putExtra(MangaReaderVerificationActivity.EXTRA_RESET_PROGRESS, resetProgress)
            putExtra(MangaReaderVerificationActivity.EXTRA_DURABLE_PROGRESS, true)
            putExtra(MangaReaderVerificationActivity.EXTRA_START_PAGE, startPage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActivityScenario.launch(intent)
    }
}
