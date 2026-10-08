package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderForegroundTtsCheckpointControllerTest {
    @Test
    fun committedCheckpointContainsPositionAndPreferencesButNoPublicationText() = runTest {
        var persisted: ReaderTtsCheckpoint? = null
        val controller = ReaderForegroundTtsCheckpointController("book", { null }, { persisted = it }, { 123L })
        controller.commit(locator("chapter.xhtml", "private passage"), ReaderTtsPreferences(speed = 1.4f))
        val value = requireNotNull(persisted)
        assertFalse(value.toJson().contains("private passage"))
        assertFalse(JSONObject(value.locatorJson).has("text"))
        assertEquals(value.locatorJson, value.request.locatorJson)
        assertEquals(1.4f, value.request.preferences.speed, 0.001f)
        assertEquals(value, controller.checkpoint.value)
        assertEquals(ReaderTtsPhase.PAUSED, value.phase)
    }

    @Test
    fun restoreLoadsMatchingPublicationPausedWithoutBindingBackend() = runTest {
        val controller = ReaderForegroundTtsCheckpointController("book", { saved() }, {})
        val session = session()
        try {
            assertTrue(controller.restore(session, ReaderTtsPreferences()) { true })
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            assertEquals("saved.xhtml", session.state.value.sourceLocator?.href.toString())
        } finally { session.close() }
    }

    @Test
    fun anotherPublicationCheckpointCannotRestoreOrAdvertiseListeningPosition() = runTest {
        val controller = ReaderForegroundTtsCheckpointController("other", { saved() }, {})
        val session = session()
        try {
            assertFalse(controller.restore(session, ReaderTtsPreferences()) { true })
            assertEquals(ReaderTtsPhase.STOPPED, session.state.value.phase)
            assertNull(controller.checkpoint.value)
        } finally { session.close() }
    }

    @Test
    fun delayedRestoreCannotOverrideUserStopOrReplacementOwner() = runTest {
        for (replaceOwner in listOf(false, true)) {
            val read = CompletableDeferred<ReaderTtsCheckpoint?>()
            val controller = ReaderForegroundTtsCheckpointController("book", { read.await() }, {})
            val session = session()
            var owner = true
            try {
                val restored = async { controller.restore(session, ReaderTtsPreferences()) { owner } }
                runCurrent()
                if (replaceOwner) owner = false else session.stop()
                read.complete(saved()); runCurrent()
                assertFalse(restored.await())
                assertEquals(ReaderTtsPhase.STOPPED, session.state.value.phase)
                assertNull(controller.checkpoint.value)
            } finally { session.close() }
        }
    }

    @Test
    fun failedCheckpointWriteCannotPublishAcknowledgedPosition() = runTest {
        val controller = ReaderForegroundTtsCheckpointController("book", { null }, { error("disk failure") })
        var failed = false
        try { controller.commit(locator("chapter.xhtml"), ReaderTtsPreferences()) }
        catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
        assertNull(controller.checkpoint.value)
    }

    private fun kotlinx.coroutines.test.TestScope.session() = ReaderTtsSession(
        contentFactory = { object : ReaderTtsContent {
            override suspend fun next(): ReaderTtsUtterance? = null
        } },
        backendFactory = { error("Restoration must not bind a backend") },
        publicationLanguage = "en", canPlay = { true },
        dispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun locator(href: String, text: String = "") = requireNotNull(Locator.fromJSON(
        JSONObject().put("href", href).put("type", "application/xhtml+xml")
            .put("text", JSONObject().put("highlight", text))
    ))

    private fun saved(): ReaderTtsCheckpoint {
        val json = locator("saved.xhtml").toJSON().toString()
        return ReaderTtsCheckpoint(ReaderTtsPlaybackRequest("book", json), json, ReaderTtsPhase.PLAYING, 123L)
    }
}
