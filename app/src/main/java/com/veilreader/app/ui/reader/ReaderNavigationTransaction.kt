package com.veilreader.app.ui.reader

/**
 * Owns one programmatic Reader jump until the debounced navigator stream publishes its settled
 * destination. Intermediate navigator positions must not be treated as user page turns.
 */
internal data class ReaderNavigationTransaction(
    val token: Long,
    val originLocatorJson: String?
)

internal class ReaderNavigationTransactionGate {
    private var nextToken = 0L
    private var active: ReaderNavigationTransaction? = null

    fun begin(originLocatorJson: String?): ReaderNavigationTransaction {
        val transaction = ReaderNavigationTransaction(
            token = ++nextToken,
            originLocatorJson = originLocatorJson
        )
        active = transaction
        return transaction
    }

    fun cancel(token: Long) {
        if (active?.token == token) active = null
    }

    fun consumeSettled(): ReaderNavigationTransaction? {
        val transaction = active ?: return null
        active = null
        return transaction
    }

    fun reset() {
        active = null
    }
}
