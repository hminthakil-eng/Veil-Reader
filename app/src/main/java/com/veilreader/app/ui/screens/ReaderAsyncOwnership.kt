package com.veilreader.app.ui.screens

/**
 * Async work may outlive the Reader instance that started it when the same book is reopened.
 * Durable writes may still complete, but their UI/accounting side effects belong only to the
 * session that initiated them.
 */
internal fun readerAsyncResultBelongsToSession(
    currentSessionInstanceId: String,
    expectedSessionInstanceId: String
): Boolean =
    currentSessionInstanceId == expectedSessionInstanceId
