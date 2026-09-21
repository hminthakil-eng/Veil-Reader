package com.veilreader.app.manga.reader.screen

import com.veilreader.app.manga.reader.MangaOrientationPolicy
import com.veilreader.app.manga.reader.MangaPageDirection
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.presentation.MangaChapterPresentationRequest
import com.veilreader.app.manga.reader.presentation.MangaChapterRoute
import com.veilreader.app.manga.source.MangaSourceProvider

data class MangaReaderChapterEntry(
    val route: MangaChapterRoute,
    val provider: MangaSourceProvider? = null
) {
    init {
        if (provider != null) {
            require(provider.descriptor.id == route.sourceChapter.sourceId) {
                "Reader entry provider must own its source chapter"
            }
        }
    }

    fun request(): MangaChapterPresentationRequest =
        MangaChapterPresentationRequest(
            chapter = route.readerChapter,
            provider = provider,
            sourceChapter = provider?.let { route.sourceChapter }
        )
}

data class MangaReaderSessionOptions(
    val mode: MangaReaderMode = MangaReaderMode.PAGED,
    val direction: MangaPageDirection = MangaPageDirection.RIGHT_TO_LEFT,
    val orientationPolicy: MangaOrientationPolicy = MangaOrientationPolicy.FOLLOW_SYSTEM
)

class MangaReaderSession(
    val entriesInReadingOrder: List<MangaReaderChapterEntry>,
    val initialIndex: Int = 0,
    val options: MangaReaderSessionOptions = MangaReaderSessionOptions()
) {
    init {
        require(entriesInReadingOrder.isNotEmpty()) {
            "Manga reader session needs at least one chapter"
        }
        require(initialIndex in entriesInReadingOrder.indices) {
            "Initial chapter index is outside the session"
        }

        val mangaIds = entriesInReadingOrder
            .map { it.route.readerChapter.mangaId }
            .distinct()
        require(mangaIds.size == 1) {
            "One Manga reader session may only contain one canonical manga"
        }

        for (left in entriesInReadingOrder.indices) {
            for (right in left + 1 until entriesInReadingOrder.size) {
                require(
                    !entriesInReadingOrder[left].route.readerChapter.sameLogicalChapter(
                        entriesInReadingOrder[right].route.readerChapter
                    )
                ) {
                    "Reader session contains duplicate logical chapters"
                }
            }
        }
    }

    val initialEntry: MangaReaderChapterEntry
        get() = entriesInReadingOrder[initialIndex]

    val mangaId
        get() = initialEntry.route.readerChapter.mangaId

    val routes: List<MangaChapterRoute>
        get() = entriesInReadingOrder.map { it.route }

    val sessionKey: String
        get() = "manga-reader-" + mangaId.value

    fun entryForChapter(chapter: MangaReaderChapterRef): MangaReaderChapterEntry? =
        entriesInReadingOrder.singleOrNull {
            it.route.readerChapter.sameLogicalChapter(chapter)
        }

    fun entryForRoute(route: MangaChapterRoute): MangaReaderChapterEntry? =
        entriesInReadingOrder.firstOrNull {
            it.route.sourceChapter.sourceId == route.sourceChapter.sourceId &&
                it.route.sourceChapter.chapterKey == route.sourceChapter.chapterKey
        } ?: entryForChapter(route.readerChapter)
}
