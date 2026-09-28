package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryRelicTest {
    @Test
    fun `valid Silent Names receipt projects one canonical story relic`() {
        val receipt = SilentNamesEncounter.resolve(
            pathId = "oracle",
            choice = SilentNamesChoice.SPEAK_TO_KEEPER,
            mode = SilentNamesMode.STORY,
            dice = null,
            recordedAtEpochMs = 500L
        ).receipt

        val relic = deriveStoryRelics(receipt).single()

        assertEquals(SilentNamesEncounter.REWARD_ID, relic.relicId)
        assertEquals(SilentNamesEncounter.ID, relic.sourceEncounterId)
        assertEquals(SilentNamesEncounter.CONTENT_VERSION, relic.sourceContentVersion)
        assertEquals("oracle", relic.pathIdAtAcquisition)
        assertEquals(SilentNamesOutcome.KEEPER_TESTIMONY.name, relic.routeVariantId)
        assertEquals(SilentNamesMode.STORY.name, relic.resolutionModeId)
        assertEquals(500L, relic.recordedAtEpochMs)
    }

    @Test
    fun `future or forged Silent Names receipt projects nothing`() {
        val valid = SilentNamesEncounter.resolve(
            pathId = "oracle",
            choice = SilentNamesChoice.EXAMINE_SEAL,
            mode = SilentNamesMode.STORY,
            dice = null,
            recordedAtEpochMs = 600L
        ).receipt

        assertTrue(
            deriveStoryRelics(
                valid.copy(contentVersion = valid.contentVersion + 1)
            ).isEmpty()
        )
        assertTrue(
            deriveStoryRelics(
                valid.copy(rewardId = "forged")
            ).isEmpty()
        )
    }

    @Test
    fun `normalization keeps one deterministic earliest record per relic id`() {
        val later = StoryRelicRecord(
            relicId = "same",
            sourceEncounterId = "encounter-b",
            sourceContentVersion = 1,
            pathIdAtAcquisition = "oracle",
            recordedAtEpochMs = 900L,
            routeVariantId = "B",
            resolutionModeId = "STORY"
        )
        val earlier = later.copy(
            sourceEncounterId = "encounter-a",
            recordedAtEpochMs = 400L,
            routeVariantId = "A"
        )

        val normalized = normalizeStoryRelics(listOf(later, earlier, later))

        assertEquals(1, normalized.size)
        assertEquals(earlier, normalized.single())
    }

    @Test
    fun `catalog keeps story relic effects out of Reader and rank realms`() {
        val lantern = StoryRelicCatalog.lanternOfRemembrance

        assertEquals(SilentNamesEncounter.REWARD_ID, lantern.id)
        assertEquals(
            setOf(WorldMutationRealm.GREAT_HALL, WorldMutationRealm.TREASURY),
            lantern.visibleRealms
        )
        assertTrue(lantern.atmosphereSeedSalt != 0)
    }
}
