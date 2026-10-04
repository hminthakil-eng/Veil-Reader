package com.veilreader.app.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadiumEngineStorageTest {

    @Test
    fun `completed import becomes visible only after staging install`() {
        val root = Files.createTempDirectory("veil-import").toFile()
        try {
            val staging = File(root, ".book.importing")
            val target = File(root, "book.epub")
            val bytes = ByteArray(257) { index -> (index and 0xFF).toByte() }
            staging.writeBytes(bytes)

            assertFalse(target.exists())
            installCompletedImport(staging, target)

            assertFalse(staging.exists())
            assertTrue(target.isFile)
            assertArrayEquals(bytes, target.readBytes())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `staging install rejects cross directory moves`() {
        val root = Files.createTempDirectory("veil-import-cross").toFile()
        val other = Files.createTempDirectory("veil-import-other").toFile()
        try {
            val staging = File(root, ".book.importing").apply {
                writeText("publication")
            }
            installCompletedImport(staging, File(other, "book.epub"))
        } finally {
            root.deleteRecursively()
            other.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `staging install refuses to replace an existing publication`() {
        val root = Files.createTempDirectory("veil-import-existing").toFile()
        try {
            val staging = File(root, ".book.importing").apply {
                writeText("new")
            }
            val target = File(root, "book.epub").apply {
                writeText("owned")
            }
            installCompletedImport(staging, target)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty staging file cannot become a publication`() {
        val root = Files.createTempDirectory("veil-import-empty").toFile()
        try {
            val staging = File(root, ".book.importing").apply {
                createNewFile()
            }
            installCompletedImport(staging, File(root, "book.epub"))
        } finally {
            root.deleteRecursively()
        }
    }
}
