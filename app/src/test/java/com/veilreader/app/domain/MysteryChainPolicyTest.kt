package com.veilreader.app.domain

import com.veilreader.app.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MysteryChainPolicyTest {
    private fun profile(
        streakDays: Int = 0,
        pagesRead: Int = 0,
        minutesRead: Int = 0,
        booksFinished: Int = 0,
        rankIndex: Int = 0,
        sigils: Set<String> = emptySet(),
        discoveries: Set<String> = emptySet()
    ) = ReaderProfile(
        level = 1,
        xp = 0,
        xpForNextLevel = 100,
        streakDays = streakDays,
        pagesRead = pagesRead,
        minutesRead = minutesRead,
        booksFinished = booksFinished,
        path = SampleData.paths.first(),
        rankIndex = rankIndex,
        ritualProgress = 0,
        ritualTarget = 1,
        earnedSigils = sigils,
        earnedDiscoveries = discoveries
    )

    @Test
    fun `every durable discovery has one mystery chain`() {
        assertEquals(
            VeiledDiscoveryPolicy.orderedIds,
            mysteryChainDefinitions().map { it.id }
        )
    }

    @Test
    fun `patient flame reveals clues gradually without exposing final checklist`() {
        val initial = requireNotNull(
            mysteryChainSnapshot(
                VeiledDiscoveryPolicy.PATIENT_FLAME,
                profile(),
                0
            )
        )
        val duration = requireNotNull(
            mysteryChainSnapshot(
                VeiledDiscoveryPolicy.PATIENT_FLAME,
                profile(minutesRead = 180),
                0
            )
        )
        val returning = requireNotNull(
            mysteryChainSnapshot(
                VeiledDiscoveryPolicy.PATIENT_FLAME,
                profile(
                    minutesRead = 180,
                    sigils = setOf("seven_days")
                ),
                0
            )
        )

        assertEquals(0, initial.visibleFragmentIndex)
        assertEquals(1, duration.visibleFragmentIndex)
        assertEquals(2, returning.visibleFragmentIndex)
        assertFalse(returning.complete)
    }

    @Test
    fun `earned discovery remains complete even if transient signals later fall`() {
        val snapshot = requireNotNull(
            mysteryChainSnapshot(
                VeiledDiscoveryPolicy.PATIENT_FLAME,
                profile(
                    discoveries = setOf(VeiledDiscoveryPolicy.PATIENT_FLAME)
                ),
                0
            )
        )

        assertTrue(snapshot.complete)
        assertEquals(2, snapshot.visibleFragmentIndex)
    }
}
