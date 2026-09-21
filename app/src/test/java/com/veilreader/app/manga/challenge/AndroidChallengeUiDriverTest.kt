package com.veilreader.app.manga.challenge

import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidChallengeUiDriverTest {

    @Test
    fun solvedHostResultIsStoredForSourceDomain() = runBlocking {
        val registry = ChallengeForegroundHostRegistry()
        val registration = registry.register(Any())
        val sessions = AndroidChallengeSessionStore()
        val headers = ChallengeSessionHeadersStore()
        val driver = AndroidChallengeUiDriver(
            hostRegistry = registry,
            sessions = sessions,
            sessionHeaders = headers,
            hostLossGraceMillis = 100L
        )

        try {
            val result = async { driver.solve(request()) }
            val active = withTimeout(1_000L) { sessions.session.filterNotNull().first() }
            sessions.complete(
                active.id,
                AndroidChallengeBrowserResult(
                    uiResult = ChallengeUiResult.SOLVED,
                    cookieHeader = "cf_clearance=ok",
                    userAgent = "challenge-ua"
                )
            )

            assertEquals(ChallengeUiResult.SOLVED, result.await())
            val saved = headers.headersFor(active.request.source.id, active.request.domain)
            assertEquals("cf_clearance=ok", saved["Cookie"])
            assertEquals("challenge-ua", saved["User-Agent"])
        } finally {
            registry.unregister(registration)
        }
    }

    @Test
    fun hostLossBeyondGraceCancelsSession() = runBlocking {
        val registry = ChallengeForegroundHostRegistry()
        val registration = registry.register(Any())
        val sessions = AndroidChallengeSessionStore()
        val headers = ChallengeSessionHeadersStore()
        val driver = AndroidChallengeUiDriver(
            hostRegistry = registry,
            sessions = sessions,
            sessionHeaders = headers,
            hostLossGraceMillis = 20L
        )

        val result = async { driver.solve(request()) }
        withTimeout(1_000L) { sessions.session.filterNotNull().first() }
        registry.unregister(registration)

        assertEquals(
            ChallengeUiResult.CANCELLED,
            withTimeout(1_000L) { result.await() }
        )
        assertFalse(registry.canLaunchInteractiveChallenge())
        assertEquals(null, sessions.activeId())
    }

    @Test
    fun noForegroundHostFailsClosedWithoutOpeningSession() = runBlocking {
        val registry = ChallengeForegroundHostRegistry()
        val sessions = AndroidChallengeSessionStore()
        val driver = AndroidChallengeUiDriver(
            hostRegistry = registry,
            sessions = sessions,
            sessionHeaders = ChallengeSessionHeadersStore(),
            hostLossGraceMillis = 20L
        )

        assertEquals(ChallengeUiResult.CANCELLED, driver.solve(request()))
        assertEquals(null, sessions.activeId())
        assertTrue(sessions.session.value == null)
    }

    private fun request() = ChallengeRequest(
        source = MangaSourceDescriptor(
            id = SourceId("android.challenge"),
            displayName = "Android Challenge",
            domains = listOf("challenge.example")
        ),
        domain = "challenge.example",
        failureKind = SourceFailureKind.CHALLENGE_REQUIRED
    )
}