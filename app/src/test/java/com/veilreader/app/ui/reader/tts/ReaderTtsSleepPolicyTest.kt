package com.veilreader.app.ui.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderTtsSleepPolicyTest {
    @Test
    fun sleepTimerAcceptsExplicitOffAndBoundedMinuteDurations() {
        assertEquals(0, normalizedTtsSleepMinutes(0))
        assertEquals(5, normalizedTtsSleepMinutes(5))
        assertEquals(180, normalizedTtsSleepMinutes(180))
        assertNull(normalizedTtsSleepMinutes(-1))
        assertNull(normalizedTtsSleepMinutes(4))
        assertNull(normalizedTtsSleepMinutes(181))
    }

    @Test
    fun deadlineUsesEpochTimeWithoutPollingMathDrift() {
        assertEquals(1_900_000L, ttsSleepDeadline(1_000_000L, 15))
        assertEquals(4_600_000L, ttsSleepDeadline(1_000_000L, 60))
    }
}
