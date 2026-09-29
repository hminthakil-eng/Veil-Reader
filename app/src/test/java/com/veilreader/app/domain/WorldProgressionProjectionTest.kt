package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldProgressionProjectionTest {
    private val path = ReadingPath(
        id = "oracle",
        name = "Oracle",
        epithet = "Read between the lines",
        description = "",
        ranks = listOf("Observer", "Cipher", "Investigator", "Augur", "Oracle", "Veilseer")
    )

    private fun profile(
        level: Int = 1,
        rank: Int = 0,
        streak: Int = 0,
        ritualProgress: Int = 0,
        ritualTarget: Int = 5
    ) = ReaderProfile(
        level = level,
        xp = 0,
        xpForNextLevel = 350,
        streakDays = streak,
        pagesRead = 0,
        minutesRead = 0,
        booksFinished = 0,
        path = path,
        rankIndex = rank,
        ritualProgress = ritualProgress,
        ritualTarget = ritualTarget
    )

    @Test
    fun `empty world remains dormant`() {
        val projection = deriveWorldProgressionProjection(
            profile = profile(),
            quests = emptyList(),
            memory = CastleMemoryState.EMPTY
        )

        assertEquals(WorldAwakeningStage.DORMANT, projection.stage)
        assertEquals(0f, projection.rankProgress, 0.0001f)
        assertEquals(0f, projection.ritualCharge, 0.0001f)
        assertEquals(0, projection.streakEmbers)
        assertTrue(projection.architecturalPresence < 0.12f)
    }

    @Test
    fun `daily directives can warm world but cannot impersonate rank progress`() {
        val quests = listOf(
            Quest("a", "A", 1, 1, 10),
            Quest("b", "B", 2, 2, 10),
            Quest("c", "C", 3, 3, 10)
        )
        val projection = deriveWorldProgressionProjection(
            profile = profile(),
            quests = quests,
            memory = CastleMemoryState.EMPTY
        )

        assertEquals(0f, projection.rankProgress, 0.0001f)
        assertEquals(1f, projection.questResonance, 0.0001f)
        assertEquals(3, projection.completedDirectives)
        assertTrue(projection.architecturalPresence < 0.20f)
    }

    @Test
    fun `rank ritual and durable memory compound into stronger architecture`() {
        val low = deriveWorldProgressionProjection(
            profile = profile(),
            quests = emptyList(),
            memory = CastleMemoryState.EMPTY
        )
        val richMemory = CastleMemoryState.EMPTY.copy(
            overallPresence = 0.72f,
            archiveResonance = 0.65f,
            observatoryResonance = 0.54f,
            treasuryResonance = 0.46f,
            sanctumResonance = 0.42f
        )
        val high = deriveWorldProgressionProjection(
            profile = profile(level = 18, rank = 4, streak = 9, ritualProgress = 4),
            quests = listOf(
                Quest("a", "A", 1, 1, 10),
                Quest("b", "B", 1, 2, 10)
            ),
            memory = richMemory
        )

        assertTrue(high.architecturalPresence > low.architecturalPresence)
        assertTrue(high.archiveDepth > low.archiveDepth)
        assertTrue(high.observatorySignal > low.observatorySignal)
        assertTrue(high.relicWeight > low.relicWeight)
        assertTrue(high.sanctumPresence > low.sanctumPresence)
        assertTrue(high.stage.ordinal > low.stage.ordinal)
    }

    @Test
    fun `projection values remain bounded under corrupt counters`() {
        val projection = deriveWorldProgressionProjection(
            profile = profile(
                level = Int.MAX_VALUE,
                rank = Int.MAX_VALUE,
                streak = Int.MAX_VALUE,
                ritualProgress = Int.MAX_VALUE,
                ritualTarget = 1
            ),
            quests = listOf(Quest("x", "X", Int.MAX_VALUE, 1, 10)),
            memory = CastleMemoryState.EMPTY.copy(
                overallPresence = 2f,
                archiveResonance = 2f,
                observatoryResonance = 2f,
                treasuryResonance = 2f,
                sanctumResonance = 2f
            )
        )

        listOf(
            projection.rankProgress,
            projection.ritualCharge,
            projection.questResonance,
            projection.architecturalPresence,
            projection.archiveDepth,
            projection.mirrorClarity,
            projection.observatorySignal,
            projection.relicWeight,
            projection.sanctumPresence
        ).forEach { value ->
            assertTrue(value in 0f..1f)
        }
        assertEquals(7, projection.streakEmbers)
    }
}
