package com.veilreader.app.manga.replacement

import com.veilreader.app.manga.library.CanonicalMangaFactory
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaPublicationStatus
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceMangaSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceReplacementPlannerTest {

    private val oldSource = SourceId("old.source")
    private val newSource = SourceId("new.source")

    @Test
    fun strongWorkAndChapterMatchCreatesReadyTwoPhasePlan() {
        val canonical = CanonicalMangaFactory(
            idGenerator = { "canonical-42" },
            clock = { 1L }
        ).create(
            title = "The Veiled Library",
            initialSource = SourceMangaRef(oldSource, "old-work")
        )
        val progress = MangaReadingProgress(
            mangaId = canonical.id,
            chapter = MangaChapterAnchor(
                volume = 2.0,
                number = 12.5,
                languageTag = "en",
                normalizedTitle = "Chapter 12.5",
                providerChapterKeyHint = "old-ch-12-5"
            ),
            pageIndex = 8,
            pageCount = 20,
            chapterProgression = 8.0 / 19.0,
            updatedAtEpochMs = 10L
        )

        val plan = SourceReplacementPlanner(clock = { 99L }).plan(
            canonical = canonical,
            sourceToReplace = oldSource,
            currentDetails = details(
                oldSource,
                "old-work",
                "The Veiled Library",
                authors = setOf("Archive Team")
            ),
            targetDetails = details(
                newSource,
                "new-work",
                "Veiled Library",
                alternativeTitles = setOf("The Veiled Library"),
                authors = setOf("Archive Team")
            ),
            targetChapters = listOf(
                chapter("wrong", 11.0, 2.0),
                chapter("right", 12.5, 2.0)
            ),
            progress = progress
        )

        assertEquals(ReplacementPlanState.READY, plan.state)
        assertTrue(plan.canAutoApply)
        assertEquals(canonical.id, plan.canonicalAfterLink.id)
        assertTrue(oldSource in plan.canonicalAfterLink.sourceRefs)
        assertTrue(newSource in plan.canonicalAfterLink.sourceRefs)
        assertEquals("right", plan.chapterMatch?.chapter?.chapterKey)
        assertEquals("right", plan.migratedProgress?.chapter?.providerChapterKeyHint)
        assertEquals(99L, plan.migratedProgress?.updatedAtEpochMs)
        assertTrue(plan.preservesOfflineCacheOwnership)

        val finalized = SourceReplacementFinalizer.finalize(plan)
        assertEquals(canonical.id, finalized.id)
        assertFalse(oldSource in finalized.sourceRefs)
        assertTrue(newSource in finalized.sourceRefs)
    }

    @Test
    fun ambiguousChapterMatchRequiresManualReviewAndCannotFinalize() {
        val canonical = canonical()
        val progress = progress(canonical.id.value)
        val plan = SourceReplacementPlanner().plan(
            canonical = canonical,
            sourceToReplace = oldSource,
            currentDetails = details(oldSource, "old", "Veil"),
            targetDetails = details(newSource, "new", "Veil"),
            targetChapters = listOf(
                chapter("duplicate-a", 5.0, null),
                chapter("duplicate-b", 5.0, null)
            ),
            progress = progress
        )

        assertEquals(ReplacementPlanState.MANUAL_REVIEW, plan.state)
        assertFalse(plan.canAutoApply)
        assertNull(plan.chapterMatch)
        assertNull(plan.migratedProgress)
        assertTrue("chapter-match-ambiguous-or-weak" in plan.reasons)

        assertThrows(IllegalStateException::class.java) {
            SourceReplacementFinalizer.finalize(plan)
        }
    }

    @Test
    fun unrelatedTitleIsRejectedEvenWhenMetadataLooksSimilar() {
        val canonical = canonical()
        val plan = SourceReplacementPlanner().plan(
            canonical = canonical,
            sourceToReplace = oldSource,
            currentDetails = details(
                oldSource,
                "old",
                "The Veiled Library",
                authors = setOf("Same Author"),
                tags = setOf("fantasy", "mystery")
            ),
            targetDetails = details(
                newSource,
                "new",
                "Completely Different Story",
                authors = setOf("Same Author"),
                tags = setOf("fantasy", "mystery")
            ),
            targetChapters = emptyList(),
            progress = null
        )

        assertEquals(ReplacementPlanState.REJECTED, plan.state)
        assertFalse(plan.canAutoApply)
        assertTrue("work-confidence-too-low" in plan.reasons)
    }

    @Test
    fun strongWorkMatchWithoutProgressCanBeReadyWithoutChapterMapping() {
        val canonical = canonical()

        val plan = SourceReplacementPlanner().plan(
            canonical = canonical,
            sourceToReplace = oldSource,
            currentDetails = details(oldSource, "old", "Veil"),
            targetDetails = details(newSource, "new", "Veil"),
            targetChapters = emptyList(),
            progress = null
        )

        assertEquals(ReplacementPlanState.READY, plan.state)
        assertNull(plan.chapterMatch)
        assertNull(plan.migratedProgress)
        assertTrue("no-progress-to-migrate" in plan.reasons)
    }

    private fun canonical() = CanonicalMangaFactory(
        idGenerator = { "canonical" },
        clock = { 1L }
    ).create(
        title = "Veil",
        initialSource = SourceMangaRef(oldSource, "old")
    )

    private fun progress(id: String) = MangaReadingProgress(
        mangaId = com.veilreader.app.manga.library.CanonicalMangaId(id),
        chapter = MangaChapterAnchor(number = 5.0, languageTag = "en"),
        pageIndex = 2,
        pageCount = 10,
        chapterProgression = 2.0 / 9.0,
        updatedAtEpochMs = 10L
    )

    private fun details(
        sourceId: SourceId,
        key: String,
        title: String,
        alternativeTitles: Set<String> = emptySet(),
        authors: Set<String> = setOf("Archive Team"),
        tags: Set<String> = setOf("fantasy")
    ) = SourceMangaDetails(
        summary = SourceMangaSummary(
            ref = SourceMangaRef(sourceId, key),
            title = title,
            alternativeTitles = alternativeTitles,
            languageTag = "en",
            contentType = MangaContentType.MANGA
        ),
        authors = authors,
        status = MangaPublicationStatus.ONGOING,
        tags = tags
    )

    private fun chapter(
        key: String,
        number: Double,
        volume: Double?
    ) = SourceChapter(
        sourceId = newSource,
        mangaKey = "new-work",
        chapterKey = key,
        title = "Chapter $number",
        number = number,
        volume = volume,
        languageTag = "en"
    )
}
