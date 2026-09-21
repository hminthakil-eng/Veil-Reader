package com.veilreader.app.manga.reader.presentation

import com.veilreader.app.manga.library.OfflineChapterId
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.SourceChapter

data class MangaChapterPresentationRequest(
    val chapter: MangaReaderChapterRef,
    val provider: MangaSourceProvider? = null,
    val sourceChapter: SourceChapter? = null
) {
    init {
        require((provider == null) == (sourceChapter == null)) {
            "Provider and sourceChapter must either both be present or both be absent"
        }
        if (provider != null && sourceChapter != null) {
            require(provider.descriptor.id == sourceChapter.sourceId) {
                "Provider does not own sourceChapter"
            }
        }
    }

    val canUseNetwork: Boolean get() = provider != null && sourceChapter != null

    fun offlineChapterId(): OfflineChapterId = OfflineChapterId(
        mangaId = chapter.mangaId,
        languageTag = chapter.anchor.languageTag,
        volume = chapter.anchor.volume,
        number = chapter.anchor.number,
        discriminator = "main"
    )
}
