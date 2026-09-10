package com.veilreader.app.domain

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
    val collection: String = ""
) {
    val isImported: Boolean get() = sourceUri != null
}

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

enum class ReaderTheme { PAPER, SEPIA, DUSK, OLED }

data class ReaderAppearance(
    val theme: ReaderTheme = ReaderTheme.DUSK,
    val fontScale: Double = 1.0,
    val lineHeight: Double = 1.45,
    val pageMargins: Double = 1.0,
    val scroll: Boolean = false,
    val publisherStyles: Boolean = true
)

/** A saved reading location, independent of text selection (also supports PDF). */
data class Bookmark(
    val id: String,
    val bookId: String,
    val label: String,
    val locatorJson: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
