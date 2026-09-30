package com.veilreader.app.manga.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaWorkMergePlannerTest {

    private val planner = MangaWorkMergePlanner()

    @Test
    fun plan_keepsTargetOrder_andRebuildsMovedChaptersDeterministically() {
        val target = member(
            bookId = "target",
            chapter("target", "t0", 0, fingerprint = "a".repeat(64), number = 1.0),
            chapter("target", "t1", 1, fingerprint = "b".repeat(64), number = 2.0)
        )
        val source = member(
            bookId = "source",
            chapter("source", "s0", 0, fingerprint = "c".repeat(64), number = 3.0),
            chapter("source", "s1", 1, fingerprint = "d".repeat(64), number = 4.0)
        )

        val result = planner.plan(target, listOf(source))
        assertTrue(result is MangaMergePlanResult.Ready)
        val plan = (result as MangaMergePlanResult.Ready).plan

        assertEquals(listOf("source"), plan.sourceBookIds)
        assertEquals(listOf(2, 3), plan.chapterActions.map { it.targetReadingOrder })
        assertTrue(plan.chapterActions.all { it.rebuildDerivedCache })
        assertTrue(
            plan.chapterActions.all {
                it.disposition == MangaMergeDisposition.REBUILD_FROM_SOURCE_ARCHIVE
            }
        )
        assertTrue(plan.requiresSourceArchiveRetentionUntilCommit)
        assertFalse(plan.mayDeleteSourceBooksBeforeVerification)
        assertEquals(listOf("t0", "t1"), plan.splitReceiptSeed.targetOriginalChapterIds)
        assertEquals(
            listOf("s0", "s1"),
            plan.splitReceiptSeed.sourceSnapshots.single().chapters.map { it.chapterId }
        )
    }

    @Test
    fun plan_deduplicatesExactArchive_withoutReusingForeignCacheIdentity() {
        val sharedFingerprint = "e".repeat(64)
        val target = member(
            "target",
            chapter("target", "t0", 0, sharedFingerprint, number = 1.0)
        )
        val source = member(
            "source",
            chapter("source", "s0", 0, sharedFingerprint, number = 1.0)
        )

        val plan = (planner.plan(target, listOf(source)) as MangaMergePlanResult.Ready).plan
        val action = plan.chapterActions.single()

        assertEquals(MangaMergeDisposition.DEDUPLICATE_EXACT_ARCHIVE, action.disposition)
        assertEquals("t0", action.matchedTargetChapterId)
        assertEquals(0, action.targetReadingOrder)
        assertFalse(action.rebuildDerivedCache)
    }

    @Test
    fun plan_rejectsDifferentArchivesWithSameChapterIdentity() {
        val target = member(
            "target",
            chapter(
                "target",
                "t0",
                0,
                fingerprint = "1".repeat(64),
                volume = 2.0,
                number = 14.0,
                language = "en"
            )
        )
        val source = member(
            "source",
            chapter(
                "source",
                "s0",
                0,
                fingerprint = "2".repeat(64),
                volume = 2.0,
                number = 14.0,
                language = "EN"
            )
        )

        val result = planner.plan(target, listOf(source))

        assertTrue(result is MangaMergePlanResult.Rejected)
        result as MangaMergePlanResult.Rejected
        assertEquals(MangaMergeRejection.AMBIGUOUS_CHAPTER_COLLISION, result.reason)
        assertEquals("s0", result.conflictingSourceChapterId)
        assertEquals("t0", result.conflictingTargetChapterId)
    }

    @Test
    fun plan_rejectsTargetRepeatedAsSource() {
        val target = member(
            "same",
            chapter("same", "t0", 0, "3".repeat(64), number = 1.0)
        )
        val source = member(
            "same",
            chapter("same", "s0", 0, "4".repeat(64), number = 2.0)
        )

        val result = planner.plan(target, listOf(source))

        assertEquals(
            MangaMergeRejection.DUPLICATE_BOOK_ID,
            (result as MangaMergePlanResult.Rejected).reason
        )
    }

    @Test
    fun plan_rejectsNonContiguousChapterOrder() {
        val target = member(
            "target",
            chapter("target", "t0", 0, "5".repeat(64), number = 1.0),
            chapter("target", "t2", 2, "6".repeat(64), number = 3.0)
        )
        val source = member(
            "source",
            chapter("source", "s0", 0, "7".repeat(64), number = 4.0)
        )

        val result = planner.plan(target, listOf(source))

        assertEquals(
            MangaMergeRejection.INVALID_CHAPTER_ORDER,
            (result as MangaMergePlanResult.Rejected).reason
        )
    }

    @Test
    fun plan_doesNotGuessWhenChapterNumberIsMissing() {
        val target = member(
            "target",
            chapter("target", "t0", 0, "8".repeat(64), number = null)
        )
        val source = member(
            "source",
            chapter("source", "s0", 0, "9".repeat(64), number = null)
        )

        val result = planner.plan(target, listOf(source))

        assertTrue(result is MangaMergePlanResult.Ready)
        val plan = (result as MangaMergePlanResult.Ready).plan
        assertEquals(
            MangaMergeDisposition.REBUILD_FROM_SOURCE_ARCHIVE,
            plan.chapterActions.single().disposition
        )
    }

    private fun member(
        bookId: String,
        vararg chapters: MangaMergeChapterCandidate
    ) = MangaMergeMember(
        bookId = bookId,
        title = "Work $bookId",
        chapters = chapters.toList()
    )

    private fun chapter(
        bookId: String,
        chapterId: String,
        readingOrder: Int,
        fingerprint: String,
        volume: Double? = null,
        number: Double?,
        language: String? = null
    ) = MangaMergeChapterCandidate(
        bookId = bookId,
        chapterId = chapterId,
        readingOrder = readingOrder,
        cacheKey = "cache/$bookId/$chapterId",
        chapterKey = "cbz-$fingerprint",
        volume = volume,
        number = number,
        languageTag = language,
        normalizedTitle = "Chapter $chapterId"
    )
}
