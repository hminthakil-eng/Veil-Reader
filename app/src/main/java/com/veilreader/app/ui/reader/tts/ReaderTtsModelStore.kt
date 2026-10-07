package com.veilreader.app.ui.reader.tts

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal data class ReaderTtsInstalledModel(
    val packageInfo: ReaderTtsModelPackage,
    val directory: File,
    val archive: File,
    val lastUsedEpochMs: Long
)

internal sealed interface ReaderTtsModelInstallResult {
    data class Installed(val model: ReaderTtsInstalledModel) : ReaderTtsModelInstallResult
    data class Rejected(val reason: Reason) : ReaderTtsModelInstallResult

    enum class Reason {
        INVALID_MANIFEST,
        VERIFICATION_FAILED,
        STORAGE_BUDGET,
        IO_ERROR
    }
}

internal class ReaderTtsModelStore(
    private val root: File,
    private val budgetBytes: Long = DEFAULT_BUDGET_BYTES
) {
    init {
        require(budgetBytes > 0L)
    }

    @Synchronized
    fun installVerified(
        downloadedFile: File,
        packageInfo: ReaderTtsModelPackage,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ReaderTtsModelInstallResult {
        val safe = packageInfo.normalizedOrNull()
            ?: return ReaderTtsModelInstallResult.Rejected(
                ReaderTtsModelInstallResult.Reason.INVALID_MANIFEST
            )
        if (verifyReaderTtsModelFile(downloadedFile, safe) != ReaderTtsModelVerification.Verified) {
            return ReaderTtsModelInstallResult.Rejected(
                ReaderTtsModelInstallResult.Reason.VERIFICATION_FAILED
            )
        }
        if (safe.expectedBytes > budgetBytes) {
            return ReaderTtsModelInstallResult.Rejected(
                ReaderTtsModelInstallResult.Reason.STORAGE_BUDGET
            )
        }

        return try {
            ensureRoot()
            cleanupStaging()
            val finalDirectory = File(root, safe.installDirectoryName())
            val existingBytes = finalDirectory
                .takeIf(File::isDirectory)
                ?.let(::directoryBytes)
                ?: 0L
            if (!makeRoomFor(
                    incomingBytes = safe.expectedBytes,
                    replacingBytes = existingBytes,
                    protectedDirectory = finalDirectory
                )
            ) {
                return ReaderTtsModelInstallResult.Rejected(
                    ReaderTtsModelInstallResult.Reason.STORAGE_BUDGET
                )
            }

            val staging = File(
                root,
                ".staging-${safe.installDirectoryName()}-${nowEpochMs.coerceAtLeast(0L)}"
            )
            if (staging.exists()) staging.deleteRecursively()
            check(staging.mkdirs()) { "Could not create TTS model staging directory" }

            val stagedArchive = File(staging, ARCHIVE_NAME)
            copyAndSync(downloadedFile, stagedArchive)
            if (verifyReaderTtsModelFile(stagedArchive, safe) != ReaderTtsModelVerification.Verified) {
                staging.deleteRecursively()
                return ReaderTtsModelInstallResult.Rejected(
                    ReaderTtsModelInstallResult.Reason.VERIFICATION_FAILED
                )
            }
            writeInstallMetadata(staging, safe)
            File(staging, LAST_USED_NAME).apply {
                writeText(nowEpochMs.coerceAtLeast(0L).toString())
                setLastModified(nowEpochMs.coerceAtLeast(0L))
            }

            if (finalDirectory.exists() && !finalDirectory.deleteRecursively()) {
                throw IllegalStateException("Could not replace existing TTS model")
            }
            publishDirectory(staging, finalDirectory)

            ReaderTtsModelInstallResult.Installed(
                ReaderTtsInstalledModel(
                    packageInfo = safe,
                    directory = finalDirectory,
                    archive = File(finalDirectory, ARCHIVE_NAME),
                    lastUsedEpochMs = nowEpochMs.coerceAtLeast(0L)
                )
            )
        } catch (_: Exception) {
            ReaderTtsModelInstallResult.Rejected(
                ReaderTtsModelInstallResult.Reason.IO_ERROR
            )
        }
    }

    @Synchronized
    fun touch(
        model: ReaderTtsInstalledModel,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        val canonicalRoot = runCatching { root.canonicalFile }.getOrNull() ?: return false
        val canonicalDirectory = runCatching { model.directory.canonicalFile }.getOrNull()
            ?: return false
        if (canonicalDirectory.parentFile != canonicalRoot || !canonicalDirectory.isDirectory) {
            return false
        }
        return runCatching {
            File(canonicalDirectory, LAST_USED_NAME).apply {
                writeText(nowEpochMs.coerceAtLeast(0L).toString())
                setLastModified(nowEpochMs.coerceAtLeast(0L))
            }
            true
        }.getOrDefault(false)
    }

    @Synchronized
    fun remove(model: ReaderTtsInstalledModel): Boolean {
        val canonicalRoot = runCatching { root.canonicalFile }.getOrNull() ?: return false
        val canonicalDirectory = runCatching { model.directory.canonicalFile }.getOrNull()
            ?: return false
        if (canonicalDirectory.parentFile != canonicalRoot) return false
        return !canonicalDirectory.exists() || canonicalDirectory.deleteRecursively()
    }

    @Synchronized
    fun usedBytes(): Long {
        if (!root.isDirectory) return 0L
        return root.listFiles()
            .orEmpty()
            .filter { it.isDirectory && !it.name.startsWith(".staging-") }
            .sumOf(::directoryBytes)
    }

    @Synchronized
    fun cleanupStaging(): Int {
        if (!root.isDirectory) return 0
        var removed = 0
        root.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith(".staging-") }
            .forEach { if (it.deleteRecursively()) removed += 1 }
        return removed
    }

    private fun ensureRoot() {
        if (root.exists()) {
            check(root.isDirectory) { "TTS model root is not a directory" }
        } else {
            check(root.mkdirs()) { "Could not create TTS model root" }
        }
    }

    private fun makeRoomFor(
        incomingBytes: Long,
        replacingBytes: Long,
        protectedDirectory: File
    ): Boolean {
        val projected = usedBytes() - replacingBytes + incomingBytes
        if (projected <= budgetBytes) return true

        var current = usedBytes() - replacingBytes
        val candidates = root.listFiles()
            .orEmpty()
            .filter {
                it.isDirectory &&
                    !it.name.startsWith(".staging-") &&
                    runCatching { it.canonicalFile != protectedDirectory.canonicalFile }
                        .getOrDefault(true)
            }
            .sortedWith(
                compareBy<File> { lastUsedEpochMs(it) }
                    .thenBy { it.name }
            )

        for (candidate in candidates) {
            val bytes = directoryBytes(candidate)
            if (candidate.deleteRecursively()) {
                current = (current - bytes).coerceAtLeast(0L)
                if (current + incomingBytes <= budgetBytes) return true
            }
        }
        return current + incomingBytes <= budgetBytes
    }

    private fun publishDirectory(staging: File, destination: File) {
        try {
            Files.move(
                staging.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(staging.toPath(), destination.toPath())
        }
    }

    private fun copyAndSync(source: File, destination: File) {
        source.inputStream().buffered().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.flush()
                output.fd.sync()
            }
        }
    }

    private fun writeInstallMetadata(directory: File, safe: ReaderTtsModelPackage) {
        File(directory, METADATA_NAME).writeText(
            buildString {
                appendLine("id=${safe.id}")
                appendLine("version=${safe.version}")
                appendLine("language=${safe.languageTag}")
                appendLine("name=${safe.displayName}")
                appendLine("bytes=${safe.expectedBytes}")
                appendLine("sha256=${safe.sha256}")
                appendLine("license=${safe.licenseSpdx}")
                appendLine("license_url=${safe.licenseUrl}")
                appendLine("source_url=${safe.sourceUrl}")
            }
        )
    }

    private fun lastUsedEpochMs(directory: File): Long =
        File(directory, LAST_USED_NAME)
            .takeIf(File::isFile)
            ?.readText()
            ?.trim()
            ?.toLongOrNull()
            ?.coerceAtLeast(0L)
            ?: directory.lastModified().coerceAtLeast(0L)

    private fun directoryBytes(directory: File): Long =
        directory.walkTopDown()
            .filter(File::isFile)
            .sumOf(File::length)

    private companion object {
        const val ARCHIVE_NAME = "model.package"
        const val METADATA_NAME = "model.meta"
        const val LAST_USED_NAME = "last_used"
        const val DEFAULT_BUDGET_BYTES = 1024L * 1024L * 1024L
    }
}
