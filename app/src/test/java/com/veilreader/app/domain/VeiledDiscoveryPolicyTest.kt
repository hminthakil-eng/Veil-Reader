package com.veilreader.app.domain

import com.veilreader.app.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeiledDiscoveryPolicyTest {
    private fun profile(
        streakDays: Int = 0,
        pagesRead: Int = 0,
        minutesRead: Int = 0,
        booksFinished: Int = 0,
        rankIndex: Int = 0,
        earnedSigils: Set<String> = emptySet()
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
        earnedSigils = earnedSigils
    )

    @Test
    fun eligibility_uses_truthful_thresholds() {
        val eligible = VeiledDiscoveryPolicy.eligibleIds(
            profile(
                streakDays = 7,
                pagesRead = 1_000,
                minutesRead = 3_000,
                booksFinished = 10,
                rankIndex = 3,
                earnedSigils = setOf("a", "b", "c", "d", "e")
            ),
            highlightCount = 10
        )

        assertEquals(
            setOf(
                VeiledDiscoveryPolicy.PATIENT_FLAME,
                VeiledDiscoveryPolicy.MARGINALIA_GATE,
                VeiledDiscoveryPolicy.DEEP_SHELF,
                VeiledDiscoveryPolicy.LONG_WATCH,
                VeiledDiscoveryPolicy.VEIL_THINS,
                VeiledDiscoveryPolicy.UNNAMED_CHAMBER
            ),
            eligible
        )
    }

    @Test
    fun patient_flame_requires_both_consistency_and_time() {
        assertFalse(
            VeiledDiscoveryPolicy.PATIENT_FLAME in
                VeiledDiscoveryPolicy.eligibleIds(
                    profile(streakDays = 7, minutesRead = 599),
                    highlightCount = 0
                )
        )
        assertTrue(
            VeiledDiscoveryPolicy.PATIENT_FLAME in
                VeiledDiscoveryPolicy.eligibleIds(
                    profile(streakDays = 7, minutesRead = 600),
                    highlightCount = 0
                )
        )
    }
}
