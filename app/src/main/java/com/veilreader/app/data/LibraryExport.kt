package com.veilreader.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.room.withTransaction
import com.veilreader.app.data.db.MangaMergeChapterEntity
import com.veilreader.app.data.db.MangaMergeMemberEntity
import com.veilreader.app.data.db.MangaWorkMergeEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.manga.MangaLocalChapterMetadata
import com.veilreader.app.data.manga.MangaLocalImportCoordinator
import com.veilreader.app.data.manga.MangaLocalRestorePoint
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.PassageVisit
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingHistoryEvent
import com.veilreader.app.domain.ReadingHistoryEventKind
import com.veilreader.app.domain.ReadingMilestoneKind
import com.veilreader.app.domain.ReadingMilestoneRecord
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.manga.importing.MangaCbzIngestor
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** User-initiated local backup/export. No server or account is involved. */
class LibraryExport(
    private val context: Context,
    private val library: LocalLibraryRepository,
    private val database: VeilDatabase = VeilDatabase.get(context.applicationContext)
) {
    private val mangaImporter by lazy {
        MangaLocalImportCoordinator(context.applicationContext, library, database)
    }

    private data class MangaLocalChapterSnapshot(
        val bookId: String,
        val readingOrder: Int,
        val chapterKey: String,
        val metadata: MangaLocalChapterMetadata,
        val archiveFile: File
    )

    private data class MangaBackupArchive(
        val chapter: MangaLocalChapterSnapshot,
        val archivePath: String
    )

    private data class MangaMergeBackupSnapshot(
        val id: String,
        val targetBookId: String,
        val createdAtEpochMs: Long,
        val receiptVersion: Int,
        val targetBookProgress: Float,
        val targetBookFinished: Boolean,
        val targetBookLastOpenedAtEpochMs: Long,
        val targetProgressReadingOrder: Int?,
        val targetProgressPageIndex: Int?,
        val targetProgressPageCount: Int?,
        val targetProgressChapterProgression: Double?,
        val targetProgressUpdatedAtEpochMs: Long?,
        val members: List<MangaMergeMemberBackup>,
        val chapters: List<MangaMergeChapterBackup>
    )

    private data class MangaMergeMemberBackup(
        val sourceBookId: String,
        val sourceOrder: Int
    )

    private data class MangaMergeChapterBackup(
        val sourceBookId: String,
        val sourceReadingOrder: Int,
        val targetReadingOrder: Int,
        val disposition: String
    )

    suspend fun writeNotebook(destination: Uri) {
        val snapshot = library.snapshot()
        val books = snapshot.books.associateBy { it.id }
        val text = buildString {
            appendLine("# Veil Reader notebook")
            snapshot.highlights.groupBy { it.bookId }.forEach { (id, passages) ->
                appendLine(); appendLine("## ${books[id]?.title ?: "Unknown book"}")
                appendLine(books[id]?.author.orEmpty())
                passages.forEach { passage ->
                    appendLine()
                    passage.quote.lines().forEach { appendLine("> $it") }
                    if (passage.note.isNotBlank()) { appendLine(); appendLine(passage.note) }
                    appendLine(); appendLine("---")
                }
            }
            if (snapshot.highlights.isEmpty()) appendLine("No highlights saved yet.")
        }
        withContext(Dispatchers.IO) {
            val output = context.contentResolver.openOutputStream(destination, "wt")
                ?: error("Could not create the notebook file.")
            output.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
        }
    }

    suspend fun writeBackup(destination: Uri) {
        val snapshot = library.snapshot()
        val mangaProgress = captureMangaRestorePoints(snapshot.books)
        val mangaLocalChapters = captureMangaLocalChapters(snapshot.books)
        val mangaMerges = captureMangaMergeBackups()
        val gamePrefs = preferencesToJson(context.getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE))
        withContext(Dispatchers.IO) {
            val files = snapshot.books.filter { it.isImported }.mapIndexed { index, book ->
                val uri = Uri.parse(requireNotNull(book.sourceUri))
                require(uri.scheme == "file") { "Cannot back up ${book.title}: unsupported file location." }
                val file = File(requireNotNull(uri.path)).canonicalFile
                val allowed = File(context.filesDir, "publications").canonicalFile
                require(file.toPath().startsWith(allowed.toPath()) && file.isFile) {
                    "Cannot back up ${book.title}: the imported file is missing."
                }
                Triple(
                    book,
                    file,
                    "books/" + (index + 1) + "." + backupExtension(book)
                )
            }
            val primaryArchivePaths = files.associate { (book, _, path) -> book.id to path }
            val bookIndexes = snapshot.books.mapIndexed { index, book -> book.id to index }.toMap()
            val mangaArchives = mangaLocalChapters.map { chapter ->
                val archivePath = if (chapter.readingOrder == 0) {
                    requireNotNull(primaryArchivePaths[chapter.bookId]) {
                        "Primary Manga archive is missing from backup publications."
                    }
                } else {
                    val bookIndex = requireNotNull(bookIndexes[chapter.bookId])
                    "books/manga/" + (bookIndex + 1) + "/" +
                        (chapter.readingOrder + 1) + ".cbz"
                }
                MangaBackupArchive(chapter, archivePath)
            }

            val manifest = JSONObject().apply {
                put("schemaVersion", CURRENT_BACKUP_SCHEMA)
                put("appVersion", "0.10.0")
                put("createdAtEpochMs", System.currentTimeMillis())
                put("library", snapshot.toJson())
                put("mangaProgress", mangaRestorePointsToJson(mangaProgress))
                put("mangaLocalChapters", mangaLocalChaptersToJson(mangaArchives))
                put("mangaMerges", mangaMergeBackupsToJson(mangaMerges))
                put("gamePreferences", gamePrefs)
                put("publications", JSONArray().apply {
                    files.forEach { (book, _, path) -> put(JSONObject().apply {
                        put("bookId", book.id)
                        put("title", book.title)
                        put("author", book.author)
                        put("archivePath", path)
                    }) }
                })
            }
            val output = context.contentResolver.openOutputStream(destination, "wt")
                ?: error("Could not create the backup file.")
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("README.txt"))
                val readme = "Veil Reader local backup. Restore it from Profile > Your data > Restore library backup. " +
                    "The archive can contain private books, highlights, notes and reading state; keep it private.\n"
                zip.write(readme.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                files.forEach { (_, file, path) ->
                    zip.putNextEntry(ZipEntry(path))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                mangaArchives
                    .filter { it.chapter.readingOrder > 0 }
                    .forEach { archived ->
                        zip.putNextEntry(ZipEntry(archived.archivePath))
                        archived.chapter.archiveFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
            }
        }
    }

    private suspend fun captureMangaMergeBackups(): List<MangaMergeBackupSnapshot> =
        database.mangaMerges().listAll().map { receipt ->
            val merge = receipt.merge
            val targetProgressReadingOrder = merge.targetProgressChapterId?.let { chapterId ->
                val chapter = database.mangaCatalog().findChapter(chapterId)
                    ?: error("Manga merge receipt points to a missing target progress chapter")
                require(chapter.bookId == merge.targetBookId) {
                    "Manga merge target progress chapter belongs to another Book"
                }
                chapter.readingOrder
            }
            MangaMergeBackupSnapshot(
                id = merge.id,
                targetBookId = merge.targetBookId,
                createdAtEpochMs = merge.createdAtEpochMs,
                receiptVersion = merge.receiptVersion,
                targetBookProgress = merge.targetBookProgress,
                targetBookFinished = merge.targetBookFinished,
                targetBookLastOpenedAtEpochMs = merge.targetBookLastOpenedAtEpochMs,
                targetProgressReadingOrder = targetProgressReadingOrder,
                targetProgressPageIndex = merge.targetProgressPageIndex,
                targetProgressPageCount = merge.targetProgressPageCount,
                targetProgressChapterProgression = merge.targetProgressChapterProgression,
                targetProgressUpdatedAtEpochMs = merge.targetProgressUpdatedAtEpochMs,
                members = receipt.members
                    .sortedBy { it.sourceOrder }
                    .map {
                        MangaMergeMemberBackup(
                            sourceBookId = it.sourceBookId,
                            sourceOrder = it.sourceOrder
                        )
                    },
                chapters = receipt.chapters
                    .sortedWith(
                        compareBy<MangaMergeChapterEntity> { it.sourceBookId }
                            .thenBy { it.sourceReadingOrder }
                    )
                    .map {
                        MangaMergeChapterBackup(
                            sourceBookId = it.sourceBookId,
                            sourceReadingOrder = it.sourceReadingOrder,
                            targetReadingOrder = it.targetReadingOrder,
                            disposition = it.disposition
                        )
                    }
            )
        }

    private fun mangaMergeBackupsToJson(
        merges: List<MangaMergeBackupSnapshot>
    ): JSONArray = JSONArray().apply {
        merges.sortedBy { it.id }.forEach { merge ->
            put(JSONObject().apply {
                put("id", merge.id)
                put("targetBookId", merge.targetBookId)
                put("createdAtEpochMs", merge.createdAtEpochMs)
                put("receiptVersion", merge.receiptVersion)
                put("targetBookProgress", merge.targetBookProgress.toDouble())
                put("targetBookFinished", merge.targetBookFinished)
                put(
                    "targetBookLastOpenedAtEpochMs",
                    merge.targetBookLastOpenedAtEpochMs
                )
                put(
                    "targetProgressReadingOrder",
                    merge.targetProgressReadingOrder ?: JSONObject.NULL
                )
                put(
                    "targetProgressPageIndex",
                    merge.targetProgressPageIndex ?: JSONObject.NULL
                )
                put(
                    "targetProgressPageCount",
                    merge.targetProgressPageCount ?: JSONObject.NULL
                )
                put(
                    "targetProgressChapterProgression",
                    merge.targetProgressChapterProgression ?: JSONObject.NULL
                )
                put(
                    "targetProgressUpdatedAtEpochMs",
                    merge.targetProgressUpdatedAtEpochMs ?: JSONObject.NULL
                )
                put("members", JSONArray().apply {
                    merge.members.forEach { member ->
                        put(JSONObject().apply {
                            put("sourceBookId", member.sourceBookId)
                            put("sourceOrder", member.sourceOrder)
                        })
                    }
                })
                put("chapters", JSONArray().apply {
                    merge.chapters.forEach { chapter ->
                        put(JSONObject().apply {
                            put("sourceBookId", chapter.sourceBookId)
                            put("sourceReadingOrder", chapter.sourceReadingOrder)
                            put("targetReadingOrder", chapter.targetReadingOrder)
                            put("disposition", chapter.disposition)
                        })
                    }
                })
            })
        }
    }

    private fun parseMangaMergeBackups(
        manifest: JSONObject
    ): List<MangaMergeBackupSnapshot> {
        val records = manifest.optJSONArray("mangaMerges") ?: return emptyList()
        val mergeIds = mutableSetOf<String>()
        val targetIds = mutableSetOf<String>()
        val sourceIds = mutableSetOf<String>()
        return buildList {
            for (index in 0 until records.length()) {
                val record = records.getJSONObject(index)
                val id = record.getString("id")
                val targetBookId = record.getString("targetBookId")
                require(id.isNotBlank() && targetBookId.isNotBlank()) {
                    "Invalid Manga merge backup identity."
                }
                require(mergeIds.add(id)) { "Backup contains duplicate Manga merge ids." }
                require(targetIds.add(targetBookId)) {
                    "Backup contains more than one active Manga merge for a target Book."
                }

                val membersJson = record.getJSONArray("members")
                val members = buildList {
                    val orders = mutableSetOf<Int>()
                    for (memberIndex in 0 until membersJson.length()) {
                        val member = membersJson.getJSONObject(memberIndex)
                        val sourceBookId = member.getString("sourceBookId")
                        val sourceOrder = member.getInt("sourceOrder")
                        require(sourceBookId.isNotBlank() && sourceOrder >= 0)
                        require(sourceBookId != targetBookId)
                        require(sourceIds.add(sourceBookId)) {
                            "A source Manga Book belongs to more than one merge in this backup."
                        }
                        require(orders.add(sourceOrder)) {
                            "Manga merge backup contains duplicate source order."
                        }
                        add(MangaMergeMemberBackup(sourceBookId, sourceOrder))
                    }
                }.sortedBy { it.sourceOrder }
                require(members.isNotEmpty()) { "Manga merge backup has no source Books." }
                members.forEachIndexed { expected, member ->
                    require(member.sourceOrder == expected) {
                        "Manga merge source order must be contiguous."
                    }
                }

                val chapterJson = record.getJSONArray("chapters")
                val chapterKeys = mutableSetOf<Pair<String, Int>>()
                val chapters = buildList {
                    for (chapterIndex in 0 until chapterJson.length()) {
                        val chapter = chapterJson.getJSONObject(chapterIndex)
                        val sourceBookId = chapter.getString("sourceBookId")
                        val sourceReadingOrder = chapter.getInt("sourceReadingOrder")
                        val targetReadingOrder = chapter.getInt("targetReadingOrder")
                        val disposition = chapter.getString("disposition")
                        require(
                            sourceBookId.isNotBlank() &&
                                sourceReadingOrder >= 0 &&
                                targetReadingOrder >= 0
                        )
                        require(members.any { it.sourceBookId == sourceBookId }) {
                            "Manga merge chapter references a non-member source Book."
                        }
                        require(
                            disposition in MangaMergeChapterEntity.VALID_DISPOSITIONS
                        ) {
                            "Manga merge backup contains an unsupported chapter disposition."
                        }
                        require(chapterKeys.add(sourceBookId to sourceReadingOrder)) {
                            "Manga merge backup contains a duplicate source chapter mapping."
                        }
                        add(
                            MangaMergeChapterBackup(
                                sourceBookId = sourceBookId,
                                sourceReadingOrder = sourceReadingOrder,
                                targetReadingOrder = targetReadingOrder,
                                disposition = disposition
                            )
                        )
                    }
                }
                require(chapters.isNotEmpty()) { "Manga merge backup has no chapter mappings." }

                val progressOrder = if (record.isNull("targetProgressReadingOrder")) {
                    null
                } else {
                    record.getInt("targetProgressReadingOrder")
                }
                val progressPageIndex = if (record.isNull("targetProgressPageIndex")) {
                    null
                } else {
                    record.getInt("targetProgressPageIndex")
                }
                val progressPageCount = if (record.isNull("targetProgressPageCount")) {
                    null
                } else {
                    record.getInt("targetProgressPageCount")
                }
                val progressChapterProgression =
                    if (record.isNull("targetProgressChapterProgression")) {
                        null
                    } else {
                        record.getDouble("targetProgressChapterProgression")
                    }
                val progressUpdatedAt =
                    if (record.isNull("targetProgressUpdatedAtEpochMs")) {
                        null
                    } else {
                        record.getLong("targetProgressUpdatedAtEpochMs")
                    }

                require(
                    if (progressOrder == null) {
                        progressPageIndex == null &&
                            progressPageCount == null &&
                            progressChapterProgression == null &&
                            progressUpdatedAt == null
                    } else {
                        progressOrder >= 0 &&
                            progressPageIndex != null &&
                            progressPageIndex >= 0 &&
                            (progressPageCount == null || progressPageCount > 0) &&
                            progressChapterProgression != null &&
                            progressChapterProgression.isFinite() &&
                            progressChapterProgression in 0.0..1.0 &&
                            progressUpdatedAt != null
                    }
                ) {
                    "Manga merge backup contains an incomplete target progress receipt."
                }

                add(
                    MangaMergeBackupSnapshot(
                        id = id,
                        targetBookId = targetBookId,
                        createdAtEpochMs = record.getLong("createdAtEpochMs"),
                        receiptVersion = record.optInt("receiptVersion", 1).coerceAtLeast(1),
                        targetBookProgress = record
                            .getDouble("targetBookProgress")
                            .toFloat()
                            .coerceIn(0f, 1f),
                        targetBookFinished = record.getBoolean("targetBookFinished"),
                        targetBookLastOpenedAtEpochMs =
                            record.getLong("targetBookLastOpenedAtEpochMs").coerceAtLeast(0L),
                        targetProgressReadingOrder = progressOrder,
                        targetProgressPageIndex = progressPageIndex,
                        targetProgressPageCount = progressPageCount,
                        targetProgressChapterProgression = progressChapterProgression,
                        targetProgressUpdatedAtEpochMs = progressUpdatedAt,
                        members = members,
                        chapters = chapters
                    )
                )
            }
        }
    }

    private suspend fun restoreMangaMergeBackups(
        merges: List<MangaMergeBackupSnapshot>
    ) {
        for (backup in merges) {
            val target = database.books().findEntity(backup.targetBookId)
                ?: error("Restored Manga merge target Book is missing")
            require(target.format == BookFormat.COMIC.name) {
                "Restored Manga merge target is not a comic"
            }
            val targetChapters = database.mangaCatalog()
                .listChapters(backup.targetBookId)
                .associateBy { it.readingOrder }
            val progressChapterId = backup.targetProgressReadingOrder?.let { order ->
                targetChapters[order]?.id
                    ?: error("Restored Manga merge target progress chapter is missing")
            }

            val members = backup.members.map { member ->
                val source = database.books().findEntity(member.sourceBookId)
                    ?: error("Restored Manga merge source Book is missing")
                require(source.format == BookFormat.COMIC.name)
                MangaMergeMemberEntity(
                    mergeId = backup.id,
                    sourceBookId = member.sourceBookId,
                    sourceOrder = member.sourceOrder
                )
            }
            val mappings = backup.chapters.map { chapter ->
                val sourceChapter = database.mangaCatalog()
                    .listChapters(chapter.sourceBookId)
                    .firstOrNull { it.readingOrder == chapter.sourceReadingOrder }
                    ?: error("Restored Manga merge source chapter is missing")
                val targetChapter = targetChapters[chapter.targetReadingOrder]
                    ?: error("Restored Manga merge target chapter is missing")
                MangaMergeChapterEntity(
                    mergeId = backup.id,
                    sourceChapterId = sourceChapter.id,
                    sourceBookId = chapter.sourceBookId,
                    targetChapterId = targetChapter.id,
                    sourceReadingOrder = chapter.sourceReadingOrder,
                    targetReadingOrder = chapter.targetReadingOrder,
                    disposition = chapter.disposition
                )
            }

            database.withTransaction {
                database.mangaMerges().upsertMerge(
                    MangaWorkMergeEntity(
                        id = backup.id,
                        targetBookId = backup.targetBookId,
                        createdAtEpochMs = backup.createdAtEpochMs,
                        receiptVersion = backup.receiptVersion,
                        targetBookProgress = backup.targetBookProgress,
                        targetBookFinished = backup.targetBookFinished,
                        targetBookLastOpenedAtEpochMs =
                            backup.targetBookLastOpenedAtEpochMs,
                        targetProgressChapterId = progressChapterId,
                        targetProgressPageIndex = backup.targetProgressPageIndex,
                        targetProgressPageCount = backup.targetProgressPageCount,
                        targetProgressChapterProgression =
                            backup.targetProgressChapterProgression,
                        targetProgressUpdatedAtEpochMs =
                            backup.targetProgressUpdatedAtEpochMs
                    )
                )
                database.mangaMerges().upsertMembers(members)
                database.mangaMerges().upsertChapters(mappings)
            }
        }
    }

    private suspend fun captureMangaRestorePoints(
        books: List<Book>
    ): Map<String, MangaLocalRestorePoint> = buildMap {
        for (book in books) {
            if (book.format != BookFormat.COMIC) continue
            val progress = database.mangaProgress().find(book.id) ?: continue
            val chapter = database.mangaCatalog().findChapter(progress.chapterId)
                ?: continue
            put(
                book.id,
                MangaLocalRestorePoint(
                    pageIndex = progress.pageIndex,
                    pageCount = progress.pageCount,
                    chapterProgression = progress.chapterProgression,
                    updatedAtEpochMs = progress.updatedAtEpochMs,
                    chapterReadingOrder = chapter.readingOrder
                )
            )
        }
    }

    private fun mangaRestorePointsToJson(
        points: Map<String, MangaLocalRestorePoint>
    ): JSONArray = JSONArray().apply {
        points.toSortedMap().forEach { (bookId, point) ->
            put(JSONObject().apply {
                put("bookId", bookId)
                put("pageIndex", point.pageIndex)
                put("pageCount", point.pageCount ?: JSONObject.NULL)
                put("chapterProgression", point.chapterProgression)
                put("updatedAtEpochMs", point.updatedAtEpochMs)
                put("chapterReadingOrder", point.chapterReadingOrder)
            })
        }
    }

    private fun parseMangaRestorePoints(
        manifest: JSONObject
    ): Map<String, MangaLocalRestorePoint> {
        val records = manifest.optJSONArray("mangaProgress") ?: return emptyMap()
        return buildMap {
            for (index in 0 until records.length()) {
                val record = records.getJSONObject(index)
                val bookId = record.getString("bookId")
                require(bookId.isNotBlank()) { "Manga progress has an empty book id." }
                val point = MangaLocalRestorePoint(
                    pageIndex = record.getInt("pageIndex"),
                    pageCount = if (record.isNull("pageCount")) {
                        null
                    } else {
                        record.getInt("pageCount")
                    },
                    chapterProgression = record.getDouble("chapterProgression"),
                    updatedAtEpochMs = record.getLong("updatedAtEpochMs"),
                    chapterReadingOrder = record.optInt("chapterReadingOrder", 0)
                )
                require(put(bookId, point) == null) {
                    "Backup contains duplicate Manga progress for one book."
                }
            }
        }
    }

    private suspend fun captureMangaLocalChapters(
        books: List<Book>
    ): List<MangaLocalChapterSnapshot> = buildList {
        for (book in books) {
            if (book.format != BookFormat.COMIC || !book.isImported) continue
            val chapters = database.mangaCatalog().listChapters(book.id)
            for (chapter in chapters) {
                val localSource = database.mangaCatalog()
                    .listChapterSources(chapter.id)
                    .firstOrNull {
                        it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                    }
                    ?: error(
                        "Cannot create a complete local backup for " + book.title +
                            ": chapter " + (chapter.readingOrder + 1) +
                            " has no local CBZ source."
                    )
                val archive = mangaImporter.resolveLocalArchiveFile(
                    book = book,
                    chapterKey = localSource.chapterKey,
                    readingOrder = chapter.readingOrder
                ) ?: error(
                    "Cannot back up " + book.title +
                        ": local Manga chapter " + (chapter.readingOrder + 1) +
                        " has no source archive."
                )
                add(
                    MangaLocalChapterSnapshot(
                        bookId = book.id,
                        readingOrder = chapter.readingOrder,
                        chapterKey = localSource.chapterKey,
                        metadata = MangaLocalChapterMetadata(
                            title = chapter.title ?: chapter.normalizedTitle,
                            volume = chapter.volume,
                            number = chapter.number,
                            languageTag = chapter.languageTag
                        ),
                        archiveFile = archive
                    )
                )
            }
        }
    }

    private fun mangaLocalChaptersToJson(
        archives: List<MangaBackupArchive>
    ): JSONArray = JSONArray().apply {
        archives
            .sortedWith(
                compareBy<MangaBackupArchive> { it.chapter.bookId }
                    .thenBy { it.chapter.readingOrder }
            )
            .forEach { archived ->
                val chapter = archived.chapter
                put(JSONObject().apply {
                    put("bookId", chapter.bookId)
                    put("readingOrder", chapter.readingOrder)
                    put("chapterKey", chapter.chapterKey)
                    put("title", chapter.metadata.title ?: JSONObject.NULL)
                    put("volume", chapter.metadata.volume ?: JSONObject.NULL)
                    put("number", chapter.metadata.number ?: JSONObject.NULL)
                    put("languageTag", chapter.metadata.languageTag ?: JSONObject.NULL)
                    put("archivePath", archived.archivePath)
                })
            }
    }

    private fun parseMangaLocalChapters(
        manifest: JSONObject,
        stagingRoot: File
    ): Map<String, List<MangaLocalChapterSnapshot>> {
        val records = manifest.optJSONArray("mangaLocalChapters") ?: return emptyMap()
        val unique = mutableSetOf<Pair<String, Int>>()
        val parsed = buildList {
            for (index in 0 until records.length()) {
                val record = records.getJSONObject(index)
                val bookId = record.getString("bookId")
                val readingOrder = record.getInt("readingOrder")
                require(bookId.isNotBlank() && readingOrder >= 0) {
                    "Invalid Manga chapter backup identity."
                }
                require(unique.add(bookId to readingOrder)) {
                    "Backup contains duplicate Manga chapter reading order."
                }
                val archivePath = requireSafeArchivePath(record.getString("archivePath"))
                require(archivePath.startsWith("books/")) {
                    "A Manga chapter archive is stored outside books/."
                }
                val archive = File(stagingRoot, archivePath).canonicalFile
                require(
                    archive.toPath().startsWith(stagingRoot.canonicalFile.toPath()) &&
                        archive.isFile
                ) {
                    "A Manga chapter archive referenced by the manifest is missing."
                }
                add(
                    MangaLocalChapterSnapshot(
                        bookId = bookId,
                        readingOrder = readingOrder,
                        chapterKey = record.getString("chapterKey"),
                        metadata = MangaLocalChapterMetadata(
                            title = record.optNullableString("title"),
                            volume = if (record.isNull("volume")) null
                                else record.getDouble("volume"),
                            number = if (record.isNull("number")) null
                                else record.getDouble("number"),
                            languageTag = record.optNullableString("languageTag")
                        ),
                        archiveFile = archive
                    )
                )
            }
        }
        return parsed
            .groupBy { it.bookId }
            .mapValues { (_, chapters) -> chapters.sortedBy { it.readingOrder } }
    }

    private fun backupExtension(book: Book): String = when (book.format) {
        BookFormat.EPUB -> "epub"
        BookFormat.PDF -> "pdf"
        BookFormat.COMIC -> "cbz"
        BookFormat.AUDIO -> error("Audio publications are not backed up yet.")
    }

    private fun resetMangaDerivedCache() {
        val root = mangaImporter.cacheRoot
        if (root.exists()) {
            check(root.deleteRecursively()) {
                "Could not clear derived Manga cache before restore rebuild."
            }
        }
        check(root.mkdirs() || root.isDirectory) {
            "Could not recreate derived Manga cache."
        }
    }

    private suspend fun verifyRestoredLocalChapterIdentity(
        bookId: String,
        readingOrder: Int,
        expectedChapterKey: String
    ) {
        val expectedFingerprint = expectedChapterKey
            .takeIf { it.startsWith("cbz-") }
            ?.removePrefix("cbz-")
            ?.takeIf { value ->
                value.length == 64 &&
                    value.all { it in '0'..'9' || it in 'a'..'f' }
            }
            ?: return

        val chapter = database.mangaCatalog()
            .listChapters(bookId)
            .firstOrNull { it.readingOrder == readingOrder }
            ?: error("Restored Manga chapter is missing from the catalog")
        val source = database.mangaCatalog()
            .listChapterSources(chapter.id)
            .firstOrNull {
                it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
            }
            ?: error("Restored Manga chapter has no local source")
        require(source.chapterKey == "cbz-" + expectedFingerprint) {
            "Restored Manga chapter archive does not match its backup fingerprint."
        }
    }

    private suspend fun rebuildMangaBooks(
        books: List<Book>,
        restorePoints: Map<String, MangaLocalRestorePoint>,
        localChapters: Map<String, List<MangaLocalChapterSnapshot>> = emptyMap()
    ) {
        for (book in books) {
            if (book.format != BookFormat.COMIC || !book.isImported) continue

            val chapters = localChapters[book.id].orEmpty()
            val restorePoint = restorePoints[book.id]
            if (chapters.isEmpty()) {
                mangaImporter.rebuildPersistedManga(
                    book = book,
                    restorePoint = restorePoint
                ).getOrThrow()
                continue
            }

            require(chapters.first().readingOrder == 0) {
                "Restored Manga chapter sequence must start at reading order 0."
            }
            chapters.forEachIndexed { expected, chapter ->
                require(chapter.readingOrder == expected) {
                    "Restored Manga chapter sequence must be contiguous."
                }
            }

            val primary = chapters.first()
            mangaImporter.rebuildPersistedManga(
                book = book,
                restorePoint = restorePoint?.takeIf { it.chapterReadingOrder == 0 },
                primaryMetadata = primary.metadata
            ).getOrThrow()

            verifyRestoredLocalChapterIdentity(
                bookId = book.id,
                readingOrder = 0,
                expectedChapterKey = primary.chapterKey
            )

            chapters.drop(1).forEach { chapter ->
                val appended = mangaImporter.appendChapter(
                    bookId = book.id,
                    uri = Uri.fromFile(chapter.archiveFile),
                    metadata = chapter.metadata
                ).getOrThrow()
                require(!appended.duplicate && appended.readingOrder == chapter.readingOrder) {
                    "Restored Manga chapter order diverged from the backup manifest."
                }
                verifyRestoredLocalChapterIdentity(
                    bookId = book.id,
                    readingOrder = chapter.readingOrder,
                    expectedChapterKey = chapter.chapterKey
                )
            }

            restorePoint
                ?.takeIf { it.chapterReadingOrder > 0 }
                ?.let { mangaImporter.restoreLocalProgress(book.id, it).getOrThrow() }
        }
        library.flushWrites()
    }

    /** Restores current schema-6 backups and older schema-1/2/3/4/5 local backups. */
    suspend fun restoreBackup(source: Uri): BackupRestoreResult = withContext(Dispatchers.IO) {
        val stagingRoot = File(context.cacheDir, "veil-restore-" + UUID.randomUUID()).apply {
            mkdirs()
        }
        var installedRoot: File? = null
        try {
            extractValidatedBackup(source, stagingRoot)
            val manifestFile = File(stagingRoot, "manifest.json")
            require(manifestFile.isFile) { "This archive has no Veil Reader manifest." }
            require(manifestFile.length() <= MAX_MANIFEST_BYTES) {
                "The backup manifest is unexpectedly large."
            }
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val schema = manifest.optInt("schemaVersion", -1)
            require(schema in SUPPORTED_BACKUP_SCHEMAS) {
                "This backup version is not supported by this Veil Reader build."
            }

            val incoming = when (schema) {
                1 -> parseLegacySchemaOne(manifest.getJSONObject("libraryPreferences"))
                2, 3, 4, 5, CURRENT_BACKUP_SCHEMA ->
                    LibrarySnapshot.fromJson(manifest.getJSONObject("library"))
                else -> error("Unsupported backup schema.")
            }
            val incomingMangaProgress = parseMangaRestorePoints(manifest)
            val incomingMangaChapters = parseMangaLocalChapters(manifest, stagingRoot)
            val incomingMangaMerges = parseMangaMergeBackups(manifest)
            val incomingComicIds = incoming.books
                .filter { it.format == BookFormat.COMIC }
                .mapTo(mutableSetOf()) { it.id }
            require(incomingMangaProgress.keys.all(incomingComicIds::contains)) {
                "Manga progress references a book that is not a comic in this backup."
            }
            require(incomingMangaChapters.keys.all(incomingComicIds::contains)) {
                "Manga chapter archives reference a book that is not a comic in this backup."
            }
            incomingMangaMerges.forEach { merge ->
                require(merge.targetBookId in incomingComicIds) {
                    "Manga merge target is not a comic in this backup."
                }
                require(merge.members.all { it.sourceBookId in incomingComicIds }) {
                    "Manga merge source is not a comic in this backup."
                }
                require(merge.members.none { it.sourceBookId == merge.targetBookId }) {
                    "Manga merge backup cannot merge a Book into itself."
                }
            }
            if (schema >= 5) {
                incoming.books
                    .filter { it.format == BookFormat.COMIC && it.isImported }
                    .forEach { book ->
                        require(incomingMangaChapters[book.id].orEmpty().isNotEmpty()) {
                            "Schema-5 Manga backup is missing chapter source metadata."
                        }
                    }
            }
            val gamePreferences = JSONObject(
                manifest.getJSONObject("gamePreferences").toString()
            )
            val archivedByBookId = stagedPublications(manifest, stagingRoot)

            val publicationsRoot = File(context.filesDir, "publications")
                .apply { mkdirs() }
                .canonicalFile
            installedRoot = File(
                publicationsRoot,
                "restore-" + UUID.randomUUID()
            ).apply { mkdirs() }.canonicalFile
            require(installedRoot.toPath().startsWith(publicationsRoot.toPath())) {
                "Invalid restore location."
            }

            var restoredBooks = 0
            val restoredBookModels = incoming.books.map { book ->
                val archived = archivedByBookId[book.id]
                if (book.isImported) {
                    requireNotNull(archived) {
                        "The backup is missing the publication file for " + book.title + "."
                    }
                }
                if (archived == null) return@map book.copy(sourceUri = null)

                val extension = backupExtension(book)
                val target = File(
                    installedRoot,
                    UUID.randomUUID().toString() + "." + extension
                )
                archived.inputStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                require(target.length() > 0) { "A restored publication is empty." }
                restoredBooks += 1
                // Cover paths and fingerprints are derived. Manga cache is rebuilt from CBZ.
                book.copy(
                    sourceUri = Uri.fromFile(target).toString(),
                    coverCachePath = null,
                    contentFingerprint = null
                )
            }
            val restoredSnapshot = incoming.copy(books = restoredBookModels)

            val gamePrefs = context.getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE)
            val oldLibrary = library.snapshot()
            val oldMangaProgress = captureMangaRestorePoints(oldLibrary.books)
            val oldMangaChapters = captureMangaLocalChapters(oldLibrary.books)
                .groupBy { it.bookId }
                .mapValues { (_, chapters) -> chapters.sortedBy { it.readingOrder } }
            val oldMangaMerges = captureMangaMergeBackups()
            val oldGame = gamePrefs.all.toMap()

            try {
                database.mangaMerges().deleteAll()
                library.replaceAll(restoredSnapshot)
                resetMangaDerivedCache()
                rebuildMangaBooks(
                    restoredSnapshot.books,
                    incomingMangaProgress,
                    incomingMangaChapters
                )
                restoreMangaMergeBackups(incomingMangaMerges)
                if (!replacePreferences(gamePrefs, gamePreferences)) {
                    error("Could not commit restored progression data.")
                }
            } catch (error: Throwable) {
                val rollbackError = runCatching {
                    database.mangaMerges().deleteAll()
                    library.replaceAll(oldLibrary)
                    resetMangaDerivedCache()
                    rebuildMangaBooks(
                        oldLibrary.books,
                        oldMangaProgress,
                        oldMangaChapters
                    )
                    restoreMangaMergeBackups(oldMangaMerges)
                }.exceptionOrNull()
                restorePreferencesSnapshot(gamePrefs, oldGame)

                val wrapped = IllegalStateException(
                    if (rollbackError == null) {
                        "Restore could not be committed. Your previous data was kept."
                    } else {
                        "Restore failed and previous Manga state could not be fully rebuilt."
                    },
                    error
                )
                rollbackError?.let(wrapped::addSuppressed)
                throw wrapped
            }

            publicationsRoot.listFiles()?.forEach { child ->
                if (child.canonicalFile != installedRoot) child.deleteRecursively()
            }

            BackupRestoreResult(restoredBooks, restoredSnapshot.highlights.size)
        } catch (error: Throwable) {
            installedRoot?.deleteRecursively()
            throw error
        } finally {
            stagingRoot.deleteRecursively()
        }
    }

    private fun stagedPublications(manifest: JSONObject, stagingRoot: File): Map<String, File> {
        val records = manifest.optJSONArray("publications") ?: JSONArray()
        return buildMap {
            for (index in 0 until records.length()) {
                val record = records.getJSONObject(index)
                val bookId = record.getString("bookId")
                val archivePath = requireSafeArchivePath(record.getString("archivePath"))
                require(archivePath.startsWith("books/")) { "A publication is stored outside books/." }
                val staged = File(stagingRoot, archivePath).canonicalFile
                require(staged.toPath().startsWith(stagingRoot.canonicalFile.toPath()) && staged.isFile) {
                    "A publication referenced by the manifest is missing."
                }
                require(put(bookId, staged) == null) { "The backup contains duplicate publication records." }
            }
        }
    }

    private fun extractValidatedBackup(source: Uri, stagingRoot: File) {
        val input = context.contentResolver.openInputStream(source) ?: error("Could not read the selected backup.")
        var totalBytes = 0L
        var entryCount = 0
        val seenEntries = mutableSetOf<String>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                require(entryCount <= MAX_ENTRIES) { "The backup contains too many files." }
                val safePath = requireSafeArchivePath(entry.name)
                require(seenEntries.add(safePath)) { "The backup contains duplicate file entries." }
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                if (safePath != "manifest.json" && !safePath.startsWith("books/")) {
                    zip.closeEntry()
                    continue
                }
                val target = File(stagingRoot, safePath).canonicalFile
                require(target.toPath().startsWith(stagingRoot.canonicalFile.toPath())) { "Unsafe backup entry." }
                target.parentFile?.mkdirs()
                target.outputStream().use { output ->
                    totalBytes += copyWithLimit(zip, output, MAX_BACKUP_BYTES - totalBytes)
                }
                require(totalBytes <= MAX_BACKUP_BYTES) { "The backup is too large to restore safely." }
                zip.closeEntry()
            }
        }
    }

    private fun requireSafeArchivePath(raw: String): String {
        val normalized = raw.replace('\\', '/').trimStart('/')
        require(raw.isNotBlank() && !raw.startsWith('/') && !raw.startsWith('\\')) { "Unsafe backup path." }
        require(normalized.split('/').none { it == ".." || it.isBlank() }) { "Unsafe backup path." }
        return normalized
    }

    private fun copyWithLimit(input: InputStream, output: OutputStream, remaining: Long): Long {
        require(remaining > 0) { "The backup is too large to restore safely." }
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            copied += read
            require(copied <= remaining) { "The backup is too large to restore safely." }
            output.write(buffer, 0, read)
        }
        return copied
    }

    private fun preferencesToJson(prefs: SharedPreferences): JSONObject = JSONObject().apply {
        prefs.all.forEach { (key, value) ->
            put(key, when (value) {
                is Set<*> -> JSONArray(value.filterIsInstance<String>())
                else -> value
            })
        }
    }

    private fun replacePreferences(target: SharedPreferences, source: JSONObject): Boolean {
        val editor = target.edit().clear()
        source.keys().forEach { key -> putJsonPreference(editor, key, source.get(key)) }
        return editor.commit()
    }

    private fun putJsonPreference(editor: SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            null, JSONObject.NULL -> Unit
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Double -> editor.putFloat(key, value.toFloat())
            is String -> editor.putString(key, value)
            is JSONArray -> editor.putStringSet(
                key,
                buildSet { for (i in 0 until value.length()) add(value.optString(i)) }
                    .filter { it.isNotBlank() }.toSet()
            )
            else -> error("Unsupported preference value in backup: $key")
        }
    }

    private fun restorePreferencesSnapshot(target: SharedPreferences, snapshot: Map<String, *>) {
        val editor = target.edit().clear()
        snapshot.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.commit()
    }

    companion object {
        private const val GAME_PREFS = "veil_game_v1"
        private const val CURRENT_BACKUP_SCHEMA = 6
        private val SUPPORTED_BACKUP_SCHEMAS =
            setOf(1, 2, 3, 4, 5, CURRENT_BACKUP_SCHEMA)
        private const val MAX_ENTRIES = 2_000
        private const val MAX_MANIFEST_BYTES = 5L * 1024L * 1024L
        private const val MAX_BACKUP_BYTES = 2L * 1024L * 1024L * 1024L
    }
}

data class BackupRestoreResult(val booksRestored: Int, val highlightsRestored: Int)

private fun LibrarySnapshot.toJson(): JSONObject = JSONObject().apply {
    put("books", JSONArray().apply { books.forEach { put(it.toJson()) } })
    put("highlights", JSONArray().apply { highlights.forEach { put(it.toJson()) } })
    put("bookmarks", JSONArray().apply { bookmarks.forEach { put(it.toJson()) } })
    put("appearance", appearance.toJson())
    put("readingSessions", JSONArray().apply { readingSessions.forEach { put(it.toJson()) } })
    put("readingCycles", JSONArray().apply { readingCycles.forEach { put(it.toJson()) } })
    put("passageVisits", JSONArray().apply { passageVisits.forEach { put(it.toJson()) } })
    put("readingMilestones", JSONArray().apply { readingMilestones.forEach { put(it.toJson()) } })
}

private fun LibrarySnapshot.Companion.fromJson(json: JSONObject): LibrarySnapshot = LibrarySnapshot(
    books = json.getJSONArray("books").mapObjects(::bookFromJson),
    highlights = json.optJSONArray("highlights")?.mapObjects(::highlightFromJson).orEmpty(),
    bookmarks = json.optJSONArray("bookmarks")?.mapObjects(::bookmarkFromJson).orEmpty(),
    appearance = appearanceFromJson(json.optJSONObject("appearance") ?: JSONObject()),
    readingSessions = json.optJSONArray("readingSessions")?.mapObjects(::readingSessionFromJson).orEmpty(),
    readingCycles = json.optJSONArray("readingCycles")?.mapObjects(::readingCycleFromJson).orEmpty(),
    passageVisits = json.optJSONArray("passageVisits")?.mapObjects(::passageVisitFromJson).orEmpty(),
    readingMilestones = json.optJSONArray("readingMilestones")
        ?.mapObjects(::readingMilestoneFromJson)
        .orEmpty()
)

private fun parseLegacySchemaOne(prefs: JSONObject): LibrarySnapshot = LibrarySnapshot(
    books = JSONArray(prefs.optString("books", "[]")).mapObjects(::bookFromJson),
    highlights = JSONArray(prefs.optString("highlights", "[]")).mapObjects(::highlightFromJson),
    bookmarks = JSONArray(prefs.optString("bookmarks", "[]")).mapObjects(::bookmarkFromJson),
    appearance = appearanceFromJson(JSONObject(prefs.optString("appearance", "{}")))
)

private fun Book.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("title", title); put("author", author); put("progress", progress.toDouble())
    put("currentChapter", currentChapter); put("totalPages", totalPages); put("pagesRead", pagesRead)
    put("format", format.name); put("sourceUri", sourceUri); put("mediaType", mediaType); put("locatorJson", locatorJson)
    put("addedAt", addedAtEpochMs); put("lastOpenedAt", lastOpenedAtEpochMs); put("finished", finished)
    put("favorite", favorite)
    put("collection", allCollections.firstOrNull().orEmpty())
    put("collections", JSONArray(allCollections))
    put("seriesName", seriesName ?: JSONObject.NULL)
    put("seriesIndex", seriesIndex ?: JSONObject.NULL)
    put("language", language ?: JSONObject.NULL)
}

private fun Highlight.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("bookId", bookId); put("quote", quote); put("locatorJson", locatorJson)
    put("note", note); put("createdAt", createdAtEpochMs)
}

private fun Bookmark.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("bookId", bookId); put("label", label); put("locatorJson", locatorJson); put("createdAt", createdAtEpochMs)
}

private fun ReadingSessionSnapshot.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("bookId", bookId ?: JSONObject.NULL)
    put("startedAt", startedAtEpochMs)
    put("endedAt", endedAtEpochMs)
    put("activeMillis", activeMillis)
    put("pacedPageTurns", pacedPageTurns)
    put("highlightCount", highlightCount)
    put("noteCount", noteCount)
}

private fun ReadingCycleRecord.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("bookId", bookId)
    put("cycleIndex", cycleIndex)
    put("titleSnapshot", titleSnapshot)
    put("authorSnapshot", authorSnapshot)
    put("startedAt", startedAtEpochMs ?: JSONObject.NULL)
    put("completedAt", completedAtEpochMs)
    put("finalLocatorJson", finalLocatorJson)
    put("sessionCount", sessionCount)
    put("totalActiveMillis", totalActiveMillis)
    put("pacedPageTurns", pacedPageTurns)
    put("highlightCount", highlightCount)
    put("noteCount", noteCount)
    put("bookmarkCount", bookmarkCount)
    put("sealCode", sealCode)
    put("timeline", JSONArray().apply { timeline.forEach { put(it.toJson()) } })
}

private fun ReadingHistoryEvent.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("kind", kind.name)
    put("timestamp", timestampEpochMs)
    put("title", title)
    put("detail", detail ?: JSONObject.NULL)
}

private fun ReadingMilestoneRecord.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("bookId", bookId)
    put("kind", kind.name)
    put("reachedAt", reachedAtEpochMs)
    put("progression", progression.toDouble())
    put("locatorJson", locatorJson ?: JSONObject.NULL)
}

private fun readingMilestoneFromJson(o: JSONObject): ReadingMilestoneRecord = ReadingMilestoneRecord(
    id = o.getString("id"),
    bookId = o.getString("bookId"),
    kind = runCatching {
        ReadingMilestoneKind.valueOf(o.getString("kind"))
    }.getOrDefault(ReadingMilestoneKind.FIRST_OPENED),
    reachedAtEpochMs = o.optLong("reachedAt", 0L),
    progression = o.optDouble("progression", 0.0).toFloat().coerceIn(0f, 1f),
    locatorJson = o.optNullableString("locatorJson")
)

private fun PassageVisit.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("highlightId", highlightId)
    put("bookId", bookId)
    put("locatorJson", locatorJson)
    put("viewedAt", viewedAtEpochMs)
}

private fun readingCycleFromJson(o: JSONObject): ReadingCycleRecord = ReadingCycleRecord(
    id = o.getString("id"),
    bookId = o.getString("bookId"),
    cycleIndex = o.optInt("cycleIndex", 1).coerceAtLeast(1),
    titleSnapshot = o.optString("titleSnapshot"),
    authorSnapshot = o.optString("authorSnapshot"),
    startedAtEpochMs = if (o.isNull("startedAt")) null else o.optLong("startedAt"),
    completedAtEpochMs = o.getLong("completedAt"),
    finalLocatorJson = o.optString("finalLocatorJson"),
    sessionCount = o.optInt("sessionCount", 0).coerceAtLeast(0),
    totalActiveMillis = o.optLong("totalActiveMillis", 0L).coerceAtLeast(0L),
    pacedPageTurns = o.optInt("pacedPageTurns", 0).coerceAtLeast(0),
    highlightCount = o.optInt("highlightCount", 0).coerceAtLeast(0),
    noteCount = o.optInt("noteCount", 0).coerceAtLeast(0),
    bookmarkCount = o.optInt("bookmarkCount", 0).coerceAtLeast(0),
    sealCode = o.getString("sealCode"),
    timeline = o.optJSONArray("timeline")?.mapObjects(::readingHistoryEventFromJson).orEmpty()
)

private fun readingHistoryEventFromJson(o: JSONObject): ReadingHistoryEvent = ReadingHistoryEvent(
    id = o.getString("id"),
    kind = runCatching {
        ReadingHistoryEventKind.valueOf(o.getString("kind"))
    }.getOrDefault(ReadingHistoryEventKind.LATEST_VOLUME_ACTIVITY),
    timestampEpochMs = o.optLong("timestamp", 0L),
    title = o.optString("title"),
    detail = o.optNullableString("detail")
)

private fun passageVisitFromJson(o: JSONObject): PassageVisit = PassageVisit(
    id = o.getString("id"),
    highlightId = o.getString("highlightId"),
    bookId = o.getString("bookId"),
    locatorJson = o.getString("locatorJson"),
    viewedAtEpochMs = o.optLong("viewedAt", 0L)
)

private fun ReaderAppearance.toJson(): JSONObject = JSONObject().apply {
    put("theme", theme.name); put("fontScale", fontScale); put("lineHeight", lineHeight); put("pageMargins", pageMargins)
    put("scroll", scroll); put("publisherStyles", publisherStyles); put("pageTurnStyle", pageTurnStyle.name)
    put("screenBrightness", screenBrightness ?: JSONObject.NULL)
}

private fun bookFromJson(o: JSONObject): Book {
    val collectionNames = buildList {
        o.optJSONArray("collections")?.let { array ->
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
        o.optString("collection", "").trim().takeIf { it.isNotEmpty() }?.let(::add)
    }.distinctBy { it.lowercase(Locale.ROOT) }

    return Book(
        id = o.getString("id"),
        title = o.getString("title"),
        author = o.optString("author", "Unknown author"),
        progress = o.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
        currentChapter = o.optString("currentChapter", "Not started"),
        totalPages = o.optInt("totalPages", 0).coerceAtLeast(0),
        pagesRead = o.optInt("pagesRead", 0).coerceAtLeast(0),
        format = runCatching { BookFormat.valueOf(o.optString("format", "EPUB")) }.getOrDefault(BookFormat.EPUB),
        sourceUri = o.optNullableString("sourceUri"),
        mediaType = o.optNullableString("mediaType"),
        locatorJson = o.optNullableString("locatorJson"),
        addedAtEpochMs = o.optLong("addedAt", 0L),
        lastOpenedAtEpochMs = o.optLong("lastOpenedAt", 0L),
        finished = o.optBoolean("finished", false),
        favorite = o.optBoolean("favorite", false),
        seriesName = o.optNullableString("seriesName"),
        seriesIndex = o.optFiniteDouble("seriesIndex"),
        language = o.optNullableString("language"),
        collection = collectionNames.firstOrNull().orEmpty(),
        collections = collectionNames
    )
}

private fun highlightFromJson(o: JSONObject): Highlight = Highlight(
    id = o.getString("id"), bookId = o.getString("bookId"), quote = o.optString("quote"),
    locatorJson = o.getString("locatorJson"), note = o.optString("note"), createdAtEpochMs = o.optLong("createdAt", 0L)
)

private fun bookmarkFromJson(o: JSONObject): Bookmark = Bookmark(
    id = o.getString("id"), bookId = o.getString("bookId"), label = o.optString("label", "Bookmark"),
    locatorJson = o.getString("locatorJson"), createdAtEpochMs = o.optLong("createdAt", 0L)
)

private fun readingSessionFromJson(o: JSONObject): ReadingSessionSnapshot = ReadingSessionSnapshot(
    id = o.getString("id"),
    bookId = o.optNullableString("bookId"),
    startedAtEpochMs = o.optLong("startedAt", 0L),
    endedAtEpochMs = o.optLong("endedAt", o.optLong("startedAt", 0L)),
    activeMillis = o.optLong("activeMillis", 0L).coerceAtLeast(0L),
    pacedPageTurns = o.optInt("pacedPageTurns", 0).coerceAtLeast(0),
    highlightCount = o.optInt("highlightCount", 0).coerceAtLeast(0),
    noteCount = o.optInt("noteCount", 0).coerceAtLeast(0)
)

private fun appearanceFromJson(o: JSONObject): ReaderAppearance = ReaderAppearance(
    theme = runCatching { ReaderTheme.valueOf(o.optString("theme", "DUSK")) }.getOrDefault(ReaderTheme.DUSK),
    fontScale = (o.optDouble("fontScale", 1.0).takeIf { it.isFinite() } ?: 1.0).coerceIn(.75, 1.8),
    lineHeight = (o.optDouble("lineHeight", 1.45).takeIf { it.isFinite() } ?: 1.45).coerceIn(1.1, 2.0),
    pageMargins = (o.optDouble("pageMargins", 1.0).takeIf { it.isFinite() } ?: 1.0).coerceIn(.5, 2.0),
    scroll = o.optBoolean("scroll", false),
    publisherStyles = o.optBoolean("publisherStyles", true),
    pageTurnStyle = runCatching {
        PageTurnStyle.valueOf(o.optString("pageTurnStyle", PageTurnStyle.PAPER.name))
    }.getOrDefault(PageTurnStyle.PAPER),
    screenBrightness = o.optFiniteDouble("screenBrightness")?.coerceIn(.05, 1.0)
)

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = buildList {
    for (i in 0 until length()) add(transform(getJSONObject(i)))
}

private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.optFiniteDouble(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key, Double.NaN).takeIf { it.isFinite() }
