package com.veilreader.app.manga.challenge

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AndroidChallengeUiDriver(
    private val hostRegistry: ChallengeForegroundHostRegistry,
    private val sessions: AndroidChallengeSessionStore,
    private val sessionHeaders: ChallengeSessionHeadersStore,
    private val hostLossGraceMillis: Long = 10_000L
) : ChallengeUiDriver {

    init { require(hostLossGraceMillis > 0) }

    override suspend fun solve(request: ChallengeRequest): ChallengeUiResult = coroutineScope {
        if (!hostRegistry.canLaunchInteractiveChallenge()) {
            return@coroutineScope ChallengeUiResult.CANCELLED
        }

        val key = ChallengeKey(request.source.id, request.domain)
        sessionHeaders.clear(key)
        val session = sessions.begin(
            request = request,
            startUrl = ChallengeUrlPolicy.startUrl(request.domain)
        )

        val hostMonitor = launch {
            hostRegistry.foreground.collectLatest { foreground ->
                if (!foreground) {
                    delay(hostLossGraceMillis)
                    if (!hostRegistry.canLaunchInteractiveChallenge()) {
                        sessions.complete(
                            session.id,
                            AndroidChallengeBrowserResult(ChallengeUiResult.CANCELLED)
                        )
                    }
                }
            }
        }

        try {
            val result = sessions.await(session.id)
            if (result.uiResult == ChallengeUiResult.SOLVED) {
                sessionHeaders.put(
                    key = key,
                    cookieHeader = result.cookieHeader,
                    userAgent = result.userAgent
                )
            }
            result.uiResult
        } catch (cancelled: CancellationException) {
            sessions.complete(
                session.id,
                AndroidChallengeBrowserResult(ChallengeUiResult.CANCELLED)
            )
            throw cancelled
        } finally {
            hostMonitor.cancel()
        }
    }
}