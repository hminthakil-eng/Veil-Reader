package com.veilreader.app.domain;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReadingPolicyPageGateTest {

    @Test
    public void firstCommittedTurnCountsAfterRealResumeDwell() {
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();

        gate.resume(1_000L);

        assertFalse(gate.visit("chapter-2", 8_999L, 0, 0));
        assertTrue(gate.visit("chapter-3", 17_000L, 0, 0));
    }

    @Test
    public void pauseRequiresNewDwellBeforeNextCredit() {
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();

        gate.resume(1_000L);
        assertTrue(gate.visit("chapter-2", 9_000L, 0, 0));

        gate.pause();
        gate.resume(20_000L);

        assertFalse(gate.visit("chapter-3", 27_999L, 1, 1));
        assertTrue(gate.visit("chapter-4", 36_000L, 1, 1));
    }

    @Test
    public void duplicateLocationStillDoesNotFarmCredit() {
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();

        gate.resume(1_000L);
        assertTrue(gate.visit("chapter-2", 9_000L, 0, 0));
        assertFalse(gate.visit("chapter-2", 18_000L, 1, 1));
    }
}
