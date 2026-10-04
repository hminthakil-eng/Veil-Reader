package com.veilreader.app.ui.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

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

@GrayfogReviewSizes
@Composable
private fun ThresholdActiveReview() = GrayfogReviewContent(GrayfogReviewSurface.THRESHOLD_ACTIVE)

@GrayfogReviewSizes
@Composable
private fun ThresholdEmptyReview() = GrayfogReviewContent(GrayfogReviewSurface.THRESHOLD_EMPTY)

@GrayfogReviewSizes
@Composable
private fun LibraryGalleryReview() = GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_GALLERY)

@GrayfogReviewSizes
@Composable
private fun LibraryShelvesReview() = GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_SHELVES)

@GrayfogReviewSizes
@Composable
private fun LibraryIndexReview() = GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_INDEX)

@GrayfogReviewSizes
@Composable
private fun LibrarySearchReview() = GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_SEARCH)

@GrayfogReviewSizes
@Composable
private fun LibraryNoResultsReview() = GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_NO_RESULTS)

@GrayfogReviewSizes
@Composable
private fun LibraryEmptyReview() = GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_EMPTY)

@GrayfogReviewSizes
@Composable
private fun LibraryManyReview() = GrayfogReviewContent(GrayfogReviewSurface.LIBRARY_MANY)

@GrayfogReviewSizes
@Composable
private fun BookDetailReview() = GrayfogReviewContent(GrayfogReviewSurface.BOOK_DETAIL)

@GrayfogReviewSizes
@Composable
private fun BookDetailPersianReview() = GrayfogReviewContent(GrayfogReviewSurface.BOOK_DETAIL_PERSIAN)

@GrayfogReviewSizes
@Composable
private fun BookDetailMissingReview() = GrayfogReviewContent(GrayfogReviewSurface.BOOK_DETAIL_MISSING)

@GrayfogReviewSizes
@Composable
private fun AppearanceQuickReview() = GrayfogReviewContent(GrayfogReviewSurface.APPEARANCE_QUICK)

@GrayfogReviewSizes
@Composable
private fun AppearanceAdvancedReview() = GrayfogReviewContent(GrayfogReviewSurface.APPEARANCE_ADVANCED)

@GrayfogReviewSizes
@Composable
private fun SettingsReview() = GrayfogReviewContent(GrayfogReviewSurface.SETTINGS)

@GrayfogReviewSizes
@Composable
private fun NotesReview() = GrayfogReviewContent(GrayfogReviewSurface.NOTES)

@GrayfogReviewSizes
@Composable
private fun NotesEmptyReview() = GrayfogReviewContent(GrayfogReviewSurface.NOTES_EMPTY)

@GrayfogReviewSizes
@Composable
private fun HighlightsReview() = GrayfogReviewContent(GrayfogReviewSurface.HIGHLIGHTS)

@GrayfogReviewSizes
@Composable
private fun BookmarksReview() = GrayfogReviewContent(GrayfogReviewSurface.BOOKMARKS)

@GrayfogReviewSizes
@Composable
private fun ObservatoryIsolatedReview() = GrayfogReviewContent(GrayfogReviewSurface.OBSERVATORY_ISOLATED)

@GrayfogReviewSizes
@Composable
private fun ObservatoryDenseReview() = GrayfogReviewContent(GrayfogReviewSurface.OBSERVATORY_DENSE)

@GrayfogReviewSizes
@Composable
private fun CastleLowReview() = GrayfogReviewContent(GrayfogReviewSurface.CASTLE_LOW)

@GrayfogReviewSizes
@Composable
private fun CastleAdvancedReview() = GrayfogReviewContent(GrayfogReviewSurface.CASTLE_ADVANCED)

@GrayfogReviewSizes
@Composable
private fun PathReview() = GrayfogReviewContent(GrayfogReviewSurface.PATH)

@GrayfogReviewSizes
@Composable
private fun RitualReview() = GrayfogReviewContent(GrayfogReviewSurface.RITUAL)

@GrayfogReviewSizes
@Composable
private fun LoadingReview() = GrayfogReviewContent(GrayfogReviewSurface.LOADING)

@GrayfogReviewSizes
@Composable
private fun ErrorReview() = GrayfogReviewContent(GrayfogReviewSurface.ERROR)

@GrayfogReviewSizes
@Composable
private fun SanctumLockedReview() = GrayfogReviewContent(GrayfogReviewSurface.SANCTUM_LOCKED)

@GrayfogReviewSizes
@Composable
private fun SanctumPopulatedReview() = GrayfogReviewContent(GrayfogReviewSurface.SANCTUM_POPULATED)

@GrayfogReviewSizes
@Composable
private fun ProfileReview() = GrayfogReviewContent(GrayfogReviewSurface.PROFILE)

@GrayfogReviewSizes
@Composable
private fun ThresholdPersianLongReview() = GrayfogReviewContent(GrayfogReviewSurface.THRESHOLD_PERSIAN_LONG)
