package com.veilreader.app.manga.library

import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalMangaTest {

    @Test
    fun sourceReplacementNeverChangesCanonicalIdentity() {
        val oldRef = SourceMangaRef(SourceId("old.source"), "old-key")
        val replacement = SourceMangaRef(SourceId("new.source"), "new-key")
        val factory = CanonicalMangaFactory(
            idGenerator = { "canonical-1" },
            clock = { 123L }
        )

        val original = factory.create(
            title = "Veil",
            initialSource = oldRef,
            alternativeTitles = setOf(" The Veil ")
        )
        val migrated = original.linkSource(replacement).unlinkSource(oldRef.sourceId)

        assertEquals(CanonicalMangaId("canonical-1"), migrated.id)
        assertEquals(replacement, migrated.sourceRef(replacement.sourceId))
        assertNull(migrated.sourceRef(oldRef.sourceId))
        assertEquals(setOf("The Veil"), migrated.alternativeTitles)
        assertEquals(123L, migrated.createdAtEpochMs)
    }

    @Test
    fun relinkingSameSourceReplacesOnlyThatProviderKey() {
        val source = SourceId("same.source")
        val first = SourceMangaRef(source, "v1")
        val second = SourceMangaRef(source, "v2")
        val manga = CanonicalMangaFactory(idGenerator = { "stable" }).create("Title", first)

        val updated = manga.linkSource(second)

        assertEquals(manga.id, updated.id)
        assertEquals(1, updated.sourceRefs.size)
        assertEquals(second, updated.sourceRef(source))
    }
}
