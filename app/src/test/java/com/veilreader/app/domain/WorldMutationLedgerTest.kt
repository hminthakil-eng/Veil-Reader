package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldMutationLedgerTest {
    private val path = ReadingPath(
        id = "archivist",
        name = "Archivist",
        epithet = "Keeper",
        description = "",
        ranks = listOf("Novice", "Reader", "Keeper", "Curator", "Archivist", "Elder")
    )

    private fun profile(rank: Int = 0): ReaderProfile =
        ReaderProfile(
            level = 5,
            xp = 0,
            xpForNextLevel = 300,
            streakDays = 0,
            pagesRead = 0,
            minutesRead = 0,
            booksFinished = 0,
            path = path,
            rankIndex = rank,
            ritualProgress = 0,
            ritualTarget = 10
        )

    @Test
    fun `empty evidence and base rank produce no invented mutations`() {
        val ledger = deriveWorldMutationLedger(profile(), CastleMemoryState.EMPTY)
        assertTrue(ledger.entries.isEmpty())
    }

    @Test
    fun `rank consequence is durable but never claims reading evidence`() {
        val ledger = deriveWorldMutationLedger(profile(rank = 3), CastleMemoryState.EMPTY)
        val mutation = ledger.entries.single()

        assertEquals(WorldMutationKind.PATH_ASCENSION, mutation.kind)
        assertEquals(WorldMutationEvidence.PATH_RANK, mutation.evidence)
        assertEquals(3, mutation.evidenceCount)
        assertTrue(mutation.durable)
        assertTrue(WorldMutationRealm.SANCTUM in mutation.realms)
    }

    @Test
    fun `recorded advancement seal persists into Hall Treasury and Sanctum`() {
        val profile = profile(rank = 2).copy(
            ritualAftermath = RitualAftermathRecord(
                pathId = path.id,
                fromRankIndex = 1,
                toRankIndex = 2,
                sealedAtEpochMs = 100L
            )
        )
        val seal = deriveWorldMutationLedger(profile, CastleMemoryState.EMPTY)
            .entries
            .first { it.kind == WorldMutationKind.ADVANCEMENT_SEAL }

        assertEquals(WorldMutationEvidence.RITUAL_SEAL, seal.evidence)
        assertTrue(seal.durable)
        assertTrue(WorldMutationRealm.GREAT_HALL in seal.realms)
        assertTrue(WorldMutationRealm.TREASURY in seal.realms)
        assertTrue(WorldMutationRealm.SANCTUM in seal.realms)
    }

    @Test
    fun `sealed Silent Names receipt becomes a durable Hall and Treasury story relic only`() {
        val receipt = SilentNamesEncounter.resolve(
            pathId = "archivist",
            choice = SilentNamesChoice.EXAMINE_SEAL,
            mode = SilentNamesMode.STORY,
            dice = null,
            recordedAtEpochMs = 1234L
        ).receipt

        val ledger = deriveWorldMutationLedger(
            profile = profile(),
            memory = CastleMemoryState.EMPTY,
            storyRelics = deriveStoryRelics(receipt)
        )
        val mutation = ledger.entries.single()

        assertEquals(WorldMutationKind.STORY_RELIC, mutation.kind)
        assertEquals(WorldMutationEvidence.STORY_RECEIPT, mutation.evidence)
        assertEquals(1, mutation.evidenceCount)
        assertTrue(mutation.durable)
        assertEquals(
            setOf(WorldMutationRealm.GREAT_HALL, WorldMutationRealm.TREASURY),
            mutation.realms
        )
    }

    @Test
    fun `invalid Silent Names receipt cannot manifest a world relic`() {
        val valid = SilentNamesEncounter.resolve(
            pathId = "archivist",
            choice = SilentNamesChoice.EXAMINE_SEAL,
            mode = SilentNamesMode.STORY,
            dice = null,
            recordedAtEpochMs = 1234L
        ).receipt
        val invalid = valid.copy(contentVersion = valid.contentVersion + 1)

        val ledger = deriveWorldMutationLedger(
            profile = profile(),
            memory = CastleMemoryState.EMPTY,
            storyRelics = deriveStoryRelics(invalid)
        )

        assertTrue(ledger.entries.isEmpty())
    }

    @Test
    fun `return awakening remains Hall-only and non-durable`() {
        val memory = CastleMemoryState.EMPTY.copy(returnAwakening = 0.72f)
        val mutation = deriveWorldMutationLedger(profile(), memory).entries.single()

        assertEquals(WorldMutationKind.RETURN_AWAKENING, mutation.kind)
        assertFalse(mutation.durable)
        assertEquals(setOf(WorldMutationRealm.GREAT_HALL), mutation.realms)
    }

    @Test
    fun `durable reading evidence fans out to the intended realms`() {
        val memory = CastleMemoryState.EMPTY.copy(
            completedCount = 4,
            annotationCount = 12,
            atlasLinkCount = 9,
            rereadCycleCount = 2,
            sessionCount = 20,
            completionAlcoves = 4,
            scriptoriumLamps = 3,
            rereadRings = 2,
            foundationCourses = 5,
            observatoryResonance = 0.62f
        )
        val ledger = deriveWorldMutationLedger(profile(rank = 2), memory)

        assertTrue(
            ledger.forRealm(WorldMutationRealm.TREASURY)
                .any { it.kind == WorldMutationKind.COMPLETION_ALCOVES }
        )
        assertTrue(
            ledger.forRealm(WorldMutationRealm.ARCHIVE)
                .any { it.kind == WorldMutationKind.SCRIPTORIUM_LIGHT }
        )
        assertTrue(
            ledger.forRealm(WorldMutationRealm.OBSERVATORY)
                .any { it.kind == WorldMutationKind.CONSTELLATION_WEB }
        )
        assertTrue(
            ledger.forRealm(WorldMutationRealm.SANCTUM)
                .any { it.kind == WorldMutationKind.REREAD_PATINA }
        )
        assertTrue(ledger.durableCount >= 6)
    }
}
