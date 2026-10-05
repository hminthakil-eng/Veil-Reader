package com.veilreader.app.ui.review

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.veilreader.app.data.SampleData
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.domain.*
import com.veilreader.app.R
import androidx.compose.ui.res.stringResource
import com.veilreader.app.ui.VeilBottomDock
import com.veilreader.app.ui.VeilNavigationRail
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.VeilNoticeDialog
import com.veilreader.app.ui.VeilNoticeKind
import com.veilreader.app.ui.VeilLoadingState
import com.veilreader.app.ui.VeilWorldBackdrop
import com.veilreader.app.ui.screens.*
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilTheme

/** Fictional, deterministic debug records. Never persisted or used as production history. */
object GrayfogReviewFixtures {
    private const val fixtureEpoch = 1_700_000_000_000L
    val books = listOf(
        Book("review-0", "The Cartographer of Quiet Rooms", "Eleanor Vale", .42f,
            "Chapter 12 · The western register", 400, 168),
        Book("review-1", "فهرست اتاق‌های خاموش و یادداشت‌های آخرین نگهبان رصدخانه در شهر مه‌آلود", "نویسندهٔ دفترهای فراموش‌شده", .99f,
            "فصل ۲۱ · بازگشت به تالار", 320, 317, language = "fa"),
        Book("review-2", "An Account of the Instruments Kept in the Northern Observatory and Their Uncertain Provenance", "Alexandra Margaret Ellison of the Northern Institute", 0f),
        Book("review-3", "Still", "", 1f, finished = true),
        Book("review-4", "Ledger", "I. North", 0f)
    ).mapIndexed { index, book -> book.copy(
        // Non-openable review URI enables the real imported-book presentation only. Callbacks are inert.
        sourceUri = "veil-review://fictional/${book.id}", addedAtEpochMs = fixtureEpoch,
        lastOpenedAtEpochMs = if (index == 0) fixtureEpoch else 0,
        favorite = index == 1, seriesName = if (index < 3) "The Records of the Northern Observatory" else null,
        seriesIndex = if (index < 3) index + 1.0 else null,
        collections = listOf("Private records", "Registers, instruments and unresolved correspondence")
    ) }
    val manyBooks = (0 until 48).map { index -> books[index % books.size].copy(
        id = "review-many-$index", seriesName = "Register ${index / 3 + 1}", seriesIndex = index % 3 + 1.0
    ) }
    val notes = books.flatMap { book -> (0 until 5).map { index -> Highlight(
        "${book.id}-note-$index", book.id,
        if (book.language == "fa") "ردّی از نور بر حاشیهٔ دفتر باقی مانده بود؛ تاریخ آن هنوز روشن نیست."
        else "A narrow line of light remained across the register; its date was still uncertain.",
        """{"href":"chapter.xhtml","type":"application/xhtml+xml","locations":{"progression":0.42}}""",
        if (index % 2 == 0) "Fictional review note: compare the instrument with the western record." else "",
        createdAtEpochMs = 0L
    ) } }
    /** A fictional review cover from our original environment art, decoded by the real BookCover path. */
    fun booksWithOriginalCover(context: Context): List<Book> {
        // Validate the real resource before handing its cache file to asynchronous BookCover.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, R.drawable.grayfog_keep_v3, bounds)
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Original review artwork could not be decoded" }
        val directory = context.cacheDir.resolve("grayfog-review-art").apply { mkdirs() }
        val cover = directory.resolve("original-keep-v3.png")
        if (!cover.exists()) {
            val bitmap = checkNotNull(BitmapFactory.decodeResource(context.resources, R.drawable.grayfog_keep_v3))
            try {
                cover.outputStream().use { output ->
                    check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
                }
            } finally {
                bitmap.recycle()
            }
        }
        return books.mapIndexed { index, book ->
            if (index == 0) book.copy(coverCachePath = cover.absolutePath) else book
        }
    }

    val lowProfile = SampleData.profile.copy(level = 1, xp = 0, streakDays = 0,
        pagesRead = 0, minutesRead = 0, booksFinished = 0, rankIndex = 0, ritualProgress = 0)
    val ritualProfile = lowProfile.copy(
        pathMastery = PathMasterySnapshot(PathMasteryAxis(1, 1), PathMasteryAxis(1, 1), PathMasteryAxis(1, 1), 0),
        ritualProgress = 1, ritualTarget = 1
    )
    val denseNotes = manyBooks.take(24).mapIndexed { index, book ->
        notes[index % notes.size].copy(id = "${book.id}-dense-note", bookId = book.id)
    }
    val advancedProfile = SampleData.profile.copy(rankIndex = 5, booksFinished = 48,
        earnedSigils = setOf("first_hour", "passage_keeper", "seven_days", "ten_tomes", "first_threshold"))
}

/** Each entry invokes production composition. A specimen never simulates publication content. */
enum class GrayfogReviewSurface {
    THRESHOLD_ACTIVE, THRESHOLD_PERSIAN_LONG, THRESHOLD_EMPTY, LIBRARY_GALLERY, LIBRARY_SHELVES, LIBRARY_INDEX,
    LIBRARY_SEARCH, LIBRARY_NO_RESULTS, LIBRARY_EMPTY, LIBRARY_MANY, LIBRARY_MISSING_METADATA, BOOK_DETAIL,
    BOOK_DETAIL_PERSIAN, BOOK_DETAIL_MISSING, APPEARANCE_QUICK, APPEARANCE_ADVANCED,
    SETTINGS, NOTES, NOTES_EMPTY, HIGHLIGHTS, BOOKMARKS, OBSERVATORY_ISOLATED, OBSERVATORY_DENSE,
    CASTLE_LOW, CASTLE_ADVANCED, PATH, RITUAL, LOADING, ERROR, SANCTUM_LOCKED, SANCTUM_POPULATED, PROFILE,
    READER_ACCESS_PAPER, READER_ACCESS_DUSK, NAVIGATION_DOCK, NAVIGATION_RAIL
}

@Composable
fun GrayfogReviewContent(surface: GrayfogReviewSurface, highContrast: Boolean = false) {
    val context = LocalContext.current
    val books = remember(context) { GrayfogReviewFixtures.booksWithOriginalCover(context) }
    val notes = GrayfogReviewFixtures.notes
    val profile = GrayfogReviewFixtures.advancedProfile
    VeilTheme(themeMode = AppThemeMode.DARK, highContrastEnabled = highContrast) {
        CompositionLocalProvider(LocalVeilReducedMotion provides true) {
            VeilWorldBackdrop {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    when (surface) {
                        GrayfogReviewSurface.THRESHOLD_ACTIVE, GrayfogReviewSurface.THRESHOLD_PERSIAN_LONG, GrayfogReviewSurface.THRESHOLD_EMPTY ->
                            ReadingNowScreen(when (surface) {
                                GrayfogReviewSurface.THRESHOLD_EMPTY -> emptyList()
                                GrayfogReviewSurface.THRESHOLD_PERSIAN_LONG -> listOf(books[1].copy(
                                    lastOpenedAtEpochMs = 1_700_000_000_001L,
                                    seriesName = "دفترهای پژوهش‌های رصدخانه و نگهبانان بایگانی فراموش‌شده"))
                                else -> books
                            },
                                profile, emptyList(), onOpenBook = {}, onOpenPassage = { _, _ -> }, onOpenLibrary = {}, onOpenCastle = {})
                        GrayfogReviewSurface.LIBRARY_GALLERY, GrayfogReviewSurface.LIBRARY_SHELVES,
                        GrayfogReviewSurface.LIBRARY_INDEX, GrayfogReviewSurface.LIBRARY_SEARCH,
                        GrayfogReviewSurface.LIBRARY_NO_RESULTS, GrayfogReviewSurface.LIBRARY_EMPTY,
                        GrayfogReviewSurface.LIBRARY_MANY, GrayfogReviewSurface.LIBRARY_MISSING_METADATA -> LibraryArchiveContent(
                            books = when (surface) {
                                GrayfogReviewSurface.LIBRARY_EMPTY -> emptyList()
                                GrayfogReviewSurface.LIBRARY_MANY -> GrayfogReviewFixtures.manyBooks
                                GrayfogReviewSurface.LIBRARY_MISSING_METADATA -> listOf(books[3].copy(title = "   ", author = ""))
                                else -> books
                            }, isImporting = false, onImportUri = {}, onOpenBook = {}, onFavorite = {},
                            onEditMetadata = {}, onDeleteBook = {}, onOpenSettings = {},
                            initialViewMode = when (surface) {
                                GrayfogReviewSurface.LIBRARY_SHELVES -> LibraryViewMode.SHELVES
                                GrayfogReviewSurface.LIBRARY_INDEX -> LibraryViewMode.INDEX
                                else -> LibraryViewMode.GALLERY
                            }, initialQuery = when (surface) {
                                GrayfogReviewSurface.LIBRARY_SEARCH -> "register"
                                GrayfogReviewSurface.LIBRARY_NO_RESULTS -> "no-fictional-match"
                                else -> ""
                            })
                        GrayfogReviewSurface.BOOK_DETAIL, GrayfogReviewSurface.BOOK_DETAIL_PERSIAN,
                        GrayfogReviewSurface.BOOK_DETAIL_MISSING -> BookDetailDestination(
                            book = books[when (surface) {
                                GrayfogReviewSurface.BOOK_DETAIL_PERSIAN -> 1
                                GrayfogReviewSurface.BOOK_DETAIL_MISSING -> 3
                                else -> 0
                            }], archiveMemory = null, artifactMemory = null, readingCycles = emptyList(),
                            readingMilestones = emptyList(), preservedHighlights = notes,
                            onDismiss = {}, onOpen = {}, onFavorite = {}, onEditMetadata = {}, onDelete = {})
                        GrayfogReviewSurface.APPEARANCE_QUICK, GrayfogReviewSurface.APPEARANCE_ADVANCED ->
                            EpubAppearancePanel(ReaderAppearance(), false, ReaderFixedLayoutSpread.AUTO, null,
                                onSpreadChange = {}, onChange = {}, onDone = {},
                                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                                initiallyAdvanced = surface == GrayfogReviewSurface.APPEARANCE_ADVANCED)
                        GrayfogReviewSurface.SETTINGS -> SettingsScreen(AppSettings(appThemeMode = AppThemeMode.DARK), false, false,
                            onSetAppThemeMode = {}, onSetHighContrastEnabled = {}, onSaveReaderAppearance = {},
                            onSaveReaderTapGrid = {}, onSaveReaderHardwareKeys = {}, onSaveReaderFocusGuide = {},
                            onSaveSensorySettings = {}, onSetGameVisible = {}, onExportBackup = {}, onRestoreBackup = {},
                            onExportNotes = {}, onClose = {})
                        GrayfogReviewSurface.NOTES, GrayfogReviewSurface.NOTES_EMPTY, GrayfogReviewSurface.HIGHLIGHTS,
                        GrayfogReviewSurface.BOOKMARKS -> ArchiveRecordContent(
                            books, if (surface == GrayfogReviewSurface.NOTES_EMPTY) emptyList() else notes,
                            if (surface == GrayfogReviewSurface.BOOKMARKS) books.map { bookmarked ->
                                Bookmark("${bookmarked.id}-mark", bookmarked.id, "The western record", notes.first().locatorJson, 0L)
                            } else emptyList(), emptyList(), emptyList(), emptyList(), onClose = {},
                            onOpenPassage = { _, _ -> }, onSaveNote = { _, _ -> }, onDeleteHighlight = {}, onDeleteBookmark = {},
                            initialSection = when (surface) {
                                GrayfogReviewSurface.HIGHLIGHTS -> NotebookSection.HIGHLIGHTS
                                GrayfogReviewSurface.BOOKMARKS -> NotebookSection.BOOKMARKS
                                else -> NotebookSection.NOTES
                            })
                        GrayfogReviewSurface.OBSERVATORY_ISOLATED, GrayfogReviewSurface.OBSERVATORY_DENSE ->
                            ObservatoryScreen(if (surface == GrayfogReviewSurface.OBSERVATORY_ISOLATED) books.take(1) else GrayfogReviewFixtures.manyBooks.take(24),
                                if (surface == GrayfogReviewSurface.OBSERVATORY_ISOLATED) emptyList() else GrayfogReviewFixtures.denseNotes,
                                emptyList(), onOpenBook = {}, onClose = {})
                        GrayfogReviewSurface.CASTLE_LOW, GrayfogReviewSurface.CASTLE_ADVANCED -> CastleScreen(
                            if (surface == GrayfogReviewSurface.CASTLE_LOW) GrayfogReviewFixtures.lowProfile else profile,
                            onAdvanceRank = {}, onOpenRoom = {}, books = books, highlights = notes)
                        GrayfogReviewSurface.PATH -> PathScreen(profile, onAdvanceRank = {}, onChoosePath = {})
                        GrayfogReviewSurface.SANCTUM_LOCKED, GrayfogReviewSurface.SANCTUM_POPULATED -> SanctumScreen(
                            if (surface == GrayfogReviewSurface.SANCTUM_LOCKED) GrayfogReviewFixtures.lowProfile else profile,
                            "The Quiet Archive", if (surface == GrayfogReviewSurface.SANCTUM_LOCKED) emptyList()
                            else listOf("The Quiet Archive", "The Observatory of Preserved Records"), onSelectTitle = {}, onClose = {})
                        GrayfogReviewSurface.RITUAL -> AdvancementCeremonyDialog(
                            GrayfogReviewFixtures.ritualProfile, GrayfogReviewFixtures.ritualProfile.path.ranks[1],
                            onDismiss = {}, onConfirm = {})
                        GrayfogReviewSurface.LOADING -> VeilLoadingState()
                        GrayfogReviewSurface.ERROR -> VeilNoticeDialog(VeilNoticeKind.ERROR,
                            stringResource(R.string.notice_category_persistence), stringResource(R.string.notice_error_title),
                            stringResource(R.string.notice_note_save_failed), stringResource(R.string.notice_return), onDismiss = {})
                        GrayfogReviewSurface.READER_ACCESS_PAPER, GrayfogReviewSurface.READER_ACCESS_DUSK -> {
                            val theme = if (surface == GrayfogReviewSurface.READER_ACCESS_PAPER) ReaderTheme.PAPER else ReaderTheme.DUSK
                            val colors = readerAccessColors(theme)
                            // Access component only: this intentionally contains no simulated publication.
                            Box(Modifier.fillMaxSize().background(readerCanvasColor(theme))) {
                                Box(Modifier.align(Alignment.BottomCenter).padding(12.dp)) {
                                    ReaderAccessDock(
                                        settingsLabel = stringResource(R.string.reader_chrome_appearance),
                                        background = colors.background,
                                        foreground = colors.foreground,
                                        accent = colors.accent,
                                        onMenu = {}, onSettings = {}
                                    )
                                }
                            }
                        }
                        GrayfogReviewSurface.NAVIGATION_DOCK -> Box(Modifier.align(Alignment.BottomCenter)) {
                            VeilBottomDock(selected = VeilTab.LIBRARY, onSelect = {})
                        }
                        GrayfogReviewSurface.NAVIGATION_RAIL -> VeilNavigationRail(selected = VeilTab.LIBRARY, onSelect = {})
                        GrayfogReviewSurface.PROFILE -> ProfileScreen(profile, notes.size, 20, "The Quiet Archive", null,
                            books = books, onSetDailyGoal = {}, onOpenArchive = {}, onOpenSettings = {})
                    }
                }
            }
        }
    }
}
