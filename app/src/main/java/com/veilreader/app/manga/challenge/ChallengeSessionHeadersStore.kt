package com.veilreader.app.manga.challenge

import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceSessionHeadersProvider

data class ChallengeSessionHeaders(
    val cookieHeader: String?,
    val userAgent: String?,
    val capturedAtEpochMs: Long
) {
    override fun toString(): String =
        "ChallengeSessionHeaders(cookieHeader=<redacted>, userAgent=<redacted>, capturedAtEpochMs=" +
            capturedAtEpochMs + ")"
}

class ChallengeSessionHeadersStore(
    private val ttlMillis: Long = 30 * 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) : SourceSessionHeadersProvider {

    init {
        require(ttlMillis > 0) { "Challenge session header TTL must be positive" }
    }

    private val values = linkedMapOf<ChallengeKey, ChallengeSessionHeaders>()

    @Synchronized
    fun put(
        key: ChallengeKey,
        cookieHeader: String?,
        userAgent: String?
    ) {
        val cookie = sanitizeHeaderValue(cookieHeader, MAX_COOKIE_HEADER_LENGTH)
        val agent = sanitizeHeaderValue(userAgent, MAX_USER_AGENT_LENGTH)

        if (cookie == null && agent == null) {
            values.remove(key)
            return
        }

        values[key] = ChallengeSessionHeaders(
            cookieHeader = cookie,
            userAgent = agent,
            capturedAtEpochMs = clock()
        )
    }

    @Synchronized
    override fun headersFor(
        sourceId: SourceId,
        domain: String
    ): Map<String, String> {
        val key = ChallengeKey(sourceId, domain)
        val value = values[key] ?: return emptyMap()
        val age = (clock() - value.capturedAtEpochMs).coerceAtLeast(0L)
        if (age >= ttlMillis) {
            values.remove(key)
            return emptyMap()
        }

        return buildMap {
            value.cookieHeader?.let { put("Cookie", it) }
            value.userAgent?.let { put("User-Agent", it) }
        }
    }

    @Synchronized
    fun clear(key: ChallengeKey) { values.remove(key) }

    @Synchronized
    fun clearAll() { values.clear() }

    @Synchronized
    fun snapshot(key: ChallengeKey): ChallengeSessionHeaders? = values[key]

    private fun sanitizeHeaderValue(value: String?, maxLength: Int): String? {
        val clean = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (clean.length > maxLength) return null
        if (clean.contains('\r') || clean.contains('\n')) return null
        return clean
    }

    private companion object {
        const val MAX_COOKIE_HEADER_LENGTH = 32 * 1024
        const val MAX_USER_AGENT_LENGTH = 2 * 1024
    }
}