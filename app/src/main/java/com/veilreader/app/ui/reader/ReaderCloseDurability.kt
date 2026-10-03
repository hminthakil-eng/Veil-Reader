package com.veilreader.app.ui.reader

/**
 * Finalizes a Reader session without clearing the app-level Reader route until every write queued
 * by that finalization has crossed the repository's durable storage barrier.
 *
 * Keeping the route alive also keeps its SavedState locator checkpoint available if the process is
 * lost while storage acknowledgement is still pending.
 */
internal suspend fun awaitDurableReaderClose(
    finalizeSession: () -> Unit,
    awaitDurability: suspend () -> Unit,
    clearRoute: () -> Unit,
    awaitOwnerRelease: suspend () -> Unit = {}
) {
    finalizeSession()
    awaitDurability()
    awaitOwnerRelease()
    clearRoute()
}
