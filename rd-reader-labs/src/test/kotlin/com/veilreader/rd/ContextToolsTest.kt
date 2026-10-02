package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals

class ContextToolsTest {
    @Test fun offlineModePrefersOfflineDictionary() {
        val providers = listOf(
            ContextToolProvider("web", ContextToolKind.DICTIONARY, requiresNetwork = true),
            ContextToolProvider("local", ContextToolKind.DICTIONARY, requiresNetwork = false)
        )
        assertEquals(
            "local",
            ContextToolRegistry.preferred(providers, ContextToolKind.DICTIONARY, "fa", offlineOnly = true)?.id
        )
    }
}
