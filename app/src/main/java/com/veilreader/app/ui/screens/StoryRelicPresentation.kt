package com.veilreader.app.ui.screens

import androidx.annotation.StringRes
import com.veilreader.app.R
import com.veilreader.app.domain.SilentNamesEncounter
import com.veilreader.app.domain.SilentNamesOutcome
import com.veilreader.app.domain.StoryRelicRecord
import com.veilreader.app.domain.normalizeStoryRelics

internal enum class StoryRelicGlyph {
    LANTERN
}

internal data class StoryRelicPresentation(
    val relicId: String,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val glyph: StoryRelicGlyph,
    val routeVariantRes: Map<String, Int>
)

internal data class StoryRelicDisplayModel(
    val record: StoryRelicRecord,
    val presentation: StoryRelicPresentation,
    @StringRes val routeVariantRes: Int?
)

private val storyRelicPresentations = listOf(
    StoryRelicPresentation(
        relicId = SilentNamesEncounter.REWARD_ID,
        titleRes = R.string.silent_names_reward_title,
        bodyRes = R.string.silent_names_reward_body,
        glyph = StoryRelicGlyph.LANTERN,
        routeVariantRes = mapOf(
            SilentNamesOutcome.RESTORED_INSCRIPTION.name to
                R.string.silent_names_restored_inscription,
            SilentNamesOutcome.WORKSHOP_TRAIL.name to
                R.string.silent_names_workshop_trail,
            SilentNamesOutcome.LANTERN_BRIDGE.name to
                R.string.silent_names_lantern_bridge,
            SilentNamesOutcome.LOWER_PASSAGE.name to
                R.string.silent_names_lower_passage,
            SilentNamesOutcome.KEEPER_TESTIMONY.name to
                R.string.silent_names_keeper_testimony,
            SilentNamesOutcome.KEEPER_REQUEST.name to
                R.string.silent_names_keeper_request
        )
    )
).associateBy(StoryRelicPresentation::relicId)

internal fun storyRelicPresentationFor(relicId: String): StoryRelicPresentation? =
    storyRelicPresentations[relicId]

internal fun storyRelicDisplayModels(
    records: Iterable<StoryRelicRecord>
): List<StoryRelicDisplayModel> =
    normalizeStoryRelics(records)
        .mapNotNull { record ->
            val presentation = storyRelicPresentationFor(record.relicId)
                ?: return@mapNotNull null
            StoryRelicDisplayModel(
                record = record,
                presentation = presentation,
                routeVariantRes = presentation.routeVariantRes[record.routeVariantId]
            )
        }

@StringRes
internal fun storyRelicModeRes(modeId: String): Int =
    when (modeId) {
        "DICE" -> R.string.story_relic_dice_mode
        "STORY" -> R.string.story_relic_story_mode
        else -> R.string.story_relic_unknown_variant
    }
