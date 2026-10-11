package com.veilreader.app.ui.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Model-independent checks; these tests must never load a native JNI library. */
class ReaderTtsSherpaKokoroRuntimeTest {
    @Test
    fun onlyExactInstalledVoiceIdsMaySelectNativeSpeaker() {
        assertEquals(0, sherpaKokoroSpeakerId("kokoro-en-v0_19-0", 11))
        assertEquals(7, sherpaKokoroSpeakerId("kokoro-en-v0_19-7", 11))
        assertEquals(10, sherpaKokoroSpeakerId("kokoro-en-v0_19-10", 11))
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_19-11", 11))
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_19--1", 11))
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_19-07", 11))
    }

    @Test
    fun rejectedSpeakerNeverFallsBackToDefaultVoice() {
        assertNull(sherpaKokoroSpeakerId(null, 11))
        assertNull(sherpaKokoroSpeakerId("", 11))
        assertNull(sherpaKokoroSpeakerId("af_heart", 11))
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_19-0", 0))
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_19-0", -1))
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_19-9999999999999999999999", 11))
    }

    @Test
    fun unknownOrChangedModelVoiceNamespaceIsNotReused() {
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_20-0", 11))
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_19-0-extra", 11))
        assertEquals(2, sherpaKokoroSpeakerId("kokoro-en-v0_19-2", 3))
        assertNull(sherpaKokoroSpeakerId("kokoro-en-v0_19-3", 3))
    }
}
