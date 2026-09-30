package com.veilreader.app.ui.reader

/**
 * Owns an opened Reader resource until the app state explicitly accepts responsibility for it.
 *
 * Any early return, cancellation or exception before [transfer] closes the resource exactly once.
 */
internal class ReaderOpenResourceGuard<T : AutoCloseable>(
    private val resource: T
) {
    private var transferred = false
    private var closed = false

    @Synchronized
    fun transfer(): T {
        check(!closed) { "Cannot transfer a closed Reader resource." }
        check(!transferred) { "Reader resource ownership was already transferred." }
        transferred = true
        return resource
    }

    @Synchronized
    fun closeIfUntransferred() {
        if (transferred || closed) return
        closed = true
        resource.close()
    }
}
