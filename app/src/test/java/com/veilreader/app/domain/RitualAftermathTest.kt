package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RitualAftermathTest {
    private val valid = RitualAftermathRecord(
        pathId = "oracle",
        fromRankIndex = 1,
        toRankIndex = 2,
        sealedAtEpochMs = 1_000L
    )

    @Test
    fun `valid seal is descriptive only when it matches authoritative progression`() {
        assertSame(
            valid,
            validateRitualAftermath(
                record = valid,
                currentPathId = "oracle",
                currentRankIndex = 2,
                rankCount = 6
            )
        )
    }

    @Test
    fun `corrupt or future-rank seal fails closed`() {
        assertNull(
            validateRitualAftermath(
                record = valid.copy(pathId = "nocturne"),
                currentPathId = "oracle",
                currentRankIndex = 2,
                rankCount = 6
            )
        )
        assertNull(
            validateRitualAftermath(
                record = valid.copy(toRankIndex = 4),
                currentPathId = "oracle",
                currentRankIndex = 4,
                rankCount = 6
            )
        )
        assertNull(
            validateRitualAftermath(
                record = valid.copy(fromRankIndex = 3, toRankIndex = 4),
                currentPathId = "oracle",
                currentRankIndex = 2,
                rankCount = 6
            )
        )
    }

    @Test
    fun `afterglow is full then fades while the seal remains valid`() {
        val sixHours = 6L * 60L * 60L * 1000L
        val seventyTwoHours = 72L * 60L * 60L * 1000L
        val record = valid.copy(sealedAtEpochMs = 10_000L)

        assertEquals(1f, ritualAfterglowIntensity(record, 10_000L + sixHours), 0.0001f)
        val middle = ritualAfterglowIntensity(
            record,
            10_000L + 36L * 60L * 60L * 1000L
        )
        assertTrue(middle in 0f..1f)
        assertTrue(middle < 1f)
        assertEquals(
            0f,
            ritualAfterglowIntensity(record, 10_000L + seventyTwoHours),
            0.0001f
        )
        assertEquals(0f, ritualAfterglowIntensity(record, 9_999L), 0.0001f)
    }
}
