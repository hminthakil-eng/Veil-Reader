package com.veilreader.app.manga.reader.image

import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import java.net.URI

sealed interface RemotePageResolution {
    data class Ready(
        val url: String,
        val headers: Map<String, String>
    ) : RemotePageResolution {
        override fun toString(): String =
            "Ready(url=<redacted>, headers=<redacted>)"
    }

    data class Invalid(val reason: String) : RemotePageResolution
}

object MangaRemotePagePolicy {
    private const val MAX_HEADERS = 32
    private const val MAX_HEADER_NAME_LENGTH = 128
    private const val MAX_HEADER_VALUE_LENGTH = 8_192

    fun resolve(asset: MangaPageAsset.Remote): RemotePageResolution {
        val uri = runCatching { URI(asset.imageUrl) }.getOrNull()
            ?: return RemotePageResolution.Invalid("Remote page URL is invalid")

        if (uri.scheme !in setOf("https", "http") || uri.host.isNullOrBlank()) {
            return RemotePageResolution.Invalid("Remote page URL must use HTTP(S)")
        }
        if (uri.userInfo != null) {
            return RemotePageResolution.Invalid(
                "Remote page URL must not contain embedded credentials"
            )
        }
        if (asset.requestHeaders.size > MAX_HEADERS) {
            return RemotePageResolution.Invalid("Too many remote image headers")
        }

        val sanitized = linkedMapOf<String, String>()
        for ((rawName, rawValue) in asset.requestHeaders) {
            val name = rawName.trim()
            val value = rawValue.trim()

            if (
                name.isBlank() ||
                name.length > MAX_HEADER_NAME_LENGTH ||
                !name.matches(Regex("[!#$%&'*+.^_|~0-9A-Za-z-]+"))
            ) {
                return RemotePageResolution.Invalid("Remote image header name is invalid")
            }
            if (
                value.length > MAX_HEADER_VALUE_LENGTH ||
                value.contains('\r') ||
                value.contains('\n')
            ) {
                return RemotePageResolution.Invalid("Remote image header value is invalid")
            }

            sanitized[name] = value
        }

        return RemotePageResolution.Ready(
            url = asset.imageUrl,
            headers = sanitized
        )
    }
}
