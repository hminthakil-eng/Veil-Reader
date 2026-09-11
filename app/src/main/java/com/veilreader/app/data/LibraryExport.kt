package com.veilreader.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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
        val books = library.books.value.associateBy { it.id }
        val highlights = library.highlights.value.toList()
        val text = buildString {
            appendLine("# Veil Reader notebook")
            highlights.groupBy { it.bookId }.forEach { (id, passages) ->
                appendLine(); appendLine("## ${books[id]?.title ?: "Unknown book"}")
                appendLine(books[id]?.author.orEmpty())
                passages.forEach { passage ->
                    appendLine()
                    passage.quote.lines().forEach { appendLine("> $it") }
                    if (passage.note.isNotBlank()) { appendLine(); appendLine(passage.note) }
                    appendLine(); appendLine("---")
                }
            }
            if (highlights.isEmpty()) appendLine("No highlights saved yet.")
        }
        withContext(Dispatchers.IO) {
            val output = context.contentResolver.openOutputStream(destination, "wt")
                ?: error("Could not create the notebook file.")
            output.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
        }
    }

    suspend fun writeBackup(destination: Uri) {
        val books = library.books.value.toList()
        val libraryPrefs = JSONObject(context.getSharedPreferences(LIBRARY_PREFS, Context.MODE_PRIVATE).all)
        val gamePrefs = JSONObject(context.getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE).all)
        withContext(Dispatchers.IO) {
            val files = books.filter { it.isImported }.mapIndexed { index, book ->
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
                put("schemaVersion", BACKUP_SCHEMA)
                put("appVersion", "0.6.0")
                put("createdAtEpochMs", System.currentTimeMillis())
                put("libraryPreferences", libraryPrefs)
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
                val readme = "Veil Reader 0.6 backup. Restore it from Profile > Your data > Restore library backup. " +
                    "The archive can contain private books, highlights, and notes; keep it private.\n"
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

    /**
     * Restores a schema-1 Veil Reader backup after validating and staging it.
     * Existing preferences are replaced only after the archive has been fully checked and all
     * referenced publications have been copied into app-private storage.
     */
    suspend fun restoreBackup(source: Uri): BackupRestoreResult = withContext(Dispatchers.IO) {
        val stagingRoot = File(context.cacheDir, "veil-restore-${UUID.randomUUID()}").apply { mkdirs() }
        var installedRoot: File? = null
        try {
            extractValidatedBackup(source, stagingRoot)
            val manifestFile = File(stagingRoot, "manifest.json")
            require(manifestFile.isFile) { "This archive has no Veil Reader manifest." }
            require(manifestFile.length() <= MAX_MANIFEST_BYTES) { "The backup manifest is unexpectedly large." }
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            require(manifest.optInt("schemaVersion", -1) == BACKUP_SCHEMA) {
                "This backup version is not supported by this Veil Reader build."
            }

            val libraryPreferences = JSONObject(manifest.getJSONObject("libraryPreferences").toString())
            val gamePreferences = JSONObject(manifest.getJSONObject("gamePreferences").toString())
            val publicationRecords = manifest.optJSONArray("publications") ?: JSONArray()
            val archivedByBookId = mutableMapOf<String, File>()
            for (index in 0 until publicationRecords.length()) {
                val record = publicationRecords.getJSONObject(index)
                val bookId = record.getString("bookId")
                val archivePath = requireSafeArchivePath(record.getString("archivePath"))
                require(archivePath.startsWith("books/")) { "A publication is stored outside books/." }
                val staged = File(stagingRoot, archivePath).canonicalFile
                require(staged.toPath().startsWith(stagingRoot.canonicalFile.toPath()) && staged.isFile) {
                    "A publication referenced by the manifest is missing."
                }
                archivedByBookId[bookId] = staged
            }

            val books = JSONArray(libraryPreferences.optString("books", "[]"))
            val publicationsRoot = File(context.filesDir, "publications").apply { mkdirs() }.canonicalFile
            installedRoot = File(publicationsRoot, "restore-${UUID.randomUUID()}").apply { mkdirs() }.canonicalFile
            require(installedRoot.toPath().startsWith(publicationsRoot.toPath())) { "Invalid restore location." }

            var restoredBooks = 0
            for (index in 0 until books.length()) {
                val book = books.getJSONObject(index)
                val id = book.getString("id")
                val archived = archivedByBookId[id]
                if (archived == null) {
                    book.put("sourceUri", JSONObject.NULL)
                    continue
                }
                val format = book.optString("format", "EPUB").lowercase()
                require(format == "epub" || format == "pdf") { "Unsupported publication format in backup." }
                val target = File(installedRoot, "${UUID.randomUUID()}.$format")
                archived.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                require(target.length() > 0) { "A restored publication is empty." }
                book.put("sourceUri", Uri.fromFile(target).toString())
                restoredBooks += 1
            }
            libraryPreferences.put("books", books.toString())

            val libraryPrefs = context.getSharedPreferences(LIBRARY_PREFS, Context.MODE_PRIVATE)
            val gamePrefs = context.getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE)
            val oldLibrary = libraryPrefs.all.toMap()
            val oldGame = gamePrefs.all.toMap()

            require(replacePreferences(libraryPrefs, libraryPreferences)) { "Could not commit restored library data." }
            if (!replacePreferences(gamePrefs, gamePreferences)) {
                restorePreferencesSnapshot(libraryPrefs, oldLibrary)
                restorePreferencesSnapshot(gamePrefs, oldGame)
                error("Could not commit restored progression data. Your previous data was kept.")
            }

            publicationsRoot.listFiles()?.forEach { child ->
                if (child.canonicalFile != installedRoot) child.deleteRecursively()
            }

            val restoredHighlights = runCatching {
                JSONArray(libraryPreferences.optString("highlights", "[]")).length()
            }.getOrDefault(0)
            BackupRestoreResult(restoredBooks, restoredHighlights)
        } catch (error: Throwable) {
            installedRoot?.deleteRecursively()
            throw error
        } finally {
            stagingRoot.deleteRecursively()
        }
    }

    private fun extractValidatedBackup(source: Uri, stagingRoot: File) {
        val input = context.contentResolver.openInputStream(source) ?: error("Could not read the selected backup.")
        var totalBytes = 0L
        var entryCount = 0
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                require(entryCount <= MAX_ENTRIES) { "The backup contains too many files." }
                val safePath = requireSafeArchivePath(entry.name)
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
                    .filter { it.isNotBlank() }
                    .toSet()
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
        private const val LIBRARY_PREFS = "veil_library_v1"
        private const val GAME_PREFS = "veil_game_v1"
        private const val BACKUP_SCHEMA = 1
        private const val MAX_ENTRIES = 2_000
        private const val MAX_MANIFEST_BYTES = 5L * 1024L * 1024L
        private const val MAX_BACKUP_BYTES = 2L * 1024L * 1024L * 1024L
    }
}

data class BackupRestoreResult(val booksRestored: Int, val highlightsRestored: Int)
