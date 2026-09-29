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
                put("schemaVersion", READING_HISTORY_SCHEMA_VERSION)
                put("id", event.id)
                put("kind", event.kind.name)
                put("timestampEpochMs", event.timestampEpochMs)
                put("activeMillis", event.activeMillis ?: JSONObject.NULL)
                put("pacedPageTurns", event.pacedPageTurns)
                put("highlightEventCount", event.highlightEventCount)
                put("noteEventCount", event.noteEventCount)
                put("annotated", event.annotated)
                put("excerpt", event.excerpt ?: JSONObject.NULL)
                put("locationLabel", event.locationLabel ?: JSONObject.NULL)
                put("milestoneKind", event.milestoneKind?.name ?: JSONObject.NULL)
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
            readingHistoryEventFromJson(item, kind)?.let(::add)
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

private const val READING_HISTORY_SCHEMA_VERSION = 2

private fun readingHistoryEventFromJson(
    item: JSONObject,
    kind: ReadingHistoryEventKind
): ReadingHistoryEvent? {
    val id = item.optString("id").takeIf { it.isNotBlank() } ?: return null
    val timestamp = item.optLong("timestampEpochMs", 0L)
    val semantic =
        item.optInt("schemaVersion", 1) >= READING_HISTORY_SCHEMA_VERSION ||
            item.has("activeMillis") ||
            item.has("excerpt") ||
            item.has("milestoneKind")

    if (semantic) {
        return ReadingHistoryEvent(
            id = id,
            kind = kind,
            timestampEpochMs = timestamp,
            activeMillis = item.optNullableLong("activeMillis"),
            pacedPageTurns = item.optInt("pacedPageTurns", 0).coerceAtLeast(0),
            highlightEventCount = item.optInt("highlightEventCount", 0).coerceAtLeast(0),
            noteEventCount = item.optInt("noteEventCount", 0).coerceAtLeast(0),
            annotated = item.optBoolean("annotated", false),
            excerpt = item.optNullableString("excerpt"),
            locationLabel = item.optNullableString("locationLabel"),
            milestoneKind = item.optNullableString("milestoneKind")
                ?.let { raw ->
                    runCatching { ReadingMilestoneKind.valueOf(raw) }.getOrNull()
                }
        )
    }

    return legacyReadingHistoryEvent(
        id = id,
        kind = kind,
        timestamp = timestamp,
        title = item.optString("title"),
        detail = item.optNullableString("detail")
    )
}

private fun legacyReadingHistoryEvent(
    id: String,
    kind: ReadingHistoryEventKind,
    timestamp: Long,
    title: String,
    detail: String?
): ReadingHistoryEvent =
    when (kind) {
        ReadingHistoryEventKind.READING_SESSION -> ReadingHistoryEvent(
            id = id,
            kind = kind,
            timestampEpochMs = timestamp,
            activeMillis = parseLegacyActiveMillis(detail),
            pacedPageTurns = parseLegacyCount(detail, "paced turns"),
            highlightEventCount = parseLegacyCount(detail, "highlight events"),
            noteEventCount = parseLegacyCount(detail, "note events")
        )

        ReadingHistoryEventKind.PASSAGE_PRESERVED -> ReadingHistoryEvent(
            id = id,
            kind = kind,
            timestampEpochMs = timestamp,
            annotated = title.contains("annotated", ignoreCase = true),
            excerpt = detail
        )

        ReadingHistoryEventKind.LOCATION_MARKED -> ReadingHistoryEvent(
            id = id,
            kind = kind,
            timestampEpochMs = timestamp,
            locationLabel = detail
        )

        ReadingHistoryEventKind.READING_MILESTONE -> ReadingHistoryEvent(
            id = id,
            kind = kind,
            timestampEpochMs = timestamp,
            milestoneKind = when {
                title.equals("First opened", ignoreCase = true) ->
                    ReadingMilestoneKind.FIRST_OPENED
                "25%" in title -> ReadingMilestoneKind.PROGRESS_25
                "50%" in title -> ReadingMilestoneKind.PROGRESS_50
                "75%" in title -> ReadingMilestoneKind.PROGRESS_75
                else -> null
            }
        )

        else -> ReadingHistoryEvent(
            id = id,
            kind = kind,
            timestampEpochMs = timestamp
        )
    }

private fun JSONObject.optNullableLong(name: String): Long? =
    if (!has(name) || isNull(name)) null else optLong(name)

private fun JSONObject.optNullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

private fun parseLegacyActiveMillis(detail: String?): Long? {
    val value = detail?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (value.startsWith("<1m active", ignoreCase = true)) return 0L

    Regex("""(\d+)h(?:\s+(\d+)m)?\s+active""", RegexOption.IGNORE_CASE)
        .find(value)
        ?.let { match ->
            val hours = match.groupValues[1].toLongOrNull() ?: 0L
            val minutes = match.groupValues.getOrNull(2)?.toLongOrNull() ?: 0L
            return (hours * 60L + minutes) * 60_000L
        }

    Regex("""(\d+)m\s+active""", RegexOption.IGNORE_CASE)
        .find(value)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.let { return it * 60_000L }

    return null
}

private fun parseLegacyCount(detail: String?, label: String): Int {
    val value = detail ?: return 0
    val pattern = Regex("""(\d+)\s+""" + Regex.escape(label), RegexOption.IGNORE_CASE)
    return pattern.find(value)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.coerceAtLeast(0)
        ?: 0
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
