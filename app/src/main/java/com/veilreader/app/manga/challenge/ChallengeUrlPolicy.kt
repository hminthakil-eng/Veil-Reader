package com.veilreader.app.manga.challenge

import android.net.Uri
import java.net.IDN
import java.util.Locale

object ChallengeUrlPolicy {

    fun startUrl(domain: String): String {
        val host = normalizeHost(domain)
        return Uri.Builder()
            .scheme("https")
            .encodedAuthority(host)
            .path("/")
            .build()
            .toString()
    }

    fun isAllowedTopLevelNavigation(domain: String, rawUrl: String): Boolean {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme != "https") return false
        if (uri.userInfo != null) return false

        val requestedHost = uri.host?.let(::normalizeHost) ?: return false
        val sourceHost = normalizeHost(domain)
        return requestedHost == sourceHost || requestedHost.endsWith("." + sourceHost)
    }

    private fun normalizeHost(value: String): String {
        val raw = value.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .substringBefore(":")
            .trimEnd('.')
        require(raw.isNotBlank()) { "Challenge domain cannot be blank" }
        val ascii = IDN.toASCII(raw).lowercase(Locale.ROOT)
        require(ascii.matches(Regex("[a-z0-9.-]+"))) {
            "Challenge domain contains unsupported characters"
        }
        require(!ascii.startsWith(".") && !ascii.endsWith(".")) {
            "Challenge domain is malformed"
        }
        return ascii
    }
}