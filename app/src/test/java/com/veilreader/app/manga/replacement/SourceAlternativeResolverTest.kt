package com.veilreader.app.manga.replacement

import com.veilreader.app.manga.library.CanonicalMangaFactory
import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaPublicationStatus
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceMangaSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class SourceAlternativeResolverTest {

    private val oldSource = SourceId("old.source")

    @Test
    fun twoNearlyEqualReadyAlternativesRequireManualChoice() {
        val canonical = canonical()
        val current = details(oldSource, "old", "Veil")

        val resolution = SourceAlternativeResolver(minimumLead = 0.08).resolve(
            canonical = canonical,
            sourceToReplace = oldSource,
            currentDetails = current,
            candidates = listOf(
                ReplacementCandidateInput(details(SourceId("a.source"), "a", "Veil"), emptyList()),
                ReplacementCandidateInput(details(SourceId("b.source"), "b", "Veil"), emptyList())
            ),
            progress = null
        )

        assertEquals(2, resolution.ranked.size)
        assertNull(resolution.recommended)
    }

    @Test
    fun clearlyStrongerReadyAlternativeCanBeRecommended() {
        val canonical = canonical()
        val current = details(
            oldSource,
            "old",
            "The Veiled Library",
            authors = setOf("Archive Team"),
            tags = setOf("fantasy", "mystery")
        )

        val resolution = SourceAlternativeResolver(minimumLead = 0.08).resolve(
            canonical = canonical,
            sourceToReplace = oldSource,
            currentDetails = current,
            candidates = listOf(
                ReplacementCandidateInput(
                    details(
                        SourceId("strong.source"),
                        "strong",
                        "The Veiled Library",
                        authors = setOf("Archive Team"),
                        tags = setOf("fantasy", "mystery")
                    ),
                    emptyList()
                ),
                ReplacementCandidateInput(
                    details(
                        SourceId("weak.source"),
                        "weak",
                        "Veiled",
                        authors = emptySet(),
                        tags = emptySet()
                    ),
                    emptyList()
                )
            ),
            progress = null
        )

        assertNotNull(resolution.recommended)
        assertEquals(
            SourceId("strong.source"),
            resolution.recommended!!.target.summary.ref.sourceId
        )
    }

    private fun canonical() = CanonicalMangaFactory(
        idGenerator = { "canonical" },
        clock = { 1L }
    ).create(
        title = "Veil",
        initialSource = SourceMangaRef(oldSource, "old")
    )

    private fun details(
        source: SourceId,
        key: String,
        title: String,
        authors: Set<String> = setOf("Archive Team"),
        tags: Set<String> = setOf("fantasy")
    ) = SourceMangaDetails(
        summary = SourceMangaSummary(
            ref = SourceMangaRef(source, key),
            title = title,
            languageTag = "en",
            contentType = MangaContentType.MANGA
        ),
        authors = authors,
        status = MangaPublicationStatus.ONGOING,
        tags = tags
    )
}
