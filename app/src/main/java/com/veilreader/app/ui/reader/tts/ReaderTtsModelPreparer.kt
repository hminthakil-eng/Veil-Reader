package com.veilreader.app.ui.reader.tts

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal data class ReaderTtsPreparedModel(
    val installed: ReaderTtsInstalledModel,
    val payloadRoot: File,
    val layout: ReaderTtsModelLayout
)

internal sealed interface ReaderTtsModelPrepareResult {
    data class Ready(val model: ReaderTtsPreparedModel) : ReaderTtsModelPrepareResult
    data class Rejected(val reason: Reason) : ReaderTtsModelPrepareResult

    enum class Reason {
        INSTALLED_ARCHIVE_MISSING,
        EXTRACTION_REJECTED,
        LAYOUT_REJECTED,
        IO_ERROR
    }
}

/**
 * Expands a verified installed archive into a runtime payload without exposing partial files.
 *
 * The installed archive is immutable input. Extraction happens in a sibling staging directory,
 * family-specific layout validation runs there, and only then is the directory atomically renamed
 * to "payload". An already-valid payload is reused and never rewritten on app start.
 */
internal class ReaderTtsModelPreparer(
    private val extractor: ReaderTtsModelArchiveExtractor = ReaderTtsModelArchiveExtractor()
) {
    @Synchronized
    fun prepare(
        installed: ReaderTtsInstalledModel,
        layout: ReaderTtsModelLayout,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ReaderTtsModelPrepareResult {
        if (!installed.archive.isFile) {
            return ReaderTtsModelPrepareResult.Rejected(
                ReaderTtsModelPrepareResult.Reason.INSTALLED_ARCHIVE_MISSING
            )
        }
        val safeLayout = layout.normalizedOrNull()
            ?: return ReaderTtsModelPrepareResult.Rejected(
                ReaderTtsModelPrepareResult.Reason.LAYOUT_REJECTED
            )

        val modelDirectory = runCatching { installed.directory.canonicalFile }.getOrNull()
            ?: return ReaderTtsModelPrepareResult.Rejected(
                ReaderTtsModelPrepareResult.Reason.IO_ERROR
            )
        val payload = File(modelDirectory, PAYLOAD_DIRECTORY)

        if (
            payload.isDirectory &&
            validateReaderTtsModelLayout(payload, safeLayout) ==
                ReaderTtsModelLayoutValidation.Valid
        ) {
            return ReaderTtsModelPrepareResult.Ready(
                ReaderTtsPreparedModel(installed, payload, safeLayout)
            )
        }

        return try {
            cleanupStaging(modelDirectory)
            val staging = File(
                modelDirectory,
                ".payload-staging-${nowEpochMs.coerceAtLeast(0L)}"
            )
            if (staging.exists()) staging.deleteRecursively()

            when (extractor.extractTarBz2(installed.archive, staging)) {
                is ReaderTtsArchiveExtractResult.Success -> Unit
                is ReaderTtsArchiveExtractResult.Rejected -> {
                    staging.deleteRecursively()
                    return ReaderTtsModelPrepareResult.Rejected(
                        ReaderTtsModelPrepareResult.Reason.EXTRACTION_REJECTED
                    )
                }
            }

            if (
                validateReaderTtsModelLayout(staging, safeLayout) !=
                    ReaderTtsModelLayoutValidation.Valid
            ) {
                staging.deleteRecursively()
                return ReaderTtsModelPrepareResult.Rejected(
                    ReaderTtsModelPrepareResult.Reason.LAYOUT_REJECTED
                )
            }

            if (payload.exists() && !payload.deleteRecursively()) {
                staging.deleteRecursively()
                return ReaderTtsModelPrepareResult.Rejected(
                    ReaderTtsModelPrepareResult.Reason.IO_ERROR
                )
            }
            publish(staging, payload)

            if (
                validateReaderTtsModelLayout(payload, safeLayout) !=
                    ReaderTtsModelLayoutValidation.Valid
            ) {
                payload.deleteRecursively()
                return ReaderTtsModelPrepareResult.Rejected(
                    ReaderTtsModelPrepareResult.Reason.LAYOUT_REJECTED
                )
            }

            ReaderTtsModelPrepareResult.Ready(
                ReaderTtsPreparedModel(installed, payload, safeLayout)
            )
        } catch (_: Exception) {
            cleanupStaging(modelDirectory)
            ReaderTtsModelPrepareResult.Rejected(
                ReaderTtsModelPrepareResult.Reason.IO_ERROR
            )
        }
    }

    fun cleanupStaging(modelDirectory: File): Int {
        if (!modelDirectory.isDirectory) return 0
        var removed = 0
        modelDirectory.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith(".payload-staging-") }
            .forEach { if (it.deleteRecursively()) removed += 1 }
        return removed
    }

    private fun publish(staging: File, payload: File) {
        try {
            Files.move(
                staging.toPath(),
                payload.toPath(),
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(staging.toPath(), payload.toPath())
        }
    }

    private companion object {
        const val PAYLOAD_DIRECTORY = "payload"
    }
}
