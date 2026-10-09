package com.veilreader.app.ui.screens

import java.io.IOException
import org.readium.r2.shared.util.resource.Resource

internal const val READER_IMAGE_MAX_BYTES = 64 * 1024 * 1024
internal const val READER_IMAGE_LOAD_TIMEOUT_MS = 30_000L

internal class ReaderImageTooLargeException : IOException("Reader image exceeds viewing limit")

/** Length metadata may itself read the whole resource. Always request a capped range instead. */
internal suspend fun readReaderImageBytes(
    resource: Resource,
    maxBytes: Int = READER_IMAGE_MAX_BYTES
): ByteArray? {
    require(maxBytes > 0)
    // Inclusive endpoint reads one extra byte to distinguish exact-limit EOF from truncation.
    val bytes = resource.read(0L..maxBytes.toLong()).getOrNull() ?: return null
    if (bytes.size > maxBytes) throw ReaderImageTooLargeException()
    return bytes.takeIf { it.isNotEmpty() }
}
