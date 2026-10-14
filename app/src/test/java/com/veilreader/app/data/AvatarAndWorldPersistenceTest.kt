package com.veilreader.app.data

import android.content.Context
import com.veilreader.app.domain.RagsToRichesAvatar
import com.veilreader.app.domain.RagsToRichesStage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AvatarAndWorldPersistenceTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        game().edit().clear().commit()
        legacy().edit().clear().commit()
    }

    @After fun tearDown() {
        game().edit().clear().commit()
        legacy().edit().clear().commit()
    }

    @Test fun avatarSaveSurvivesRecreationInBackupIncludedGamePreferences() {
        val original = RagsToRichesAvatar(hairStyle = 3, hairColor = 2, skinTone = 0,
            outfitStyle = 2, accentColor = 1)
        assertTrue(AvatarAppearanceStore(context).save(original))
        assertEquals(original, AvatarAppearanceStore(context).read())
        assertEquals(3, game().getInt("avatarV1.hairStyle", -1))
        assertTrue(game().contains("avatarV1.schemaVersion"))
    }

    @Test fun legacyAvatarMigratesOnlyWhenNoRestoredIdentityExists() {
        legacy().edit().putInt("schemaVersion", 1).putInt("hairStyle", 3).commit()
        assertEquals(3, AvatarAppearanceStore(context).read().hairStyle)
        // A restored backup always takes precedence over leftover pilot prefs.
        game().edit().putInt("avatarV1.schemaVersion", 1)
            .putInt("avatarV1.hairStyle", 1).commit()
        assertEquals(1, AvatarAppearanceStore(context).read().hairStyle)
    }

    @Test fun restoredInvalidCosmeticsFailSafeWithoutRewritingTheBackup() {
        game().edit().putInt("avatarV1.schemaVersion", 87)
            .putInt("avatarV1.hairStyle", 900).commit()
        assertEquals(3, AvatarAppearanceStore(context).read().hairStyle)
        assertEquals(87, game().getInt("avatarV1.schemaVersion", -1))
    }

    @Test fun highWaterDoesNotDemoteAndIsSavedInGameBackupPreferences() {
        val stages = WorldStageStore(context)
        assertTrue(stages.reconcile(600, 0))
        assertEquals(RagsToRichesStage.KEEPER_CASTLE, stages.read())
        assertTrue(stages.reconcile(0, 0))
        assertEquals(RagsToRichesStage.KEEPER_CASTLE, WorldStageStore(context).read())
        assertEquals(3, game().getInt("world.highestEarnedTier.v1", -1))
    }

    @Test fun importedCompletionsAloneDoNotBuildTheWorld() {
        val stages = WorldStageStore(context)
        assertTrue(stages.reconcile(0, 1_000))
        assertEquals(RagsToRichesStage.WANDERER_CAMP, stages.read())
        assertTrue(stages.reconcile(15, 0))
        assertEquals(RagsToRichesStage.APPRENTICE_HUT, stages.read())
    }

    @Test fun repositoryPublishesDurableMilestoneWithoutCompose() {
        game().edit().putInt("minutesRead", 120).commit()
        GameRepository(context)
        assertEquals(RagsToRichesStage.SCHOLAR_HOUSE, WorldStageStore(context).read())
    }

    private fun game() = context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
    private fun legacy() = context.getSharedPreferences("veil_avatar_pilot_v1", Context.MODE_PRIVATE)
}
