package com.veilreader.app.ui.reader.tts

import java.io.File
import java.security.MessageDigest
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsModelStoreTest {
    @Test
    fun installPublishesOnlyVerifiedBytesAndCleansCrashStaging() {
        val root = createTempDirectory("veil-tts-store").toFile()
        val download = createTempDirectory("veil-tts-download").toFile()
        try {
            File(root, ".staging-dead").mkdirs()
            val bytes = ByteArray(2_048) { index -> (index % 251).toByte() }
            val archive = File(download, "model.bin").apply { writeBytes(bytes) }
            val model = packageFor("fa-primary", bytes)
            val store = ReaderTtsModelStore(root, budgetBytes = 8_192)

            val result = store.installVerified(archive, model, nowEpochMs = 10L)
            assertTrue(result is ReaderTtsModelInstallResult.Installed)
            val installed = (result as ReaderTtsModelInstallResult.Installed).model

            assertTrue(installed.directory.isDirectory)
            assertEquals(bytes.size.toLong(), installed.archive.length())
            assertEquals(bytes.size.toLong(), store.usedBytes())
            assertFalse(File(root, ".staging-dead").exists())
            assertTrue(File(installed.directory, "model.meta").readText().contains("Apache-2.0"))
        } finally {
            root.deleteRecursively()
            download.deleteRecursively()
        }
    }

    @Test
    fun tamperedDownloadIsRejectedBeforeAnythingIsPublished() {
        val root = createTempDirectory("veil-tts-store").toFile()
        val download = createTempDirectory("veil-tts-download").toFile()
        try {
            val expected = ByteArray(2_048) { 7 }
            val actual = ByteArray(2_048) { 8 }
            val archive = File(download, "model.bin").apply { writeBytes(actual) }
            val result = ReaderTtsModelStore(root, budgetBytes = 8_192)
                .installVerified(archive, packageFor("fa-tamper", expected), nowEpochMs = 20L)

            assertEquals(
                ReaderTtsModelInstallResult.Rejected(
                    ReaderTtsModelInstallResult.Reason.VERIFICATION_FAILED
                ),
                result
            )
            assertEquals(0L, ReaderTtsModelStore(root, 8_192).usedBytes())
        } finally {
            root.deleteRecursively()
            download.deleteRecursively()
        }
    }

    @Test
    fun leastRecentlyUsedModelIsEvictedWhenBudgetNeedsSpace() {
        val root = createTempDirectory("veil-tts-store").toFile()
        val download = createTempDirectory("veil-tts-download").toFile()
        try {
            val firstBytes = ByteArray(2_048) { 1 }
            val secondBytes = ByteArray(2_048) { 2 }
            val thirdBytes = ByteArray(2_048) { 3 }
            val store = ReaderTtsModelStore(root, budgetBytes = 4_096)

            val first = install(store, download, "first", firstBytes, 10L)
            val second = install(store, download, "second", secondBytes, 20L)
            assertTrue(store.touch(second, nowEpochMs = 30L))

            val third = install(store, download, "third", thirdBytes, 40L)

            assertFalse(first.directory.exists())
            assertTrue(second.directory.exists())
            assertTrue(third.directory.exists())
            assertEquals(4_096L, store.usedBytes())
        } finally {
            root.deleteRecursively()
            download.deleteRecursively()
        }
    }

    @Test
    fun singleModelLargerThanBudgetIsRejectedWithoutEvictingExistingModel() {
        val root = createTempDirectory("veil-tts-store").toFile()
        val download = createTempDirectory("veil-tts-download").toFile()
        try {
            val store = ReaderTtsModelStore(root, budgetBytes = 4_096)
            val existing = install(store, download, "existing", ByteArray(2_048) { 4 }, 10L)
            val tooLarge = ByteArray(5_120) { 5 }
            val file = File(download, "too-large.bin").apply { writeBytes(tooLarge) }

            val result = store.installVerified(
                file,
                packageFor("too-large", tooLarge),
                nowEpochMs = 20L
            )

            assertEquals(
                ReaderTtsModelInstallResult.Rejected(
                    ReaderTtsModelInstallResult.Reason.STORAGE_BUDGET
                ),
                result
            )
            assertTrue(existing.directory.exists())
            assertEquals(2_048L, store.usedBytes())
        } finally {
            root.deleteRecursively()
            download.deleteRecursively()
        }
    }

    @Test
    fun preparedCrashTransactionRollsBackPublishedReplacementAndRestoresEvictions() {
        val root = createTempDirectory("veil-tts-store").toFile()
        val download = createTempDirectory("veil-tts-download").toFile()
        try {
            val store = ReaderTtsModelStore(root, budgetBytes = 8_192)
            val oldBytes = ByteArray(2_048) { 11 }
            val victimBytes = ByteArray(2_048) { 12 }
            val old = install(store, download, "replace-me", oldBytes, 10L)
            val victim = install(store, download, "victim", victimBytes, 20L)

            val transaction = File(
                root,
                ".install-txn-replace-me-1.0-99"
            ).apply { mkdirs() }
            File(transaction, "state").writeText(
                "phase=prepared\n" +
                    "final=" + old.directory.name + "\n" +
                    "had_replacement=true\n"
            )

            Files.move(
                old.directory.toPath(),
                File(transaction, "replacement").toPath()
            )
            val evictions = File(transaction, "evictions").apply { mkdirs() }
            Files.move(
                victim.directory.toPath(),
                File(evictions, victim.directory.name).toPath()
            )

            val uncommitted = File(root, old.directory.name).apply { mkdirs() }
            File(uncommitted, "model.package").writeBytes(ByteArray(2_048) { 99 })

            assertTrue(store.recoverInterruptedTransactions())
            assertFalse(transaction.exists())
            assertTrue(old.directory.isDirectory)
            assertTrue(victim.directory.isDirectory)
            assertArrayEquals(
                oldBytes,
                File(old.directory, "model.package").readBytes()
            )
            assertArrayEquals(
                victimBytes,
                File(victim.directory, "model.package").readBytes()
            )
        } finally {
            root.deleteRecursively()
            download.deleteRecursively()
        }
    }

    @Test
    fun publishedCrashTransactionKeepsNewModelAndDiscardsRollbackCopies() {
        val root = createTempDirectory("veil-tts-store").toFile()
        val download = createTempDirectory("veil-tts-download").toFile()
        try {
            val store = ReaderTtsModelStore(root, budgetBytes = 8_192)
            val activeBytes = ByteArray(2_048) { 21 }
            val active = install(store, download, "active", activeBytes, 10L)

            val transaction = File(
                root,
                ".install-txn-active-1.0-100"
            ).apply { mkdirs() }
            File(transaction, "state").writeText(
                "phase=published\n" +
                    "final=" + active.directory.name + "\n" +
                    "had_replacement=true\n"
            )
            File(transaction, "replacement").apply {
                mkdirs()
                resolve("model.package").writeBytes(ByteArray(2_048) { 22 })
            }
            File(transaction, "evictions").resolve("old-victim").apply {
                mkdirs()
                resolve("model.package").writeBytes(ByteArray(2_048) { 23 })
            }

            assertTrue(store.recoverInterruptedTransactions())
            assertTrue(active.directory.isDirectory)
            assertArrayEquals(
                activeBytes,
                File(active.directory, "model.package").readBytes()
            )
            assertFalse(transaction.exists())
            assertFalse(File(root, "old-victim").exists())
        } finally {
            root.deleteRecursively()
            download.deleteRecursively()
        }
    }

    private fun install(
        store: ReaderTtsModelStore,
        download: File,
        id: String,
        bytes: ByteArray,
        time: Long
    ): ReaderTtsInstalledModel {
        val file = File(download, "$id.bin").apply { writeBytes(bytes) }
        return (
            store.installVerified(file, packageFor(id, bytes), time)
                as ReaderTtsModelInstallResult.Installed
            ).model
    }

    private fun packageFor(id: String, bytes: ByteArray): ReaderTtsModelPackage =
        ReaderTtsModelPackage(
            id = id,
            version = "1.0",
            languageTag = "fa-IR",
            displayName = id,
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
