package com.veilreader.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityIntentPolicyTest {

    @Test
    fun `external publication boundary accepts only local readable schemes`() {
        assertTrue(isSupportedExternalPublicationScheme("content"))
        assertTrue(isSupportedExternalPublicationScheme("CONTENT"))
        assertTrue(isSupportedExternalPublicationScheme("file"))

        assertFalse(isSupportedExternalPublicationScheme(null))
        assertFalse(isSupportedExternalPublicationScheme(""))
        assertFalse(isSupportedExternalPublicationScheme("http"))
        assertFalse(isSupportedExternalPublicationScheme("https"))
        assertFalse(isSupportedExternalPublicationScheme("javascript"))
        assertFalse(isSupportedExternalPublicationScheme("veil"))
    }
}
