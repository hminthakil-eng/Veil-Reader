package com.veilreader.app.ui.reader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Release the UI at the deadline even when a worker cannot stop immediately. Structured cancellation
 * still waits for that worker's cleanup; its result must retain the normal ownership/serial checks.
 */
internal suspend fun runReaderRequestWithDeadline(
    timeoutMs: Long,
    onTimeout: () -> Unit,
    block: suspend () -> Unit
) = coroutineScope {
    require(timeoutMs > 0)
    val request = requireNotNull(coroutineContext[Job])
    val deadline = launch {
        delay(timeoutMs)
        onTimeout()
        request.cancel(CancellationException("Reader request deadline exceeded"))
    }
    try {
        block()
    } finally {
        deadline.cancel()
    }
}
