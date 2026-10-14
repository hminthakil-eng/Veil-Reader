package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RagsToRichesAvatarTest {
    @Test fun stageMappingIsExhaustiveAndStable() {
        assertEquals(RagsToRichesStage.WANDERER_CAMP, ragsToRichesStageFor(ReadingHomeStage.CAMP))
        assertEquals(RagsToRichesStage.APPRENTICE_HUT, ragsToRichesStageFor(ReadingHomeStage.HUT))
        assertEquals(RagsToRichesStage.SCHOLAR_HOUSE, ragsToRichesStageFor(ReadingHomeStage.HOUSE))
        assertEquals(RagsToRichesStage.KEEPER_CASTLE, ragsToRichesStageFor(ReadingHomeStage.CASTLE))
        assertEquals(4, RagsToRichesStage.entries.map { it.assetId }.distinct().size)
    }

    @Test fun invalidAndRestoredAvatarOptionsFailCalm() {
        val invalid = RagsToRichesAvatar(
            schemaVersion = 999, hairStyle = -70, hairColor = 99,
            skinTone = Int.MAX_VALUE, outfitStyle = Int.MIN_VALUE, accentColor = 13
        )
        val safe = invalid.safeForPreview()
        assertEquals(1, safe.schemaVersion)
        assertEquals(0, safe.hairStyle)
        assertEquals(3, safe.hairColor)
        assertEquals(3, safe.skinTone)
        assertEquals(0, safe.outfitStyle)
        assertEquals(3, safe.accentColor)
        assertEquals(999, invalid.schemaVersion) // projection did not mutate the stored object
    }

    @Test fun earlierWorldTiersNeverDemoteEarnedCastle() {
        val scene = previewRagsScene(
            activeMinutes = 0, completedBooks = 0, avatar = RagsToRichesAvatar(),
            highestEarnedStage = RagsToRichesStage.KEEPER_CASTLE
        )
        assertEquals(RagsToRichesStage.KEEPER_CASTLE, scene.stage)
        assertEquals("static", scene.qualityId)
    }

    @Test fun progressionUsesOnlyRealCountersAndIsDeterministic() {
        val character = RagsToRichesAvatar(hairColor = 2, outfitStyle = 3)
        assertEquals(RagsToRichesStage.WANDERER_CAMP, previewRagsScene(0, 999, character).stage)
        assertEquals(RagsToRichesStage.APPRENTICE_HUT, previewRagsScene(15, 0, character).stage)
        assertEquals(RagsToRichesStage.SCHOLAR_HOUSE, previewRagsScene(120, 0, character).stage)
        assertEquals(RagsToRichesStage.KEEPER_CASTLE, previewRagsScene(600, 0, character).stage)
        assertEquals(RagsToRichesStage.KEEPER_CASTLE, previewRagsScene(600, 0, character).stage)
    }

    @Test fun noRuntimeEngineTypesLeakIntoPortableDomain() {
        val scene = previewRagsScene(60, 0, RagsToRichesAvatar())
        assertTrue(scene.stage.assetId.startsWith("world.grayfog."))
        assertEquals("reading", scene.poseId)
    }
}
