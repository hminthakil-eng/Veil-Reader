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

    @Test
    fun `orphan reconciliation deletes only old unowned direct files`() {
        val root = Files.createTempDirectory("veil-orphans").toFile()
        try {
            val now = 2_000_000L
            val grace = 100_000L
            val owned = File(root, "owned.epub").apply {
                writeText("owned")
                setLastModified(now - grace * 2)
            }
            val oldOrphan = File(root, "orphan.epub").apply {
                writeText("orphan")
                setLastModified(now - grace * 2)
            }
            val recentOrphan = File(root, "recent.epub").apply {
                writeText("recent")
                setLastModified(now - grace / 2)
            }
            val futureOrphan = File(root, "future.epub").apply {
                writeText("future")
                setLastModified(now + 1_000L)
            }
            val nested = File(root, "nested").apply { mkdirs() }
            val nestedOrphan = File(nested, "nested.epub").apply {
                writeText("nested")
                setLastModified(now - grace * 2)
            }

            val deleted = reconcileOrphanedArtifactDirectory(
                root = root,
                ownedCanonicalPaths = setOf(owned.canonicalPath),
                nowEpochMs = now,
                graceMillis = grace
            )

            assertTrue(deleted == 1)
            assertTrue(owned.exists())
            assertFalse(oldOrphan.exists())
            assertTrue(recentOrphan.exists())
            assertTrue(futureOrphan.exists())
            assertTrue(nestedOrphan.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `artifact ownership rejects paths outside managed root`() {
        val root = Files.createTempDirectory("veil-owned-root").toFile()
        val outsideRoot = Files.createTempDirectory("veil-owned-outside").toFile()
        try {
            val inside = File(root, "inside.epub").apply { writeText("inside") }
            val outside = File(outsideRoot, "outside.epub").apply { writeText("outside") }

            assertTrue(
                canonicalOwnedArtifactPath(inside.absolutePath, root) ==
                    inside.canonicalPath
            )
            assertTrue(
                canonicalOwnedArtifactPath(outside.absolutePath, root) == null
            )
            assertTrue(canonicalOwnedArtifactPath(null, root) == null)
        } finally {
            root.deleteRecursively()
            outsideRoot.deleteRecursively()
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
