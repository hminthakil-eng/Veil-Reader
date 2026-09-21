package com.veilreader.app.manga.reader.image

import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MangaLocalPageVerifierTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun validLocalFilePassesSizeAndHashVerification() = runBlocking {
        val root = temp.newFolder("cache")
        val page = java.io.File(root, "manga/work/page-00000.jpg")
        page.parentFile.mkdirs()
        val bytes = "verified-page".toByteArray()
        page.writeBytes(bytes)

        val verifier = MangaLocalPageVerifier(root)
        val result = verifier.resolve(
            MangaPageAsset.Local(
                index = 0,
                relativePath = "manga/work/page-00000.jpg",
                byteSize = bytes.size.toLong(),
                contentSha256 = sha256(bytes)
            )
        )

        assertTrue(result is LocalPageResolution.Ready)
        assertEquals(page.canonicalFile, (result as LocalPageResolution.Ready).file)
    }

    @Test
    fun traversalOutsideCacheRootIsRejected() = runBlocking {
        val root = temp.newFolder("cache")
        val outside = temp.newFile("outside.jpg")
        outside.writeText("secret")

        val result = MangaLocalPageVerifier(root).resolve(
            MangaPageAsset.Local(
                index = 0,
                relativePath = "../outside.jpg",
                byteSize = outside.length()
            )
        )

        assertTrue(result is LocalPageResolution.Invalid)
    }

    @Test
    fun manifestSizeMismatchIsRejected() = runBlocking {
        val root = temp.newFolder("cache")
        val page = java.io.File(root, "page.jpg")
        page.writeText("abc")

        val result = MangaLocalPageVerifier(root).resolve(
            MangaPageAsset.Local(
                index = 0,
                relativePath = "page.jpg",
                byteSize = 99L
            )
        )

        assertTrue(result is LocalPageResolution.Invalid)
    }

    @Test
    fun checksumMismatchIsRejected() = runBlocking {
        val root = temp.newFolder("cache")
        val page = java.io.File(root, "page.jpg")
        page.writeText("abc")

        val result = MangaLocalPageVerifier(root).resolve(
            MangaPageAsset.Local(
                index = 0,
                relativePath = "page.jpg",
                byteSize = page.length(),
                contentSha256 = "0".repeat(64)
            )
        )

        assertTrue(result is LocalPageResolution.Invalid)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
