package com.veilreader.app.data.db

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.ReadingSessionSnapshot

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    progress = progress,
    currentChapter = currentChapter,
    totalPages = totalPages,
    pagesRead = pagesRead,
    format = format.name,
    sourceUri = sourceUri,
    mediaType = mediaType,
    locatorJson = locatorJson,
    addedAtEpochMs = addedAtEpochMs,
    lastOpenedAtEpochMs = lastOpenedAtEpochMs,
    finished = finished,
    favorite = favorite
)

fun BookWithCollections.toDomain(): Book = Book(
    id = book.id,
    title = book.title,
    author = book.author,
    progress = book.progress,
    currentChapter = book.currentChapter,
    totalPages = book.totalPages,
    pagesRead = book.pagesRead,
    format = runCatching { BookFormat.valueOf(book.format) }.getOrDefault(BookFormat.EPUB),
    sourceUri = book.sourceUri,
    mediaType = book.mediaType,
    locatorJson = book.locatorJson,
    addedAtEpochMs = book.addedAtEpochMs,
    lastOpenedAtEpochMs = book.lastOpenedAtEpochMs,
    finished = book.finished,
    favorite = book.favorite,
    collection = collections.firstOrNull()?.name.orEmpty()
)

fun Highlight.toEntity(): HighlightEntity = HighlightEntity(
    id = id,
    bookId = bookId,
    quote = quote,
    locatorJson = locatorJson,
    note = note,
    createdAtEpochMs = createdAtEpochMs
)

fun HighlightEntity.toDomain(): Highlight = Highlight(
    id = id,
    bookId = bookId,
    quote = quote,
    locatorJson = locatorJson,
    note = note,
    createdAtEpochMs = createdAtEpochMs
)

fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    bookId = bookId,
    label = label,
    locatorJson = locatorJson,
    createdAtEpochMs = createdAtEpochMs
)

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    bookId = bookId,
    label = label,
    locatorJson = locatorJson,
    createdAtEpochMs = createdAtEpochMs
)

fun ReadingSessionSnapshot.toEntity(): ReadingSessionEntity = ReadingSessionEntity(
    id = id,
    bookId = bookId,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    activeMillis = activeMillis,
    pacedPageTurns = pacedPageTurns,
    highlightCount = highlightCount,
    noteCount = noteCount
)

fun ReadingSessionEntity.toSnapshot(): ReadingSessionSnapshot = ReadingSessionSnapshot(
    id = id,
    bookId = bookId,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs ?: startedAtEpochMs,
    activeMillis = activeMillis,
    pacedPageTurns = pacedPageTurns,
    highlightCount = highlightCount,
    noteCount = noteCount
)
