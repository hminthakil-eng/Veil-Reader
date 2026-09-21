package com.veilreader.app.manga.challenge

import com.veilreader.app.manga.health.SourceExecutionHealthObserver
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import kotlinx.coroutines.CoroutineScope

object MangaChallengeRuntime {
    val hostRegistry = ChallengeForegroundHostRegistry()
    val sessions = AndroidChallengeSessionStore()
    val sessionHeaders = ChallengeSessionHeadersStore()

    val uiDriver: ChallengeUiDriver = AndroidChallengeUiDriver(
        hostRegistry = hostRegistry,
        sessions = sessions,
        sessionHeaders = sessionHeaders
    )

    fun createChallengeCoordinator(
        sessionScope: CoroutineScope
    ): BrowserChallengeCoordinator = BrowserChallengeCoordinator(
        uiDriver = uiDriver,
        launchPolicy = hostRegistry,
        cooldowns = ChallengeCooldownRegistry(),
        sessionScope = sessionScope
    )

    fun createSourceExecutionCoordinator(
        sessionScope: CoroutineScope,
        healthObserver: SourceExecutionHealthObserver? = null
    ): SourceExecutionCoordinator {
        val challenge = createChallengeCoordinator(sessionScope)
        return SourceExecutionCoordinator(
            challengeAdapter = challenge,
            sessionHeadersProvider = sessionHeaders,
            healthObserver = healthObserver
        )
    }
}