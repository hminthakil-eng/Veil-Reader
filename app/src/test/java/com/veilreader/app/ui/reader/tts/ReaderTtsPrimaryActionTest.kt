package com.veilreader.app.ui.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Test

/** Regressions for owner-reported "Pause won't pause" while a book is still loading. */
class ReaderTtsPrimaryActionTest {
    @Test
    fun pendingStartIsPauseEvenWhenTheServiceStillLooksStopped() {
        assertEquals(
            ReaderTtsPrimaryAction.PAUSE,
            readerTtsPrimaryAction(ReaderTtsPhase.STOPPED, startPending = true)
        )
        assertEquals(
            ReaderTtsPrimaryAction.PAUSE,
            readerTtsPrimaryAction(ReaderTtsPhase.PAUSED, startPending = true)
        )
    }

    @Test
    fun preparingAndSpeakingAreBothCancellableByTheSameVisibleButton() {
        assertEquals(
            ReaderTtsPrimaryAction.PAUSE,
            readerTtsPrimaryAction(ReaderTtsPhase.PREPARING, startPending = false)
        )
        assertEquals(
            ReaderTtsPrimaryAction.PAUSE,
            readerTtsPrimaryAction(ReaderTtsPhase.PLAYING, startPending = false)
        )
    }

    @Test
    fun pausedResumesAndStoppedRestartsOnlyWithExplicitUserTap() {
        assertEquals(
            ReaderTtsPrimaryAction.RESUME,
            readerTtsPrimaryAction(ReaderTtsPhase.PAUSED, startPending = false)
        )
        assertEquals(
            ReaderTtsPrimaryAction.START,
            readerTtsPrimaryAction(ReaderTtsPhase.STOPPED, startPending = false)
        )
    }
}
