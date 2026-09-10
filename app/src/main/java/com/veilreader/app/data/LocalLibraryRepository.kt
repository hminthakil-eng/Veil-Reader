package com.veilreader.app.data

import android.content.Context
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Lightweight local bookshelf storage for the first shippable build.
 *
 * We intentionally use a compact JSON document in SharedPreferences instead of introducing a
 * database migration surface before the reader UX is settled. The repository boundary makes a
 * later Room/cloud implementation a drop-in replacement.
 */
class LocalLibraryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("veil_library_v1", Context.MODE_PRIVATE)

    private val _books = MutableStateFlow(loadBooks())
    val books: StateFlow<List<Book>> = _books

    private val _highlights = MutableStateFlow(loadHighlights())
    val highlights: StateFlow<List<Highlight>> = _highlights

    private val _bookmarks = MutableStateFlow(loadBookmarks())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks

    fun addBookmark(bookId: String, label: String, locatorJson: String): Boolean {
        if (_bookmarks.value.any { it.bookId == bookId && it.locatorJson == locatorJson }) return false
        _bookmarks.value = listOf(Bookmark(UUID.randomUUID().toString(), bookId, label, locatorJson)) + _bookmarks.value
        persistBookmarks()
        return true
    }

    fun deleteBookmark(id: String) {
        _bookmarks.value = _bookmarks.value.filterNot { it.id == id }
        persistBookmarks()
    }

    fun updateHighlightNote(id: String, note: String) {
        _highlights.value = _highlights.value.map { if (it.id == id) it.copy(note = note.trim()) else it }
        persistHighlights()
    }

    fun loadAppearance(): ReaderAppearance = runCatching {
        val o = JSONObject(prefs.getString("appearance", "{}") ?: "{}")
        ReaderAppearance(
            theme = runCatching { ReaderTheme.valueOf(o.optString("theme", "DUSK")) }.getOrDefault(ReaderTheme.DUSK),
            fontScale = (o.optDouble("fontScale", 1.0).takeIf { it.isFinite() } ?: 1.0).coerceIn(.75, 1.8),
            lineHeight = (o.optDouble("lineHeight", 1.45).takeIf { it.isFinite() } ?: 1.45).coerceIn(1.1, 2.0),
            scroll = o.optBoolean("scroll", false),
            publisherStyles = o.optBoolean("publisherStyles", true)
        )
    }.getOrDefault(ReaderAppearance())

    fun saveAppearance(value: ReaderAppearance) {
        val o = JSONObject().apply {
            put("theme", value.theme.name)
            put("fontScale", value.fontScale)
            put("lineHeight", value.lineHeight)
            put("scroll", value.scroll)
            put("publisherStyles", value.publisherStyles)
        }
        prefs.edit().putString("appearance", o.toString()).apply()
    }

    private fun persistBookmarks() {
        val array = JSONArray()
        _bookmarks.value.forEach { b -> array.put(JSONObject().apply {
            put("id", b.id)
            put("bookId", b.bookId)
            put("label", b.label)
            put("locatorJson", b.locatorJson)
            put("createdAt", b.createdAtEpochMs)
        }) }
        prefs.edit().putString("bookmarks", array.toString()).apply()
    }

    private fun loadBookmarks(): List<Bookmark> = runCatching {
        val array = JSONArray(prefs.getString("bookmarks", "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(Bookmark(o.getString("id"), o.getString("bookId"),
                    o.optString("label", "Bookmark"), o.getString("locatorJson"), o.optLong("createdAt", 0L)))
            }
        }
    }.getOrDefault(emptyList())

    fun addImportedBook(book: Book) {
        val existing = _books.value.indexOfFirst { it.sourceUri == book.sourceUri }
        _books.value = if (existing >= 0) {
            _books.value.toMutableList().also { it[existing] = book.copy(id = it[existing].id) }
        } else {
            listOf(book) + _books.value
        }
        persistBooks()
    }

    fun getBook(id: String): Book? = _books.value.firstOrNull { it.id == id }

    fun editMetadata(id: String, title: String, author: String, collection: String) {
        require(title.isNotBlank()) { "A book title cannot be empty." }
        updateBook(id) { it.copy(title = title.trim(), author = author.trim().ifEmpty { "Unknown author" }, collection = collection.trim()) }
    }

    fun toggleFavorite(id: String) { updateBook(id) { it.copy(favorite = !it.favorite) } }

    fun markOpened(id: String) {
        updateBook(id) { it.copy(lastOpenedAtEpochMs = System.currentTimeMillis()) }
    }

    /** Returns true when this update completed the book for the first time. */
    fun saveProgress(id: String, progression: Double, locatorJson: String): Boolean {
        var newlyFinished = false
        updateBook(id) { book ->
            val safe = (if (progression.isFinite()) progression else book.progress.toDouble()).coerceIn(0.0, 1.0).toFloat()
            val finishedNow = safe >= 0.995f
            newlyFinished = finishedNow && !book.finished
            val estimatedRead = if (book.totalPages > 0) {
                (book.totalPages * safe).toInt().coerceAtMost(book.totalPages)
            } else book.pagesRead
            book.copy(
                progress = safe,
                pagesRead = estimatedRead,
                locatorJson = locatorJson,
                lastOpenedAtEpochMs = System.currentTimeMillis(),
                finished = book.finished || finishedNow
            )
        }
        return newlyFinished
    }

    fun addHighlight(bookId: String, quote: String, locatorJson: String): Highlight {
        _highlights.value.firstOrNull {
            it.bookId == bookId && it.locatorJson == locatorJson && it.quote == quote.trim()
        }?.let { return it }
        val record = Highlight(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            quote = quote.trim(),
            locatorJson = locatorJson
        )
        _highlights.value = listOf(record) + _highlights.value
        persistHighlights()
        return record
    }

    fun highlightsFor(bookId: String): List<Highlight> =
        _highlights.value.filter { it.bookId == bookId }

    fun deleteHighlight(id: String) {
        _highlights.value = _highlights.value.filterNot { it.id == id }
        persistHighlights()
    }

    private fun updateBook(id: String, transform: (Book) -> Book) {
        _books.value = _books.value.map { if (it.id == id) transform(it) else it }
        persistBooks()
    }

    private fun persistBooks() {
        val array = JSONArray()
        _books.value.forEach { b ->
            array.put(JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("author", b.author)
                put("progress", b.progress.toDouble())
                put("currentChapter", b.currentChapter)
                put("totalPages", b.totalPages)
                put("pagesRead", b.pagesRead)
                put("format", b.format.name)
                put("sourceUri", b.sourceUri)
                put("mediaType", b.mediaType)
                put("locatorJson", b.locatorJson)
                put("addedAt", b.addedAtEpochMs)
                put("lastOpenedAt", b.lastOpenedAtEpochMs)
                put("finished", b.finished)
                put("favorite", b.favorite)
                put("collection", b.collection)
            })
        }
        prefs.edit().putString("books", array.toString()).apply()
    }

    private fun loadBooks(): List<Book> = runCatching {
        val raw = prefs.getString("books", null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Book(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        author = o.optString("author", "Unknown author"),
                        progress = o.optDouble("progress", 0.0).toFloat(),
                        currentChapter = o.optString("currentChapter", "Not started"),
                        totalPages = o.optInt("totalPages", 0),
                        pagesRead = o.optInt("pagesRead", 0),
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
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun persistHighlights() {
        val array = JSONArray()
        _highlights.value.forEach { h ->
            array.put(JSONObject().apply {
                put("id", h.id)
                put("bookId", h.bookId)
                put("quote", h.quote)
                put("locatorJson", h.locatorJson)
                put("note", h.note)
                put("createdAt", h.createdAtEpochMs)
            })
        }
        prefs.edit().putString("highlights", array.toString()).apply()
    }

    private fun loadHighlights(): List<Highlight> = runCatching {
        val raw = prefs.getString("highlights", null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Highlight(
                        id = o.getString("id"),
                        bookId = o.getString("bookId"),
                        quote = o.optString("quote"),
                        locatorJson = o.getString("locatorJson"),
                        note = o.optString("note"),
                        createdAtEpochMs = o.optLong("createdAt", 0L)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotBlank() && it != "null" }
