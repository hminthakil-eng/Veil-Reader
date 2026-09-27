package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryAtlasTest {
    @Test
    fun `metadata relations remain explicit and additive`() {
        val books = listOf(
            Book(
                id = "a",
                title = "A",
                author = "Same Author",
                seriesName = "Veil Cycle",
                collections = listOf("Mystery")
            ),
            Book(
                id = "b",
                title = "B",
                author = "Same Author",
                seriesName = "Veil Cycle",
                collections = listOf("Mystery")
            )
        )

        val atlas = buildMemoryAtlas(books, emptyList(), emptyList())
        val edge = atlas.edges.single()

        assertTrue(MemoryRelationKind.AUTHOR in edge.reasons)
        assertTrue(MemoryRelationKind.SERIES in edge.reasons)
        assertTrue(MemoryRelationKind.COLLECTION in edge.reasons)
        // AUTHOR(3) + SERIES(4) + one shared COLLECTION(2) = 9.
        assertEquals(9, edge.strength)
    }

    @Test
    fun `one common word never fabricates a preserved passage relation`() {
        val books = listOf(
            Book(id = "a", title = "A", author = "One"),
            Book(id = "b", title = "B", author = "Two")
        )
        val highlights = listOf(
            Highlight("ha", "a", "lantern solitary chamber archive", "{}"),
            Highlight("hb", "b", "lantern ocean distant kingdom", "{}")
        )

        val atlas = buildMemoryAtlas(books, highlights, emptyList())

        assertTrue(atlas.edges.isEmpty())
        assertEquals(2, atlas.isolatedCount)
    }

    @Test
    fun `three repeated preserved terms can form a factual word-pattern link`() {
        val books = listOf(
            Book(id = "a", title = "A", author = "One"),
            Book(id = "b", title = "B", author = "Two")
        )
        val highlights = listOf(
            Highlight("ha", "a", "lantern threshold archive chamber cipher", "{}"),
            Highlight("hb", "b", "archive threshold lantern winter memory", "{}")
        )

        val edge = buildMemoryAtlas(books, highlights, emptyList()).edges.single()

        assertEquals(setOf(MemoryRelationKind.PASSAGE_PATTERN), edge.reasons)
        assertEquals(listOf("archive", "lantern", "threshold"), edge.sharedPassageTerms)
    }

    @Test
    fun `completed books keep highlight and session engagement bonuses`() {
        val book = Book(
            id = "a",
            title = "A",
            author = "One",
            progress = 1f,
            finished = true
        )
        val base = buildMemoryAtlas(listOf(book), emptyList(), emptyList())
            .nodes.single().engagementScore
        val highlights = listOf(
            Highlight("h1", "a", "first preserved passage", "{}"),
            Highlight("h2", "a", "second preserved passage", "{}")
        )
        val sessions = listOf(
            ReadingSessionSnapshot("s1", "a", 1, 2, 1000, 8, 0, 0),
            ReadingSessionSnapshot("s2", "a", 3, 4, 1000, 8, 0, 0),
            ReadingSessionSnapshot("s3", "a", 5, 6, 1000, 8, 0, 0)
        )

        val enriched = buildMemoryAtlas(listOf(book), highlights, sessions)
            .nodes.single().engagementScore

        assertEquals(3.5f, base, 0.0001f)
        assertEquals(4.48f, enriched, 0.0001f)
        assertTrue(enriched > base)
    }

    @Test
    fun `orphan sessions increase no visible book engagement`() {
        val book = Book(id = "a", title = "A", author = "One")
        val base = buildMemoryAtlas(listOf(book), emptyList(), emptyList())
        val withOrphan = buildMemoryAtlas(
            listOf(book),
            emptyList(),
            listOf(ReadingSessionSnapshot("s", "missing", 1, 2, 1000, 8, 0, 0))
        )

        assertEquals(base.nodes.single().engagementScore, withOrphan.nodes.single().engagementScore)
    }

    @Test
    fun `atlas node cap is deterministic`() {
        val books = (1..30).map { index ->
            Book(
                id = "b$index",
                title = "Book $index",
                author = "Author $index",
                progress = index / 30f
            )
        }

        val atlas = buildMemoryAtlas(books, emptyList(), emptyList(), maxNodes = 24)

        assertEquals(24, atlas.nodes.size)
        assertFalse(atlas.nodes.any { it.book.id == "b1" })
        assertEquals("b30", atlas.nodes.first().book.id)
    }
}
