package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeiledDiscoveryPolicyTest {
    private val path = ReadingPath(
        id = "oracle",
        name = "Oracle",
        epithet = "Test",
        description = "Test",
        ranks = listOf("Observer", "Cipher", "Investigator", "Augur")
    )

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
        xpForNextLevel = 350,
        streakDays = streakDays,
        pagesRead = pagesRead,
        minutesRead = minutesRead,
        booksFinished = booksFinished,
        path = path,
        rankIndex = rankIndex,
        ritualProgress = 0,
        ritualTarget = 5,
        earnedSigils = earnedSigils
    )

    @Test
    fun `patient flame qualifies from factual reading history`() {
        val qualified = VeiledDiscoveryPolicy.currentlyQualified(
            profile = profile(streakDays = 7, minutesRead = 600),
            highlightCount = 0
        )

        assertTrue(VeiledDiscoveryPolicy.PATIENT_FLAME in qualified)
    }

    @Test
    fun `historical discovery stays revealed after live condition disappears`() {
        val persisted = setOf(VeiledDiscoveryPolicy.PATIENT_FLAME)
        val current = VeiledDiscoveryPolicy.currentlyQualified(
            profile = profile(streakDays = 1, minutesRead = 700),
            highlightCount = 0
        )

        assertFalse(VeiledDiscoveryPolicy.PATIENT_FLAME in current)
        assertTrue(
            VeiledDiscoveryPolicy.PATIENT_FLAME in
                VeiledDiscoveryPolicy.mergeHistorical(persisted, current)
        )
    }

    @Test
    fun `unknown persisted discovery ids are discarded`() {
        val merged = VeiledDiscoveryPolicy.mergeHistorical(
            persisted = setOf("old_removed_discovery"),
            currentlyQualified = emptySet()
        )

        assertEquals(emptySet<String>(), merged)
    }

    @Test
    fun `all discovery conditions can accumulate without consuming one another`() {
        val qualified = VeiledDiscoveryPolicy.currentlyQualified(
            profile = profile(
                streakDays = 12,
                pagesRead = 2_000,
                minutesRead = 4_000,
                booksFinished = 20,
                rankIndex = 3,
                earnedSigils = setOf("a", "b", "c", "d", "e")
            ),
            highlightCount = 20
        )

        assertEquals(VeiledDiscoveryPolicy.allIds, qualified)
    }
}
