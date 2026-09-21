package com.veilreader.app.manga.replacement

import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.ChapterMatch
import com.veilreader.app.manga.library.MangaChapterMatcher
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails

enum class ReplacementPlanState {
    READY,
    MANUAL_REVIEW,
    REJECTED
}

data class SourceReplacementPlan(
    val state: ReplacementPlanState,
    val canonicalBefore: CanonicalManga,
    /**
     * New source is linked but old source is intentionally retained.
     * Unlinking the old source is a later transactional finalize step after persistence succeeds.
     */
    val canonicalAfterLink: CanonicalManga,
    val sourceToReplace: SourceId,
    val target: SourceMangaDetails,
    val workMatch: WorkMatchScore,
    val chapterMatch: ChapterMatch?,
    val migratedProgress: MangaReadingProgress?,
    val reasons: Set<String>,
    /** Offline manifests remain owned by CanonicalMangaId and need no source-key rewrite. */
    val preservesOfflineCacheOwnership: Boolean = true
) {
    val canAutoApply: Boolean get() = state == ReplacementPlanState.READY
}

class SourceReplacementPlanner(
    private val readyWorkConfidence: Double = 0.80,
    private val reviewWorkConfidence: Double = 0.55,
    private val minimumChapterConfidence: Double = 0.75,
    private val clock: () -> Long = System::currentTimeMillis
) {
    init {
        require(readyWorkConfidence in 0.0..1.0)
        require(reviewWorkConfidence in 0.0..readyWorkConfidence)
        require(minimumChapterConfidence in 0.0..1.0)
    }

    fun plan(
        canonical: CanonicalManga,
        sourceToReplace: SourceId,
        currentDetails: SourceMangaDetails,
        targetDetails: SourceMangaDetails,
        targetChapters: List<SourceChapter>,
        progress: MangaReadingProgress?
    ): SourceReplacementPlan {
        require(sourceToReplace in canonical.sourceRefs) {
            "Source to replace is not linked to canonical manga"
        }
        require(currentDetails.summary.ref.sourceId == sourceToReplace) {
            "Current details do not belong to sourceToReplace"
        }
        require(targetDetails.summary.ref.sourceId != sourceToReplace) {
            "Replacement target must use a different source"
        }
        require(targetChapters.all { it.sourceId == targetDetails.summary.ref.sourceId }) {
            "Target chapters must belong to replacement source"
        }
        require(progress == null || progress.mangaId == canonical.id) {
            "Progress belongs to a different canonical manga"
        }

        val workMatch = SourceAlternativeScorer.score(currentDetails, targetDetails)
        val linked = canonical.linkSource(targetDetails.summary.ref)
        val reasons = linkedSetOf<String>().apply {
            addAll(workMatch.reasons)
        }

        if (workMatch.confidence < reviewWorkConfidence || workMatch.titleConfidence < 0.45) {
            reasons += "work-confidence-too-low"
            return SourceReplacementPlan(
                state = ReplacementPlanState.REJECTED,
                canonicalBefore = canonical,
                canonicalAfterLink = linked,
                sourceToReplace = sourceToReplace,
                target = targetDetails,
                workMatch = workMatch,
                chapterMatch = null,
                migratedProgress = null,
                reasons = reasons
            )
        }

        if (progress == null) {
            val state = if (workMatch.confidence >= readyWorkConfidence) {
                ReplacementPlanState.READY
            } else {
                ReplacementPlanState.MANUAL_REVIEW
            }
            reasons += if (state == ReplacementPlanState.READY) {
                "no-progress-to-migrate"
            } else {
                "work-needs-review"
            }
            return SourceReplacementPlan(
                state = state,
                canonicalBefore = canonical,
                canonicalAfterLink = linked,
                sourceToReplace = sourceToReplace,
                target = targetDetails,
                workMatch = workMatch,
                chapterMatch = null,
                migratedProgress = null,
                reasons = reasons
            )
        }

        val chapterMatch = MangaChapterMatcher.bestSafeMatch(
            anchor = progress.chapter,
            candidates = targetChapters,
            minimumConfidence = minimumChapterConfidence
        )

        if (chapterMatch == null) {
            reasons += "chapter-match-ambiguous-or-weak"
            return SourceReplacementPlan(
                state = ReplacementPlanState.MANUAL_REVIEW,
                canonicalBefore = canonical,
                canonicalAfterLink = linked,
                sourceToReplace = sourceToReplace,
                target = targetDetails,
                workMatch = workMatch,
                chapterMatch = null,
                migratedProgress = null,
                reasons = reasons
            )
        }

        val migrated = progress.copy(
            chapter = progress.chapter.copy(
                volume = chapterMatch.chapter.volume ?: progress.chapter.volume,
                number = chapterMatch.chapter.number ?: progress.chapter.number,
                languageTag = chapterMatch.chapter.languageTag ?: progress.chapter.languageTag,
                normalizedTitle = chapterMatch.chapter.title ?: progress.chapter.normalizedTitle,
                providerChapterKeyHint = chapterMatch.chapter.chapterKey
            ),
            updatedAtEpochMs = clock()
        )

        val state = if (workMatch.confidence >= readyWorkConfidence) {
            ReplacementPlanState.READY
        } else {
            ReplacementPlanState.MANUAL_REVIEW
        }
        reasons += chapterMatch.reasons
        reasons += if (state == ReplacementPlanState.READY) {
            "work-and-chapter-confident"
        } else {
            "work-needs-review"
        }

        return SourceReplacementPlan(
            state = state,
            canonicalBefore = canonical,
            canonicalAfterLink = linked,
            sourceToReplace = sourceToReplace,
            target = targetDetails,
            workMatch = workMatch,
            chapterMatch = chapterMatch,
            migratedProgress = migrated,
            reasons = reasons
        )
    }
}
