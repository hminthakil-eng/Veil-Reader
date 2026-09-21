package com.veilreader.app.manga.health

import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId

interface SourceExecutionHealthObserver {
    fun onEvent(event: SourceHealthEvent)
}

class StoreBackedSourceHealthObserver(
    private val store: SourceHealthStore
) : SourceExecutionHealthObserver {
    override fun onEvent(event: SourceHealthEvent) {
        store.record(event)
    }
}

/**
 * Helper used by the execution coordinator. Keeping time measurement here makes observation
 * payload-free and easy to disable or replace.
 */
class SourceHealthRecorder(
    private val observer: SourceExecutionHealthObserver?,
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun start(): Long = clock()

    fun success(
        sourceId: SourceId,
        operation: SourceOperation,
        startedAtEpochMs: Long
    ) {
        emit(sourceId, operation, startedAtEpochMs, null)
    }

    fun failure(
        sourceId: SourceId,
        operation: SourceOperation,
        startedAtEpochMs: Long,
        failureKind: SourceFailureKind
    ) {
        emit(sourceId, operation, startedAtEpochMs, failureKind)
    }

    private fun emit(
        sourceId: SourceId,
        operation: SourceOperation,
        startedAtEpochMs: Long,
        failureKind: SourceFailureKind?
    ) {
        val observedAt = clock()
        observer?.onEvent(
            SourceHealthEvent(
                sourceId = sourceId,
                operation = operation,
                success = failureKind == null,
                latencyMillis = (observedAt - startedAtEpochMs).coerceAtLeast(0L),
                failureKind = failureKind,
                observedAtEpochMs = observedAt
            )
        )
    }
}
