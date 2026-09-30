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
    val startedAtElapsedMs: Long
)

internal class ReaderNavigationTransactionGate(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
    private var nextToken = 0L
    private var active: ReaderNavigationTransaction? = null

    @Synchronized
    fun begin(
        originLocatorJson: String?,
        nowElapsedMs: Long
    ): ReaderNavigationTransaction {
        val transaction = ReaderNavigationTransaction(
            token = ++nextToken,
            originLocatorJson = originLocatorJson,
            startedAtElapsedMs = nowElapsedMs
        )
        active = transaction
        return transaction
    }

    @Synchronized
    fun cancel(token: Long) {
        if (active?.token == token) active = null
    }

    @Synchronized
    fun consumeSettled(nowElapsedMs: Long): ReaderNavigationTransaction? {
        val transaction = active ?: return null
        val elapsed = nowElapsedMs - transaction.startedAtElapsedMs
        if (elapsed < 0L || elapsed > timeoutMs.coerceAtLeast(0L)) {
            active = null
            return null
        }
        active = null
        return transaction
    }

    @Synchronized
    fun reset() {
        active = null
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 1_500L
    }
}
