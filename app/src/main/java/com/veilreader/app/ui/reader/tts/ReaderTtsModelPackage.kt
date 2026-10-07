package com.veilreader.app.ui.reader.tts

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * A downloadable local-neural voice package must be content-addressed and license-attributed.
 *
 * No model is considered installable until all metadata below is supplied by a reviewed registry.
 * This contract intentionally contains no downloader and no default remote URL: adding a model is a
 * product/security decision, not an implicit network side effect of opening Listening Mode.
 */
internal data class ReaderTtsModelPackage(
    val id: String,
    val version: String,
    val languageTag: String,
    val displayName: String,
    val expectedBytes: Long,
    val maxExpandedBytes: Long = expectedBytes,
    val sha256: String,
    val licenseSpdx: String,
    val licenseUrl: String,
    val sourceUrl: String
) {
    fun normalizedOrNull(): ReaderTtsModelPackage? {
        val safeId = id.trim().lowercase(Locale.ROOT)
            .takeIf { it.matches(MODEL_ID_REGEX) }
            ?: return null
        val safeVersion = version.trim()
            .takeIf { it.matches(VERSION_REGEX) }
            ?: return null
        val locale = Locale.forLanguageTag(languageTag.trim())
            .takeUnless { it.language.isBlank() || it.language == "und" }
            ?: return null
        val safeName = displayName.trim().takeIf { it.length in 1..96 } ?: return null
        val safeHash = sha256.trim().lowercase(Locale.ROOT)
            .takeIf { it.matches(SHA256_REGEX) }
            ?: return null
        val safeLicense = licenseSpdx.trim().takeIf { it.length in 1..64 } ?: return null
        val safeLicenseUrl = licenseUrl.trim().takeIf(::isHttpsUrl) ?: return null
        val safeSourceUrl = sourceUrl.trim().takeIf(::isHttpsUrl) ?: return null
        if (expectedBytes !in MIN_MODEL_BYTES..MAX_MODEL_BYTES) return null
        if (maxExpandedBytes !in expectedBytes..MAX_EXPANDED_MODEL_BYTES) return null

        return copy(
            id = safeId,
            version = safeVersion,
            languageTag = locale.toLanguageTag(),
            displayName = safeName,
            sha256 = safeHash,
            licenseSpdx = safeLicense,
            licenseUrl = safeLicenseUrl,
            sourceUrl = safeSourceUrl
        )
    }

    fun installDirectoryName(): String {
        val safe = requireNotNull(normalizedOrNull()) { "Invalid TTS model package" }
        return safe.id + "-" + safe.version
    }

    private companion object {
        val MODEL_ID_REGEX = Regex("[a-z0-9][a-z0-9._-]{1,63}")
        val VERSION_REGEX = Regex("[A-Za-z0-9][A-Za-z0-9._+-]{0,31}")
        val SHA256_REGEX = Regex("[0-9a-fA-F]{64}")
        const val MIN_MODEL_BYTES = 1_024L
        const val MAX_MODEL_BYTES = 2L * 1024L * 1024L * 1024L
        const val MAX_EXPANDED_MODEL_BYTES = 4L * 1024L * 1024L * 1024L

        fun isHttpsUrl(value: String): Boolean =
            value.startsWith("https://", ignoreCase = true) &&
                !value.contains('\n') &&
                !value.contains('\r')
    }
}

internal sealed interface ReaderTtsModelVerification {
    data object Verified : ReaderTtsModelVerification
    data class Rejected(val reason: Reason) : ReaderTtsModelVerification

    enum class Reason {
        INVALID_MANIFEST,
        MISSING_FILE,
        SIZE_MISMATCH,
        HASH_MISMATCH,
        IO_ERROR
    }
}

/**
 * Verifies bytes before a neural runtime may load them.
 *
 * A temporary download must stay outside the active model directory until this returns Verified;
 * callers can then atomically rename the verified package into its final directory.
 */
internal fun verifyReaderTtsModelFile(
    file: File,
    packageInfo: ReaderTtsModelPackage
): ReaderTtsModelVerification {
    val safe = packageInfo.normalizedOrNull()
        ?: return ReaderTtsModelVerification.Rejected(
            ReaderTtsModelVerification.Reason.INVALID_MANIFEST
        )
    if (!file.isFile) {
        return ReaderTtsModelVerification.Rejected(
            ReaderTtsModelVerification.Reason.MISSING_FILE
        )
    }
    if (file.length() != safe.expectedBytes) {
        return ReaderTtsModelVerification.Rejected(
            ReaderTtsModelVerification.Reason.SIZE_MISMATCH
        )
    }

    val actual = runCatching {
        FileInputStream(file).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }
    }.getOrElse {
        return ReaderTtsModelVerification.Rejected(
            ReaderTtsModelVerification.Reason.IO_ERROR
        )
    }

    return if (actual.equals(safe.sha256, ignoreCase = true)) {
        ReaderTtsModelVerification.Verified
    } else {
        ReaderTtsModelVerification.Rejected(
            ReaderTtsModelVerification.Reason.HASH_MISMATCH
        )
    }
}
