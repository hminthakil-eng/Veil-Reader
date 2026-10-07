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

        var transaction: File? = null
        return try {
            ensureRoot()
            if (!recoverInterruptedTransactions()) {
                return ReaderTtsModelInstallResult.Rejected(
                    ReaderTtsModelInstallResult.Reason.IO_ERROR
                )
            }
            cleanupStaging()

            val finalDirectory = File(root, safe.installDirectoryName())
            val existingBytes = finalDirectory
                .takeIf(File::isDirectory)
                ?.let(::modelBytes)
                ?: 0L
            val evictions = planEvictions(
                incomingBytes = safe.expectedBytes,
                replacingBytes = existingBytes,
                protectedDirectory = finalDirectory
            ) ?: return ReaderTtsModelInstallResult.Rejected(
                ReaderTtsModelInstallResult.Reason.STORAGE_BUDGET
            )

            val transactionDirectory = createInstallTransaction(
                safe = safe,
                nowEpochMs = nowEpochMs,
                hadReplacement = finalDirectory.isDirectory
            )
            transaction = transactionDirectory

            val stagedModel = File(transactionDirectory, TRANSACTION_STAGED_DIRECTORY)
            check(stagedModel.mkdirs()) { "Could not create TTS model transaction staging" }

            val stagedArchive = File(stagedModel, ARCHIVE_NAME)
            copyAndSync(downloadedFile, stagedArchive)
            if (verifyReaderTtsModelFile(stagedArchive, safe) != ReaderTtsModelVerification.Verified) {
                transactionDirectory.deleteRecursively()
                return ReaderTtsModelInstallResult.Rejected(
                    ReaderTtsModelInstallResult.Reason.VERIFICATION_FAILED
                )
            }
            writeInstallMetadata(stagedModel, safe)
            File(stagedModel, LAST_USED_NAME).apply {
                writeText(nowEpochMs.coerceAtLeast(0L).toString())
                setLastModified(nowEpochMs.coerceAtLeast(0L))
            }

            reserveEvictions(transactionDirectory, evictions)

            val replacementBackup = File(
                transactionDirectory,
                TRANSACTION_REPLACEMENT_DIRECTORY
            )
            if (finalDirectory.exists()) {
                moveDirectory(finalDirectory, replacementBackup)
            }

            publishDirectory(stagedModel, finalDirectory)
            if (
                verifyReaderTtsModelFile(
                    File(finalDirectory, ARCHIVE_NAME),
                    safe
                ) != ReaderTtsModelVerification.Verified
            ) {
                throw IllegalStateException("Published TTS model failed verification")
            }

            writeTransactionState(
                transactionDirectory,
                InstallTransactionState(
                    phase = TRANSACTION_PHASE_PUBLISHED,
                    finalName = finalDirectory.name,
                    hadReplacement = replacementBackup.exists()
                )
            )

            cleanupPublishedTransaction(transactionDirectory)
            transaction = null

            ReaderTtsModelInstallResult.Installed(
                ReaderTtsInstalledModel(
                    packageInfo = safe,
                    directory = finalDirectory,
                    archive = File(finalDirectory, ARCHIVE_NAME),
                    lastUsedEpochMs = nowEpochMs.coerceAtLeast(0L)
                )
            )
        } catch (_: Exception) {
            transaction?.let(::recoverInstallTransaction)
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
            .filter {
                it.isDirectory &&
                    !it.name.startsWith(".staging-") &&
                    !it.name.startsWith(INSTALL_TRANSACTION_PREFIX)
            }
            .sumOf(::modelBytes)
    }

    @Synchronized
    fun ensureCapacityForModelGrowth(
        model: ReaderTtsInstalledModel,
        additionalBytes: Long
    ): Boolean {
        if (additionalBytes < 0L) return false
        if (!recoverInterruptedTransactions()) return false
        cleanupStaging()

        val canonicalRoot = runCatching { root.canonicalFile }.getOrNull() ?: return false
        val canonicalDirectory = runCatching { model.directory.canonicalFile }.getOrNull()
            ?: return false
        if (
            canonicalDirectory.parentFile != canonicalRoot ||
            !canonicalDirectory.isDirectory
        ) {
            return false
        }

        // Capacity probing must never evict a working model. Extraction can still fail after this
        // point, so destructive LRU eviction here would turn a failed preparation into data loss.
        val stableBytes = usedBytes()
        return additionalBytes <= (budgetBytes - stableBytes).coerceAtLeast(0L)
    }

    @Synchronized
    fun cleanupStaging(): Int {
        if (!root.isDirectory) return 0
        var removed = 0
        val children = root.listFiles().orEmpty()

        children
            .filter { it.isDirectory && it.name.startsWith(".staging-") }
            .forEach { if (it.deleteRecursively()) removed += 1 }

        children
            .filter {
                it.isDirectory &&
                    !it.name.startsWith(".staging-") &&
                    !it.name.startsWith(INSTALL_TRANSACTION_PREFIX)
            }
            .forEach { modelDirectory ->
                modelDirectory.listFiles()
                    .orEmpty()
                    .filter {
                        it.isDirectory &&
                            it.name.startsWith(PAYLOAD_STAGING_PREFIX)
                    }
                    .forEach { if (it.deleteRecursively()) removed += 1 }
            }

        return removed
    }

    private fun ensureRoot() {
        if (root.exists()) {
            check(root.isDirectory) { "TTS model root is not a directory" }
        } else {
            check(root.mkdirs()) { "Could not create TTS model root" }
        }
    }

    @Synchronized
    fun recoverInterruptedTransactions(): Boolean {
        if (!root.exists()) return true
        if (!root.isDirectory) return false

        var allRecovered = true
        root.listFiles()
            .orEmpty()
            .filter {
                it.isDirectory &&
                    it.name.startsWith(INSTALL_TRANSACTION_PREFIX)
            }
            .sortedBy { it.name }
            .forEach { transaction ->
                if (!recoverInstallTransaction(transaction)) {
                    allRecovered = false
                }
            }
        return allRecovered
    }

    private fun planEvictions(
        incomingBytes: Long,
        replacingBytes: Long,
        protectedDirectory: File
    ): List<File>? {
        val current = (usedBytes() - replacingBytes).coerceAtLeast(0L)
        if (current + incomingBytes <= budgetBytes) return emptyList()

        var projected = current
        val selected = mutableListOf<File>()
        val candidates = root.listFiles()
            .orEmpty()
            .filter {
                it.isDirectory &&
                    !it.name.startsWith(".staging-") &&
                    !it.name.startsWith(INSTALL_TRANSACTION_PREFIX) &&
                    runCatching { it.canonicalFile != protectedDirectory.canonicalFile }
                        .getOrDefault(true)
            }
            .sortedWith(
                compareBy<File> { lastUsedEpochMs(it) }
                    .thenBy { it.name }
            )

        for (candidate in candidates) {
            selected += candidate
            projected = (projected - modelBytes(candidate)).coerceAtLeast(0L)
            if (projected + incomingBytes <= budgetBytes) return selected
        }
        return null
    }

    private fun createInstallTransaction(
        safe: ReaderTtsModelPackage,
        nowEpochMs: Long,
        hadReplacement: Boolean
    ): File {
        val base = INSTALL_TRANSACTION_PREFIX +
            safe.installDirectoryName() +
            "-" +
            nowEpochMs.coerceAtLeast(0L)
        var attempt = 0
        while (attempt < 100) {
            val suffix = if (attempt == 0) "" else "-" + attempt
            val directory = File(root, base + suffix)
            if (directory.mkdir()) {
                writeTransactionState(
                    directory,
                    InstallTransactionState(
                        phase = TRANSACTION_PHASE_PREPARED,
                        finalName = safe.installDirectoryName(),
                        hadReplacement = hadReplacement
                    )
                )
                return directory
            }
            attempt += 1
        }
        throw IllegalStateException("Could not create TTS model install transaction")
    }

    private fun reserveEvictions(
        transaction: File,
        candidates: List<File>
    ) {
        if (candidates.isEmpty()) return
        val evictionRoot = File(transaction, TRANSACTION_EVICTIONS_DIRECTORY)
        check(evictionRoot.mkdirs()) { "Could not create TTS eviction reservation" }

        candidates.forEach { candidate ->
            val destination = File(evictionRoot, candidate.name)
            check(!destination.exists()) { "Duplicate TTS eviction reservation" }
            moveDirectory(candidate, destination)
        }
    }

    private fun recoverInstallTransaction(transaction: File): Boolean {
        if (!transaction.isDirectory) return true
        val state = readTransactionState(transaction) ?: return false
        val finalDirectory = safeRootChild(state.finalName) ?: return false
        val replacementBackup = File(
            transaction,
            TRANSACTION_REPLACEMENT_DIRECTORY
        )
        val evictionRoot = File(
            transaction,
            TRANSACTION_EVICTIONS_DIRECTORY
        )

        return runCatching {
            when (state.phase) {
                TRANSACTION_PHASE_PREPARED -> {
                    if (state.hadReplacement) {
                        if (replacementBackup.exists()) {
                            if (finalDirectory.exists()) {
                                check(finalDirectory.deleteRecursively()) {
                                    "Could not remove uncommitted TTS replacement"
                                }
                            }
                            moveDirectory(replacementBackup, finalDirectory)
                        } else {
                            check(finalDirectory.exists()) {
                                "Interrupted TTS replacement lost both active model and backup"
                            }
                        }
                    } else if (finalDirectory.exists()) {
                        check(finalDirectory.deleteRecursively()) {
                            "Could not remove uncommitted TTS model"
                        }
                    }

                    evictionRoot.listFiles()
                        .orEmpty()
                        .sortedBy { it.name }
                        .forEach { reserved ->
                            val destination = safeRootChild(reserved.name)
                                ?: error("Unsafe TTS eviction recovery path")
                            check(!destination.exists()) {
                                "TTS eviction recovery destination already exists"
                            }
                            moveDirectory(reserved, destination)
                        }
                }

                TRANSACTION_PHASE_PUBLISHED -> {
                    check(finalDirectory.isDirectory) {
                        "Published TTS transaction lost final model"
                    }
                }

                else -> error("Unknown TTS transaction phase")
            }

            check(cleanupPublishedTransaction(transaction)) {
                "Could not clean TTS install transaction"
            }
            true
        }.getOrDefault(false)
    }

    private fun cleanupPublishedTransaction(transaction: File): Boolean {
        if (!transaction.exists()) return true
        val stateFile = File(transaction, TRANSACTION_STATE_NAME)
        val stateTemp = File(transaction, TRANSACTION_STATE_TEMP_NAME)

        val payloadClean = transaction.listFiles()
            .orEmpty()
            .filter { it != stateFile && it != stateTemp }
            .all { entry ->
                if (entry.isDirectory) entry.deleteRecursively() else entry.delete()
            }
        if (!payloadClean) return false
        if (stateTemp.exists() && !stateTemp.delete()) return false
        if (stateFile.exists() && !stateFile.delete()) return false
        return transaction.delete() || !transaction.exists()
    }

    private fun safeRootChild(name: String): File? {
        if (
            name.isBlank() ||
            name == "." ||
            name == ".." ||
            name.contains('/') ||
            name.contains('\\') ||
            name.indexOf('\u0000') >= 0
        ) {
            return null
        }
        val canonicalRoot = runCatching { root.canonicalFile }.getOrNull() ?: return null
        val child = runCatching { File(canonicalRoot, name).canonicalFile }.getOrNull()
            ?: return null
        return child.takeIf { it.parentFile == canonicalRoot }
    }

    private fun writeTransactionState(
        transaction: File,
        state: InstallTransactionState
    ) {
        val temporary = File(transaction, TRANSACTION_STATE_TEMP_NAME)
        val destination = File(transaction, TRANSACTION_STATE_NAME)
        val stateText = buildString {
            append("phase=").append(state.phase).append('\n')
            append("final=").append(state.finalName).append('\n')
            append("had_replacement=").append(state.hadReplacement).append('\n')
        }
        FileOutputStream(temporary).use { output ->
            output.write(stateText.toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
        }
        try {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun readTransactionState(
        transaction: File
    ): InstallTransactionState? {
        val values = File(transaction, TRANSACTION_STATE_NAME)
            .takeIf(File::isFile)
            ?.readLines()
            ?.mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null
                else line.substring(0, separator) to line.substring(separator + 1)
            }
            ?.toMap()
            ?: return null
        val phase = values["phase"] ?: return null
        val finalName = values["final"] ?: return null
        val hadReplacement = when (values["had_replacement"]) {
            "true" -> true
            "false" -> false
            else -> return null
        }
        return InstallTransactionState(
            phase = phase,
            finalName = finalName,
            hadReplacement = hadReplacement
        )
    }

    private fun moveDirectory(source: File, destination: File) {
        check(source.exists()) { "TTS transaction source is missing" }
        check(!destination.exists()) { "TTS transaction destination already exists" }
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath())
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
            val bytes = modelBytes(candidate)
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
                appendLine("max_expanded_bytes=${safe.maxExpandedBytes}")
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

    private fun modelBytes(directory: File): Long {
        var bytes = File(directory, ARCHIVE_NAME)
            .takeIf(File::isFile)
            ?.length()
            ?: 0L

        File(directory, PAYLOAD_DIRECTORY)
            .takeIf(File::isDirectory)
            ?.walkTopDown()
            ?.filter(File::isFile)
            ?.forEach { bytes += it.length() }

        directory.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith(PAYLOAD_STAGING_PREFIX) }
            .forEach { staging ->
                staging.walkTopDown()
                    .filter(File::isFile)
                    .forEach { bytes += it.length() }
            }

        return bytes
    }

    private data class InstallTransactionState(
        val phase: String,
        val finalName: String,
        val hadReplacement: Boolean
    )

    private companion object {
        const val ARCHIVE_NAME = "model.package"
        const val METADATA_NAME = "model.meta"
        const val LAST_USED_NAME = "last_used"
        const val PAYLOAD_DIRECTORY = "payload"
        const val PAYLOAD_STAGING_PREFIX = ".payload-staging-"
        const val INSTALL_TRANSACTION_PREFIX = ".install-txn-"
        const val TRANSACTION_STAGED_DIRECTORY = "staged"
        const val TRANSACTION_REPLACEMENT_DIRECTORY = "replacement"
        const val TRANSACTION_EVICTIONS_DIRECTORY = "evictions"
        const val TRANSACTION_STATE_NAME = "state"
        const val TRANSACTION_STATE_TEMP_NAME = "state.tmp"
        const val TRANSACTION_PHASE_PREPARED = "prepared"
        const val TRANSACTION_PHASE_PUBLISHED = "published"
        const val DEFAULT_BUDGET_BYTES = 1024L * 1024L * 1024L
    }
}
