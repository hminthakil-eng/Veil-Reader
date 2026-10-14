package com.veilreader.app.domain

/**
 * Stable, offline-first character identity model. No Reader, UI, renderer or Android dependency.
 * Integers in the stored schema are fixed catalog slots; changing their meanings requires a migration.
 * Optional external asset identifiers are intentionally not part of this first built-in-only slice.
 */
data class RagsToRichesAvatar(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val hairStyle: Int = 0,
    val hairColor: Int = 0,
    val skinTone: Int = 1,
    val outfitStyle: Int = 0,
    val accentColor: Int = 0
) {
    fun safeForPreview(): RagsToRichesAvatar = copy(
        schemaVersion = CURRENT_SCHEMA,
        hairStyle = hairStyle.coerceIn(0, HAIR_STYLES - 1),
        hairColor = hairColor.coerceIn(0, HAIR_COLORS - 1),
        skinTone = skinTone.coerceIn(0, SKIN_TONES - 1),
        outfitStyle = outfitStyle.coerceIn(0, OUTFITS - 1),
        accentColor = accentColor.coerceIn(0, ACCENTS - 1)
    )

    companion object {
        const val CURRENT_SCHEMA = 1
        const val HAIR_STYLES = 4
        const val HAIR_COLORS = 4
        const val SKIN_TONES = 4
        const val OUTFITS = 4
        const val ACCENTS = 4
    }
}

/** Logical stage IDs must remain stable across save migrations, art revisions and engine swaps. */
enum class RagsToRichesStage(val tier: Int, val assetId: String) {
    WANDERER_CAMP(0, "world.grayfog.camp.v1"),
    APPRENTICE_HUT(1, "world.grayfog.hut.v1"),
    SCHOLAR_HOUSE(2, "world.grayfog.house.v1"),
    KEEPER_CASTLE(3, "world.grayfog.castle.v1")
}

/** Exact, auditable mapping; enum ordinal ordering is NOT used as a persistence contract. */
fun ragsToRichesStageFor(stage: ReadingHomeStage): RagsToRichesStage = when (stage) {
    ReadingHomeStage.CAMP -> RagsToRichesStage.WANDERER_CAMP
    ReadingHomeStage.HUT -> RagsToRichesStage.APPRENTICE_HUT
    ReadingHomeStage.HOUSE -> RagsToRichesStage.SCHOLAR_HOUSE
    ReadingHomeStage.CASTLE -> RagsToRichesStage.KEEPER_CASTLE
}

/**
 * Non-decreasing stage projection. High-water storage and milestone reconciliation must happen
 * in a future repository transaction; never save state from Compose drawing or recomposition.
 */
fun highestEarnedRagsStage(
    newlyEligible: RagsToRichesStage,
    previouslyEarned: RagsToRichesStage
): RagsToRichesStage =
    if (newlyEligible.tier > previouslyEarned.tier) newlyEligible else previouslyEarned

/** Read-only scene contract; the actual 3D renderer is optional and may fail over to Canvas. */
data class RagsToRichesScene(
    val stage: RagsToRichesStage,
    val avatar: RagsToRichesAvatar,
    val poseId: String = "reading",
    val qualityId: String = "static"
)

fun previewRagsScene(
    activeMinutes: Int,
    completedBooks: Int,
    avatar: RagsToRichesAvatar,
    highestEarnedStage: RagsToRichesStage = RagsToRichesStage.WANDERER_CAMP
): RagsToRichesScene {
    val eligible = ragsToRichesStageFor(deriveReadingHomeProgression(activeMinutes, completedBooks).stage)
    return RagsToRichesScene(
        stage = highestEarnedRagsStage(eligible, highestEarnedStage),
        avatar = avatar.safeForPreview()
    )
}
