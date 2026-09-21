package com.veilreader.app.manga.challenge

import android.os.Bundle
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AndroidChallengeBrowserSession(
    val id: String,
    val request: ChallengeRequest,
    val startUrl: String,
    val createdAtEpochMs: Long,
    val restoredWebViewState: Bundle? = null
) {
    override fun toString(): String =
        "AndroidChallengeBrowserSession(id=" + id +
            ", source=" + request.source.id +
            ", domain=" + request.domain +
            ", startUrl=<redacted>)"
}

data class AndroidChallengeBrowserResult(
    val uiResult: ChallengeUiResult,
    val cookieHeader: String? = null,
    val userAgent: String? = null
) {
    override fun toString(): String =
        "AndroidChallengeBrowserResult(uiResult=" + uiResult +
            ", cookieHeader=<redacted>, userAgent=<redacted>)"
}

class AndroidChallengeSessionStore(
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Active(
        var session: AndroidChallengeBrowserSession,
        val result: CompletableDeferred<AndroidChallengeBrowserResult>
    )

    private var active: Active? = null
    private val _session = MutableStateFlow<AndroidChallengeBrowserSession?>(null)
    val session: StateFlow<AndroidChallengeBrowserSession?> = _session.asStateFlow()

    @Synchronized
    fun begin(request: ChallengeRequest, startUrl: String): AndroidChallengeBrowserSession {
        check(active == null) { "Only one Android challenge session may be active" }
        val session = AndroidChallengeBrowserSession(
            id = UUID.randomUUID().toString(),
            request = request,
            startUrl = startUrl,
            createdAtEpochMs = clock()
        )
        active = Active(session, CompletableDeferred())
        _session.value = session
        return session
    }

    suspend fun await(id: String): AndroidChallengeBrowserResult {
        val deferred = synchronized(this) {
            active?.takeIf { it.session.id == id }?.result
        } ?: return AndroidChallengeBrowserResult(ChallengeUiResult.CANCELLED)
        return deferred.await()
    }

    @Synchronized
    fun saveWebViewState(id: String, state: Bundle?) {
        val current = active ?: return
        if (current.session.id != id) return
        current.session = current.session.copy(
            restoredWebViewState = state?.let(::Bundle)
        )
        _session.value = current.session
    }

    @Synchronized
    fun complete(id: String, result: AndroidChallengeBrowserResult): Boolean {
        val current = active ?: return false
        if (current.session.id != id) return false
        active = null
        _session.value = null
        current.result.complete(result)
        return true
    }

    @Synchronized
    fun cancelActive(): Boolean {
        val current = active ?: return false
        active = null
        _session.value = null
        current.result.complete(AndroidChallengeBrowserResult(ChallengeUiResult.CANCELLED))
        return true
    }

    @Synchronized
    fun activeId(): String? = active?.session?.id
}