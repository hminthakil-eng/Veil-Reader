package com.veilreader.app.data

/**
 * A process-local lease for one logical Reader open request.
 *
 * Epoch orders successive owners of the same book even though each Reader session restarts its
 * locator sequence at one.
 */
internal data class ReaderProgressWriterLease(
    val bookId: String,
    val sessionId: String,
    val epoch: Long
)

internal data class ReaderProgressWriteOrder(
    val epoch: Long,
    val sequence: Long
) : Comparable<ReaderProgressWriteOrder> {
    override fun compareTo(other: ReaderProgressWriteOrder): Int =
        when {
            epoch != other.epoch -> epoch.compareTo(other.epoch)
            else -> sequence.compareTo(other.sequence)
        }
}

internal data class ReaderProgressSaveOutcome(
    val accepted: Boolean,
    val newlyFinished: Boolean = false
)

internal fun shouldReplacePendingProgress(
    current: ReaderProgressWriteOrder?,
    incoming: ReaderProgressWriteOrder?
): Boolean =
    when {
        current == null || incoming == null -> true
        else -> incoming >= current
    }
