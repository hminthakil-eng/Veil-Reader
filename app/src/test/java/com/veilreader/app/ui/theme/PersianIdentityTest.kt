package com.veilreader.app.ui.theme

import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianIdentityTest {
    @Test
    fun `Persian Arabic locale family selection is explicit`() {
        listOf("fa", "ar", "ur", "ps", "ckb").forEach { language ->
            assertEquals(VeilScriptGroup.PERSIAN_ARABIC, veilScriptGroupFor(language))
        }
        assertEquals(VeilScriptGroup.LATIN, veilScriptGroupFor("en"))
    }

    @Test
    fun `Persian Urdu Pashto and Sorani use eastern Arabic numerals`() {
        listOf("fa", "ur", "ps", "ckb").forEach { language ->
            assertEquals(VeilDigitSet.EASTERN_ARABIC, digitSetForLanguage(language))
        }
        assertEquals(VeilDigitSet.ARABIC_INDIC, digitSetForLanguage("ar"))
        assertEquals(VeilDigitSet.LATIN, digitSetForLanguage("en"))
    }

    @Test
    fun `app metrics localize digits decimal grouping and percent without rewriting prose punctuation`() {
        assertEquals("۱۲٬۳۴۵٫۶٪", localizeAppNumerals("12,345.6%", "fa"))
        assertEquals("١٢٬٣٤٥٫٦٪", localizeAppNumerals("12,345.6%", "ar"))
        assertEquals("version 12. build 3", localizeAppNumerals("version 12. build 3", "en"))
        assertEquals("نسخه ۱۲. build ۳", localizeAppNumerals("نسخه 12. build 3", "fa"))
    }

    @Test
    fun `bidi isolate wraps mixed metadata without changing the value`() {
        val isolated = bidiIsolate("The Castle 12")
        assertEquals(0x2068, isolated.first().code)
        assertEquals(0x2069, isolated.last().code)
        assertEquals("The Castle 12", isolated.substring(1, isolated.lastIndex))
        assertEquals("", bidiIsolate(""))
    }

    @Test
    fun `Arabic script ornament removes tracking and gives metadata more measure`() {
        val latin = scriptOrnamentPolicyFor(VeilScriptGroup.LATIN)
        val rtl = scriptOrnamentPolicyFor(VeilScriptGroup.PERSIAN_ARABIC)
        assertEquals(0f, rtl.eyebrowTrackingSp, 0.0001f)
        assertTrue(rtl.metadataLabelWidthDp > latin.metadataLabelWidthDp)
        assertTrue(rtl.headerRuleWidthDp > latin.headerRuleWidthDp)
        assertTrue(rtl.headerTerminalMarks > latin.headerTerminalMarks)
    }

    @Test
    fun `Persian typography keeps zero tracking and RTL content fallback`() {
        assertEquals(0.sp, VeilPersianTypography.bodyLarge.letterSpacing)
        assertEquals(TextDirection.ContentOrRtl, VeilPersianTypography.bodyLarge.textDirection)
        assertTrue(VeilPersianTypography.bodyLarge.lineHeight > VeilLatinTypography.bodyLarge.lineHeight)
        assertNotEquals(VeilLatinTypography.bodyLarge.fontFamily, VeilPersianTypography.bodyLarge.fontFamily)
    }

    @Test
    fun `publication Arabic script detection includes Persian presentation forms`() {
        assertTrue(usesArabicScript("کتاب"))
        assertTrue(usesArabicScript("العربية"))
        assertTrue(usesArabicScript("کوردی"))
    }

    @Test
    fun `Arabic script punctuation policy uses native signs while Latin remains unchanged`() {
        val fa = punctuationPolicyFor("fa")
        assertEquals('٫', fa.decimalSeparator)
        assertEquals('٬', fa.groupingSeparator)
        assertEquals('٪', fa.percentSign)
        assertEquals('؟', fa.questionMark)
        assertEquals("، ", fa.listSeparator)

        val en = punctuationPolicyFor("en")
        assertEquals('.', en.decimalSeparator)
        assertEquals(',', en.groupingSeparator)
        assertEquals('%', en.percentSign)
        assertEquals('?', en.questionMark)
        assertEquals(", ", en.listSeparator)
    }
}
