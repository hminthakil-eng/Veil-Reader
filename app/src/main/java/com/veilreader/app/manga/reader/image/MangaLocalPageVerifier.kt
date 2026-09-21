package com.veilreader.app.manga.reader.image

import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LocalPageResolution {
    data class Ready(val file: File) : LocalPageResolution
    data class Invalid(val reason: String) : LocalPageResolution
}

class MangaLocalPageVerifier(
    private val cacheRoot: File
) {
    private data class CacheKey(
        val canonicalPath: String,
        val length: Long,
        val lastModified: Long,
        val expectedSha256: String?
    )

    private val verified = ConcurrentHashMap<CacheKey, Boolean>()

    suspend fun resolve(asset: MangaPageAsset.Local): LocalPageResolution =
        withContext(Dispatchers.IO) {
            val root = runCatching { cacheRoot.canonicalFile }.getOrElse {
                return@withContext LocalPageResolution.Invalid("Offline cache root is unavailable")
            }
            val candidate = runCatching {
                File(root, asset.relativePath).canonicalFile
            }.getOrElse {
                return@withContext LocalPageResolution.Invalid("Offline page path is invalid")
            }

            val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
            if (!candidate.path.startsWith(rootPrefix)) {
                return@withContext LocalPageResolution.Invalid(
                    "Offline page escaped the cache root"
                )
            }
            if (!candidate.isFile) {
                return@withContext LocalPageResolution.Invalid("Offline page file is missing")
            }

            val actualLength = candidate.length()
            if (actualLength <= 0L) {
                return@withContext LocalPageResolution.Invalid("Offline page file is empty")
            }
            if (asset.byteSize > 0L && actualLength != asset.byteSize) {
                return@withContext LocalPageResolution.Invalid(
                    "Offline page size does not match the manifest"
                )
            }

            val expected = asset.contentSha256
                ?.trim()
                ?.lowercase()
                ?.takeIf(String::isNotBlank)

            if (expected != null && !expected.matches(Regex("[a-f0-9]{64}"))) {
                return@withContext LocalPageResolution.Invalid(
                    "Offline page checksum format is invalid"
                )
            }

            val key = CacheKey(
                canonicalPath = candidate.path,
                length = actualLength,
                lastModified = candidate.lastModified(),
                expectedSha256 = expected
            )
            if (verified[key] == true) {
                return@withContext LocalPageResolution.Ready(candidate)
            }

            if (expected != null) {
                val actual = sha256(candidate)
                if (actual != expected) {
                    return@withContext LocalPageResolution.Invalid(
                        "Offline page checksum verification failed"
                    )
                }
            }

            verified[key] = true
            LocalPageResolution.Ready(candidate)
        }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }
}
