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

data class RitualAftermathRecord(
    val pathId: String,
    val fromRankIndex: Int,
    val toRankIndex: Int,
    val sealedAtEpochMs: Long
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
    val earnedSigils: Set<String> = emptySet(),
    val earnedDiscoveries: Set<String> = emptySet(),
    val ritualAftermath: RitualAftermathRecord? = null
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

enum class ReaderFontFamily {
    PUBLISHER,
    SERIF,
    SANS_SERIF,
    MONOSPACE,
    OPEN_DYSLEXIC,
    ACCESSIBLE_DFA,
    IA_WRITER_DUOSPACE
}

enum class ReaderTextAlignment { PUBLISHER, START, JUSTIFY, CENTER }

enum class ReaderColumnMode { AUTO, ONE, TWO }

enum class ReaderPreferenceToggle { DEFAULT, ON, OFF }

data class ReaderAppearance(
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val fontScale: Double = 1.0,
    val lineHeight: Double = 1.45,
    val pageMargins: Double = 1.0,
    val scroll: Boolean = false,
    val publisherStyles: Boolean = true,
    val pageTurnStyle: PageTurnStyle = PageTurnStyle.PAPER,
    val screenBrightness: Double? = null,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.PUBLISHER,
    val textAlignment: ReaderTextAlignment = ReaderTextAlignment.PUBLISHER,
    val columnMode: ReaderColumnMode = ReaderColumnMode.AUTO,
    val hyphenation: ReaderPreferenceToggle = ReaderPreferenceToggle.DEFAULT,
    val ligatures: ReaderPreferenceToggle = ReaderPreferenceToggle.DEFAULT,
    val textNormalization: ReaderPreferenceToggle = ReaderPreferenceToggle.DEFAULT,
    val paragraphSpacing: Double? = null,
    val paragraphIndent: Double? = null,
    val letterSpacing: Double? = null,
    val wordSpacing: Double? = null,
    val typeScale: Double? = null,
    val paperPatina: Double = 0.72
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
                copy(scroll = true)
        }
    fun withTheme(theme: ReaderTheme): ReaderAppearance =
        copy(theme = theme, publisherStyles = false)

    fun withFontScale(value: Double): ReaderAppearance =
        copy(fontScale = value, publisherStyles = false)

    fun withLineHeight(value: Double): ReaderAppearance =
        copy(lineHeight = value, publisherStyles = false)

    fun withPageMargins(value: Double): ReaderAppearance =
        copy(pageMargins = value, publisherStyles = false)

    fun withFontFamily(value: ReaderFontFamily): ReaderAppearance =
        copy(
            fontFamily = value,
            publisherStyles = if (value == ReaderFontFamily.PUBLISHER) publisherStyles else false
        )

    fun withTextAlignment(value: ReaderTextAlignment): ReaderAppearance =
        copy(
            textAlignment = value,
            publisherStyles = if (value == ReaderTextAlignment.PUBLISHER) publisherStyles else false
        )

    fun withParagraphSpacing(value: Double?): ReaderAppearance =
        copy(
            paragraphSpacing = value?.takeIf { it.isFinite() }?.coerceIn(0.0, 2.0),
            publisherStyles = false
        )

    fun withParagraphIndent(value: Double?): ReaderAppearance =
        copy(
            paragraphIndent = value?.takeIf { it.isFinite() }?.coerceIn(0.0, 3.0),
            publisherStyles = false
        )

    fun withLetterSpacing(value: Double?): ReaderAppearance =
        copy(
            letterSpacing = value?.takeIf { it.isFinite() }?.coerceIn(0.0, 0.2),
            publisherStyles = false
        )

    fun withWordSpacing(value: Double?): ReaderAppearance =
        copy(
            wordSpacing = value?.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0),
            publisherStyles = false
        )

    fun withTypeScale(value: Double?): ReaderAppearance =
        copy(
            typeScale = value?.takeIf { it.isFinite() }?.coerceIn(1.0, 2.0),
            publisherStyles = false
        )

    fun withPaperPatina(value: Double): ReaderAppearance =
        copy(
            paperPatina = value.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.72
        )

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
