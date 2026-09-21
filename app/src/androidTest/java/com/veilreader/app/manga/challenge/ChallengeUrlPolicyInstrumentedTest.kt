package com.veilreader.app.manga.challenge

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChallengeUrlPolicyInstrumentedTest {

    @Test
    fun sourceHostAndSubdomainAreAllowed() {
        assertTrue(
            ChallengeUrlPolicy.isAllowedTopLevelNavigation(
                "example.com",
                "https://example.com/challenge"
            )
        )
        assertTrue(
            ChallengeUrlPolicy.isAllowedTopLevelNavigation(
                "example.com",
                "https://verify.example.com/challenge"
            )
        )
    }

    @Test
    fun externalCredentialsAndCustomSchemesAreBlocked() {
        assertFalse(
            ChallengeUrlPolicy.isAllowedTopLevelNavigation(
                "example.com",
                "https://evil.example.org/"
            )
        )
        assertFalse(
            ChallengeUrlPolicy.isAllowedTopLevelNavigation(
                "example.com",
                "https://user:pass@example.com/"
            )
        )
        assertFalse(
            ChallengeUrlPolicy.isAllowedTopLevelNavigation(
                "example.com",
                "file:///data/local.html"
            )
        )
        assertFalse(
            ChallengeUrlPolicy.isAllowedTopLevelNavigation(
                "example.com",
                "http://example.com/insecure"
            )
        )
    }
}