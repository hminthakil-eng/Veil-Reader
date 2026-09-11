package com.veilreader.app.data.migration

import android.content.Context
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import org.json.JSONArray
import org.json.JSONObject

/** Read-only parser for the pre-Room 0.x bookshelf. */
internal class LegacyLibrarySnapshotReader(context: Context) {
    private val prefs = context.getSharedPreferences("veil_library_v1", Context.MODE_PRIVATE)

    val books: List<Book> by lazy { loadBooks() }
    val highlights: List<Highlight> by lazy { loadHighlights() }
    val bookmarks: List<Bookmark> by lazy { loadBookmarks() }

    fun appearance(): ReaderAppearance = runCatching {
        val o = JSONObject(prefs.getString("appearance", "{}") ?: "{}")
        ReaderAppearance(
            theme = runCatching { ReaderTheme.valueOf(o.optString("theme", "DUSK")) }
                .getOrDefault(ReaderTheme.DUSK),
            fontScale = (o.optDouble("fontScale", 1.0).takeIf { it.isFinite() } ?: 1.0)
                .coerceIn(.75, 1.8),
            lineHeight = (o.optDouble("lineHeight", 1.45).takeIf { it.isFinite() } ?: 1.45)
                .coerceIn(1.1, 2.0),
            pageMargins = (o.optDouble("pageMargins", 1.0).takeIf { it.isFinite() } ?: 1.0)
                .coerceIn(.5, 2.0),
            scroll = o.optBoolean("scroll", false),
            publisherStyles = o.optBoolean("publisherStyles", true)
        )
    }.getOrDefault(ReaderAppearance())

    private fun loadBookmarks(): List<Bookmark> = runCatching {
        val array = JSONArray(prefs.getString("bookmarks", "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Bookmark(
                        id = o.getString("id"),
                        bookId = o.getString("bookId"),
                        label = o.optString("label", "Bookmark"),
                        locatorJson = o.getString("locatorJson"),
                        createdAtEpochMs = o.optLong("createdAt", 0L)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

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
                        format = runCatching { BookFormat.valueOf(o.optString("format", "EPUB")) }
                            .getOrDefault(BookFormat.EPUB),
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
