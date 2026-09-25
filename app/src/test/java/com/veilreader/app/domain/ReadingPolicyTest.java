package com.veilreader.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReadingPolicyTest {
    @Test
    public void firstVisitWithoutBaselineFailsClosed() {
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();

        assertFalse(gate.visit("book:page-2", 8_000L, 0, 1));
    }

    @Test
    public void rebasedGateCreditsFirstRealTurnAfterDwell() {
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();
        gate.rebase(1_000L);

        assertTrue(gate.visit("book:page-2", 9_000L, 0, 1));
    }

    @Test
    public void fastTurnIsRejectedAndBecomesTheNextBaseline() {
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();
        gate.rebase(1_000L);

        assertFalse(gate.visit("book:page-2", 5_000L, 0, 1));
        assertTrue(gate.visit("book:page-3", 13_000L, 0, 1));
    }

    @Test
    public void pauseRequiresARealResumeBaselineBeforeCreditingAgain() {
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();
        gate.rebase(1_000L);
        assertTrue(gate.visit("book:page-2", 9_000L, 0, 1));

        gate.pause();
        assertFalse(gate.visit("book:page-3", 20_000L, 1, 2));

        gate.rebase(20_000L);
        assertTrue(gate.visit("book:page-4", 28_000L, 1, 2));
    }

    @Test
    public void duplicateLocationStillCannotEarnTwice() {
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();
        gate.rebase(0L);
        assertTrue(gate.visit("book:page-2", 8_000L, 0, 1));

        assertFalse(gate.visit("book:page-2", 16_000L, 1, 2));
    }
}
