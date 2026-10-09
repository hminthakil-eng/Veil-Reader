package com.veilreader.app.ui.screens

import org.junit.Assert.*
import org.junit.Test

class ReaderTtsVoicePresentationTest {
    @Test fun exactDialectIsPreserved() {
        assertEquals("en-US", readerTtsPickerLanguage("en-US", listOf("en-GB", "en-US")))
    }
    @Test fun missingExactDialectKeepsThePublicationRequestForPlaybackRanking() {
        assertEquals("en-AU", readerTtsPickerLanguage("en-AU", listOf("en-GB", "en-US")))
    }
    @Test fun missingPublicationVoiceDoesNotPretendAnotherLanguageWorks() {
        assertEquals("fa-IR", readerTtsPickerLanguage("fa-IR", listOf("en-US")))
        assertEquals("fa-IR", readerTtsPickerLanguage("fa-IR", emptyList()))
    }
    @Test fun explicitScriptCannotBeSubstitutedByOppositeScript() {
        assertEquals("zh-Hant-TW", readerTtsPickerLanguage("zh-Hant-TW", listOf("zh-Hans-CN")))
    }
    @Test fun unknownLanguageAllowsExplicitCatalogBrowsing() {
        assertEquals("en-US", readerTtsPickerLanguage("und", listOf("en-US")))
        assertNull(readerTtsPickerLanguage(null, emptyList()))
    }
}
