package com.veilreader.app.manga.storage

import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaOfflineStore
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaProgressStore
import com.veilreader.app.manga.core.MangaReadingProgress
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaResourceRequest
import java.io.File
import java.security.MessageDigest
import java.util.Properties

fun interface MangaBinaryFetcher {
    suspend fun fetch(request: MangaResourceRequest): ByteArray
}

class FileMangaOfflineStore(
    private val root: File,
    private val fetcher: MangaBinaryFetcher
) : MangaOfflineStore {
    override suspend fun isChapterAvailable(ref: MangaChapterRef): Boolean {
        val dir = chapterDir(ref)
        val expected = dir.resolve(COMPLETE_FILE).takeIf(File::isFile)
            ?.readText()?.trim()?.toIntOrNull()
            ?: return false
        val actual = dir.listFiles { file -> file.extension == PAGE_EXTENSION }?.size ?: 0
        return expected > 0 && expected == actual
    }

    override suspend fun loadChapter(ref: MangaChapterRef): List<MangaPage>? {
        if (!isChapterAvailable(ref)) return null
        return chapterDir(ref)
            .listFiles { file -> file.extension == PAGE_EXTENSION }
            .orEmpty()
            .sortedBy(File::getName)
            .mapIndexed { index, file ->
                MangaPage(
                    index = index,
                    image = MangaResourceRequest(file.toURI().toString())
                )
            }
    }

    override suspend fun saveChapter(ref: MangaChapterRef, pages: List<MangaPage>) {
        require(pages.isNotEmpty()) { "Cannot save an empty manga chapter." }
        val ordered = pages.sortedBy(MangaPage::index)
        require(ordered.map(MangaPage::index).distinct().size == ordered.size) {
            "Cannot save duplicate manga page indices."
        }

        root.mkdirs()
        val finalDir = chapterDir(ref)
        val tempDir = root.resolve(".${chapterKey(ref)}.tmp")
        tempDir.deleteRecursively()
        check(tempDir.mkdirs()) { "Could not create manga offline temp directory." }

        try {
            ordered.forEachIndexed { ordinal, page ->
                val bytes = fetcher.fetch(page.image)
                require(bytes.isNotEmpty()) { "Downloaded manga page was empty." }
                tempDir.resolve(pageName(ordinal)).writeBytes(bytes)
            }
            tempDir.resolve(COMPLETE_FILE).writeText(ordered.size.toString())

            finalDir.deleteRecursively()
            check(tempDir.renameTo(finalDir)) { "Could not finalize offline manga chapter." }
        } catch (error: Throwable) {
            tempDir.deleteRecursively()
            throw error
        }
    }

    override suspend fun removeChapter(ref: MangaChapterRef) {
        chapterDir(ref).deleteRecursively()
    }

    private fun chapterDir(ref: MangaChapterRef): File = root.resolve(chapterKey(ref))

    private fun chapterKey(ref: MangaChapterRef): String = hash(
        ref.manga.sourceId.value + "\n" + ref.manga.key + "\n" + ref.key
    )

    private fun pageName(index: Int): String = "%05d.%s".format(index, PAGE_EXTENSION)

    private companion object {
        const val COMPLETE_FILE = "complete.txt"
        const val PAGE_EXTENSION = "img"
    }
}

class FileMangaProgressStore(
    private val root: File
) : MangaProgressStore {
    override suspend fun load(manga: MangaRef): MangaReadingProgress? {
        val file = progressFile(manga)
        if (!file.isFile) return null

        val properties = Properties().apply { file.inputStream().use(::load) }
        if (properties.getProperty("sourceId") != manga.sourceId.value) return null
        if (properties.getProperty("mangaKey") != manga.key) return null

        val chapterKey = properties.getProperty("chapterKey") ?: return null
        val pageIndex = properties.getProperty("pageIndex")?.toIntOrNull() ?: return null
        val updatedAt = properties.getProperty("updatedAtEpochMs")?.toLongOrNull() ?: return null

        return MangaReadingProgress(
            manga = manga,
            chapter = MangaChapterRef(manga, chapterKey),
            pageIndex = pageIndex,
            updatedAtEpochMs = updatedAt
        )
    }

    override suspend fun save(progress: MangaReadingProgress) {
        root.mkdirs()
        val file = progressFile(progress.manga)
        val temp = File(file.parentFile, file.name + ".tmp")
        val properties = Properties().apply {
            setProperty("sourceId", progress.manga.sourceId.value)
            setProperty("mangaKey", progress.manga.key)
            setProperty("chapterKey", progress.chapter.key)
            setProperty("pageIndex", progress.pageIndex.toString())
            setProperty("updatedAtEpochMs", progress.updatedAtEpochMs.toString())
        }

        temp.outputStream().use { properties.store(it, null) }
        if (file.exists()) check(file.delete()) { "Could not replace manga progress." }
        check(temp.renameTo(file)) { "Could not finalize manga progress." }
    }

    private fun progressFile(manga: MangaRef): File =
        root.resolve(hash(manga.sourceId.value + "\n" + manga.key) + ".properties")
}

private fun hash(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }