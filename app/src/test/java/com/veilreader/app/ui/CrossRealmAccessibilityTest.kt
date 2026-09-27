package com.veilreader.app.ui

import com.veilreader.app.ui.screens.backLabelFor
import com.veilreader.app.ui.theme.VeilScriptGroup
import com.veilreader.app.ui.theme.veilScriptGroupFor
import org.junit.Assert.assertEquals
import org.junit.Test

class CrossRealmAccessibilityTest {
    @Test
    fun `back affordance mirrors without changing its destination`() {
        assertEquals("‹ Archive", backLabelFor("Archive", rtl = false))
        assertEquals("Archive ›", backLabelFor("Archive", rtl = true))
    }

    @Test
    fun `tab movement mirrors between ltr and rtl`() {
        assertEquals(1, tabSlideDirection(0, 2, rtl = false))
        assertEquals(-1, tabSlideDirection(0, 2, rtl = true))
        assertEquals(-1, tabSlideDirection(3, 1, rtl = false))
        assertEquals(1, tabSlideDirection(3, 1, rtl = true))
    }

    @Test
    fun `persian and neighboring arabic-script locales use rtl typography group`() {
        listOf("fa", "ar", "ur", "ps", "ckb").forEach { language ->
            assertEquals(
                VeilScriptGroup.PERSIAN_ARABIC,
                veilScriptGroupFor(language)
            )
        }
        assertEquals(VeilScriptGroup.LATIN, veilScriptGroupFor("en"))
    }
}
