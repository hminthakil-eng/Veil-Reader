package com.veilreader.app.ui.reader

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.veilreader.app.MainActivity
import com.veilreader.app.data.db.VeilDatabase
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.publication.Locator

@RunWith(AndroidJUnit4::class)
class ReaderResumeHarnessInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Before
    fun resetReaderState() {
        runBlocking {
            withContext(Dispatchers.IO) {
                VeilDatabase.get(context).clearAllTables()
                context.filesDir.resolve("publications").deleteRecursively()
                context.filesDir.resolve("covers").deleteRecursively()
            }
        }
    }

    @Test
    fun ltr_reader_recreates_at_durable_locator_and_survives_orientation() {
        val fixture = ReaderQaFixtureFactory.createEpub(context, ReaderFixtureDirection.LTR)
        launchFixture(fixture).use { scenario ->
            val first = awaitNavigator(scenario)
            assertEquals(ReadingProgression.LTR, first.overflow.value.readingProgression)

            val initial = first.currentLocator.value
            advance(scenario, first)
            val moved = awaitLocatorChange(first, initial)
            assertNotEquals(initial.locations.totalProgression, moved.locations.totalProgression)

            val bookId = requireBookId(first)
            val persisted = awaitPersistedLocator(bookId)
            val durable = Locator.fromJSON(JSONObject(persisted))
            assertNotNull(durable)

            scenario.recreate()
            val recreated = awaitNavigator(scenario, previous = first)
            val afterRecreate = awaitLocatorNear(
                recreated,
                requireNotNull(durable),
                progressionTolerance = SAME_ORIENTATION_TOLERANCE
            )
            assertSameSemanticLocation(requireNotNull(durable), afterRecreate, SAME_ORIENTATION_TOLERANCE)

            scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            val landscape = awaitNavigator(scenario, previous = recreated)
            val afterRotation = awaitLocatorNear(
                landscape,
                afterRecreate,
                progressionTolerance = ORIENTATION_TOLERANCE
            )
            assertSameSemanticLocation(afterRecreate, afterRotation, ORIENTATION_TOLERANCE)
        }
    }

    @Test
    fun rtl_fixture_drives_real_reader_progression_and_resume_contract() {
        val fixture = ReaderQaFixtureFactory.createEpub(context, ReaderFixtureDirection.RTL)
        launchFixture(fixture).use { scenario ->
            val first = awaitNavigator(scenario)
            assertEquals(ReadingProgression.RTL, first.overflow.value.readingProgression)

            val initial = first.currentLocator.value
            advance(scenario, first)
            val moved = awaitLocatorChange(first, initial)
            assertNotEquals(initial.locations.totalProgression, moved.locations.totalProgression)

            val bookId = requireBookId(first)
            val persisted = awaitPersistedLocator(bookId)
            val durable = requireNotNull(Locator.fromJSON(JSONObject(persisted)))

            scenario.recreate()
            val recreated = awaitNavigator(scenario, previous = first)
            val resumed = awaitLocatorNear(
                recreated,
                durable,
                progressionTolerance = SAME_ORIENTATION_TOLERANCE
            )
            assertEquals(ReadingProgression.RTL, recreated.overflow.value.readingProgression)
            assertSameSemanticLocation(durable, resumed, SAME_ORIENTATION_TOLERANCE)
        }
    }

    private fun launchFixture(file: java.io.File): ActivityScenario<MainActivity> {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(Uri.fromFile(file), "application/epub+zip")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActivityScenario.launch(intent)
    }

    private fun awaitNavigator(
        scenario: ActivityScenario<MainActivity>,
        previous: EpubNavigatorFragment? = null
    ): EpubNavigatorFragment {
        var found: EpubNavigatorFragment? = null
        await("EPUB navigator") {
            scenario.onActivity { activity ->
                found = activity.supportFragmentManager.fragments
                    .filterIsInstance<EpubNavigatorFragment>()
                    .firstOrNull { it !== previous && it.isAdded }
            }
            found != null
        }
        return requireNotNull(found)
    }

    private fun advance(
        scenario: ActivityScenario<MainActivity>,
        navigator: EpubNavigatorFragment
    ) {
        var moved = false
        scenario.onActivity {
            moved = navigator.goForward(animated = false)
        }
        assertTrue("Readium refused the deterministic forward page action.", moved)
    }

    private fun awaitLocatorChange(
        navigator: EpubNavigatorFragment,
        previous: Locator
    ): Locator {
        var result = navigator.currentLocator.value
        await("locator change") {
            result = navigator.currentLocator.value
            !sameProgression(previous, result, epsilon = 0.000001)
        }
        return result
    }

    private fun awaitPersistedLocator(bookId: String): String {
        var locatorJson: String? = null
        await("durable Room locator") {
            locatorJson = runBlocking(Dispatchers.IO) {
                VeilDatabase.get(context)
                    .books()
                    .findWithCollections(bookId)
                    ?.book
                    ?.locatorJson
            }
            !locatorJson.isNullOrBlank()
        }
        return requireNotNull(locatorJson)
    }

    private fun awaitLocatorNear(
        navigator: EpubNavigatorFragment,
        expected: Locator,
        progressionTolerance: Double
    ): Locator {
        var result = navigator.currentLocator.value
        await("resumed semantic locator") {
            result = navigator.currentLocator.value
            result.href == expected.href &&
                progressionDistance(result, expected) <= progressionTolerance
        }
        return result
    }

    private fun assertSameSemanticLocation(
        expected: Locator,
        actual: Locator,
        progressionTolerance: Double
    ) {
        assertEquals(expected.href, actual.href)
        assertTrue(
            "Reading progression drifted too far. expected=${expected.locations.totalProgression}, actual=${actual.locations.totalProgression}",
            progressionDistance(actual, expected) <= progressionTolerance
        )
    }

    private fun requireBookId(navigator: EpubNavigatorFragment): String {
        val tag = requireNotNull(navigator.tag)
        assertTrue(tag.startsWith("reader-"))
        return tag.removePrefix("reader-")
    }

    private fun sameProgression(a: Locator, b: Locator, epsilon: Double): Boolean =
        progressionDistance(a, b) <= epsilon && a.href == b.href

    private fun progressionDistance(a: Locator, b: Locator): Double {
        val first = a.locations.totalProgression
        val second = b.locations.totalProgression
        if (first == null || second == null) return if (a.href == b.href) 0.0 else 1.0
        return abs(first - second)
    }

    private fun await(label: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + WAIT_TIMEOUT_MS
        var lastFailure: Throwable? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                if (condition()) return
                lastFailure = null
            } catch (error: Throwable) {
                lastFailure = error
            }
            SystemClock.sleep(POLL_MS)
        }
        throw AssertionError("Timed out waiting for $label.", lastFailure)
    }

    companion object {
        private const val WAIT_TIMEOUT_MS = 15_000L
        private const val POLL_MS = 100L
        private const val SAME_ORIENTATION_TOLERANCE = 0.01
        private const val ORIENTATION_TOLERANCE = 0.08
    }
}