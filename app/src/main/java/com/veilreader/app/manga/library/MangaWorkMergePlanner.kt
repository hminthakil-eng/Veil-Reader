package com.veilreader.app.manga.library

import java.util.Locale
import java.util.UUID

/**
 * Pure preflight contract for explicit local Manga work merges.
 *
 * This class never touches Room or the filesystem. Its purpose is to make a destructive-looking
 * operation fully reviewable before any mutation is allowed:
 *
 * - the target Book remains the canonical catalog owner;
 * - source CBZ archives are the authority for chapters moved to another Book identity;
 * - derived page cache is never copied across canonical Manga identities;
 * - exact archive duplicates are deduplicated;
 * - ambiguous semantic collisions are rejected rather than guessed;
 * - a split receipt seed records enough chapter ownership/order truth for a later compensating
 *   transaction.
 *
 * Automatic grouping must not bypass this planner.
 */
class MangaWorkMergePlanner {

    fun plan(
        target: MangaMergeMember,
        sources: List<MangaMergeMember>
    ): MangaMergePlanResult {
        if (target.bookId.isBlank()) {
            return MangaMergePlanResult.Rejected(MangaMergeRejection.BLANK_BOOK_ID)
        }
        if (sources.isEmpty()) {
            return MangaMergePlanResult.Rejected(MangaMergeRejection.NO_SOURCE_WORKS)
        }

        val allMembers = listOf(target) + sources
        if (allMembers.any { it.bookId.isBlank() }) {
            return MangaMergePlanResult.Rejected(MangaMergeRejection.BLANK_BOOK_ID)
        }
        if (allMembers.map { it.bookId }.distinct().size != allMembers.size) {
            return MangaMergePlanResult.Rejected(MangaMergeRejection.DUPLICATE_BOOK_ID)
        }
        if (allMembers.any { it.chapters.isEmpty() }) {
            return MangaMergePlanResult.Rejected(MangaMergeRejection.EMPTY_WORK)
        }
        if (allMembers.any { member -> member.chapters.any { it.bookId != member.bookId } }) {
            return MangaMergePlanResult.Rejected(MangaMergeRejection.CHAPTER_OWNER_MISMATCH)
        }
        val allChapterIds = allMembers.flatMap { member -> member.chapters.map { it.chapterId } }
        if (allChapterIds.distinct().size != allChapterIds.size) {
            return MangaMergePlanResult.Rejected(MangaMergeRejection.DUPLICATE_CHAPTER_ID)
        }
        if (allMembers.any { !hasStableChapterOrder(it.chapters) }) {
            return MangaMergePlanResult.Rejected(MangaMergeRejection.INVALID_CHAPTER_ORDER)
        }

        data class TargetProjection(
            val chapterId: String,
            val readingOrder: Int,
            val semanticIdentity: String?
        )

        val exactOwnerByFingerprint = linkedMapOf<String, TargetProjection>()
        target.chapters
            .sortedBy(MangaMergeChapterCandidate::readingOrder)
            .forEach { chapter ->
                chapter.localArchiveFingerprint()?.let { fingerprint ->
                    exactOwnerByFingerprint.putIfAbsent(
                        fingerprint,
                        TargetProjection(
                            chapterId = chapter.chapterId,
                            readingOrder = chapter.readingOrder,
                            semanticIdentity = chapter.semanticIdentity()
                        )
                    )
                }
            }

        val semanticOwner = linkedMapOf<String, String>()
        target.chapters.forEach { chapter ->
            chapter.semanticIdentity()?.let { semanticOwner.putIfAbsent(it, chapter.chapterId) }
        }

        var nextReadingOrder =
            (target.chapters.maxOfOrNull(MangaMergeChapterCandidate::readingOrder) ?: -1) + 1
        val actions = mutableListOf<MangaMergeChapterAction>()

        sources.forEach { source ->
            source.chapters
                .sortedBy(MangaMergeChapterCandidate::readingOrder)
                .forEach { chapter ->
                    val fingerprint = chapter.localArchiveFingerprint()
                    val sourceSemantic = chapter.semanticIdentity()
                    val exact = fingerprint?.let(exactOwnerByFingerprint::get)
                    if (
                        exact != null &&
                        exact.semanticIdentity != null &&
                        sourceSemantic != null &&
                        exact.semanticIdentity != sourceSemantic
                    ) {
                        return MangaMergePlanResult.Rejected(
                            reason = MangaMergeRejection.EXACT_ARCHIVE_METADATA_CONFLICT,
                            conflictingSourceChapterId = chapter.chapterId,
                            conflictingTargetChapterId = exact.chapterId
                        )
                    }
                    if (exact != null) {
                        actions += MangaMergeChapterAction(
                            sourceBookId = source.bookId,
                            sourceChapterId = chapter.chapterId,
                            sourceReadingOrder = chapter.readingOrder,
                            targetReadingOrder = exact.readingOrder,
                            disposition = MangaMergeDisposition.DEDUPLICATE_EXACT_ARCHIVE,
                            plannedTargetChapterId = exact.chapterId,
                            matchedTargetChapterId = exact.chapterId,
                            rebuildDerivedCache = false
                        )
                        return@forEach
                    }

                    val semantic = sourceSemantic
                    val semanticMatch = semantic?.let(semanticOwner::get)
                    if (semanticMatch != null) {
                        return MangaMergePlanResult.Rejected(
                            reason = MangaMergeRejection.AMBIGUOUS_CHAPTER_COLLISION,
                            conflictingSourceChapterId = chapter.chapterId,
                            conflictingTargetChapterId = semanticMatch
                        )
                    }

                    val targetReadingOrder = nextReadingOrder++
                    val projectedTargetChapterId = projectedTargetChapterId(
                        targetBookId = target.bookId,
                        chapter = chapter
                    ) ?: return MangaMergePlanResult.Rejected(
                        reason = MangaMergeRejection.UNSTABLE_TARGET_IDENTITY,
                        conflictingSourceChapterId = chapter.chapterId
                    )
                    actions += MangaMergeChapterAction(
                        sourceBookId = source.bookId,
                        sourceChapterId = chapter.chapterId,
                        sourceReadingOrder = chapter.readingOrder,
                        targetReadingOrder = targetReadingOrder,
                        disposition = MangaMergeDisposition.REBUILD_FROM_SOURCE_ARCHIVE,
                        plannedTargetChapterId = projectedTargetChapterId,
                        matchedTargetChapterId = null,
                        rebuildDerivedCache = true
                    )
                    if (fingerprint != null) {
                        exactOwnerByFingerprint[fingerprint] = TargetProjection(
                            chapterId = projectedTargetChapterId,
                            readingOrder = targetReadingOrder,
                            semanticIdentity = semantic
                        )
                    }
                    if (semantic != null) {
                        semanticOwner[semantic] = projectedTargetChapterId
                    }
                }
        }

        val receipt = MangaMergeSplitReceiptSeed(
            targetBookId = target.bookId,
            targetOriginalChapterIds = target.chapters
                .sortedBy(MangaMergeChapterCandidate::readingOrder)
                .map(MangaMergeChapterCandidate::chapterId),
            sourceSnapshots = sources.map { source ->
                MangaMergeSourceSnapshot(
                    bookId = source.bookId,
                    title = source.title,
                    author = source.author,
                    sourceUri = source.sourceUri,
                    contentFingerprint = source.contentFingerprint,
                    seriesName = source.seriesName,
                    seriesIndex = source.seriesIndex,
                    language = source.language,
                    collections = source.collections,
                    chapters = source.chapters
                        .sortedBy(MangaMergeChapterCandidate::readingOrder)
                        .map { chapter ->
                            MangaMergeSourceChapterSnapshot(
                                chapterId = chapter.chapterId,
                                readingOrder = chapter.readingOrder,
                                cacheKey = chapter.cacheKey,
                                chapterKey = chapter.chapterKey,
                                volume = chapter.volume,
                                number = chapter.number,
                                languageTag = chapter.languageTag,
                                normalizedTitle = chapter.normalizedTitle
                            )
                        }
                )
            }
        )

        return MangaMergePlanResult.Ready(
            MangaWorkMergePlan(
                targetBookId = target.bookId,
                sourceBookIds = sources.map(MangaMergeMember::bookId),
                chapterActions = actions,
                splitReceiptSeed = receipt,
                requiresSourceArchiveRetentionUntilCommit = true,
                mayDeleteSourceBooksBeforeVerification = false
            )
        )
    }

    private fun projectedTargetChapterId(
        targetBookId: String,
        chapter: MangaMergeChapterCandidate
    ): String? {
        val offlineId = MangaOfflineChapterLocator.idFor(
            mangaId = CanonicalMangaId(targetBookId),
            anchor = MangaChapterAnchor(
                volume = chapter.volume,
                number = chapter.number,
                languageTag = chapter.languageTag,
                normalizedTitle = chapter.normalizedTitle,
                providerChapterKeyHint = chapter.chapterKey
            )
        ) ?: return null
        val cacheKey = MangaCacheLayout.chapterDirectory(offlineId)
        return UUID.nameUUIDFromBytes(
            ("veil-cbz:" + targetBookId + ":" + cacheKey).toByteArray(Charsets.UTF_8)
        ).toString()
    }

    private fun hasStableChapterOrder(chapters: List<MangaMergeChapterCandidate>): Boolean {
        val orders = chapters.map(MangaMergeChapterCandidate::readingOrder)
        if (orders.any { it < 0 } || orders.distinct().size != orders.size) return false
        return orders.sorted() == (0..orders.lastIndex).toList()
    }
}

data class MangaMergeMember(
    val bookId: String,
    val title: String,
    val author: String = "",
    val sourceUri: String? = null,
    val contentFingerprint: String? = null,
    val seriesName: String? = null,
    val seriesIndex: Double? = null,
    val language: String? = null,
    val collections: List<String> = emptyList(),
    val chapters: List<MangaMergeChapterCandidate>
)

data class MangaMergeChapterCandidate(
    val bookId: String,
    val chapterId: String,
    val readingOrder: Int,
    val cacheKey: String,
    val chapterKey: String,
    val volume: Double?,
    val number: Double?,
    val languageTag: String?,
    val normalizedTitle: String?
) {
    init {
        require(bookId.isNotBlank())
        require(chapterId.isNotBlank())
        require(readingOrder >= 0)
        require(cacheKey.isNotBlank())
        require(chapterKey.isNotBlank())
        require(volume == null || volume.isFinite())
        require(number == null || number.isFinite())
    }

    fun localArchiveFingerprint(): String? =
        chapterKey
            .takeIf { it.startsWith(LOCAL_CBZ_PREFIX) }
            ?.removePrefix(LOCAL_CBZ_PREFIX)
            ?.lowercase(Locale.ROOT)
            ?.takeIf { value ->
                value.length == SHA256_HEX_LENGTH &&
                    value.all { it in '0'..'9' || it in 'a'..'f' }
            }

    fun semanticIdentity(): String? {
        val cleanNumber = number?.takeIf(Double::isFinite) ?: return null
        val cleanVolume = volume?.takeIf(Double::isFinite)
        val language = languageTag
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        return buildString {
            append(cleanVolume?.normalizedDecimal() ?: "-")
            append('|')
            append(cleanNumber.normalizedDecimal())
            append('|')
            append(language)
        }
    }

    private fun Double.normalizedDecimal(): String =
        if (this % 1.0 == 0.0) toLong().toString() else toString()

    private companion object {
        const val LOCAL_CBZ_PREFIX = "cbz-"
        const val SHA256_HEX_LENGTH = 64
    }
}

enum class MangaMergeDisposition {
    REBUILD_FROM_SOURCE_ARCHIVE,
    DEDUPLICATE_EXACT_ARCHIVE
}

data class MangaMergeChapterAction(
    val sourceBookId: String,
    val sourceChapterId: String,
    val sourceReadingOrder: Int,
    val targetReadingOrder: Int,
    val disposition: MangaMergeDisposition,
    val plannedTargetChapterId: String,
    val matchedTargetChapterId: String?,
    /**
     * True means an executor must derive a fresh cache identity under the target canonical Manga id.
     * A merge executor must never rename/copy the old cache directory as if it remained authoritative.
     */
    val rebuildDerivedCache: Boolean
)

data class MangaWorkMergePlan(
    val targetBookId: String,
    val sourceBookIds: List<String>,
    val chapterActions: List<MangaMergeChapterAction>,
    val splitReceiptSeed: MangaMergeSplitReceiptSeed,
    val requiresSourceArchiveRetentionUntilCommit: Boolean,
    val mayDeleteSourceBooksBeforeVerification: Boolean
)

data class MangaMergeSplitReceiptSeed(
    val targetBookId: String,
    val targetOriginalChapterIds: List<String>,
    val sourceSnapshots: List<MangaMergeSourceSnapshot>
)

data class MangaMergeSourceSnapshot(
    val bookId: String,
    val title: String,
    val author: String,
    val sourceUri: String?,
    val contentFingerprint: String?,
    val seriesName: String?,
    val seriesIndex: Double?,
    val language: String?,
    val collections: List<String>,
    val chapters: List<MangaMergeSourceChapterSnapshot>
)

data class MangaMergeSourceChapterSnapshot(
    val chapterId: String,
    val readingOrder: Int,
    val cacheKey: String,
    val chapterKey: String,
    val volume: Double?,
    val number: Double?,
    val languageTag: String?,
    val normalizedTitle: String?
)

enum class MangaMergeRejection {
    BLANK_BOOK_ID,
    NO_SOURCE_WORKS,
    DUPLICATE_BOOK_ID,
    EMPTY_WORK,
    CHAPTER_OWNER_MISMATCH,
    DUPLICATE_CHAPTER_ID,
    INVALID_CHAPTER_ORDER,
    UNSTABLE_TARGET_IDENTITY,
    EXACT_ARCHIVE_METADATA_CONFLICT,
    AMBIGUOUS_CHAPTER_COLLISION
}

sealed interface MangaMergePlanResult {
    data class Ready(val plan: MangaWorkMergePlan) : MangaMergePlanResult

    data class Rejected(
        val reason: MangaMergeRejection,
        val conflictingSourceChapterId: String? = null,
        val conflictingTargetChapterId: String? = null
    ) : MangaMergePlanResult
}
