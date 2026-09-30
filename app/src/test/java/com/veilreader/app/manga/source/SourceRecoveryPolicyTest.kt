package com.veilreader.app.manga.source

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceRecoveryPolicyTest {

    private val policy = SourceRecoveryPolicy()

    @Test
    fun networkFailurePrefersAlternateDomain_beforeRetryingSameSource() {
        val failure = SourceFailure(SourceFailureKind.NETWORK, "offline or host unreachable")

        assertEquals(
            SourceRecoveryAction.TRY_ALTERNATE_DOMAIN,
            policy.decide(failure, hasAlternateDomain = true, hasAlternativeSource = true)
        )
        assertEquals(
            SourceRecoveryAction.RETRY_SAME_SOURCE,
            policy.decide(failure, hasAlternateDomain = false, hasAlternativeSource = true)
        )
    }

    @Test
    fun parserBreakageFallsBackToAlternativeSource() {
        val failure = SourceFailure(SourceFailureKind.PARSE_CHANGED, "site structure changed")

        assertEquals(
            SourceRecoveryAction.TRY_ALTERNATIVE_SOURCE,
            policy.decide(failure, hasAlternateDomain = true, hasAlternativeSource = true)
        )
        assertEquals(
            SourceRecoveryAction.STOP,
            policy.decide(failure, hasAlternateDomain = true, hasAlternativeSource = false)
        )
    }

    @Test
    fun challengeAndRateLimitHaveExplicitNonLoopingActions() {
        assertEquals(
            SourceRecoveryAction.REQUIRE_BROWSER_CHALLENGE,
            policy.decide(
                SourceFailure(SourceFailureKind.CHALLENGE_REQUIRED, "verification required"),
                hasAlternateDomain = false,
                hasAlternativeSource = true
            )
        )
        assertEquals(
            SourceRecoveryAction.WAIT_AND_RETRY,
            policy.decide(
                SourceFailure(SourceFailureKind.RATE_LIMITED, "slow down", retryAfterMillis = 5_000),
                hasAlternateDomain = true,
                hasAlternativeSource = true
            )
        )
    }
}
