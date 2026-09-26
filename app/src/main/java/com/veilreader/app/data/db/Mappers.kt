package com.veilreader.app.data.db

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.PassageVisit
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingHistoryEvent
import com.veilreader.app.domain.ReadingHistoryEventKind
import com.veilreader.app.domain.ReadingMilestoneKind
import com.veilreader.app.domain.ReadingMilestoneRecord
import com.veilreader.app.domain.ReadingSessionSnapshot
import org.json.JSONArray
import org.json.JSONObject

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
    favorite = favorite,
    coverCachePath = coverCachePath,
    contentFingerprint = contentFingerprint,
    seriesName = seriesName,
    seriesIndex = seriesIndex,
    language = language
)

fun BookWithCollections.toDomain(): Book {
    val collectionNames = collections
        .map { it.name.trim() }
        .filter { it.isNotEmpty() }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
    return Book(
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
        coverCachePath = book.coverCachePath,
        contentFingerprint = book.contentFingerprint,
        seriesName = book.seriesName,
        seriesIndex = book.seriesIndex,
        language = book.language,
        collection = collectionNames.firstOrNull().orEmpty(),
        collections = collectionNames
    )
}

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


fun ReadingCycleRecord.toEntity(): ReadingCycleEntity = ReadingCycleEntity(
    id = id,
    bookId = bookId,
    cycleIndex = cycleIndex,
    titleSnapshot = titleSnapshot,
    authorSnapshot = authorSnapshot,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    finalLocatorJson = finalLocatorJson,
    sessionCount = sessionCount,
    totalActiveMillis = totalActiveMillis,
    pacedPageTurns = pacedPageTurns,
    highlightCount = highlightCount,
    noteCount = noteCount,
    bookmarkCount = bookmarkCount,
    sealCode = sealCode,
    timelineJson = JSONArray().apply {
        timeline.forEach { event ->
            put(JSONObject().apply {
                put("id", event.id)
                put("kind", event.kind.name)
                put("timestampEpochMs", event.timestampEpochMs)
                put("title", event.title)
                put("detail", event.detail ?: JSONObject.NULL)
            })
        }
    }.toString()
)

fun ReadingCycleEntity.toDomain(): ReadingCycleRecord {
    val events = buildList {
        val array = runCatching { JSONArray(timelineJson) }.getOrDefault(JSONArray())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val kind = runCatching {
                ReadingHistoryEventKind.valueOf(item.optString("kind"))
            }.getOrNull() ?: continue
            add(
                ReadingHistoryEvent(
                    id = item.optString("id"),
                    kind = kind,
                    timestampEpochMs = item.optLong("timestampEpochMs", 0L),
                    title = item.optString("title"),
                    detail = if (item.isNull("detail")) null
                    else item.optString("detail").takeIf { it.isNotBlank() }
                )
            )
        }
    }

    return ReadingCycleRecord(
        id = id,
        bookId = bookId,
        cycleIndex = cycleIndex,
        titleSnapshot = titleSnapshot,
        authorSnapshot = authorSnapshot,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        finalLocatorJson = finalLocatorJson,
        sessionCount = sessionCount,
        totalActiveMillis = totalActiveMillis,
        pacedPageTurns = pacedPageTurns,
        highlightCount = highlightCount,
        noteCount = noteCount,
        bookmarkCount = bookmarkCount,
        sealCode = sealCode,
        timeline = events
    )
}

fun PassageVisit.toEntity(): PassageVisitEntity = PassageVisitEntity(
    id = id,
    highlightId = highlightId,
    bookId = bookId,
    locatorJson = locatorJson,
    viewedAtEpochMs = viewedAtEpochMs
)

fun PassageVisitEntity.toDomain(): PassageVisit = PassageVisit(
    id = id,
    highlightId = highlightId,
    bookId = bookId,
    locatorJson = locatorJson,
    viewedAtEpochMs = viewedAtEpochMs
)


fun ReadingMilestoneRecord.toEntity(): ReadingMilestoneEntity = ReadingMilestoneEntity(
    id = id,
    bookId = bookId,
    kind = kind.name,
    reachedAtEpochMs = reachedAtEpochMs,
    progression = progression,
    locatorJson = locatorJson
)

fun ReadingMilestoneEntity.toDomain(): ReadingMilestoneRecord = ReadingMilestoneRecord(
    id = id,
    bookId = bookId,
    kind = runCatching { ReadingMilestoneKind.valueOf(kind) }
        .getOrDefault(ReadingMilestoneKind.FIRST_OPENED),
    reachedAtEpochMs = reachedAtEpochMs,
    progression = progression.coerceIn(0f, 1f),
    locatorJson = locatorJson
)
