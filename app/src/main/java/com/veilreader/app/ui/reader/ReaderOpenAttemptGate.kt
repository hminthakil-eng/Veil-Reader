package com.veilreader.app.ui.reader

/**
 * Orders overlapping suspend Reader opens.
 *
 * Starting a newer attempt immediately supersedes the previous attempt. A stale attempt that
 * resumes from storage later cannot install a tracker/writer after the newer request.
 */
internal data class ReaderOpenAttempt(
    val generation: Long,
    val bookId: String,
    val sessionInstanceId: String
)

internal class ReaderOpenAttemptGate {
    private var nextGeneration = 0L
    private var active: ReaderOpenAttempt? = null

    @Synchronized
    fun begin(bookId: String, sessionInstanceId: String): ReaderOpenAttempt {
        require(bookId.isNotBlank())
        require(sessionInstanceId.isNotBlank())
        return ReaderOpenAttempt(
            generation = ++nextGeneration,
            bookId = bookId,
            sessionInstanceId = sessionInstanceId
        ).also { active = it }
    }

    @Synchronized
    fun isCurrent(attempt: ReaderOpenAttempt): Boolean =
        active == attempt

    @Synchronized
    fun complete(attempt: ReaderOpenAttempt): Boolean {
        if (active != attempt) return false
        active = null
        return true
    }

    @Synchronized
    fun cancel(attempt: ReaderOpenAttempt): Boolean {
        if (active != attempt) return false
        active = null
        return true
    }

    @Synchronized
    fun cancelSession(sessionInstanceId: String): ReaderOpenAttempt? {
        val attempt = active?.takeIf { it.sessionInstanceId == sessionInstanceId } ?: return null
        active = null
        return attempt
    }
}
