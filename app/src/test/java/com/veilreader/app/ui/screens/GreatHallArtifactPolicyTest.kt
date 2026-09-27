package com.veilreader.app.ui.screens

import com.veilreader.app.domain.CastleMemoryState
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GreatHallArtifactPolicyTest {
    private val path = ReadingPath(
        id = "oracle",
        name = "Oracle",
        epithet = "See",
        description = "Test path",
        ranks = listOf("R0", "R1", "R2", "R3", "R4", "R5")
    )

    private fun profile(rankIndex: Int) = ReaderProfile(
        level = 1,
        xp = 0,
        xpForNextLevel = 100,
        streakDays = 0,
        pagesRead = 0,
        minutesRead = 0,
        booksFinished = 0,
        path = path,
        rankIndex = rankIndex,
        ritualProgress = 0,
        ritualTarget = 1
    )

    @Test
    fun `Great Hall exposes the eight canonical artifact destinations`() {
        val artifacts = greatHallArtifacts(
            profile = profile(5),
            memoryState = CastleMemoryState.EMPTY,
            livingMirrorNoteCount = 0,
            canAdvance = false
        )

        assertEquals(8, artifacts.size)
        assertEquals(
            mapOf(
                GreatHallArtifactKind.MIRROR to "mirror",
                GreatHallArtifactKind.ASTROLABE to "observatory",
                GreatHallArtifactKind.ARCHIVE_GATE to "library",
                GreatHallArtifactKind.LEDGER to "profile",
                GreatHallArtifactKind.RITUAL_SEAL to "ritual",
                GreatHallArtifactKind.RELIQUARY to "treasury",
                GreatHallArtifactKind.VEILED_DOOR to "sanctum",
                GreatHallArtifactKind.READING_SEAT to "reading"
            ),
            artifacts.associate { it.kind to it.route }
        )
    }

    @Test
    fun `artifact unlock ranks preserve existing progression authority`() {
        val artifacts = greatHallArtifacts(
            profile = profile(0),
            memoryState = CastleMemoryState.EMPTY,
            livingMirrorNoteCount = 0,
            canAdvance = false
        ).associateBy { it.kind }

        assertEquals(0, artifacts.getValue(GreatHallArtifactKind.MIRROR).unlockRank)
        assertEquals(0, artifacts.getValue(GreatHallArtifactKind.ARCHIVE_GATE).unlockRank)
        assertEquals(0, artifacts.getValue(GreatHallArtifactKind.LEDGER).unlockRank)
        assertEquals(0, artifacts.getValue(GreatHallArtifactKind.READING_SEAT).unlockRank)
        assertEquals(1, artifacts.getValue(GreatHallArtifactKind.RITUAL_SEAL).unlockRank)
        assertEquals(2, artifacts.getValue(GreatHallArtifactKind.ASTROLABE).unlockRank)
        assertEquals(4, artifacts.getValue(GreatHallArtifactKind.RELIQUARY).unlockRank)
        assertEquals(5, artifacts.getValue(GreatHallArtifactKind.VEILED_DOOR).unlockRank)
    }

    @Test
    fun `Mirror awakening is factual and does not gate access`() {
        val dormant = greatHallArtifacts(
            profile = profile(0),
            memoryState = CastleMemoryState.EMPTY,
            livingMirrorNoteCount = 0,
            canAdvance = false
        ).first { it.kind == GreatHallArtifactKind.MIRROR }

        val awakened = greatHallArtifacts(
            profile = profile(0),
            memoryState = CastleMemoryState.EMPTY,
            livingMirrorNoteCount = 3,
            canAdvance = false
        ).first { it.kind == GreatHallArtifactKind.MIRROR }

        assertEquals(0, dormant.unlockRank)
        assertFalse(dormant.awakened)
        assertTrue(awakened.awakened)
        assertTrue(awakened.resonance > dormant.resonance)
    }

    @Test
    fun `Ritual glow reflects readiness without changing unlock authority`() {
        val notReady = greatHallArtifacts(
            profile = profile(1),
            memoryState = CastleMemoryState.EMPTY,
            livingMirrorNoteCount = 0,
            canAdvance = false
        ).first { it.kind == GreatHallArtifactKind.RITUAL_SEAL }

        val ready = greatHallArtifacts(
            profile = profile(1),
            memoryState = CastleMemoryState.EMPTY,
            livingMirrorNoteCount = 0,
            canAdvance = true
        ).first { it.kind == GreatHallArtifactKind.RITUAL_SEAL }

        assertEquals(notReady.unlockRank, ready.unlockRank)
        assertFalse(notReady.awakened)
        assertTrue(ready.awakened)
        assertEquals(1f, ready.resonance)
    }
}
