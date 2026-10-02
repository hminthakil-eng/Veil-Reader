package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyFormatsTest {
    @Test fun detectsRepresentativeMoonFormats() {
        assertEquals(LegacyFormat.MOBI, LegacyFormatDetector.fromFileName("book.mobi"))
        assertEquals(LegacyFormat.AZW3, LegacyFormatDetector.fromFileName("book.azw3"))
        assertEquals(LegacyFormat.FB2, LegacyFormatDetector.fromFileName("book.fb2.zip"))
        assertEquals(LegacyFormat.DJVU, LegacyFormatDetector.fromFileName("scan.djvu"))
        assertEquals(LegacyFormat.MHTML, LegacyFormatDetector.fromFileName("page.mhtml"))
    }
}
