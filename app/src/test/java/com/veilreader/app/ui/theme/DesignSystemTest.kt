package com.veilreader.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {
    @Test
    fun `Arabic script detection protects connected letterforms from Latin tracking`() {
        assertTrue(usesArabicScript("کتابخانه خاکستری"))
        assertTrue(usesArabicScript("الأرشيف"))
        assertEquals(false, usesArabicScript("GRAYFOG ARCHIVE"))
    }

    @Test
    fun `Persian Arabic family languages use shaping-safe typography`() {
        listOf("fa", "ar", "ur", "ps", "ckb").forEach { language ->
            assertEquals(VeilScriptGroup.PERSIAN_ARABIC, veilScriptGroupFor(language))
        }
    }

    @Test
    fun `Latin and unknown languages keep the editorial Latin scale`() {
        listOf("en", "fr", "de", "tr", "ja", "").forEach { language ->
            assertEquals(VeilScriptGroup.LATIN, veilScriptGroupFor(language))
        }
    }

    @Test
    fun `Persian body metrics preserve more vertical breathing room`() {
        assertTrue(
            VeilPersianTypography.bodyLarge.lineHeight >
                VeilLatinTypography.bodyLarge.lineHeight
        )
        assertEquals(0f, VeilPersianTypography.labelSmall.letterSpacing.value, 0f)
    }

    @Test
    fun `Arena is the single palette source for the legacy shell aliases`() {
        assertEquals(ArenaPalette.Void, VeilPalette.Ink)
        assertEquals(ArenaPalette.Archive, VeilPalette.Archive)
        assertEquals(ArenaPalette.AntiqueGold, VeilPalette.Brass)
        assertEquals(ArenaPalette.Parchment, VeilPalette.ReaderPaper)
        assertEquals(ArenaPalette.Oxblood, VeilPalette.MoonCrimson)
    }

    @Test
    fun `Sanctuary remains quieter than every world-facing realm`() {
        val sanctuary = arenaDensityFor(VeilRealm.SANCTUARY)
        val worldRealms = listOf(
            VeilRealm.THRESHOLD,
            VeilRealm.ARCHIVE,
            VeilRealm.CASTLE,
            VeilRealm.WORLD,
            VeilRealm.RITUAL,
            VeilRealm.SANCTUM
        )

        worldRealms.forEach { realm ->
            val density = arenaDensityFor(realm)
            assertTrue(sanctuary.ornament < density.ornament)
            assertTrue(sanctuary.atmosphere < density.atmosphere)
            assertTrue(sanctuary.motion < density.motion)
        }
    }

    @Test
    fun `Threshold can carry authored world art without reaching ritual density`() {
        val threshold = arenaDensityFor(VeilRealm.THRESHOLD)
        val ritual = arenaDensityFor(VeilRealm.RITUAL)

        assertTrue(threshold.authoredImage > 0.80f)
        assertTrue(threshold.ornament < ritual.ornament)
        assertTrue(threshold.motion < ritual.motion)
    }
}
