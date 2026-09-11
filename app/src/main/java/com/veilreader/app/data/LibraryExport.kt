package com.veilreader.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** User-initiated local backup/export. No server or account is involved. */
class LibraryExport(private val context: Context, private val library: LocalLibraryRepository) {
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
                Triple(book, file, "books/${index + 1}.${book.format.name.lowercase()}")
            }
            val manifest = JSONObject().apply {
                put("schemaVersion", CURRENT_BACKUP_SCHEMA)
                put("appVersion", "0.7.0")
                put("createdAtEpochMs", System.currentTimeMillis())
                put("library", snapshot.toJson())
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
            }
        }
    }

    /** Restores both current schema-2 backups and older 0.6 schema-1 backups. */
    suspend fun restoreBackup(source: Uri): BackupRestoreResult = withContext(Dispatchers.IO) {
        val stagingRoot = File(context.cacheDir, "veil-restore-${UUID.randomUUID()}").apply { mkdirs() }
        var installedRoot: File? = null
        try {
            extractValidatedBackup(source, stagingRoot)
            val manifestFile = File(stagingRoot, "manifest.json")
            require(manifestFile.isFile) { "This archive has no Veil Reader manifest." }
            require(manifestFile.length() <= MAX_MANIFEST_BYTES) { "The backup manifest is unexpectedly large." }
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val schema = manifest.optInt("schemaVersion", -1)
            require(schema in SUPPORTED_BACKUP_SCHEMAS) {
                "This backup version is not supported by this Veil Reader build."
            }

            val incoming = when (schema) {
                1 -> parseLegacySchemaOne(manifest.getJSONObject("libraryPreferences"))
                CURRENT_BACKUP_SCHEMA -> LibrarySnapshot.fromJson(manifest.getJSONObject("library"))
                else -> error("Unsupported backup schema.")
            }
            val gamePreferences = JSONObject(manifest.getJSONObject("gamePreferences").toString())
            val archivedByBookId = stagedPublications(manifest, stagingRoot)

            val publicationsRoot = File(context.filesDir, "publications").apply { mkdirs() }.canonicalFile
            installedRoot = File(publicationsRoot, "restore-${UUID.randomUUID()}").apply { mkdirs() }.canonicalFile
            require(installedRoot.toPath().startsWith(publicationsRoot.toPath())) { "Invalid restore location." }

            var restoredBooks = 0
            val restoredBookModels = incoming.books.map { book ->
                val archived = archivedByBookId[book.id]
                if (book.isImported) requireNotNull(archived) {
                    "The backup is missing the publication file for ${book.title}."
                }
                if (archived == null) return@map book.copy(sourceUri = null)
                val format = book.format.name.lowercase()
                require(format == "epub" || format == "pdf") { "Unsupported publication format in backup." }
                val target = File(installedRoot, "${UUID.randomUUID()}.$format")
                archived.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                require(target.length() > 0) { "A restored publication is empty." }
                restoredBooks += 1
                book.copy(sourceUri = Uri.fromFile(target).toString())
            }
            val restoredSnapshot = incoming.copy(books = restoredBookModels)

            val gamePrefs = context.getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE)
            val oldLibrary = library.snapshot()
            val oldGame = gamePrefs.all.toMap()

            try {
                library.replaceAll(restoredSnapshot)
                if (!replacePreferences(gamePrefs, gamePreferences)) {
                    error("Could not commit restored progression data.")
                }
            } catch (error: Throwable) {
                runCatching { library.replaceAll(oldLibrary) }
                restorePreferencesSnapshot(gamePrefs, oldGame)
                throw IllegalStateException("Restore could not be committed. Your previous data was kept.", error)
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
        private const val CURRENT_BACKUP_SCHEMA = 2
        private val SUPPORTED_BACKUP_SCHEMAS = setOf(1, CURRENT_BACKUP_SCHEMA)
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
}

private fun LibrarySnapshot.Companion.fromJson(json: JSONObject): LibrarySnapshot = LibrarySnapshot(
    books = json.getJSONArray("books").mapObjects(::bookFromJson),
    highlights = json.optJSONArray("highlights")?.mapObjects(::highlightFromJson).orEmpty(),
    bookmarks = json.optJSONArray("bookmarks")?.mapObjects(::bookmarkFromJson).orEmpty(),
    appearance = appearanceFromJson(json.optJSONObject("appearance") ?: JSONObject())
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
    put("favorite", favorite); put("collection", collection)
}

private fun Highlight.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("bookId", bookId); put("quote", quote); put("locatorJson", locatorJson)
    put("note", note); put("createdAt", createdAtEpochMs)
}

private fun Bookmark.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("bookId", bookId); put("label", label); put("locatorJson", locatorJson); put("createdAt", createdAtEpochMs)
}

private fun ReaderAppearance.toJson(): JSONObject = JSONObject().apply {
    put("theme", theme.name); put("fontScale", fontScale); put("lineHeight", lineHeight); put("pageMargins", pageMargins)
    put("scroll", scroll); put("publisherStyles", publisherStyles)
}

private fun bookFromJson(o: JSONObject): Book = Book(
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
    collection = o.optString("collection", "")
)

private fun highlightFromJson(o: JSONObject): Highlight = Highlight(
    id = o.getString("id"), bookId = o.getString("bookId"), quote = o.optString("quote"),
    locatorJson = o.getString("locatorJson"), note = o.optString("note"), createdAtEpochMs = o.optLong("createdAt", 0L)
)

private fun bookmarkFromJson(o: JSONObject): Bookmark = Bookmark(
    id = o.getString("id"), bookId = o.getString("bookId"), label = o.optString("label", "Bookmark"),
    locatorJson = o.getString("locatorJson"), createdAtEpochMs = o.optLong("createdAt", 0L)
)

private fun appearanceFromJson(o: JSONObject): ReaderAppearance = ReaderAppearance(
    theme = runCatching { ReaderTheme.valueOf(o.optString("theme", "DUSK")) }.getOrDefault(ReaderTheme.DUSK),
    fontScale = (o.optDouble("fontScale", 1.0).takeIf { it.isFinite() } ?: 1.0).coerceIn(.75, 1.8),
    lineHeight = (o.optDouble("lineHeight", 1.45).takeIf { it.isFinite() } ?: 1.45).coerceIn(1.1, 2.0),
    pageMargins = (o.optDouble("pageMargins", 1.0).takeIf { it.isFinite() } ?: 1.0).coerceIn(.5, 2.0),
    scroll = o.optBoolean("scroll", false), publisherStyles = o.optBoolean("publisherStyles", true)
)

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = buildList {
    for (i in 0 until length()) add(transform(getJSONObject(i)))
}

private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() && it != "null" }
