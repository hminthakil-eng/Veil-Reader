package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** No device audio claim: deterministic contract tests using a fake AudioTrack device. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderTtsAndroidPcmSinkTest {
    private class FakeDevice(
        override val sampleRateHz: Int = 24_000
    ) : ReaderTtsAudioDevice {
        var released = false
        var playbackFrame = 0L
        var written = 0
        var silenceCalls = 0
        var zeroWrites = 0
        var maxWrite = 1024
        override var isPlaying = false
        override val playbackHeadPosition: Long get() = playbackFrame
        override fun play() { check(!released); isPlaying = true }
        override fun write(pcm: FloatArray, offset: Int, length: Int): Int {
            check(!released)
            if (zeroWrites > 0) {
                zeroWrites--
                return 0
            }
            val accepted = minOf(length, maxWrite)
            written += accepted
            return accepted
        }
        override fun silenceAndRelease() {
            released = true
            isPlaying = false
            silenceCalls++
        }
    }

    @Test
    fun pausesReleaseCurrentOutputAndPreventAnyLaterPcm() = runTest {
        val device = FakeDevice()
        val sink = ReaderTtsAndroidPcmSink(
            openDevice = { device },
            clockMs = System::currentTimeMillis
        )
        var allowed = true
        assertTrue(sink.prepare(24_000) { allowed })
        assertTrue(sink.submit(FloatArray(2048) { 0.1f }) { allowed })
        assertEquals(2048, device.written)
        allowed = false // revoked epoch before the hardware flush
        sink.silenceImmediately()
        assertEquals(1, device.silenceCalls)
        assertFalse(sink.submit(floatArrayOf(0.1f)) { allowed })
        assertEquals(2048, device.written)
    }

    @Test
    fun ordinarySentencesShareOneOutputWithoutFlushOrDrain() = runTest {
        val device = FakeDevice()
        var calls = 0
        val sink = ReaderTtsAndroidPcmSink(openDevice = {
            calls++
            device
        }, clockMs = System::currentTimeMillis)
        assertTrue(sink.prepare(24_000) { true })
        assertTrue(sink.submit(floatArrayOf(0.1f, 0.2f)) { true })
        assertTrue(sink.prepare(24_000) { true })
        assertTrue(sink.submit(floatArrayOf(-0.1f, -0.2f)) { true })
        assertEquals(1, calls)
        assertEquals(4, device.written)
        assertEquals(0, device.silenceCalls)
        device.playbackFrame = 4
        assertTrue(sink.drain { true })
        sink.close()
        assertEquals(1, device.silenceCalls)
    }

    @Test
    fun partialAndZeroWritesMakeBoundedProgress() = runTest {
        val device = FakeDevice()
        device.maxWrite = 75
        device.zeroWrites = 1
        val sink = ReaderTtsAndroidPcmSink(
            openDevice = { device }, clockMs = System::currentTimeMillis
        )
        assertTrue(sink.prepare(24_000) { true })
        assertTrue(sink.submit(FloatArray(300) { 0.2f }) { true })
        assertEquals(300, device.written)
        sink.close()
    }

    @Test
    fun pauseDuringDeviceInitializationDiscardsStaleNewOutput() = runTest {
        val openEntered = CountDownLatch(1)
        val finishOpen = CountDownLatch(1)
        val device = FakeDevice()
        val sink = ReaderTtsAndroidPcmSink(
            openDevice = {
                openEntered.countDown()
                check(finishOpen.await(3, TimeUnit.SECONDS))
                device
            },
            clockMs = System::currentTimeMillis
        )
        var allowed = true
        val opening = async { sink.prepare(24_000) { allowed } }
        runCurrent() // Start coroutine before awaiting real IO test-device latch.
        try {
            assertTrue(openEntered.await(3, TimeUnit.SECONDS))
            allowed = false
            sink.silenceImmediately()
            finishOpen.countDown()
            assertFalse(opening.await())
            assertTrue(device.released)
        } finally {
            finishOpen.countDown()
        }
    }

    @Test
    fun unsupportedOrChangedSampleRateCannotReselectOutputSilently() = runTest {
        val device = FakeDevice()
        val sink = ReaderTtsAndroidPcmSink(
            openDevice = { device }, clockMs = System::currentTimeMillis
        )
        assertFalse(sink.prepare(0) { true })
        assertTrue(sink.prepare(24_000) { true })
        assertFalse(sink.prepare(48_000) { true })
        sink.close()
    }

    @Test
    fun hardwareDrainCannotSucceedAfterRevokedVoiceEpoch() = runTest {
        val device = FakeDevice()
        val sink = ReaderTtsAndroidPcmSink(
            openDevice = { device }, clockMs = System::currentTimeMillis
        )
        var allowed = true
        assertTrue(sink.prepare(24_000) { allowed })
        assertTrue(sink.submit(FloatArray(10) { 0.1f }) { allowed })
        allowed = false
        sink.silenceImmediately()
        assertFalse(sink.drain { allowed })
    }
}
