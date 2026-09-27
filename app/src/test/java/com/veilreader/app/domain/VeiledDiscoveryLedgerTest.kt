package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeiledDiscoveryLedgerTest {
    private val path = ReadingPath(
        id = "archivist",
        name = "Archivist",
        epithet = "Keeper of Memory",
        description = "A test path",
        ranks = listOf("Witness", "Curator", "Keeper", "Sovereign")
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
        ritualTarget = 1,
        earnedSigils = earnedSigils
    )

    @Test
    fun `patient flame remains in ledger after streak resets`() {
        val earned = VeiledDiscoveryPolicy.mergeEarned(
            existingIds = emptySet(),
            profile = profile(streakDays = 7, minutesRead = 600),
            highlightCount = 0
        )
        assertTrue(VeiledDiscoveryPolicy.PATIENT_FLAME in earned)

        val afterReset = VeiledDiscoveryPolicy.mergeEarned(
            existingIds = earned,
            profile = profile(streakDays = 0, minutesRead = 600),
            highlightCount = 0
        )
        assertEquals(earned, afterReset)
    }

    @Test
    fun `every discovery has an explicit qualifying rule`() {
        val qualified = VeiledDiscoveryPolicy.eligibleIds(
            profile = profile(
                streakDays = 8,
                pagesRead = 1_200,
                minutesRead = 3_100,
                booksFinished = 12,
                rankIndex = 3,
                earnedSigils = setOf("a", "b", "c", "d", "e")
            ),
            highlightCount = 12
        )
        assertEquals(VeiledDiscoveryPolicy.orderedIds.toSet(), qualified)
    }

    @Test
    fun `marginalia gate requires both passages and page history`() {
        assertFalse(
            VeiledDiscoveryPolicy.MARGINALIA_GATE in VeiledDiscoveryPolicy.eligibleIds(
                profile = profile(pagesRead = 999),
                highlightCount = 20
            )
        )
        assertFalse(
            VeiledDiscoveryPolicy.MARGINALIA_GATE in VeiledDiscoveryPolicy.eligibleIds(
                profile = profile(pagesRead = 2_000),
                highlightCount = 9
            )
        )
        assertTrue(
            VeiledDiscoveryPolicy.MARGINALIA_GATE in VeiledDiscoveryPolicy.eligibleIds(
                profile = profile(pagesRead = 1_000),
                highlightCount = 10
            )
        )
    }

    @Test
    fun `merge preserves unknown future ids`() {
        val merged = VeiledDiscoveryPolicy.mergeEarned(
            existingIds = setOf(VeiledDiscoveryPolicy.LONG_WATCH, "unknown_future_fragment"),
            profile = profile(),
            highlightCount = 0
        )
        assertTrue("unknown_future_fragment" in merged)
        assertFalse("unknown_future_fragment" in VeiledDiscoveryPolicy.orderedIds)
    }

    @Test
    fun `durable seven day sigil can grandfather patient flame after streak expiry`() {
        val qualified = VeiledDiscoveryPolicy.eligibleIds(
            profile = profile(
                streakDays = 0,
                minutesRead = 600,
                earnedSigils = setOf("seven_days")
            ),
            highlightCount = 0
        )
        assertTrue(VeiledDiscoveryPolicy.PATIENT_FLAME in qualified)
    }
}
