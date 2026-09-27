package com.veilreader.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeArtDirectionTest {
    @Test
    fun `Reader sanctuary has no narrative ornament grammar`() {
        val sanctuary = narrativeGrammarFor(VeilRealm.SANCTUARY)

        assertEquals(0f, sanctuary.passageDepth, 0.0001f)
        assertEquals(0, sanctuary.lampCount)
        assertEquals(0, sanctuary.verticalPierCount)
        assertEquals(0, sanctuary.archiveRailCount)
        assertEquals(0, sanctuary.mechanicalTickCount)
        assertEquals(0, sanctuary.fractureCount)
        assertEquals(0, sanctuary.bellMarkCount)
    }

    @Test
    fun `Archive owns catalog rails and more fracture memory than Threshold`() {
        val threshold = narrativeGrammarFor(VeilRealm.THRESHOLD)
        val archive = narrativeGrammarFor(VeilRealm.ARCHIVE)

        assertEquals(0, threshold.archiveRailCount)
        assertTrue(archive.archiveRailCount > 0)
        assertTrue(archive.fractureCount > threshold.fractureCount)
    }

    @Test
    fun `Castle is more vertically architectural than Archive`() {
        val archive = narrativeGrammarFor(VeilRealm.ARCHIVE)
        val castle = narrativeGrammarFor(VeilRealm.CASTLE)

        assertTrue(castle.verticalPierCount > archive.verticalPierCount)
        assertTrue(castle.bellMarkCount > archive.bellMarkCount)
        assertTrue(castle.mechanicalTickCount > archive.mechanicalTickCount)
    }

    @Test
    fun `Threshold passage field remains subordinate to dedicated hero architecture`() {
        val threshold = narrativeGrammarFor(VeilRealm.THRESHOLD)

        assertTrue(threshold.passageDepth in 0f..0.35f)
        assertTrue(threshold.lampCount > 0)
        assertTrue(threshold.mechanicalTickCount > 0)
    }

    @Test
    fun `all grammar channels remain bounded to restrained counts`() {
        VeilRealm.values().forEach { realm ->
            val grammar = narrativeGrammarFor(realm)
            assertTrue(grammar.passageDepth in 0f..1f)
            assertTrue(grammar.lampCount in 0..4)
            assertTrue(grammar.verticalPierCount in 0..6)
            assertTrue(grammar.archiveRailCount in 0..5)
            assertTrue(grammar.mechanicalTickCount in 0..12)
            assertTrue(grammar.fractureCount in 0..4)
            assertTrue(grammar.bellMarkCount in 0..4)
        }
    }
}
