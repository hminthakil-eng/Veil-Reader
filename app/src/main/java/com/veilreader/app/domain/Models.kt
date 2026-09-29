package com.veilreader.app.domain

import java.util.Locale

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val progress: Float = 0f,
    val currentChapter: String = "Not started",
    val totalPages: Int = 0,
    val pagesRead: Int = 0,
    val format: BookFormat = BookFormat.EPUB,
    val sourceUri: String? = null,
    val mediaType: String? = null,
    val locatorJson: String? = null,
    val addedAtEpochMs: Long = System.currentTimeMillis(),
    val lastOpenedAtEpochMs: Long = 0L,
    val finished: Boolean = false,
    val favorite: Boolean = false,
    /** Absolute app-private thumbnail path. Empty means cover extraction was attempted but unavailable. */
    val coverCachePath: String? = null,
    /** SHA-256 of the app-private publication copy. Derived metadata used for duplicate detection. */
    val contentFingerprint: String? = null,
    val seriesName: String? = null,
    val seriesIndex: Double? = null,
    /** Primary BCP-47 language tag when supplied by the publication. */
    val language: String? = null,
    /**
     * Compatibility field for older UI/backup code. New code should use [collections].
     * When both are present, [allCollections] merges them case-insensitively.
     */
    val collection: String = "",
    /** Every user collection this book belongs to. */
    val collections: List<String> = emptyList()
) {
    val isImported: Boolean get() = sourceUri != null
    val hasCachedCover: Boolean get() = !coverCachePath.isNullOrBlank()
    val allCollections: List<String>
        get() = (collections + collection)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase(Locale.ROOT) }
}

data class BookMetadataUpdate(
    val bookId: String,
    val title: String,
    val author: String,
    val collections: List<String> = emptyList(),
    val seriesName: String? = null,
    val seriesIndex: Double? = null,
    val language: String? = null
)

enum class BookFormat { EPUB, PDF, AUDIO, COMIC }

data class ReadingPath(
    val id: String,
    val name: String,
    val epithet: String,
    val description: String,
    val ranks: List<String>
)

data class ReaderProfile(
    val level: Int,
    val xp: Int,
    val xpForNextLevel: Int,
    val streakDays: Int,
    val pagesRead: Int,
    val minutesRead: Int,
    val booksFinished: Int,
    val path: ReadingPath,
    val rankIndex: Int,
    val ritualProgress: Int,
    val ritualTarget: Int,
    val earnedSigils: Set<String> = emptySet()
) {
    val rankName: String get() = path.ranks.getOrElse(rankIndex) { path.ranks.last() }
}

data class CastleRoom(
    val id: String,
    val name: String,
    val purpose: String,
    val unlockRankIndex: Int,
    val symbol: String
)

data class Quest(
    val id: String,
    val title: String,
    val progress: Int,
    val target: Int,
    val xpReward: Int
)

data class Highlight(
    val id: String,
    val bookId: String,
    val quote: String,
    val locatorJson: String,
    val note: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

enum class ReaderTheme { PAPER, SEPIA, DUSK, OLED }

enum class PageTurnStyle { PAPER, SLIDE, NONE }

/**
 * User-facing reader navigation modes.
 *
 * Persistence stays backward-compatible through [ReaderAppearance.scroll] and
 * [ReaderAppearance.pageTurnStyle]; this enum gives the UI one clear,
 * mutually-exclusive mode selector.
 */
enum class ReaderNavigationMode { PAPER_CURL, SLIDE, PAGED, SCROLL }

enum class ReaderAppearanceScope { GLOBAL, BOOK }

data class ReaderAppearance(
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val fontScale: Double = 1.0,
    val lineHeight: Double = 1.45,
    val pageMargins: Double = 1.0,
    val scroll: Boolean = false,
    val publisherStyles: Boolean = true,
    val pageTurnStyle: PageTurnStyle = PageTurnStyle.PAPER,
    val screenBrightness: Double? = null
) {
    val navigationMode: ReaderNavigationMode
        get() = when {
            scroll -> ReaderNavigationMode.SCROLL
            pageTurnStyle == PageTurnStyle.SLIDE -> ReaderNavigationMode.SLIDE
            pageTurnStyle == PageTurnStyle.NONE -> ReaderNavigationMode.PAGED
            else -> ReaderNavigationMode.PAPER_CURL
        }

    fun withNavigationMode(mode: ReaderNavigationMode): ReaderAppearance =
        when (mode) {
            ReaderNavigationMode.PAPER_CURL ->
                copy(scroll = false, pageTurnStyle = PageTurnStyle.PAPER)
            ReaderNavigationMode.SLIDE ->
                copy(scroll = false, pageTurnStyle = PageTurnStyle.SLIDE)
            ReaderNavigationMode.PAGED ->
                copy(scroll = false, pageTurnStyle = PageTurnStyle.NONE)
            ReaderNavigationMode.SCROLL ->
                copy(scroll = true, pageTurnStyle = PageTurnStyle.NONE)
        }

    /**
     * Repairs legacy or externally-constructed states where continuous scroll still carries a
     * hidden paginated transition. Keeping one canonical representation prevents Scroll from
     * resurrecting Curl/Slide when older persistence or UI code toggles the boolean directly.
     */
    fun canonicalizedNavigation(): ReaderAppearance =
        if (scroll && pageTurnStyle != PageTurnStyle.NONE) {
            copy(pageTurnStyle = PageTurnStyle.NONE)
        } else {
            this
        }

    fun withTheme(theme: ReaderTheme): ReaderAppearance =
        copy(theme = theme, publisherStyles = false)

    fun withFontScale(value: Double): ReaderAppearance =
        copy(fontScale = value, publisherStyles = false)

    fun withLineHeight(value: Double): ReaderAppearance =
        copy(lineHeight = value, publisherStyles = false)

    fun withPageMargins(value: Double): ReaderAppearance =
        copy(pageMargins = value, publisherStyles = false)

    fun withScreenBrightness(value: Double?): ReaderAppearance =
        copy(
            screenBrightness = value
                ?.takeIf { it.isFinite() }
                ?.coerceIn(0.05, 1.0)
        )
}

/** A saved reading location, independent of text selection (also supports PDF). */
data class Bookmark(
    val id: String,
    val bookId: String,
    val label: String,
    val locatorJson: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
