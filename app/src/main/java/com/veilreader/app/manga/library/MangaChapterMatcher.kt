package com.veilreader.app.manga.library

import com.veilreader.app.manga.source.SourceChapter

data class ChapterMatch(
    val chapter: SourceChapter,
    val confidence: Double,
    val reasons: Set<String>
) {
    init {
        require(confidence.isFinite() && confidence in 0.0..1.0) {
            "Match confidence must be between 0 and 1"
        }
    }
}

object MangaChapterMatcher {
    fun rank(
        anchor: MangaChapterAnchor,
        candidates: List<SourceChapter>
    ): List<ChapterMatch> = candidates.map { candidate ->
        val reasons = linkedSetOf<String>()
        var score = 0.0
        var possible = 0.0

        anchor.number?.let { expected ->
            possible += 0.55
            if (candidate.number != null && close(expected, candidate.number)) {
                score += 0.55
                reasons += "chapter-number"
            }
        }

        anchor.volume?.let { expected ->
            possible += 0.20
            if (candidate.volume != null && close(expected, candidate.volume)) {
                score += 0.20
                reasons += "volume"
            }
        }

        anchor.languageTag?.let { expected ->
            possible += 0.15
            if (candidate.languageTag?.equals(expected, ignoreCase = true) == true) {
                score += 0.15
                reasons += "language"
            }
        }

        anchor.normalizedTitle?.takeIf(String::isNotBlank)?.let { expected ->
            possible += 0.10
            val actual = candidate.title?.let(::normalizeTitle)
            if (actual == normalizeTitle(expected)) {
                score += 0.10
                reasons += "title"
            }
        }

        val confidence = if (possible == 0.0) 0.0 else (score / possible).coerceIn(0.0, 1.0)
        ChapterMatch(candidate, confidence, reasons)
    }.sortedWith(
        compareByDescending<ChapterMatch> { it.confidence }
            .thenBy { it.chapter.number ?: Double.MAX_VALUE }
    )

    fun bestSafeMatch(
        anchor: MangaChapterAnchor,
        candidates: List<SourceChapter>,
        minimumConfidence: Double = 0.75
    ): ChapterMatch? {
        require(minimumConfidence in 0.0..1.0)
        val ranked = rank(anchor, candidates)
        val best = ranked.firstOrNull() ?: return null
        val second = ranked.getOrNull(1)

        if (best.confidence < minimumConfidence) return null
        if (second != null && second.confidence == best.confidence) return null
        return best
    }

    private fun close(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) < 0.0001

    private fun normalizeTitle(value: String): String =
        value.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
