package com.veilreader.rd

enum class PublicationKind { EPUB, PDF, COMIC }
enum class AnnotationKind { HIGHLIGHT, UNDERLINE, STRIKE, NOTE, INK, RECTANGLE, FREE_TEXT }

data class AnnotationCapabilities(
    val supported: Set<AnnotationKind>,
    val supportsTextSelection: Boolean,
    val supportsExport: Boolean
) {
    fun supports(kind: AnnotationKind): Boolean = kind in supported
}

data class AnnotationDraft(
    val publicationId: String,
    val locator: String,
    val kind: AnnotationKind,
    val quote: String? = null,
    val note: String? = null,
    val colorToken: String? = null
) {
    fun isValid(capabilities: AnnotationCapabilities): Boolean =
        publicationId.isNotBlank() &&
            locator.isNotBlank() &&
            capabilities.supports(kind) &&
            when (kind) {
                AnnotationKind.HIGHLIGHT,
                AnnotationKind.UNDERLINE,
                AnnotationKind.STRIKE -> !quote.isNullOrBlank()
                AnnotationKind.NOTE -> !note.isNullOrBlank()
                else -> true
            }
}
