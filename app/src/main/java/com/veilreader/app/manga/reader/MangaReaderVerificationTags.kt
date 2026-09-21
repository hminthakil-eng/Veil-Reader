package com.veilreader.app.manga.reader

object MangaReaderVerificationTags {
    const val ROOT = "manga-reader-root"
    const val CHROME = "manga-reader-chrome"
    const val BACK = "manga-reader-back"
    const val MODE = "manga-reader-mode"
    const val DIRECTION = "manga-reader-direction"
    const val PARTIAL_OFFLINE = "manga-reader-partial-offline"
    const val SUBSAMPLING = "manga-reader-subsampling"

    fun page(index: Int): String = "manga-reader-page-" + index

    fun chapter(sourceId: String, chapterKey: String): String =
        "manga-reader-chapter-" + sourceId + "-" + chapterKey
}