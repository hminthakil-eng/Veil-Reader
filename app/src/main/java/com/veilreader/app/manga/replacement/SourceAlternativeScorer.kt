package com.veilreader.app.manga.replacement

import com.veilreader.app.manga.source.SourceMangaDetails
import kotlin.math.max
import kotlin.math.min

data class WorkMatchScore(
    val confidence: Double,
    val reasons: Set<String>,
    val titleConfidence: Double
) {
    init {
        require(confidence.isFinite() && confidence in 0.0..1.0)
        require(titleConfidence.isFinite() && titleConfidence in 0.0..1.0)
    }
}

/**
 * Conservative logical-work comparison for source alternatives.
 *
 * Titles dominate the score. Metadata can strengthen a title match but cannot turn unrelated titles
 * into a high-confidence replacement.
 */
object SourceAlternativeScorer {

    fun score(
        current: SourceMangaDetails,
        candidate: SourceMangaDetails
    ): WorkMatchScore {
        val reasons = linkedSetOf<String>()

        val currentTitles = normalizedTitles(current)
        val candidateTitles = normalizedTitles(candidate)
        val titleConfidence = titleSimilarity(currentTitles, candidateTitles)
        if (titleConfidence >= 0.999) reasons += "exact-title"
        else if (titleConfidence >= 0.65) reasons += "similar-title"

        val authorOverlap = overlap(
            current.authors.map(::normalizeText).filter(String::isNotBlank).toSet(),
            candidate.authors.map(::normalizeText).filter(String::isNotBlank).toSet()
        )
        if (authorOverlap > 0.0) reasons += "author"

        val tagOverlap = overlap(
            current.tags.map(::normalizeText).filter(String::isNotBlank).toSet(),
            candidate.tags.map(::normalizeText).filter(String::isNotBlank).toSet()
        )
        if (tagOverlap >= 0.5) reasons += "tags"

        val languageMatch = current.summary.languageTag != null &&
            candidate.summary.languageTag != null &&
            current.summary.languageTag.equals(candidate.summary.languageTag, ignoreCase = true)
        if (languageMatch) reasons += "language"

        val contentTypeMatch =
            current.summary.contentType == candidate.summary.contentType
        if (contentTypeMatch) reasons += "content-type"

        val statusMatch =
            current.status == candidate.status
        if (statusMatch) reasons += "status"

        // Metadata only contributes meaningfully after title evidence exists.
        val metadataWeight = if (titleConfidence >= 0.45) 1.0 else 0.25
        val score =
            (0.65 * titleConfidence) +
                metadataWeight * (0.15 * authorOverlap) +
                metadataWeight * (0.08 * tagOverlap) +
                metadataWeight * (if (languageMatch) 0.05 else 0.0) +
                metadataWeight * (if (contentTypeMatch) 0.05 else 0.0) +
                metadataWeight * (if (statusMatch) 0.02 else 0.0)

        return WorkMatchScore(
            confidence = score.coerceIn(0.0, 1.0),
            reasons = reasons,
            titleConfidence = titleConfidence
        )
    }

    private fun normalizedTitles(details: SourceMangaDetails): Set<String> =
        (details.summary.alternativeTitles + details.summary.title)
            .map(::normalizeText)
            .filter(String::isNotBlank)
            .toSet()

    private fun titleSimilarity(left: Set<String>, right: Set<String>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        if (left.any { it in right }) return 1.0
        return left.maxOf { a ->
            right.maxOf { b -> tokenJaccard(a, b) }
        }
    }

    private fun tokenJaccard(a: String, b: String): Double {
        val left = a.split(' ').filter(String::isNotBlank).toSet()
        val right = b.split(' ').filter(String::isNotBlank).toSet()
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val intersection = left.intersect(right).size.toDouble()
        val union = left.union(right).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }

    private fun overlap(left: Set<String>, right: Set<String>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val intersection = left.intersect(right).size.toDouble()
        return intersection / min(left.size, right.size).coerceAtLeast(1)
    }

    private fun normalizeText(value: String): String =
        value.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
