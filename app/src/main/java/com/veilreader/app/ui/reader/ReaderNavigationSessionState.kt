package com.veilreader.app.ui.reader

/**
 * Session-local navigation ownership.
 *
 * The durable reading anchor is the last locator the user deliberately committed as reading
 * progress. An exploration locator is a visually settled programmatic destination that must not
 * replace that anchor until a later semantic reading action explicitly commits it.
 */
internal data class ReaderNavigationSessionState(
    val readingAnchorJson: String? = null,
    val explorationLocatorJson: String? = null,
    val explorationOriginJson: String? = null,
    val explorationReason: ReaderNavigationReason? = null
) {
    val isExploring: Boolean
        get() = explorationLocatorJson != null
}

internal class ReaderNavigationSessionStateMachine(
    initialReadingAnchorJson: String? = null
) {
    var state: ReaderNavigationSessionState =
        ReaderNavigationSessionState(readingAnchorJson = initialReadingAnchorJson)
        private set

    fun onDurableReadingCommit(locatorJson: String) {
        state = ReaderNavigationSessionState(readingAnchorJson = locatorJson)
    }

    fun onProgrammaticSettlement(
        transaction: ReaderNavigationTransaction,
        settledLocatorJson: String
    ) {
        state = when (transaction.commitPolicy) {
            ReaderNavigationCommitPolicy.COMMIT_ON_SETTLEMENT ->
                state.copy(
                    explorationLocatorJson = null,
                    explorationOriginJson = null,
                    explorationReason = null
                )

            ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR ->
                state.copy(
                    explorationLocatorJson = settledLocatorJson,
                    explorationOriginJson =
                        transaction.originLocatorJson ?: state.readingAnchorJson,
                    explorationReason = transaction.reason
                )
        }
    }

    /**
     * A real reading action after exploration promotes the visible locator into the durable anchor.
     * The caller still owns the actual repository write.
     */
    fun commitExploration(locatorJson: String) {
        state = ReaderNavigationSessionState(readingAnchorJson = locatorJson)
    }

    /**
     * Reaching a temporary destination is not a durability acknowledgement. Callers must not
     * persist a lifecycle/final snapshot while this exploration is active.
     */
    fun mayPersistFinalSnapshot(): Boolean = !state.isExploring

    /** Layout, opening and jump observations cannot promote a temporary destination. */
    fun mayCommitObservedEvent(event: ReaderLocatorEvent): Boolean =
        !state.isExploring || when (event) {
            ReaderLocatorEvent.NAVIGATOR_PAGE_TURN,
            ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT,
            ReaderLocatorEvent.PAPER_COMMIT -> true
            else -> false
        }

    /** A failed write must never be advertised as a durable anchor. */
    fun onDurableReadingCommitAccepted(locatorJson: String, accepted: Boolean) {
        if (accepted) onDurableReadingCommit(locatorJson)
    }

    fun clearExploration() {
        state = state.copy(
            explorationLocatorJson = null,
            explorationOriginJson = null,
            explorationReason = null
        )
    }

    /**
     * Close/process-death recovery must use the durable anchor while exploration is uncommitted.
     */
    fun locatorForDurabilityFallback(currentLocatorJson: String?): String? =
        if (state.isExploring) state.readingAnchorJson else currentLocatorJson ?: state.readingAnchorJson
}
