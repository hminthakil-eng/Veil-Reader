package com.veilreader.app.data

import android.net.Uri

/**
 * Stable boundary retained for future alternate reader engines and tests.
 * ReadiumEngine is the production implementation used by the app today.
 */
interface ReaderGateway {
    suspend fun importPublication(uri: Uri): Result<ImportedPublication>
    suspend fun openPublication(bookId: String): Result<Unit>
    suspend fun saveProgress(bookId: String, progression: Double)
}

data class ImportedPublication(
    val id: String,
    val title: String,
    val author: String?,
    val mediaType: String
)
