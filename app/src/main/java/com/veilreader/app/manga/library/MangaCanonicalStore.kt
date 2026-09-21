package com.veilreader.app.manga.library

import com.veilreader.app.manga.source.SourceMangaRef

interface MangaCanonicalStore {
    suspend fun loadWork(id: CanonicalMangaId): CanonicalManga?
    suspend fun findWorkBySource(ref: SourceMangaRef): CanonicalManga?
    suspend fun listWorks(): List<CanonicalManga>
    suspend fun saveWork(manga: CanonicalManga)
    suspend fun deleteWork(id: CanonicalMangaId)
}

class InMemoryMangaCanonicalStore : MangaCanonicalStore {
    private val values = linkedMapOf<CanonicalMangaId, CanonicalManga>()

    override suspend fun loadWork(id: CanonicalMangaId): CanonicalManga? =
        values[id]

    override suspend fun findWorkBySource(ref: SourceMangaRef): CanonicalManga? =
        values.values.firstOrNull { work ->
            work.sourceRef(ref.sourceId)?.key == ref.key
        }

    override suspend fun listWorks(): List<CanonicalManga> =
        values.values.sortedWith(
            compareBy<CanonicalManga> { it.createdAtEpochMs }
                .thenBy { it.id.value }
        )

    override suspend fun saveWork(manga: CanonicalManga) {
        for (ref in manga.sourceRefs.values) {
            val conflict = findWorkBySource(ref)
            require(conflict == null || conflict.id == manga.id) {
                "A provider Manga identity is already linked to another canonical work"
            }
        }
        values[manga.id] = manga
    }

    override suspend fun deleteWork(id: CanonicalMangaId) {
        values.remove(id)
    }
}
