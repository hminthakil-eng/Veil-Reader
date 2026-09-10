package com.veilreader.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** User-initiated exports through Android's document picker. No server or account is involved. */
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
        // Snapshot flows and preferences before leaving the main dispatcher.
        val books = library.books.value.toList()
        val libraryPrefs = JSONObject(context.getSharedPreferences("veil_library_v1", Context.MODE_PRIVATE).all)
        val gamePrefs = JSONObject(context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE).all)
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
                put("schemaVersion", 1)
                put("appVersion", "0.5.0")
                put("createdAtEpochMs", System.currentTimeMillis())
                put("libraryPreferences", libraryPrefs)
                put("gamePreferences", gamePrefs)
                put("publications", JSONArray().apply {
                    files.forEach { (book, _, path) -> put(JSONObject().apply {
                        put("bookId", book.id); put("title", book.title); put("author", book.author); put("archivePath", path)
                    }) }
                })
            }
            val output = context.contentResolver.openOutputStream(destination, "wt")
                ?: error("Could not create the backup file.")
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8)); zip.closeEntry()
                zip.putNextEntry(ZipEntry("README.txt"))
                zip.write("Veil Reader backup: extract books/ to recover your EPUB/PDF files. manifest.json preserves reading data and annotations. Automatic in-app restore is not implemented in 0.5.0. Keep this archive private if your books or notes are private.\n".toByteArray())
                zip.closeEntry()
                files.forEach { (_, file, path) ->
                    zip.putNextEntry(ZipEntry(path))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }
}
