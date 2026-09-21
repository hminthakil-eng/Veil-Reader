package com.veilreader.app.data.manga

import androidx.room.withTransaction
import com.veilreader.app.data.db.MangaOfflineChapterEntity
import com.veilreader.app.data.db.MangaOfflineManifestRecord
import com.veilreader.app.data.db.MangaOfflinePageEntity
import com.veilreader.app.data.db.MangaProgressEntity
import com.veilreader.app.data.db.MangaSourceLinkEntity
import com.veilreader.app.data.db.MangaWorkEntity
import com.veilreader.app.data.db.MangaWorkWithLinks
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaCanonicalStore
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaOfflineCacheIndex
import com.veilreader.app.manga.library.MangaProgressStore
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.library.OfflineChapterId
import com.veilreader.app.manga.library.OfflineChapterManifest
import com.veilreader.app.manga.library.OfflinePageEntry
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaRef
import java.security.MessageDigest
import org.json.JSONArray

data class MangaBackupSnapshot(
    val works: List<CanonicalManga> = emptyList(),
    val progress: List<MangaReadingProgress> = emptyList()
)

class MangaRoomRepository(
    private val database: VeilDatabase
) : MangaCanonicalStore, MangaProgressStore, MangaOfflineCacheIndex {

    override suspend fun loadWork(id: CanonicalMangaId): CanonicalManga? =
        database.mangaLibrary().find(id.value)?.toDomain()

    override suspend fun listWorks(): List<CanonicalManga> =
        database.mangaLibrary().listAll().map(MangaWorkWithLinks::toDomain)

    override suspend fun saveWork(manga: CanonicalManga) {
        database.withTransaction {
            database.mangaLibrary().upsertWork(manga.toEntity())
            database.mangaLibrary().deleteLinksForManga(manga.id.value)
            val links = manga.sourceRefs.values
                .sortedBy { it.sourceId.value }
                .map { it.toEntity(manga.id) }
            if (links.isNotEmpty()) {
                database.mangaLibrary().upsertLinks(links)
            }
        }
    }

    override suspend fun deleteWork(id: CanonicalMangaId) {
        database.mangaLibrary().deleteWork(id.value)
    }

    override suspend fun load(mangaId: CanonicalMangaId): MangaReadingProgress? =
        database.mangaProgress().find(mangaId.value)?.toDomain()

    override suspend fun save(progress: MangaReadingProgress) {
        require(database.mangaLibrary().find(progress.mangaId.value) != null) {
            "Canonical manga must exist before progress can be persisted"
        }
        database.mangaProgress().upsert(progress.toEntity())
    }

    override suspend fun delete(mangaId: CanonicalMangaId) {
        database.mangaProgress().delete(mangaId.value)
    }

    override suspend fun load(chapterId: OfflineChapterId): OfflineChapterManifest? =
        database.mangaOffline()
            .find(MangaOfflineManifestKey.encode(chapterId))
            ?.toDomain()

    override suspend fun put(manifest: OfflineChapterManifest) {
        require(database.mangaLibrary().find(manifest.chapterId.mangaId.value) != null) {
            "Canonical manga must exist before offline manifests can be persisted"
        }

        val manifestId = MangaOfflineManifestKey.encode(manifest.chapterId)
        database.withTransaction {
            database.mangaOffline().upsertChapter(
                manifest.toEntity(manifestId)
            )
            database.mangaOffline().deletePages(manifestId)
            val pages = manifest.pages
                .sortedBy { it.index }
                .map { it.toEntity(manifestId) }
            if (pages.isNotEmpty()) {
                database.mangaOffline().upsertPages(pages)
            }
        }
    }

    override suspend fun remove(chapterId: OfflineChapterId) {
        database.mangaOffline().deleteManifest(
            MangaOfflineManifestKey.encode(chapterId)
        )
    }

    override suspend fun listForManga(
        mangaId: CanonicalMangaId
    ): List<OfflineChapterManifest> =
        database.mangaOffline()
            .listForManga(mangaId.value)
            .map(MangaOfflineManifestRecord::toDomain)

    suspend fun backupSnapshot(): MangaBackupSnapshot =
        database.withTransaction {
            MangaBackupSnapshot(
                works = database.mangaLibrary().listAll().map(MangaWorkWithLinks::toDomain),
                progress = database.mangaProgress().listAll().map(MangaProgressEntity::toDomain)
            )
        }

    /**
     * User backup restores durable Manga library identity/progress only.
     * Offline cache bytes are intentionally not part of the archive, so manifests are cleared.
     */
    suspend fun replaceFromBackup(snapshot: MangaBackupSnapshot) {
        database.withTransaction {
            database.mangaOffline().deleteAll()
            database.mangaLibrary().deleteAll()

            snapshot.works
                .sortedBy { it.createdAtEpochMs }
                .forEach { manga ->
                    database.mangaLibrary().upsertWork(manga.toEntity())
                    val links = manga.sourceRefs.values
                        .sortedBy { it.sourceId.value }
                        .map { it.toEntity(manga.id) }
                    if (links.isNotEmpty()) database.mangaLibrary().upsertLinks(links)
                }

            snapshot.progress.forEach { progress ->
                require(snapshot.works.any { it.id == progress.mangaId }) {
                    "Backup progress references an unknown canonical manga"
                }
                database.mangaProgress().upsert(progress.toEntity())
            }
        }
    }

    suspend fun clearOfflineIndex() {
        database.mangaOffline().deleteAll()
    }
}

object MangaOfflineManifestKey {
    fun encode(id: OfflineChapterId): String {
        val raw = buildString {
            appendSegment(id.mangaId.value)
            appendSegment(id.languageTag)
            appendSegment(id.volume?.toString())
            appendSegment(id.number?.toString())
            appendSegment(id.discriminator)
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun StringBuilder.appendSegment(value: String?) {
        if (value == null) {
            append("-1:")
        } else {
            append(value.length).append(':').append(value)
        }
        append('|')
    }
}

private fun CanonicalManga.toEntity(): MangaWorkEntity =
    MangaWorkEntity(
        id = id.value,
        title = title,
        alternativeTitlesJson = JSONArray(alternativeTitles.sorted()).toString(),
        createdAtEpochMs = createdAtEpochMs
    )

private fun SourceMangaRef.toEntity(mangaId: CanonicalMangaId): MangaSourceLinkEntity =
    MangaSourceLinkEntity(
        mangaId = mangaId.value,
        sourceId = sourceId.value,
        sourceKey = key,
        publicUrl = publicUrl
    )

private fun MangaWorkWithLinks.toDomain(): CanonicalManga {
    val refs = sourceLinks.associate { link ->
        val sourceId = SourceId(link.sourceId)
        sourceId to SourceMangaRef(
            sourceId = sourceId,
            key = link.sourceKey,
            publicUrl = link.publicUrl
        )
    }
    val titles = JSONArray(work.alternativeTitlesJson)
    val alternatives = buildSet {
        for (index in 0 until titles.length()) {
            titles.optString(index)
                .trim()
                .takeIf(String::isNotEmpty)
                ?.let(::add)
        }
    }
    return CanonicalManga(
        id = CanonicalMangaId(work.id),
        title = work.title,
        alternativeTitles = alternatives,
        sourceRefs = refs,
        createdAtEpochMs = work.createdAtEpochMs
    )
}

private fun MangaReadingProgress.toEntity(): MangaProgressEntity =
    MangaProgressEntity(
        mangaId = mangaId.value,
        volume = chapter.volume,
        chapterNumber = chapter.number,
        languageTag = chapter.languageTag,
        normalizedTitle = chapter.normalizedTitle,
        providerChapterKeyHint = chapter.providerChapterKeyHint,
        pageIndex = pageIndex,
        pageCount = pageCount,
        chapterProgression = chapterProgression,
        updatedAtEpochMs = updatedAtEpochMs
    )

private fun MangaProgressEntity.toDomain(): MangaReadingProgress =
    MangaReadingProgress(
        mangaId = CanonicalMangaId(mangaId),
        chapter = MangaChapterAnchor(
            volume = volume,
            number = chapterNumber,
            languageTag = languageTag,
            normalizedTitle = normalizedTitle,
            providerChapterKeyHint = providerChapterKeyHint
        ),
        pageIndex = pageIndex,
        pageCount = pageCount,
        chapterProgression = chapterProgression,
        updatedAtEpochMs = updatedAtEpochMs
    )

private fun OfflineChapterManifest.toEntity(
    manifestId: String
): MangaOfflineChapterEntity =
    MangaOfflineChapterEntity(
        manifestId = manifestId,
        mangaId = chapterId.mangaId.value,
        languageTag = chapterId.languageTag,
        volume = chapterId.volume,
        chapterNumber = chapterId.number,
        discriminator = chapterId.discriminator,
        anchorVolume = anchor.volume,
        anchorNumber = anchor.number,
        anchorLanguageTag = anchor.languageTag,
        anchorNormalizedTitle = anchor.normalizedTitle,
        anchorProviderChapterKeyHint = anchor.providerChapterKeyHint,
        originSourceId = originSourceId.value,
        originChapterKey = originChapterKey,
        completed = completed,
        updatedAtEpochMs = updatedAtEpochMs
    )

private fun OfflinePageEntry.toEntity(
    manifestId: String
): MangaOfflinePageEntity =
    MangaOfflinePageEntity(
        manifestId = manifestId,
        pageIndex = index,
        relativePath = relativePath,
        byteSize = byteSize,
        contentSha256 = contentSha256
    )

private fun MangaOfflineManifestRecord.toDomain(): OfflineChapterManifest =
    OfflineChapterManifest(
        chapterId = OfflineChapterId(
            mangaId = CanonicalMangaId(chapter.mangaId),
            languageTag = chapter.languageTag,
            volume = chapter.volume,
            number = chapter.chapterNumber,
            discriminator = chapter.discriminator
        ),
        anchor = MangaChapterAnchor(
            volume = chapter.anchorVolume,
            number = chapter.anchorNumber,
            languageTag = chapter.anchorLanguageTag,
            normalizedTitle = chapter.anchorNormalizedTitle,
            providerChapterKeyHint = chapter.anchorProviderChapterKeyHint
        ),
        pages = pages
            .sortedBy { it.pageIndex }
            .map {
                OfflinePageEntry(
                    index = it.pageIndex,
                    relativePath = it.relativePath,
                    byteSize = it.byteSize,
                    contentSha256 = it.contentSha256
                )
            },
        originSourceId = SourceId(chapter.originSourceId),
        originChapterKey = chapter.originChapterKey,
        completed = chapter.completed,
        updatedAtEpochMs = chapter.updatedAtEpochMs
    )
