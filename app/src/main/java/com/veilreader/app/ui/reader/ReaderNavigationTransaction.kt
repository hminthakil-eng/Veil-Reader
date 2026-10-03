package com.veilreader.app.ui.reader

/**
 * Owns one programmatic Reader jump until the debounced navigator stream publishes its settled
 * destination. Intermediate navigator positions must not be treated as user page turns.
 *
 * The transaction expires defensively. If a navigator accepts a no-op jump and emits nothing,
 * a later real user page turn must not be misclassified forever.
 */
internal data class ReaderNavigationTransaction(
    val token: Long,
    val originLocatorJson: String?,
    val startedAtElapsedMs: Long,
    val expectedPdfPage: Int? = null,
    val originPdfPage: Int? = null
)

internal class ReaderNavigationTransactionGate(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
    private var nextToken = 0L
    private var active: ReaderNavigationTransaction? = null

    @Synchronized
    fun begin(
        originLocatorJson: String?,
        nowElapsedMs: Long,
        expectedPdfPage: Int? = null,
        originPdfPage: Int? = null
    ): ReaderNavigationTransaction {
        val transaction = ReaderNavigationTransaction(
            token = ++nextToken,
            originLocatorJson = originLocatorJson,
            startedAtElapsedMs = nowElapsedMs,
            expectedPdfPage = expectedPdfPage?.takeIf { it > 0 },
            originPdfPage = originPdfPage?.takeIf { it > 0 }
        )
        active = transaction
        return transaction
    }

    @Synchronized
    fun cancel(token: Long) {
        if (active?.token == token) active = null
    }

    @Synchronized
    fun isActive(nowElapsedMs: Long): Boolean =
        freshActive(nowElapsedMs) != null

    @Synchronized
    fun consumeSettled(nowElapsedMs: Long, observedPdfPage: Int? = null): ReaderNavigationTransaction? {
        val transaction = freshActive(nowElapsedMs) ?: return null
        if (transaction.expectedPdfPage != null && transaction.expectedPdfPage != observedPdfPage) return null
        active = null
        return transaction
    }

    /** A lifecycle flush may commit a reached PDF destination before the 500ms UI debounce. */
    @Synchronized
    fun consumeReachedPdfDestination(nowElapsedMs: Long, observedPdfPage: Int?): ReaderNavigationTransaction? {
        val transaction = freshActive(nowElapsedMs) ?: return null
        if (transaction.expectedPdfPage == null || observedPdfPage == null || observedPdfPage <= 0) return null
        val reachedDestination = transaction.expectedPdfPage == observedPdfPage
        // An unanimated PDF jump has no intermediate page animation. A real page beyond its
        // source can also be a user swipe immediately after that jump; final flush must not lose it.
        val movedPastSource = transaction.originPdfPage != null && transaction.originPdfPage != observedPdfPage
        if (!reachedDestination && !movedPastSource) return null
        active = null
        return transaction
    }

    @Synchronized
    fun cancelActive(nowElapsedMs: Long): ReaderNavigationTransaction? {
        val transaction = freshActive(nowElapsedMs) ?: return null
        active = null
        return transaction
    }

    @Synchronized
    fun reset() {
        active = null
    }

    private fun freshActive(nowElapsedMs: Long): ReaderNavigationTransaction? {
        val transaction = active ?: return null
        val elapsed = nowElapsedMs - transaction.startedAtElapsedMs
        if (elapsed < 0L || elapsed > timeoutMs.coerceAtLeast(0L)) {
            active = null
            return null
        }
        return transaction
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 3_000L
    }
}
