package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsInProcessCoordinatorTest {
    private val model = ReaderTtsNeuralModelSpec(
        packageId = "kokoro-en-v0_19",
        family = ReaderTtsNeuralModelFamily.KOKORO,
        languageTags = setOf("en-US"),
        supportsMultipleVoices = true,
        expectedSampleRateHz = 24_000
    )

    private class FakeRuntime(
        override val model: ReaderTtsNeuralModelSpec,
        val nativeGate: CompletableDeferred<Unit>? = null,
        val started: CompletableDeferred<Unit>? = null
    ) : ReaderTtsNeuralRuntime {
        override val sampleRateHz = 24_000
        var nativeCallbackResult = true
        var closed = false
        override suspend fun synthesize(
            text: String,
            voiceId: String?,
            speed: Float,
            onPcmChunk: suspend (FloatArray) -> Boolean
        ): ReaderTtsProblem? {
            started?.complete(Unit)
            nativeGate?.await()
            val raw = floatArrayOf(0.25f, -0.25f)
            nativeCallbackResult = onPcmChunk(raw)
            raw.fill(0f) // Simulate a JNI engine reusing its native PCM buffer.
            return null
        }
        override fun close() { closed = true }
    }

    private class FakeSink(
        private val draining: CompletableDeferred<Unit>? = null
    ) : ReaderTtsOwnedPcmSink {
        val writes = mutableListOf<FloatArray>()
        var preparedAt = 0
        var silenced = 0
        var drains = 0
        override suspend fun prepare(sampleRateHz: Int): Boolean {
            preparedAt = sampleRateHz
            return true
        }
        override suspend fun submit(pcm: FloatArray): Boolean {
            writes += pcm
            return true
        }
        override suspend fun drain(): Boolean {
            drains++
            draining?.await()
            return true
        }
        override fun silenceImmediately() { silenced++ }
    }

    @Test
    fun actualAudibleDrainPrecedesSuccessfulCompletion() = runTest {
        val finishing = CompletableDeferred<Unit>()
        val sink = FakeSink(draining = finishing)
        val worker = ReaderTtsInProcessCoordinator(FakeRuntime(model), sink)
        val play = async { worker.speak("A chapter begins.", "kokoro-en-v0_19-4", 1f) }
        runCurrent()
        assertEquals(1, sink.drains)
        assertTrue(worker.isSpeaking())
        assertFalse(play.isCompleted)
        assertEquals(24_000, sink.preparedAt)
        assertEquals(0.25f, sink.writes.single()[0], 0.001f)
        finishing.complete(Unit)
        runCurrent()
        assertTrue(play.isCompleted)
        assertNull(play.await())
        assertFalse(worker.isSpeaking())
        assertEquals(0, sink.silenced)
    }

    @Test
    fun ordinarySentenceDoesNotDrainOrFlushAlreadyBufferedPcm() = runTest {
        val sink = FakeSink()
        val worker = ReaderTtsInProcessCoordinator(FakeRuntime(model), sink)
        assertNull(worker.speak(
            "First sentence.", "kokoro-en-v0_19-4", 1f, endOfStream = false
        ))
        assertEquals(0, sink.drains)
        assertEquals(0, sink.silenced)
        assertEquals(1, sink.writes.size)
    }

    @Test
    fun pauseBeforeDelayedNativeCallbackProducesNoAudio() = runTest {
        val nativeGate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val runtime = FakeRuntime(model, nativeGate, started)
        val sink = FakeSink()
        val worker = ReaderTtsInProcessCoordinator(runtime, sink)
        val play = async { worker.speak("Do not speak after Pause.", "kokoro-en-v0_19-2", 1f) }
        runCurrent()
        started.await()
        worker.pause() // Must not suspend waiting for JNI or other engine jobs.
        assertEquals(1, sink.silenced)
        assertFalse(worker.isSpeaking())
        nativeGate.complete(Unit)
        runCurrent()
        assertFalse(runtime.nativeCallbackResult)
        assertTrue(sink.writes.isEmpty())
        assertNull(play.await())
    }

    @Test
    fun voiceIdentityIsExplicitAndStaleNativeBufferIsDetached() = runTest {
        val runtime = FakeRuntime(model)
        val sink = FakeSink()
        val worker = ReaderTtsInProcessCoordinator(runtime, sink)
        assertNull(worker.speak("Text.", "kokoro-en-v0_19-10", 1f))
        assertEquals(0.25f, sink.writes.single()[0], 0.001f)
        assertEquals(-0.25f, sink.writes.single()[1], 0.001f)
        assertFalse(worker.isSpeaking())
    }

    @Test
    fun emptyNarratorRejectedWithoutSilentFallback() = runTest {
        val runtime = FakeRuntime(model)
        val sink = FakeSink()
        val worker = ReaderTtsInProcessCoordinator(runtime, sink)
        assertEquals(
            ReaderTtsProblem.PREFERRED_VOICE_UNAVAILABLE,
            worker.speak("Text.", " ", 1f)
        )
        assertTrue(sink.writes.isEmpty())
    }
}
