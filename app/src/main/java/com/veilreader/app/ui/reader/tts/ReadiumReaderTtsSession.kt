package com.veilreader.app.ui.reader.tts

import android.content.Context
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.domain.BookFormat

/** Lazy engine creation: entering a book never binds a speech service or starts audio. */
internal fun createReadiumReaderTtsSession(
    context: Context,
    opened: OpenedPublication,
    commitCheckpoint: suspend (org.readium.r2.shared.publication.Locator, ReaderTtsPreferences) -> Unit = { _, _ -> },
    canPlay: () -> Boolean
): ReaderTtsSession? {
    if (opened.format != BookFormat.EPUB || !ReadiumTtsContent.isAvailable(opened.publication)) return null
    return ReaderTtsSession(
        contentFactory = { locator -> ReadiumTtsContent.create(opened.publication, locator) },
        backendFactory = { AndroidReaderTtsBackend(context.applicationContext) },
        publicationLanguage = opened.publication.metadata.languages.firstOrNull(),
        canPlay = canPlay,
        commitCheckpoint = commitCheckpoint
    )
}
