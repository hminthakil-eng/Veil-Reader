package com.veilreader.app.ui.reader

/**
 * Semantic reason for moving away from the durable reading anchor.
 *
 * The renderer must not infer progress ownership from gesture direction or from a raw navigator
 * emission. Programmatic exploration can settle visually without becoming the user's new reading
 * position.
 */
internal enum class ReaderNavigationReason {
    UNKNOWN,
    RETURN_PREVIOUS,
    SEARCH_RESULT,
    TABLE_OF_CONTENTS,
    FOOTNOTE,
    BOOKMARK,
    HIGHLIGHT,
    SAVED_PASSAGE,
    PAGE_PREVIEW,
    REFERENCE,
    INTERNAL_LINK,
    PDF_INTERNAL_LINK,
    LISTENING_POSITION,
    RESTORE
}

/**
 * Controls whether a settled programmatic navigation is allowed to replace durable reading
 * progress. Existing behavior uses COMMIT_ON_SETTLEMENT until each call site is explicitly
 * classified and the ReadingAnchor/ExplorationLocator state machine is integrated.
 */
internal enum class ReaderNavigationCommitPolicy {
    COMMIT_ON_SETTLEMENT,
    PRESERVE_READING_ANCHOR
}

internal data class ReaderNavigationSemanticIntent(
    val reason: ReaderNavigationReason,
    val commitPolicy: ReaderNavigationCommitPolicy
)

internal fun readerNavigationIntentFor(
    reason: ReaderNavigationReason
): ReaderNavigationSemanticIntent =
    ReaderNavigationSemanticIntent(
        reason = reason,
        commitPolicy = when (reason) {
            ReaderNavigationReason.RETURN_PREVIOUS,
            ReaderNavigationReason.RESTORE ->
                ReaderNavigationCommitPolicy.COMMIT_ON_SETTLEMENT

            ReaderNavigationReason.SEARCH_RESULT,
            ReaderNavigationReason.TABLE_OF_CONTENTS,
            ReaderNavigationReason.FOOTNOTE,
            ReaderNavigationReason.BOOKMARK,
            ReaderNavigationReason.HIGHLIGHT,
            ReaderNavigationReason.SAVED_PASSAGE,
            ReaderNavigationReason.PAGE_PREVIEW,
            ReaderNavigationReason.REFERENCE,
            ReaderNavigationReason.INTERNAL_LINK,
            ReaderNavigationReason.PDF_INTERNAL_LINK,
            ReaderNavigationReason.LISTENING_POSITION ->
                ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR

            ReaderNavigationReason.UNKNOWN ->
                // Fail compatibly while call sites are migrated one by one. UNKNOWN must never be
                // used to claim that ReadingAnchor separation is complete.
                ReaderNavigationCommitPolicy.COMMIT_ON_SETTLEMENT
        }
    )
