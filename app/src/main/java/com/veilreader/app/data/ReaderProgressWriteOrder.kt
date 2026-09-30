package com.veilreader.app.data

/**
 * A process-local lease for one concrete Reader writer instance.
 *
 * Session id preserves logical Reader identity while epoch orders successive in-process writers,
 * including a recreated ViewModel that resumes the same session and restarts locator sequence at one.
 */
internal data class ReaderProgressWriterLease(
    val bookId: String,
    val sessionId: String,
    val epoch: Long
)

/**
 * Monotonicity applies to write ownership/order, never to the numeric reading progression itself.
 * Moving backward in a book is a valid newer state and must remain persistable.
 */
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
        else -> incoming > current
    }
