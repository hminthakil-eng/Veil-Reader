package com.veilreader.app.manga.reader.ui

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.reader.MangaOrientationPolicy
import com.veilreader.app.manga.reader.MangaPageDirection
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.MangaReaderSnapshot
import com.veilreader.app.manga.reader.MangaZoomState

/**
 * Platform-neutral SavedState payload.
 *
 * Android SavedStateHandle/Bundle adapters can store these individual strings without serializing
 * internal Kotlin data classes or provider objects.
 */
object MangaReaderSavedStateCodec {
    private const val VERSION = "version"
    private const val MANGA_ID = "manga_id"
    private const val VOLUME = "volume"
    private const val CHAPTER_NUMBER = "chapter_number"
    private const val LANGUAGE = "language"
    private const val TITLE = "title"
    private const val PROVIDER_HINT = "provider_hint"
    private const val MODE = "mode"
    private const val DIRECTION = "direction"
    private const val ORIENTATION = "orientation"
    private const val ITEM_INDEX = "item_index"
    private const val WEBTOON_OFFSET = "webtoon_offset"
    private const val ZOOM_SCALE = "zoom_scale"
    private const val ZOOM_X = "zoom_x"
    private const val ZOOM_Y = "zoom_y"

    val keys: Set<String> = setOf(
        VERSION,
        MANGA_ID,
        VOLUME,
        CHAPTER_NUMBER,
        LANGUAGE,
        TITLE,
        PROVIDER_HINT,
        MODE,
        DIRECTION,
        ORIENTATION,
        ITEM_INDEX,
        WEBTOON_OFFSET,
        ZOOM_SCALE,
        ZOOM_X,
        ZOOM_Y
    )

    fun encode(snapshot: MangaReaderSnapshot): Map<String, String> = buildMap {
        put(VERSION, snapshot.version.toString())
        put(MANGA_ID, snapshot.chapter.mangaId.value)
        snapshot.chapter.anchor.volume?.let { put(VOLUME, it.toString()) }
        snapshot.chapter.anchor.number?.let { put(CHAPTER_NUMBER, it.toString()) }
        snapshot.chapter.anchor.languageTag?.let { put(LANGUAGE, it) }
        snapshot.chapter.anchor.normalizedTitle?.let { put(TITLE, it) }
        snapshot.chapter.anchor.providerChapterKeyHint?.let { put(PROVIDER_HINT, it) }
        put(MODE, snapshot.mode.name)
        put(DIRECTION, snapshot.direction.name)
        put(ORIENTATION, snapshot.orientationPolicy.name)
        put(ITEM_INDEX, snapshot.itemIndex.toString())
        put(WEBTOON_OFFSET, snapshot.webtoonOffsetFraction.toString())
        put(ZOOM_SCALE, snapshot.zoom.scale.toString())
        put(ZOOM_X, snapshot.zoom.centerXFraction.toString())
        put(ZOOM_Y, snapshot.zoom.centerYFraction.toString())
    }

    fun decode(values: Map<String, String>): MangaReaderSnapshot? = runCatching {
        val version = values[VERSION]?.toIntOrNull() ?: return null
        if (version != MangaReaderSnapshot.CURRENT_VERSION) return null

        val mangaId = values[MANGA_ID]?.takeIf(String::isNotBlank) ?: return null
        val volume = values[VOLUME]?.toDoubleOrNull()
        val chapterNumber = values[CHAPTER_NUMBER]?.toDoubleOrNull()
        val title = values[TITLE]?.takeIf(String::isNotBlank)
        val providerHint = values[PROVIDER_HINT]?.takeIf(String::isNotBlank)

        val anchor = MangaChapterAnchor(
            volume = volume,
            number = chapterNumber,
            languageTag = values[LANGUAGE]?.takeIf(String::isNotBlank),
            normalizedTitle = title,
            providerChapterKeyHint = providerHint
        )

        MangaReaderSnapshot(
            version = version,
            chapter = MangaReaderChapterRef(
                mangaId = CanonicalMangaId(mangaId),
                anchor = anchor
            ),
            mode = enumValueOf<MangaReaderMode>(requireNotNull(values[MODE])),
            direction = enumValueOf<MangaPageDirection>(requireNotNull(values[DIRECTION])),
            orientationPolicy = enumValueOf<MangaOrientationPolicy>(
                requireNotNull(values[ORIENTATION])
            ),
            itemIndex = requireNotNull(values[ITEM_INDEX]).toInt(),
            webtoonOffsetFraction = requireNotNull(values[WEBTOON_OFFSET]).toDouble(),
            zoom = MangaZoomState(
                scale = requireNotNull(values[ZOOM_SCALE]).toDouble(),
                centerXFraction = requireNotNull(values[ZOOM_X]).toDouble(),
                centerYFraction = requireNotNull(values[ZOOM_Y]).toDouble()
            )
        )
    }.getOrNull()
}
