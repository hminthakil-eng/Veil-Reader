package com.veilreader.app.data

import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale

internal enum class MangaChapterMatchReason {
    EXACT_VOLUME_AND_NUMBER,
    UNIQUE_NUMBER,
    UNIQUE_TITLE
}

internal data class ExistingMangaChapterIdentity(
    val id: String,
    val title: String,
    val chapterNumber: Double?,
    val volumeNumber: Double?
)

internal data class IncomingMangaChapterIdentity(
    val index: Int,
    val title: String,
    val chapterNumber: Double?,
    val volumeNumber: Double?
)

internal data class MangaChapterMatch(
    val incomingIndex: Int,
    val existingChapterId: String,
    val reason: MangaChapterMatchReason
)

internal data class MangaChapterMatchPlan(
    val matches: List<MangaChapterMatch>,
    val unmatchedIncomingIndices: Set<Int>
) {
    val matchedCount: Int get() = matches.size
}

internal fun planMangaChapterMatches(
    existing: List<ExistingMangaChapterIdentity>,
    incoming: List<IncomingMangaChapterIdentity>
): MangaChapterMatchPlan {
    require(existing.map { it.id }.distinct().size == existing.size) {
        "Existing manga chapter identities must have unique ids."
    }
    require(incoming.map { it.index }.distinct().size == incoming.size) {
        "Incoming manga chapter identities must have unique indices."
    }

    val unmatchedExisting = existing.associateBy { it.id }.toMutableMap()
    val unmatchedIncoming = incoming.associateBy { it.index }.toMutableMap()
    val matches = mutableListOf<MangaChapterMatch>()

    fun accept(
        incomingIndex: Int,
        existingId: String,
        reason: MangaChapterMatchReason
    ) {
        if (unmatchedIncoming.remove(incomingIndex) == null) return
        if (unmatchedExisting.remove(existingId) == null) return
        matches += MangaChapterMatch(incomingIndex, existingId, reason)
    }

    val incomingExact = unmatchedIncoming.values
        .mapNotNull { chapter ->
            val chapterNumber = canonicalNumber(chapter.chapterNumber) ?: return@mapNotNull null
            val volumeNumber = canonicalNumber(chapter.volumeNumber) ?: return@mapNotNull null
            (volumeNumber to chapterNumber) to chapter
        }
        .groupBy({ it.first }, { it.second })

    val existingExact = unmatchedExisting.values
        .mapNotNull { chapter ->
            val chapterNumber = canonicalNumber(chapter.chapterNumber) ?: return@mapNotNull null
            val volumeNumber = canonicalNumber(chapter.volumeNumber) ?: return@mapNotNull null
            (volumeNumber to chapterNumber) to chapter
        }
        .groupBy({ it.first }, { it.second })

    incomingExact.keys.intersect(existingExact.keys).forEach { key ->
        val incomingGroup = incomingExact.getValue(key)
        val existingGroup = existingExact.getValue(key)
        if (incomingGroup.size == 1 && existingGroup.size == 1) {
            accept(
                incomingIndex = incomingGroup.single().index,
                existingId = existingGroup.single().id,
                reason = MangaChapterMatchReason.EXACT_VOLUME_AND_NUMBER
            )
        }
    }

    val incomingByNumber = unmatchedIncoming.values
        .mapNotNull { chapter ->
            canonicalNumber(chapter.chapterNumber)?.let { it to chapter }
        }
        .groupBy({ it.first }, { it.second })

    val existingByNumber = unmatchedExisting.values
        .mapNotNull { chapter ->
            canonicalNumber(chapter.chapterNumber)?.let { it to chapter }
        }
        .groupBy({ it.first }, { it.second })

    incomingByNumber.keys.intersect(existingByNumber.keys).forEach { key ->
        val incomingGroup = incomingByNumber.getValue(key)
        val existingGroup = existingByNumber.getValue(key)
        if (incomingGroup.size != 1 || existingGroup.size != 1) return@forEach

        val incomingChapter = incomingGroup.single()
        val existingChapter = existingGroup.single()
        if (!volumesCompatible(incomingChapter.volumeNumber, existingChapter.volumeNumber)) {
            return@forEach
        }

        accept(
            incomingIndex = incomingChapter.index,
            existingId = existingChapter.id,
            reason = MangaChapterMatchReason.UNIQUE_NUMBER
        )
    }

    val incomingByTitle = unmatchedIncoming.values
        .mapNotNull { chapter ->
            canonicalTitle(chapter.title)?.let { it to chapter }
        }
        .groupBy({ it.first }, { it.second })

    val existingByTitle = unmatchedExisting.values
        .mapNotNull { chapter ->
            canonicalTitle(chapter.title)?.let { it to chapter }
        }
        .groupBy({ it.first }, { it.second })

    incomingByTitle.keys.intersect(existingByTitle.keys).forEach { key ->
        val incomingGroup = incomingByTitle.getValue(key)
        val existingGroup = existingByTitle.getValue(key)
        if (incomingGroup.size == 1 && existingGroup.size == 1) {
            accept(
                incomingIndex = incomingGroup.single().index,
                existingId = existingGroup.single().id,
                reason = MangaChapterMatchReason.UNIQUE_TITLE
            )
        }
    }

    return MangaChapterMatchPlan(
        matches = matches.sortedBy(MangaChapterMatch::incomingIndex),
        unmatchedIncomingIndices = unmatchedIncoming.keys.toSortedSet()
    )
}

private fun canonicalNumber(value: Double?): String? =
    value
        ?.takeIf(Double::isFinite)
        ?.let(BigDecimal::valueOf)
        ?.stripTrailingZeros()
        ?.toPlainString()

private fun volumesCompatible(left: Double?, right: Double?): Boolean {
    val leftCanonical = canonicalNumber(left)
    val rightCanonical = canonicalNumber(right)
    return leftCanonical == null ||
        rightCanonical == null ||
        leftCanonical == rightCanonical
}

private fun canonicalTitle(raw: String): String? {
    val normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .map { character ->
            if (character.isLetterOrDigit()) character else ' '
        }
        .joinToString("")
        .trim()
        .replace(Regex("\\s+"), " ")

    return normalized
        .takeIf { it.length >= 4 }
        ?.takeIf { value -> value.any(Char::isLetter) }
}
