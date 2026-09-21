package com.veilreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MangaProgressionTest {
    @Test
    fun overallProgression_combinesChapterAndPagePosition() {
        assertEquals(
            0.0,
            mangaOverallProgression(
                chapterPosition = 0,
                chapterCount = 4,
                pageIndex = 0,
                pageCount = 10
            ),
            0.0001
        )

        assertEquals(
            0.375,
            mangaOverallProgression(
                chapterPosition = 1,
                chapterCount = 4,
                pageIndex = 5,
                pageCount = 11
            ),
            0.0001
        )

        assertEquals(
            1.0,
            mangaOverallProgression(
                chapterPosition = 3,
                chapterCount = 4,
                pageIndex = 9,
                pageCount = 10
            ),
            0.0001
        )
    }

    @Test
    fun onePageChapter_countsAsCompletedWithinItsSlot() {
        assertEquals(
            0.5,
            mangaOverallProgression(
                chapterPosition = 0,
                chapterCount = 2,
                pageIndex = 0,
                pageCount = 1
            ),
            0.0001
        )
    }
}
