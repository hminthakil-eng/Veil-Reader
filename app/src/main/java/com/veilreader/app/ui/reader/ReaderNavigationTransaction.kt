package com.veilreader.app.ui.reader

/**
 * Owns one programmatic Reader jump until the debounced navigator stream publishes its settled
 * destination. A re-emitted origin locator is still part of the handoff and must not consume the
 * transaction; intermediate navigator positions must not be treated as user page turns.
 *
 * The transaction expires defensively. If a navigator accepts a no-op jump and emits nothing,
 * a later real user page turn must not be misclassified forever.
 */
internal data class ReaderNavigationTransaction(
    val token: Long,
    val originLocatorJson: String?,
    val startedAtElapsedMs: Long
)

/**
 * Starts a location-backed jump only when it can actually move away from the current locator.
 *
 * Readium may accept a no-op `go()` without publishing a new locator. Avoiding a transaction for
 * that case prevents the user's next real page turn from being mistaken for a delayed jump settle.
 */
internal fun shouldStartReaderLocationJump(
    originLocatorJson: String?,
    targetLocatorJson: String?
): Boolean {
    val target = targetLocatorJson?.takeIf { it.isNotBlank() } ?: return false
    val origin = originLocatorJson?.takeIf { it.isNotBlank() }
    return origin == null || origin != target
}

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
    fun isActive(nowElapsedMs: Long): Boolean =
        freshActive(nowElapsedMs) != null

    @Synchronized
    fun consumeSettled(
        observedLocatorJson: String?,
        nowElapsedMs: Long
    ): ReaderNavigationTransaction? {
        val transaction = freshActive(nowElapsedMs) ?: return null

        // Readium may re-publish the pre-jump currentLocator while go() is still moving the
        // resource/page. That emission is not the destination and must not consume the jump.
        if (
            transaction.originLocatorJson != null &&
            observedLocatorJson == transaction.originLocatorJson
        ) {
            return null
        }

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
