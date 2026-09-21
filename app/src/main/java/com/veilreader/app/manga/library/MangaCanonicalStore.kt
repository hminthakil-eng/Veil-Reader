package com.veilreader.app.manga.library

interface MangaCanonicalStore {
    suspend fun load(id: CanonicalMangaId): CanonicalManga?
    suspend fun listAll(): List<CanonicalManga>
    suspend fun save(manga: CanonicalManga)
    suspend fun delete(id: CanonicalMangaId)
}
