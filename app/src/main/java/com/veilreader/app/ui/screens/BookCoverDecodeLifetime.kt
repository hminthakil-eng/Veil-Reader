package com.veilreader.app.ui.screens

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private val bookCoverDecodeLeases = AtomicInteger(0)
internal fun bookCoverDecodesInFlight(): Int = bookCoverDecodeLeases.get()

/** Native decoding cannot be interrupted. Keep ownership until its read actually ends. */
internal suspend fun <T> withBookCoverDecodeLease(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: () -> T
): T {
    val ownerContext = currentCoroutineContext()
    val ownerClassLoader = Thread.currentThread().contextClassLoader
    bookCoverDecodeLeases.incrementAndGet()
    try {
        return withContext(NonCancellable + dispatcher) {
            // Cancellation before native work starts still prevents the allocation/read.
            ownerContext.ensureActive()
            val worker = Thread.currentThread()
            val previousClassLoader = worker.contextClassLoader
            try {
                // Shared IO threads must resolve native bridge classes in the caller's
                // loader, including isolated Robolectric native sandboxes.
                worker.contextClassLoader = ownerClassLoader
                block()
            } finally {
                worker.contextClassLoader = previousClassLoader
            }
        }
    } finally {
        bookCoverDecodeLeases.decrementAndGet()
    }
}
