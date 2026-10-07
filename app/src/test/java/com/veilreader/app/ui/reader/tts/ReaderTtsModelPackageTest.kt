package com.veilreader.app.ui.reader.tts

import java.io.File
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsModelPackageTest {
    @Test
    fun modelManifestRequiresHttpsLicenseHashLanguageAndBoundedSize() {
        val valid = packageFor(ByteArray(2_048) { index -> (index % 251).toByte() })
        assertEquals("fa-IR", requireNotNull(valid.normalizedOrNull()).languageTag)

        assertNull(valid.copy(sourceUrl = "http://example.invalid/model.onnx").normalizedOrNull())
        assertNull(valid.copy(licenseUrl = "file:///license").normalizedOrNull())
        assertNull(valid.copy(sha256 = "abc").normalizedOrNull())
        assertNull(valid.copy(languageTag = "und").normalizedOrNull())
        assertNull(valid.copy(id = "../escape").normalizedOrNull())
        assertNull(valid.copy(expectedBytes = 12L).normalizedOrNull())
    }

    @Test
    fun modelBytesMustMatchBothDeclaredSizeAndShaBeforeActivation() {
        val directory = createTempDirectory("veil-tts-model").toFile()
        try {
            val bytes = ByteArray(2_048) { index -> (index % 251).toByte() }
            val file = File(directory, "model.bin").apply { writeBytes(bytes) }
            val manifest = packageFor(bytes)

            assertEquals(
                ReaderTtsModelVerification.Verified,
                verifyReaderTtsModelFile(file, manifest)
            )
            assertEquals(
                ReaderTtsModelVerification.Rejected(
                    ReaderTtsModelVerification.Reason.HASH_MISMATCH
                ),
                verifyReaderTtsModelFile(
                    file,
                    manifest.copy(sha256 = "0".repeat(64))
                )
            )
            assertEquals(
                ReaderTtsModelVerification.Rejected(
                    ReaderTtsModelVerification.Reason.SIZE_MISMATCH
                ),
                verifyReaderTtsModelFile(
                    file,
                    manifest.copy(
                        expectedBytes = bytes.size.toLong() + 1L,
                        maxExpandedBytes = bytes.size.toLong() + 1L
                    )
                )
            )
        } finally {
            assertTrue(directory.deleteRecursively())
        }
    }

    private fun packageFor(bytes: ByteArray): ReaderTtsModelPackage =
        ReaderTtsModelPackage(
            id = "veil-fa-fixture",
            version = "1.0",
            languageTag = "fa-IR",
            displayName = "Veil Persian Fixture",
            expectedBytes = bytes.size.toLong(),
            sha256 = sha256(bytes),
            licenseSpdx = "Apache-2.0",
            licenseUrl = "https://example.invalid/license",
            sourceUrl = "https://example.invalid/model"
        )

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
}
