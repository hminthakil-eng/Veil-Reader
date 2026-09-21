package com.veilreader.app.manga.reader.image

import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaRemotePagePolicyTest {

    @Test
    fun validHttpRequestKeepsHeadersOnlyInMemory() {
        val asset = MangaPageAsset.Remote(
            index = 0,
            imageUrl = "https://cdn.example.test/page.jpg",
            requestHeaders = mapOf(
                "Referer" to "https://reader.example.test/",
                "Cookie" to "session=ephemeral"
            )
        )

        val result = MangaRemotePagePolicy.resolve(asset)

        assertTrue(result is RemotePageResolution.Ready)
        val ready = result as RemotePageResolution.Ready
        assertTrue(ready.headers["Referer"]!!.startsWith("https://"))
        assertTrue(ready.headers["Cookie"]!!.contains("ephemeral"))

        val text = ready.toString()
        assertFalse(text.contains("session=ephemeral"))
        assertFalse(text.contains("cdn.example.test"))
    }

    @Test
    fun embeddedUrlCredentialsAreRejected() {
        val result = MangaRemotePagePolicy.resolve(
            MangaPageAsset.Remote(
                index = 0,
                imageUrl = "https://user:pass@example.test/page.jpg"
            )
        )

        assertTrue(result is RemotePageResolution.Invalid)
    }

    @Test
    fun headerInjectionIsRejected() {
        val result = MangaRemotePagePolicy.resolve(
            MangaPageAsset.Remote(
                index = 0,
                imageUrl = "https://example.test/page.jpg",
                requestHeaders = mapOf(
                    "Referer" to "https://safe.test/\r\nX-Evil: injected"
                )
            )
        )

        assertTrue(result is RemotePageResolution.Invalid)
    }

    @Test
    fun nonHttpSchemesAreRejected() {
        val result = MangaRemotePagePolicy.resolve(
            MangaPageAsset.Remote(
                index = 0,
                imageUrl = "file:///data/local/page.jpg"
            )
        )

        assertTrue(result is RemotePageResolution.Invalid)
    }

    @Test
    fun presentationAssetToStringAlsoRedactsSecrets() {
        val asset = MangaPageAsset.Remote(
            index = 1,
            imageUrl = "https://secret.example.test/page.jpg",
            requestHeaders = mapOf("Cookie" to "secret-cookie")
        )

        val text = asset.toString()

        assertFalse(text.contains("secret.example.test"))
        assertFalse(text.contains("secret-cookie"))
        assertTrue(text.contains("<redacted>"))
    }
}
