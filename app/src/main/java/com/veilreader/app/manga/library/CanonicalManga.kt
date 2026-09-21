package com.veilreader.app.manga.library

import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaRef
import java.util.UUID

@JvmInline
value class CanonicalMangaId(val value: String) {
    init {
        require(value.isNotBlank()) { "Canonical manga id cannot be blank" }
        require(value.length <= 128) { "Canonical manga id is unexpectedly long" }
    }

    override fun toString(): String = value
}

data class CanonicalManga(
    val id: CanonicalMangaId,
    val title: String,
    val alternativeTitles: Set<String> = emptySet(),
    val sourceRefs: Map<SourceId, SourceMangaRef> = emptyMap(),
    val createdAtEpochMs: Long
) {
    init {
        require(title.isNotBlank()) { "Canonical manga title cannot be blank" }
        require(sourceRefs.all { (sourceId, ref) -> sourceId == ref.sourceId }) {
            "Canonical source map key must match SourceMangaRef.sourceId"
        }
    }

    fun linkSource(ref: SourceMangaRef): CanonicalManga =
        copy(sourceRefs = sourceRefs + (ref.sourceId to ref))

    fun unlinkSource(sourceId: SourceId): CanonicalManga =
        copy(sourceRefs = sourceRefs - sourceId)

    fun sourceRef(sourceId: SourceId): SourceMangaRef? = sourceRefs[sourceId]
}

class CanonicalMangaFactory(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun create(
        title: String,
        initialSource: SourceMangaRef,
        alternativeTitles: Set<String> = emptySet()
    ): CanonicalManga = CanonicalManga(
        id = CanonicalMangaId(idGenerator()),
        title = title.trim(),
        alternativeTitles = alternativeTitles.map(String::trim).filter(String::isNotEmpty).toSet(),
        sourceRefs = mapOf(initialSource.sourceId to initialSource),
        createdAtEpochMs = clock()
    )
}
