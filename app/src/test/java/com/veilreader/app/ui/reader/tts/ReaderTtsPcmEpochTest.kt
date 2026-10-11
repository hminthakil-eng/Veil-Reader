package com.veilreader.app.ui.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ReaderTtsPcmEpochTest {
    @Test
    fun userPauseInvalidatesStaleSynthesizerAndBufferedChunks() {
        val epoch = ReaderTtsPcmEpoch()
        val old = epoch.begin("kokoro-en-v0_19", "kokoro-en-v0_19-4")
        assertTrue(readerTtsValidPcmChunk(epoch, old, 24_000, floatArrayOf(0.1f)))
        epoch.revoke()
        assertFalse(epoch.accepts(old))
        assertFalse(readerTtsValidPcmChunk(epoch, old, 24_000, floatArrayOf(0.1f)))
        assertNull(epoch.selectedVoiceOrNull())
        assertFalse(epoch.hasOwner())
    }

    @Test
    fun explicitResumeRetainsNarratorWithoutRevivingOldPcm() {
        val epoch = ReaderTtsPcmEpoch()
        val before = epoch.begin("kokoro-en-v0_19", "kokoro-en-v0_19-7")
        epoch.revoke()
        val after = epoch.begin("kokoro-en-v0_19", "kokoro-en-v0_19-7")
        assertEquals(before.voiceId, after.voiceId)
        assertFalse(epoch.accepts(before))
        assertTrue(epoch.accepts(after))
        assertTrue(after.generation > before.generation)
    }

    @Test
    fun narratorSwitchRequiresExplicitRevokeAndNeverRetagsOldChunks() {
        val epoch = ReaderTtsPcmEpoch()
        val old = epoch.begin("kokoro", "kokoro-1")
        val rejected = runCatching { epoch.begin("kokoro", "kokoro-2") }
        assertTrue(rejected.isFailure)
        assertEquals("kokoro-1", epoch.selectedVoiceOrNull())
        epoch.revoke()
        val replacement = epoch.begin("kokoro", "kokoro-2")
        assertFalse(epoch.accepts(old))
        assertTrue(epoch.accepts(replacement))
        assertEquals("kokoro-2", epoch.selectedVoiceOrNull())
    }

    @Test
    fun invalidNativeSamplesFailClosed() {
        val epoch = ReaderTtsPcmEpoch()
        val active = epoch.begin("kokoro", "voice-0")
        assertFalse(readerTtsValidPcmChunk(epoch, active, 0, floatArrayOf(0.0f)))
        assertFalse(readerTtsValidPcmChunk(epoch, active, 24_000, floatArrayOf()))
        assertFalse(readerTtsValidPcmChunk(epoch, active, 24_000, floatArrayOf(Float.NaN)))
        assertFalse(readerTtsValidPcmChunk(epoch, active, 24_000, floatArrayOf(1.1f)))
        assertTrue(readerTtsValidPcmChunk(epoch, active, 24_000, floatArrayOf(-1f, 1f)))
    }

    @Test
    fun nativeProducerOnAnotherThreadCannotSubmitAfterPause() {
        val epoch = ReaderTtsPcmEpoch()
        val lease = epoch.begin("kokoro", "fixed-voice")
        val ready = CountDownLatch(1)
        val go = CountDownLatch(1)
        val accepted = AtomicBoolean(true)
        val worker = thread(start = true) {
            ready.countDown()
            if (go.await(2, TimeUnit.SECONDS)) {
                accepted.set(readerTtsValidPcmChunk(
                    epoch, lease, 24_000, floatArrayOf(0.25f)
                ))
            }
        }
        try {
            assertTrue(ready.await(2, TimeUnit.SECONDS))
            epoch.revoke()
            go.countDown()
            worker.join(2_000)
            assertFalse(worker.isAlive)
            assertFalse(accepted.get())
        } finally {
            go.countDown()
            worker.join(2_000)
        }
    }

    @Test
    fun pcmChunkDurationLimitUsesActualSampleRate() {
        val epoch = ReaderTtsPcmEpoch()
        val lease = epoch.begin("kokoro", "narrator-0")
        assertTrue(readerTtsValidPcmChunk(epoch, lease, 24_000, FloatArray(48_000)))
        assertFalse(readerTtsValidPcmChunk(epoch, lease, 24_000, FloatArray(48_001)))
        assertTrue(readerTtsValidPcmChunk(epoch, lease, 8_000, FloatArray(16_000)))
        assertFalse(readerTtsValidPcmChunk(epoch, lease, 8_000, FloatArray(16_001)))
        epoch.revoke()
    }

    @Test
    fun blankModelOrNarratorIsNotAccepted() {
        val epoch = ReaderTtsPcmEpoch()
        assertTrue(runCatching { epoch.begin("", "voice") }.isFailure)
        assertTrue(runCatching { epoch.begin("model", " ") }.isFailure)
        assertFalse(epoch.hasOwner())
    }
}
