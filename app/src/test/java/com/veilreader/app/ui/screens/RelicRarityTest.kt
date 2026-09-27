package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class RelicRarityTest {
    @Test
    fun `relic rarity escalates with long-term reading significance`() {
        assertEquals(RelicRarity.FOUNDATION, relicRarityFor("ember_bookmark"))
        assertEquals(RelicRarity.RESONANT, relicRarityFor("moonlit_lens"))
        assertEquals(RelicRarity.RESONANT, relicRarityFor("brass_quill"))
        assertEquals(RelicRarity.RESONANT, relicRarityFor("ivory_bookplate"))
        assertEquals(RelicRarity.ASCENDANT, relicRarityFor("astral_key"))
        assertEquals(RelicRarity.SOVEREIGN, relicRarityFor("veil_crown"))
    }

    @Test
    fun `unknown relic ids stay at foundation rarity`() {
        assertEquals(RelicRarity.FOUNDATION, relicRarityFor("unknown"))
    }
}
