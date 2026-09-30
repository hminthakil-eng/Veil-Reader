package com.veilreader.app.manga.replacement

import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaPublicationStatus
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceMangaSummary
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceAlternativeScorerTest {

    @Test
    fun exactAlternativeTitleDominatesAndMetadataStrengthensMatch() {
        val current = details(
            source = "a.source",
            title = "The Veiled Library",
            authors = setOf("A. Writer"),
            tags = setOf("fantasy", "mystery")
        )
        val candidate = details(
            source = "b.source",
            title = "Veiled Library",
            alternatives = setOf("The Veiled Library"),
            authors = setOf("A. Writer"),
            tags = setOf("fantasy", "mystery")
        )

        val score = SourceAlternativeScorer.score(current, candidate)

        assertTrue(score.confidence >= 0.90)
        assertTrue(score.titleConfidence >= 0.999)
        assertTrue("exact-title" in score.reasons)
        assertTrue("author" in score.reasons)
    }

    @Test
    fun metadataCannotRescueUnrelatedTitles() {
        val current = details(
            source = "a.source",
            title = "The Veiled Library",
            authors = setOf("Same"),
            tags = setOf("fantasy", "mystery")
        )
        val candidate = details(
            source = "b.source",
            title = "Space Cooking Academy",
            authors = setOf("Same"),
            tags = setOf("fantasy", "mystery")
        )

        val score = SourceAlternativeScorer.score(current, candidate)

        assertTrue(score.titleConfidence < 0.45)
        assertTrue(score.confidence < 0.55)
    }

    private fun details(
        source: String,
        title: String,
        alternatives: Set<String> = emptySet(),
        authors: Set<String>,
        tags: Set<String>
    ) = SourceMangaDetails(
        summary = SourceMangaSummary(
            ref = SourceMangaRef(SourceId(source), "work"),
            title = title,
            alternativeTitles = alternatives,
            languageTag = "en",
            contentType = MangaContentType.MANGA
        ),
        authors = authors,
        status = MangaPublicationStatus.ONGOING,
        tags = tags
    )
}
