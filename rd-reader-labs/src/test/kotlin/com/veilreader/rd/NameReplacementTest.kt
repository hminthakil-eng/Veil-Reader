package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals

class NameReplacementTest {
    @Test fun replacementUsesWordBoundaries() {
        val result = NameReplacementEngine.apply(
            "Rand met random travelers. RAND smiled.",
            listOf(NameReplacementRule("Rand", "Arin"))
        )
        assertEquals("Arin met random travelers. Arin smiled.", result)
    }
}
