package com.veilreader.app.ui.screens

import java.util.Locale

/** Keep an unavailable publication language visible instead of implying another language works. */
internal fun readerTtsPickerLanguage(publicationLanguage: String?, languages: List<String>): String? {
    val requested = publicationLanguage?.let(Locale::forLanguageTag)
        ?.takeUnless { it.language.isBlank() || it.language == "und" }
        ?: return languages.firstOrNull()
    return languages.firstOrNull { it.equals(requested.toLanguageTag(), ignoreCase = true) }
        ?: requested.toLanguageTag()
}
