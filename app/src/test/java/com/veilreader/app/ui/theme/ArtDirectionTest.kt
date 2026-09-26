package com.veilreader.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class ArtDirectionTest {
    @Test
    fun `Reader sanctuary stays the quietest realm`() {
        val sanctuary = visualBudgetFor(VeilRealm.SANCTUARY)

        VeilRealm.entries
            .filter { it != VeilRealm.SANCTUARY }
            .map(::visualBudgetFor)
            .forEach { other ->
                assertTrue(sanctuary.richness < other.richness)
                assertTrue(sanctuary.ornament <= other.ornament)
                assertTrue(sanctuary.atmosphere < other.atmosphere)
            }
    }

    @Test
    fun `Ritual remains the richest ceremonial realm`() {
        val ritual = visualBudgetFor(VeilRealm.RITUAL)
        val world = visualBudgetFor(VeilRealm.WORLD)
        val archive = visualBudgetFor(VeilRealm.ARCHIVE)

        assertTrue(ritual.richness > world.richness)
        assertTrue(ritual.ornament > world.ornament)
        assertTrue(world.richness > archive.richness)
        assertTrue(ritual.motion > world.motion)
    }
}
