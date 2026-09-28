package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathWorldSignatureTest {
    @Test
    fun `canonical paths project distinct motifs`() {
        val ids = listOf("oracle","dreamwalker","archivist","vanguard","nocturne","artificer")
        val motifs = ids.map {
            derivePathWorldSignature(it, 2, 6, 0.5f).motif
        }
        assertEquals(6, motifs.toSet().size)
    }

    @Test
    fun `rank and ritual deepen one stable path identity`() {
        val low = derivePathWorldSignature("nocturne", 0, 6, 0f)
        val high = derivePathWorldSignature("nocturne", 5, 6, 1f)
        assertEquals(low.motif, high.motif)
        assertTrue(high.strength > low.strength)
        assertTrue(high.ornamentCount > low.ornamentCount)
    }

    @Test
    fun `unknown paths fall back safely`() {
        val value = derivePathWorldSignature("future-path", 99, 0, 9f)
        assertEquals(PathArchitecturalMotif.CIPHER, value.motif)
        assertTrue(value.strength in 0f..1f)
        assertTrue(value.ornamentCount in 2..9)
    }
}
