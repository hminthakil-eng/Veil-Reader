package com.veilreader.app.ui.theme

import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

    @Test
    fun `local tracking override stays zero for Arabic script text`() {
        assertEquals(
            0.sp,
            veilTrackingFor(
                text = "کتابخانه خاکستری",
                latinTracking = 1.45.sp,
                scriptGroup = VeilScriptGroup.LATIN
            )
        )
    }

    @Test
    fun `Persian locale suppresses Latin display tracking too`() {
        assertEquals(
            0.sp,
            veilTrackingFor(
                text = "GRAYFOG ARCHIVE",
                latinTracking = 1.45.sp,
                scriptGroup = VeilScriptGroup.PERSIAN_ARABIC
            )
        )
    }

    @Test
    fun `Latin locale keeps deliberate Latin tracking`() {
        assertEquals(
            1.45.sp,
            veilTrackingFor(
                text = "GRAYFOG ARCHIVE",
                latinTracking = 1.45.sp,
                scriptGroup = VeilScriptGroup.LATIN
            )
        )
    }

    @Test
    fun `archival label floor is readable in both scripts`() {
        listOf(VeilLatinTypography, VeilPersianTypography).forEach { typography ->
            assertTrue(typography.labelSmall.fontSize.value >= 11f)
            assertTrue(typography.labelMedium.fontSize.value >= 12f)
            assertTrue(typography.labelSmall.lineHeight.value > typography.labelSmall.fontSize.value)
        }
    }

    @Test
    fun `all Persian shell roles preserve connected script tracking`() {
        with(VeilPersianTypography) {
            listOf(displayLarge, headlineLarge, headlineMedium, headlineSmall, titleLarge,
                titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, labelLarge,
                labelMedium, labelSmall).forEach { style ->
                assertEquals(0.sp, style.letterSpacing)
            }
        }
    }

    @Test
    fun `semantic text colors clear normal text contrast on archive material`() {
        fun contrast(foreground: Color, background: Color): Float {
            val a = foreground.luminance()
            val b = background.luminance()
            return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
        }
        listOf(VeilMaterials.TextPrimary, VeilMaterials.TextSecondary, VeilMaterials.TextMuted,
            VeilMaterials.Brass).forEach { ink ->
            listOf(VeilMaterials.RealmBackground, VeilMaterials.Surface,
                VeilMaterials.ElevatedSurface).forEach { paper ->
                assertTrue("Semantic text contrast must be at least 4.5:1", contrast(ink, paper) >= 4.5f)
            }
        }
        assertTrue(contrast(VeilMaterials.Ink, VeilMaterials.Parchment) >= 4.5f)
    }
}
