package com.veilreader.app.manga.library

interface MangaCanonicalStore {
    suspend fun loadWork(id: CanonicalMangaId): CanonicalManga?
    suspend fun findWorkBySource(ref: com.veilreader.app.manga.source.SourceMangaRef): CanonicalManga?
    suspend fun listWorks(): List<CanonicalManga>
    suspend fun saveWork(manga: CanonicalManga)
    suspend fun deleteWork(id: CanonicalMangaId)
}
