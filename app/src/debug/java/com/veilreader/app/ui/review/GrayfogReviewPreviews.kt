package com.veilreader.app.ui.review

import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.veilreader.app.data.SampleData
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.VeilWorldBackdrop
import com.veilreader.app.ui.screens.*
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilTheme

/** Debug-only review fixtures, never imported publications or historical evidence. */
@Preview(name = "Compact 100%", widthDp = 320, heightDp = 720)
@Preview(name = "Persian 130%", widthDp = 412, heightDp = 840, locale = "fa", fontScale = 1.3f)
@Preview(name = "Compact 150%", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Preview(name = "Persian 200%", widthDp = 360, heightDp = 800, locale = "fa", fontScale = 2f)
@Preview(name = "Foldable", widthDp = 720, heightDp = 720)
@Preview(name = "Landscape", widthDp = 900, heightDp = 420)
@Preview(name = "Tablet", widthDp = 1280, heightDp = 900)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
private annotation class GrayfogReviewSizes

@Composable
private fun ReviewFrame(content: @Composable () -> Unit) {
    VeilTheme(themeMode = AppThemeMode.DARK) {
        CompositionLocalProvider(LocalVeilReducedMotion provides true) {
            VeilWorldBackdrop { content() }
        }
    }
}

@GrayfogReviewSizes
@Composable
private fun ThresholdReview() = ReviewFrame {
    ReadingNowScreen(
        books = SampleData.books, profile = SampleData.profile, quests = emptyList(),
        onOpenBook = {}, onOpenPassage = { _, _ -> }, onOpenLibrary = {}, onOpenCastle = {}
    )
}

@GrayfogReviewSizes
@Composable
private fun LibraryReview() = ReviewFrame {
    LibraryScreen(
        books = SampleData.books, isImporting = false, onImportUri = {},
        onOpenBook = {}, onFavorite = {}, onEditMetadata = {}, onDeleteBook = {}, onOpenSettings = {}
    )
}

@GrayfogReviewSizes
@Composable
private fun ArchiveReview() = ReviewFrame {
    val book = SampleData.books.first()
    ArchiveScreen(
        books = SampleData.books,
        highlights = listOf(Highlight("review-note", book.id, "The archive keeps a trace of each return.",
            "{}", "A preview annotation, not a reading record.", createdAtEpochMs = 0)),
        bookmarks = emptyList(), readingSessions = emptyList(), readingCycles = emptyList(),
        passageVisits = emptyList(), onClose = {}, onOpenPassage = { _, _ -> },
        onSaveNote = { _, _ -> }, onDeleteHighlight = {}, onDeleteBookmark = {}
    )
}

@GrayfogReviewSizes
@Composable
private fun SettingsReview() = ReviewFrame {
    SettingsScreen(
        settings = AppSettings(appThemeMode = AppThemeMode.DARK), exporting = false, restoring = false,
        onSetAppThemeMode = {}, onSetHighContrastEnabled = {}, onSaveReaderAppearance = {},
        onSaveReaderTapGrid = {}, onSaveReaderHardwareKeys = {}, onSaveReaderFocusGuide = {},
        onSaveSensorySettings = {}, onSetGameVisible = {}, onExportBackup = {},
        onRestoreBackup = {}, onExportNotes = {}, onClose = {}
    )
}

@GrayfogReviewSizes
@Composable
private fun ProfileReview() = ReviewFrame {
    ProfileScreen(
        profile = SampleData.profile, highlightCount = 0, dailyGoalMinutes = 20,
        castleTitle = "Review archive", equippedSigilId = null, books = SampleData.books,
        onSetDailyGoal = {}, onOpenArchive = {}, onOpenSettings = {}
    )
}

@GrayfogReviewSizes
@Composable
private fun ObservatoryReview() = ReviewFrame {
    ObservatoryScreen(SampleData.books, emptyList(), emptyList(), onOpenBook = {}, onClose = {})
}

@GrayfogReviewSizes
@Composable
private fun CastleReview() = ReviewFrame {
    CastleScreen(SampleData.profile, onAdvanceRank = {}, onOpenRoom = {}, books = SampleData.books)
}

@GrayfogReviewSizes
@Composable
private fun PathReview() = ReviewFrame {
    PathScreen(SampleData.profile, onAdvanceRank = {}, onChoosePath = {})
}

@GrayfogReviewSizes
@Composable
private fun TreasuryReview() = ReviewFrame {
    TreasuryScreen(SampleData.profile, equippedSigil = null, onEquip = {}, onClose = {})
}

@GrayfogReviewSizes
@Composable
private fun SanctumReview() = ReviewFrame {
    SanctumScreen(SampleData.profile, castleTitle = "Review archive", availableTitles = emptyList(),
        onSelectTitle = {}, onClose = {})
}

@GrayfogReviewSizes
@Composable
private fun BookDetailReview() = ReviewFrame {
    BookDetailDestination(
        book = SampleData.books.first(), archiveMemory = null, artifactMemory = null,
        readingCycles = emptyList(), readingMilestones = emptyList(), preservedHighlights = emptyList(),
        onDismiss = {}, onOpen = {}, onFavorite = {}, onEditMetadata = {}, onDelete = {}
    )
}

@GrayfogReviewSizes
@Composable
private fun GalleryObjectReview() = ReviewFrame {
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.width(184.dp)) {
        BookLibraryTile(SampleData.books.first(), null, null, onOpen = {}, onFavorite = {}, onDetails = {})
    }
}

@GrayfogReviewSizes
@Composable
private fun ShelvesReview() = ReviewFrame {
    LibraryShelvesView(
        groups = listOf(LibraryShelfGroup("Review", "A preserved shelf", SampleData.books)),
        artifactMemoryByBookId = emptyMap(), itemWidthDp = 146f, coverWidthDp = 132f, coverHeightDp = 194f,
        onOpen = {}, onDetails = {}
    )
}
