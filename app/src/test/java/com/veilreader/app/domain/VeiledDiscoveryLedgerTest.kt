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
        val earned = VeiledDiscoveryCatalog.mergeEarned(
            existingIds = emptySet(),
            profile = profile(
                streakDays = 7,
                minutesRead = 600
            ),
            highlightCount = 0
        )

        assertTrue(VeiledDiscoveryCatalog.PATIENT_FLAME in earned)

        val afterReset = VeiledDiscoveryCatalog.mergeEarned(
            existingIds = earned,
            profile = profile(
                streakDays = 0,
                minutesRead = 600
            ),
            highlightCount = 0
        )

        assertTrue(VeiledDiscoveryCatalog.PATIENT_FLAME in afterReset)
        assertEquals(earned, afterReset)
    }

    @Test
    fun `every discovery has an explicit qualifying rule`() {
        val qualified = VeiledDiscoveryCatalog.qualifyingIds(
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

        assertEquals(
            VeiledDiscoveryCatalog.orderedIds.toSet(),
            qualified
        )
    }

    @Test
    fun `marginalia gate requires both passages and page history`() {
        val enoughMarksOnly = VeiledDiscoveryCatalog.qualifyingIds(
            profile = profile(pagesRead = 999),
            highlightCount = 20
        )
        val enoughPagesOnly = VeiledDiscoveryCatalog.qualifyingIds(
            profile = profile(pagesRead = 2_000),
            highlightCount = 9
        )
        val complete = VeiledDiscoveryCatalog.qualifyingIds(
            profile = profile(pagesRead = 1_000),
            highlightCount = 10
        )

        assertFalse(VeiledDiscoveryCatalog.MARGINALIA_GATE in enoughMarksOnly)
        assertFalse(VeiledDiscoveryCatalog.MARGINALIA_GATE in enoughPagesOnly)
        assertTrue(VeiledDiscoveryCatalog.MARGINALIA_GATE in complete)
    }

    @Test
    fun `ledger rejects unknown ids while preserving known earned discoveries`() {
        val merged = VeiledDiscoveryCatalog.mergeEarned(
            existingIds = setOf(
                VeiledDiscoveryCatalog.LONG_WATCH,
                "unknown_future_fragment"
            ),
            profile = profile(),
            highlightCount = 0
        )

        assertEquals(setOf(VeiledDiscoveryCatalog.LONG_WATCH), merged)
    }
}
