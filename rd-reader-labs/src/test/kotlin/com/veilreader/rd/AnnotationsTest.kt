package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnnotationsTest {
    private val pdf = AnnotationCapabilities(
        supported = setOf(AnnotationKind.HIGHLIGHT, AnnotationKind.NOTE, AnnotationKind.INK),
        supportsTextSelection = true,
        supportsExport = true
    )

    @Test fun textHighlightRequiresQuoteAndCapability() {
        assertTrue(AnnotationDraft("p", "loc", AnnotationKind.HIGHLIGHT, quote = "text").isValid(pdf))
        assertFalse(AnnotationDraft("p", "loc", AnnotationKind.HIGHLIGHT).isValid(pdf))
        assertFalse(AnnotationDraft("p", "loc", AnnotationKind.RECTANGLE).isValid(pdf))
    }
}
