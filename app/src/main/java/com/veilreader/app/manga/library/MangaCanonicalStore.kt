package com.veilreader.app.manga.library

interface MangaCanonicalStore {
    suspend fun loadWork(id: CanonicalMangaId): CanonicalManga?
    suspend fun listWorks(): List<CanonicalManga>
    suspend fun saveWork(manga: CanonicalManga)
    suspend fun deleteWork(id: CanonicalMangaId)
}
