package com.veilreader.app.ui.reader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * A resource created on a worker may be discarded by withContext's prompt cancellation on return.
 * Keep its guard outside the worker so cancellation, rejection and consumer failure all close it.
 * The synchronous consumer returns true only after taking responsibility for disposal.
 */
internal suspend fun <T : AutoCloseable> deliverReaderResource(
    dispatcher: CoroutineDispatcher,
    load: suspend () -> T?,
    accept: (T?) -> Boolean
) {
    var resource: T? = null
    var guard: ReaderOpenResourceGuard<T>? = null
    try {
        withContext(dispatcher) {
            resource = load()
            guard = resource?.let { ReaderOpenResourceGuard(it) }
        }
        if (accept(resource)) guard?.transfer()
    } finally {
        guard?.closeIfUntransferred()
    }
}
