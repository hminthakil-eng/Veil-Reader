package com.veilreader.app.ui.reader.tts

import org.junit.Assert.*
import org.junit.Test

class ReaderTtsReaderOwnershipTest {
    @Test
    fun startCapturedBeforeBackgroundCannotAutoPlayAfterResume() {
        assertFalse(readerCanCompleteTtsStart("one", "one", true, true, true,
            requestSerial = 1, currentSerial = 2))
        assertTrue(readerCanCompleteTtsStart("one", "one", true, true, true,
            requestSerial = 3, currentSerial = 3))
    }

    @Test
    fun freshAtomicNoteDraftBlocksSpeech_withoutAnExistingHighlightId() {
        fun allowed(draft: String?) = readerCanPlayForegroundTts(true, true, false, false, false, false, draft, false)
        assertTrue(allowed(null))
        assertFalse(allowed("fresh-selection-locator"))
    }

    @Test
    fun everyForegroundOrAccessibilityBlockerPreventsSpeech() {
        assertFalse(readerCanPlayForegroundTts(false, true, false, false, false, false, null, false))
        assertFalse(readerCanPlayForegroundTts(true, false, false, false, false, false, null, false))
        for (blockedIndex in 0..4) {
            val flags = List(5) { it == blockedIndex }
            assertFalse(readerCanPlayForegroundTts(true, true, flags[0], flags[1], flags[2], flags[3], null, flags[4]))
        }
    }

    @Test
    fun asynchronousStartRequiresTheOriginalOwnerNavigatorAndExplicitPanel() {
        assertTrue(readerCanCompleteTtsStart("one", "one", true, true, true))
        assertFalse(readerCanCompleteTtsStart("one", "two", true, true, true))
        assertFalse(readerCanCompleteTtsStart("one", "one", false, true, true))
        assertFalse(readerCanCompleteTtsStart("one", "one", true, false, true))
        assertFalse(readerCanCompleteTtsStart("one", "one", true, true, false))
    }
}
