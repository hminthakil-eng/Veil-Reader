package com.veilreader.app.ui

/**
 * Localized user-facing notice.
 *
 * Technical exception text is intentionally kept out of the user surface. The resource id and
 * structured format arguments keep language, tone and future actions centralized.
 */
internal enum class VeilNoticeTone {
    INFO,
    ERROR
}

internal data class VeilNotice(
    val messageRes: Int,
    val formatArgs: List<Any> = emptyList(),
    val tone: VeilNoticeTone = VeilNoticeTone.ERROR
)
