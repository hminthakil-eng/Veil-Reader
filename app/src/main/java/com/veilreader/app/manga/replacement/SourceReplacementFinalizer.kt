package com.veilreader.app.manga.replacement

import com.veilreader.app.manga.library.CanonicalManga

/**
 * Pure finalize step. Durable persistence must commit canonical/progress state transactionally before
 * this result replaces the previous state.
 */
object SourceReplacementFinalizer {
    fun finalize(plan: SourceReplacementPlan): CanonicalManga {
        check(plan.canAutoApply) { "Only READY replacement plans can auto-finalize" }
        return plan.canonicalAfterLink.unlinkSource(plan.sourceToReplace)
    }
}
