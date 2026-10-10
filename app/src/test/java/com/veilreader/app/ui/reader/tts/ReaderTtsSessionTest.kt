package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderTtsSessionTest {
    @Test
    fun checkpointMustCommitBeforePlaybackAndPlayingAcknowledgement() = runTest {
        val saved = CompletableDeferred<Unit>()
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { true },
            StandardTestDispatcher(testScheduler), commitCheckpoint = { _, _ -> saved.await() })
        try {
            session.start(locator()); runCurrent()
            assertEquals(ReaderTtsPhase.PREPARING, session.state.value.phase)
            assertTrue(backend.requests.isEmpty())
            saved.complete(Unit); runCurrent()
            assertEquals(ReaderTtsPhase.PLAYING, session.state.value.phase)
            assertEquals(1, backend.requests.size)
        } finally { session.close() }
    }

    @Test
    fun checkpointFailureNeverSpeaksOrAcknowledgesPlaying() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { true },
            StandardTestDispatcher(testScheduler), commitCheckpoint = { _, _ -> error("Storage failed") })
        try {
            session.start(locator()); runCurrent()
            assertEquals(ReaderTtsPhase.FAILED, session.state.value.phase)
            assertEquals(ReaderTtsProblem.STORAGE, session.state.value.problem)
            assertTrue(backend.requests.isEmpty())
        } finally { session.close() }
    }

    @Test
    fun pauseWhileCheckpointWaitsCannotStartLateSpeech() = runTest {
        val saved = CompletableDeferred<Unit>()
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { true },
            StandardTestDispatcher(testScheduler), commitCheckpoint = { _, _ -> saved.await() })
        try {
            session.start(locator()); runCurrent()
            session.pause(); saved.complete(Unit); runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            assertTrue(backend.requests.isEmpty())
        } finally { session.close() }
    }

    @Test
    fun foregroundSleepTimerPausesAtDeadlineWithoutRestartingUtterance() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { true },
            StandardTestDispatcher(testScheduler), utteranceTimeoutMs = 7_200_000L,
            nowEpochMs = { 1_000L + testScheduler.currentTime })
        try {
            session.start(locator()); runCurrent()
            assertTrue(session.setSleepTimer(10)); runCurrent()
            assertEquals(601_000L, session.sleepDeadlineEpochMs.value)
            assertEquals(1, backend.requests.size)
            advanceTimeBy(599_999L); runCurrent()
            assertEquals(ReaderTtsPhase.PLAYING, session.state.value.phase)
            advanceTimeBy(1L); runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            assertNull(session.sleepDeadlineEpochMs.value)
            assertEquals(1, backend.requests.size)
            assertTrue(backend.abandonFocusStops > 0)
        } finally { session.close() }
    }

    @Test
    fun replacingAndDisablingTimerDoesNotRestartOrPausePlaybackAtOldDeadline() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { true },
            StandardTestDispatcher(testScheduler), utteranceTimeoutMs = 7_200_000L,
            nowEpochMs = { 1_000L + testScheduler.currentTime })
        try {
            session.start(locator()); runCurrent()
            assertTrue(session.setSleepTimer(10)); runCurrent()
            advanceTimeBy(300_000L); runCurrent()
            assertTrue(session.setSleepTimer(15)); runCurrent()
            assertEquals(1_201_000L, session.sleepDeadlineEpochMs.value)
            advanceTimeBy(300_000L); runCurrent()
            assertEquals(ReaderTtsPhase.PLAYING, session.state.value.phase)
            assertTrue(session.setSleepTimer(0)); runCurrent()
            advanceTimeBy(1_200_000L); runCurrent()
            assertEquals(ReaderTtsPhase.PLAYING, session.state.value.phase)
            assertEquals(1, backend.requests.size)
            assertNull(session.sleepDeadlineEpochMs.value)
        } finally { session.close() }
    }

    @Test
    fun invalidTimerRequestPreservesValidTimer() = runTest {
        var now = 1_000L
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { true },
            StandardTestDispatcher(testScheduler), utteranceTimeoutMs = 7_200_000L,
            nowEpochMs = { now })
        try {
            session.start(locator()); runCurrent()
            assertTrue(session.setSleepTimer(10)); runCurrent()
            assertFalse(session.setSleepTimer(181))
            now = Long.MAX_VALUE
            assertFalse(session.setSleepTimer(15))
            assertEquals(601_000L, session.sleepDeadlineEpochMs.value)
            advanceTimeBy(600_000L); runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
        } finally { session.close() }
    }

    @Test
    fun stopAndCloseCancelForegroundSleepTimerOwnership() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { true },
            StandardTestDispatcher(testScheduler), utteranceTimeoutMs = 7_200_000L)
        session.start(locator()); runCurrent()
        assertTrue(session.setSleepTimer(10)); runCurrent()
        session.stop()
        assertNull(session.sleepDeadlineEpochMs.value)
        session.start(locator()); runCurrent()
        advanceTimeBy(600_000L); runCurrent()
        assertEquals(ReaderTtsPhase.PLAYING, session.state.value.phase)
        assertTrue(session.setSleepTimer(10)); runCurrent()
        session.close()
        assertNull(session.sleepDeadlineEpochMs.value)
        assertFalse(session.setSleepTimer(10))
        advanceTimeBy(600_000L); runCurrent()
        assertEquals(ReaderTtsPhase.CLOSED, session.state.value.phase)
    }

    @Test
    fun preferenceChangesApplyToNextChunkWithoutReplacingContentOrStartingPlayback() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "fa", { true }, StandardTestDispatcher(testScheduler))
        try {
            session.start(locator()); runCurrent()
            assertEquals(1f, backend.requests.single().preferences.speed)
            session.updatePreferences(ReaderTtsPreferences(speed = 1.5f, pitch = 0.8f))
            assertEquals(1, backend.requests.size)
            backend.requests.first().completion.complete(null); runCurrent()
            assertEquals("second", backend.requests.last().text)
            assertEquals(1.5f, backend.requests.last().preferences.speed)
            session.pause()
            session.updatePreferences(ReaderTtsPreferences(speed = 2f))
            runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            assertEquals(2, backend.requests.size)
        } finally { session.close() }
    }

    @Test
    fun replacingAPreparingSourceDoesNotRunConcurrentParsersOrAttachItsOldChunk() = runTest {
        val releaseOld = CompletableDeferred<Unit>()
        var factories = 0
        var newerReads = 0
        val backend = FakeBackend()
        val session = ReaderTtsSession({
            if (factories++ == 0) object : ReaderTtsContent {
                override suspend fun next(): ReaderTtsUtterance? = withContext(NonCancellable) {
                    releaseOld.await()
                    ReaderTtsUtterance("obsolete", "en", locator())
                }
            } else object : ReaderTtsContent {
                override suspend fun next(): ReaderTtsUtterance? {
                    newerReads += 1
                    return ReaderTtsUtterance("new source", "en", locator())
                }
            }
        }, { backend }, "en", { true }, StandardTestDispatcher(testScheduler))
        try {
            session.start(locator()); runCurrent()
            session.start(locator()); runCurrent()
            assertEquals(0, newerReads)
            releaseOld.complete(Unit); runCurrent()
            assertEquals(1, newerReads)
            assertEquals("new source", backend.requests.single().text)
        } finally { releaseOld.complete(Unit); session.close(); session.awaitClosed() }
    }

    @Test
    fun pauseDuringAContentReadRetainsThatChunkAndDoesNotReadPastIt() = runTest {
        val ready = CompletableDeferred<ReaderTtsUtterance?>()
        var reads = 0
        val backend = FakeBackend()
        val session = ReaderTtsSession({ object : ReaderTtsContent {
            override suspend fun next(): ReaderTtsUtterance? { reads += 1; return ready.await() }
        } }, { backend }, "en", { true }, StandardTestDispatcher(testScheduler))
        try {
            session.start(locator()); runCurrent()
            assertEquals(1, reads)
            session.pause(); runCurrent()
            ready.complete(ReaderTtsUtterance("retained", "en", locator()))
            runCurrent()
            assertTrue(backend.requests.isEmpty())
            session.resume(); runCurrent()
            assertEquals(1, reads)
            assertEquals("retained", backend.requests.single().text)
        } finally { session.close(); session.awaitClosed() }
    }

    @Test
    fun brokenContentFactoryFailsCalmlyWithoutBindingSpeech() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ throw IllegalStateException("broken source") }, { backend }, "en", { true }, StandardTestDispatcher(testScheduler))
        session.start(locator())
        runCurrent()
        assertEquals(ReaderTtsProblem.CONTENT, session.state.value.problem)
        assertEquals(0, backend.initializations)
        session.close()
        session.awaitClosed()
    }

    @Test
    fun emptyContentDoesNotBindAnEngine() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ object : ReaderTtsContent { override suspend fun next(): ReaderTtsUtterance? = null } }, { backend }, "en", { true }, StandardTestDispatcher(testScheduler))
        session.start(locator())
        runCurrent()
        assertEquals(ReaderTtsProblem.UNSUPPORTED, session.state.value.problem)
        assertEquals(0, backend.initializations)
        session.close()
        session.awaitClosed()
    }

    @Test
    fun speechStartsExplicitlyAndPauseReplaysOnlyTheCurrentBoundedUtterance() = runTest {
        val backend = FakeBackend()
        var sourceReads = 0
        val session = ReaderTtsSession({ source { sourceReads += 1 } }, { backend }, "fa-IR", { true }, StandardTestDispatcher(testScheduler))
        try {
            assertEquals(0, backend.initializations)
            session.start(locator())
            runCurrent()
            assertEquals(ReaderTtsPhase.PLAYING, session.state.value.phase)
            assertEquals("first", backend.requests.single().text)
            session.pause()
            runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            backend.requests[0].completion.complete(null) // Late old onDone must not advance.
            runCurrent()
            assertEquals(1, sourceReads)
            session.resume()
            runCurrent()
            assertEquals("first", backend.requests[1].text)
            backend.requests[1].completion.complete(null)
            runCurrent()
            assertEquals("second", backend.requests[2].text)
            backend.requests[2].completion.complete(null)
            runCurrent()
            assertEquals(ReaderTtsPhase.ENDED, session.state.value.phase)
        } finally { session.close() }
    }

    @Test
    fun closeDuringInitializationCancelsOwnershipAndCannotAffectANewerSession() = runTest {
        val oldBackend = FakeBackend().apply { initialization = CompletableDeferred() }
        val newerBackend = FakeBackend()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val old = ReaderTtsSession({ source {} }, { oldBackend }, "en", { true }, dispatcher)
        val newer = ReaderTtsSession({ source {} }, { newerBackend }, "en", { true }, dispatcher)
        try {
            old.start(locator()); runCurrent(); old.close()
            newer.start(locator()); runCurrent()
            oldBackend.initialization.complete(null); runCurrent()
            assertEquals(ReaderTtsPhase.CLOSED, old.state.value.phase)
            assertTrue(oldBackend.requests.isEmpty())
            assertEquals(ReaderTtsPhase.PLAYING, newer.state.value.phase)
            assertEquals(0, newerBackend.closes)
            assertEquals(1, oldBackend.closes)
        } finally { old.close(); newer.close() }
    }

    @Test
    fun backgroundOrAccessibilityBlocksNewSpeechAndPausesAnActiveRequest() = runTest {
        var allowed = false
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { allowed }, StandardTestDispatcher(testScheduler))
        try {
            session.start(locator()); runCurrent()
            assertEquals(0, backend.initializations)
            allowed = true
            session.start(locator()); runCurrent()
            allowed = false
            advanceTimeBy(501); runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            session.resume(); runCurrent()
            assertEquals(1, backend.requests.size)
        } finally { session.close() }
    }

    @Test
    fun focusLossOrHeadphoneInterruptionDoesNotAutomaticallyResume() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ source {} }, { backend }, "en", { true }, StandardTestDispatcher(testScheduler))
        try {
            session.start(locator()); runCurrent()
            backend.onInterruption?.invoke(ReaderTtsInterruption.BECOMING_NOISY); runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            advanceTimeBy(2000); runCurrent()
            assertEquals(1, backend.requests.size)
        } finally { session.close() }
    }

    @Test
    fun initializationTimeoutAndMissingVoiceFailCalmlyAndReleasePlayback() = runTest {
        val backend = FakeBackend().apply { initialization = CompletableDeferred() }
        val session = ReaderTtsSession({ source {} }, { backend }, "fa", { true }, StandardTestDispatcher(testScheduler), initializationTimeoutMs = 100)
        try {
            session.start(locator()); runCurrent()
            advanceTimeBy(101); runCurrent()
            assertEquals(ReaderTtsProblem.TIMEOUT, session.state.value.problem)
            assertTrue(backend.stops > 0)
            backend.initialization.complete(null)
            session.start(locator()); runCurrent()
            backend.requests.last().completion.complete(ReaderTtsProblem.NO_OFFLINE_VOICE); runCurrent()
            assertEquals(ReaderTtsPhase.FAILED, session.state.value.phase)
            assertEquals(ReaderTtsProblem.NO_OFFLINE_VOICE, session.state.value.problem)
        } finally { session.close() }
    }

    @Test
    fun unsupportedContentDoesNotBindAnEngineAndRepeatedCloseIsIdempotent() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession({ null }, { backend }, "en", { true }, StandardTestDispatcher(testScheduler))
        session.start(locator()); runCurrent()
        assertEquals(ReaderTtsProblem.UNSUPPORTED, session.state.value.problem)
        session.close(); session.close(); session.start(locator()); runCurrent()
        assertEquals(0, backend.initializations)
        assertEquals(0, backend.closes)
        assertEquals(ReaderTtsPhase.CLOSED, session.state.value.phase)
    }

    @Test
    fun pausedRecoveryLoadDoesNotBindOrAutoplayUntilExplicitResume() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession(
            { source {} },
            { backend },
            "fa-IR",
            { true },
            StandardTestDispatcher(testScheduler)
        )
        try {
            val start = locator()
            session.load(start, ReaderTtsPreferences(speed = 1.25f), autoplay = false)
            runCurrent()

            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            assertEquals(start, session.state.value.sourceLocator)
            assertEquals(0, backend.initializations)
            assertTrue(backend.requests.isEmpty())

            session.resume()
            runCurrent()
            assertEquals(1, backend.initializations)
            assertEquals("first", backend.requests.single().text)
            assertEquals(1.25f, backend.requests.single().preferences.speed)
        } finally {
            session.close()
        }
    }

    @Test
    fun nextDuringPreparingSkipsPendingCurrentSegmentBeforeResumingSpeech() = runTest {
        val backend = FakeBackend()
        val firstRead = CompletableDeferred<ReaderTtsUtterance?>()
        var reads = 0
        val first = locator("first.xhtml")
        val second = locator("second.xhtml")
        val session = ReaderTtsSession(
            {
                object : ReaderTtsContent {
                    override suspend fun next(): ReaderTtsUtterance? =
                        when (reads++) {
                            0 -> firstRead.await()
                            1 -> ReaderTtsUtterance("second", "en", second)
                            else -> null
                        }
                }
            },
            { backend },
            "en",
            { true },
            StandardTestDispatcher(testScheduler)
        )
        try {
            session.start(first)
            runCurrent()
            assertEquals(ReaderTtsPhase.PREPARING, session.state.value.phase)

            session.next()
            runCurrent()
            firstRead.complete(ReaderTtsUtterance("first", "en", first))
            runCurrent()

            assertEquals(2, reads)
            assertEquals("second", backend.requests.single().text)
            assertEquals(second, session.state.value.sourceLocator)
        } finally {
            firstRead.complete(null)
            session.close()
        }
    }

    @Test
    fun nextFromFreshPausedRecoverySkipsCurrentSegmentAndPreviousReturnsToIt() = runTest {
        val backend = FakeBackend()
        var index = 0
        val locators = listOf(
            locator("first.xhtml"),
            locator("second.xhtml"),
            locator("third.xhtml")
        )
        val session = ReaderTtsSession(
            {
                object : ReaderTtsContent {
                    override suspend fun next(): ReaderTtsUtterance? =
                        locators.getOrNull(index)?.let { target ->
                            ReaderTtsUtterance(
                                text = listOf("first", "second", "third")[index++],
                                languageTag = "en",
                                locator = target
                            )
                        }
                }
            },
            { backend },
            "en",
            { true },
            StandardTestDispatcher(testScheduler)
        )
        try {
            session.load(locators.first(), autoplay = false)
            session.next()
            runCurrent()

            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            assertEquals(locators[1], session.state.value.sourceLocator)
            assertTrue(backend.requests.isEmpty())

            session.previous()
            runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            assertEquals(locators[0], session.state.value.sourceLocator)
            assertTrue(backend.requests.isEmpty())
        } finally {
            session.close()
        }
    }

    @Test
    fun transientAudioFocusLossStopsSpeechWithoutAbandonAndResumesSameSegment() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession(
            { source {} },
            { backend },
            "en",
            { true },
            StandardTestDispatcher(testScheduler)
        )
        try {
            session.start(locator())
            runCurrent()
            assertEquals(ReaderTtsPhase.PLAYING, session.state.value.phase)
            assertEquals("first", backend.requests.single().text)

            backend.onInterruption?.invoke(ReaderTtsInterruption.TRANSIENT_FOCUS)
            runCurrent()
            assertEquals(ReaderTtsPhase.PREPARING, session.state.value.phase)
            assertEquals(1, backend.preserveFocusStops)

            backend.onFocusGained?.invoke()
            runCurrent()
            assertEquals(2, backend.requests.size)
            assertEquals("first", backend.requests[1].text)
        } finally {
            session.close()
        }
    }

    @Test
    fun userPauseAfterFocusGainButBeforeDeferredResumeWinsTheRace() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession(
            { source {} },
            { backend }, "en", { true }, StandardTestDispatcher(testScheduler)
        )
        try {
            session.start(locator())
            runCurrent()
            assertEquals(1, backend.requests.size)

            backend.onInterruption?.invoke(ReaderTtsInterruption.TRANSIENT_FOCUS)
            backend.onFocusGained?.invoke() // queues deferred resume
            session.pause() // user acts before that continuation runs
            runCurrent()

            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            assertEquals(1, backend.requests.size)
            assertTrue(backend.abandonFocusStops >= 1)
        } finally { session.close() }
    }

    @Test
    fun userPauseDuringTransientFocusLossCancelsAutomaticResume() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession(
            { source {} },
            { backend },
            "en",
            { true },
            StandardTestDispatcher(testScheduler)
        )
        try {
            session.start(locator())
            runCurrent()
            backend.onInterruption?.invoke(ReaderTtsInterruption.TRANSIENT_FOCUS)
            runCurrent()

            session.pause()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            backend.onFocusGained?.invoke()
            runCurrent()

            assertEquals(1, backend.requests.size)
            assertTrue(backend.abandonFocusStops >= 1)
        } finally {
            session.close()
        }
    }

    @Test
    fun activeSemanticTextIsAvailableForListeningUiButStaysInMemoryOnly() = runTest {
        val backend = FakeBackend()
        val session = ReaderTtsSession(
            { source {} },
            { backend },
            "en",
            { true },
            StandardTestDispatcher(testScheduler)
        )
        try {
            session.start(locator())
            runCurrent()

            assertEquals("first", session.state.value.activeText)
            session.pause()
            assertEquals("first", session.state.value.activeText)
            session.stop()
            assertNull(session.state.value.activeText)
        } finally {
            session.close()
        }
    }

    @Test
    fun mixedLanguageSpansDoNotSilentlyChangeTheNarratorMidBook() = runTest {
        val backend = FakeBackend()
        val samples = listOf(
            ReaderTtsUtterance("english first", "en-US", locator()),
            ReaderTtsUtterance("bonjour inserted", "fr-FR", locator()),
            ReaderTtsUtterance("english continues", "en-GB", locator())
        )
        var index = 0
        val session = ReaderTtsSession(
            { object : ReaderTtsContent {
                override suspend fun next(): ReaderTtsUtterance? = samples.getOrNull(index++)
            } },
            { backend }, "en", { true }, StandardTestDispatcher(testScheduler)
        )
        try {
            session.start(locator())
            runCurrent()
            assertEquals("en-US", backend.requests.single().languageTag)
            backend.requests[0].completion.complete(null)
            runCurrent()
            assertEquals("en-US", backend.requests[1].languageTag)
            backend.requests[1].completion.complete(null)
            runCurrent()
            assertEquals("en-US", backend.requests[2].languageTag)
            session.pause()
            runCurrent()
            assertEquals(ReaderTtsPhase.PAUSED, session.state.value.phase)
            session.resume()
            runCurrent()
            assertEquals("en-US", backend.requests[3].languageTag)
            assertEquals("english continues", backend.requests[3].text)
        } finally { session.close() }
    }

    @Test
    fun explicitLanguagePreferenceOverridesSourceMetadataThroughoutBook() = runTest {
        val backend = FakeBackend()
        var index = 0
        val session = ReaderTtsSession(
            { object : ReaderTtsContent {
                override suspend fun next(): ReaderTtsUtterance? =
                    listOf("one", "two").getOrNull(index++)?.let {
                        ReaderTtsUtterance(it, if (it == "one") "en-US" else "fr-FR", locator())
                    }
            } },
            { backend }, "en-US", { true }, StandardTestDispatcher(testScheduler)
        )
        try {
            session.start(locator(), ReaderTtsPreferences(languageTag = "fa-IR"))
            runCurrent()
            assertEquals("fa-IR", backend.requests[0].languageTag)
            backend.requests[0].completion.complete(null)
            runCurrent()
            assertEquals("fa-IR", backend.requests[1].languageTag)
        } finally { session.close() }
    }

    private fun source(onRead: () -> Unit) = object : ReaderTtsContent {
        var index = 0
        override suspend fun next(): ReaderTtsUtterance? {
            onRead()
            return listOf("first", "second").getOrNull(index++)?.let { ReaderTtsUtterance(it, null, locator()) }
        }
    }
    private fun locator(href: String = "chapter.xhtml") =
        Locator(requireNotNull(Url(href)), MediaType.XHTML)
    private class FakeBackend : ReaderTtsBackend {
        data class Request(val text: String, val languageTag: String, val preferences: ReaderTtsPreferences, val completion: CompletableDeferred<ReaderTtsProblem?> = CompletableDeferred())
        override val voices = emptyList<ReaderTtsVoice>()
        override var onInterruption: ((ReaderTtsInterruption) -> Unit)? = null
        override var onFocusGained: (() -> Unit)? = null
        var initializations = 0
        var initialization = CompletableDeferred<ReaderTtsProblem?>().apply { complete(null) }
        val requests = mutableListOf<Request>()
        var stops = 0
        var preserveFocusStops = 0
        var abandonFocusStops = 0
        var closes = 0
        override suspend fun initialize(): ReaderTtsProblem? { initializations += 1; return initialization.await() }
        override suspend fun speak(text: String, languageTag: String, preferences: ReaderTtsPreferences): ReaderTtsProblem? {
            val request = Request(text, languageTag, preferences); requests += request; return request.completion.await()
        }
        override fun stop(abandonAudioFocus: Boolean) {
            stops += 1
            if (abandonAudioFocus) abandonFocusStops += 1 else preserveFocusStops += 1
        }
        override fun close() {
            closes += 1
            onInterruption = null
            onFocusGained = null
        }
    }
}
