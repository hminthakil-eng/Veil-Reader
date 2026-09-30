package com.veilreader.app.manga.replacement

import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails

data class ReplacementCandidateInput(
    val details: SourceMangaDetails,
    val chapters: List<SourceChapter>
)

data class RankedReplacementCandidate(
    val plan: SourceReplacementPlan,
    val rankScore: Double
) {
    init {
        require(rankScore.isFinite() && rankScore in 0.0..1.0)
    }
}

data class AlternativeResolution(
    val ranked: List<RankedReplacementCandidate>,
    /**
     * Present only when the strongest READY candidate is sufficiently separated from the runner-up.
     * Otherwise the UI must ask the user to review alternatives.
     */
    val recommended: SourceReplacementPlan?
)

class SourceAlternativeResolver(
    private val planner: SourceReplacementPlanner = SourceReplacementPlanner(),
    private val minimumLead: Double = 0.08
) {
    init {
        require(minimumLead in 0.0..1.0)
    }

    fun resolve(
        canonical: CanonicalManga,
        sourceToReplace: SourceId,
        currentDetails: SourceMangaDetails,
        candidates: List<ReplacementCandidateInput>,
        progress: MangaReadingProgress?
    ): AlternativeResolution {
        val ranked = candidates
            .filter { it.details.summary.ref.sourceId != sourceToReplace }
            .map { candidate ->
                val plan = planner.plan(
                    canonical = canonical,
                    sourceToReplace = sourceToReplace,
                    currentDetails = currentDetails,
                    targetDetails = candidate.details,
                    targetChapters = candidate.chapters,
                    progress = progress
                )
                RankedReplacementCandidate(
                    plan = plan,
                    rankScore = rankScore(plan)
                )
            }
            .sortedWith(
                compareByDescending<RankedReplacementCandidate> { it.rankScore }
                    .thenBy { it.plan.target.summary.ref.sourceId.value }
            )

        val best = ranked.firstOrNull()
        val second = ranked.getOrNull(1)
        val recommended = when {
            best == null -> null
            !best.plan.canAutoApply -> null
            second == null -> best.plan
            !second.plan.canAutoApply -> best.plan
            best.rankScore - second.rankScore >= minimumLead -> best.plan
            else -> null
        }

        return AlternativeResolution(
            ranked = ranked,
            recommended = recommended
        )
    }

    private fun rankScore(plan: SourceReplacementPlan): Double {
        val chapter = plan.chapterMatch?.confidence ?: if (plan.migratedProgress == null) 1.0 else 0.0
        val stateFactor = when (plan.state) {
            ReplacementPlanState.READY -> 1.0
            ReplacementPlanState.MANUAL_REVIEW -> 0.7
            ReplacementPlanState.REJECTED -> 0.2
        }
        return (
            (plan.workMatch.confidence * 0.75) +
                (chapter * 0.25)
            ) * stateFactor
    }
}
