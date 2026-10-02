package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceProfilesTest {
    @Test fun bookOverrideWinsWithoutMutatingGlobalDefaults() {
        val defaults = mapOf("theme" to "PAPER", "font" to "publisher")
        val selected = AppearanceProfile("night", "Night", values = mapOf("theme" to "OLED"))
        val book = AppearanceProfile(
            "book-1", "Book override", scope = AppearanceProfileScope.BOOK, bookId = "b1",
            values = mapOf("font" to "serif")
        )

        val resolved = AppearanceProfileResolver.resolve(defaults, selected, book)

        assertEquals("OLED", resolved["theme"])
        assertEquals("serif", resolved["font"])
        assertEquals("PAPER", defaults["theme"])
    }
}
