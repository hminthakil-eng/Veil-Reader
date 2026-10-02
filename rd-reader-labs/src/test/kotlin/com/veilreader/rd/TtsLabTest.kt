package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TtsLabTest {
    @Test fun chunksRespectMaximumAndPersianSentenceBoundary() {
        val text = "سلام دنیا؟ این یک آزمایش است. جمله سوم برای خواندن."
        val chunks = TtsChunker.chunks(text, maxChars = 80)
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.length <= 80 })
    }

    @Test fun filtersRunBeforeChunking() {
        val chunks = TtsChunker.chunks(
            "Chapter 1. Keep this.",
            maxChars = 80,
            filters = listOf(TtsFilterRule(Regex("Chapter 1\\.?"), ""))
        )
        assertEquals("Keep this.", chunks.single())
    }
}
