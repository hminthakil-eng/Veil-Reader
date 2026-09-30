package com.veilreader.app.ui.theme

import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.PerformanceTier
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
    fun `Arabic-script content neutralizes Latin tracking even in Latin shell`() {
        val latinTracked = VeilLatinTypography.titleLarge.copy(letterSpacing = (-0.4).sp)
        val protected = veilContentTextStyle(latinTracked, "کتاب اسرار")
        val untouched = veilContentTextStyle(latinTracked, "Lord of Mysteries")

        assertEquals(0f, protected.letterSpacing.value, 0f)
        assertEquals(latinTracked.letterSpacing, untouched.letterSpacing)
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
    fun `performance tiers scale decoration monotonically without disabling the visual system`() {
        val full = qualityPolicyFor(PerformanceTier.FULL)
        val balanced = qualityPolicyFor(PerformanceTier.BALANCED)
        val essential = qualityPolicyFor(PerformanceTier.ESSENTIAL)

        assertTrue(full.atmosphereMultiplier > balanced.atmosphereMultiplier)
        assertTrue(balanced.atmosphereMultiplier > essential.atmosphereMultiplier)
        assertTrue(full.geometryMultiplier > balanced.geometryMultiplier)
        assertTrue(balanced.geometryMultiplier > essential.geometryMultiplier)
        assertTrue(full.particleMultiplier > balanced.particleMultiplier)
        assertTrue(balanced.particleMultiplier > essential.particleMultiplier)

        assertTrue(essential.atmosphereMultiplier > 0f)
        assertTrue(essential.ornamentMultiplier > 0f)
        assertTrue(essential.geometryMultiplier > 0f)
    }

    @Test
    fun `decorative counts never exceed source complexity and preserve a minimum when requested`() {
        assertEquals(14, scaledDecorativeCount(14, 1f, minimumWhenPresent = 2))
        assertEquals(8, scaledDecorativeCount(14, 0.58f, minimumWhenPresent = 2))
        assertEquals(3, scaledDecorativeCount(14, 0.18f, minimumWhenPresent = 2))
        assertEquals(0, scaledDecorativeCount(0, 1f, minimumWhenPresent = 2))
    }
}
