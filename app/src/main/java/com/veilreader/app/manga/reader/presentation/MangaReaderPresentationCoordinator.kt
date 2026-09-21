package com.veilreader.app.manga.reader.presentation

import com.veilreader.app.manga.reader.ui.MangaReaderUiEffect

sealed interface MangaReaderPresentationEffect {
    data class NavigateToChapter(
        val route: MangaChapterRoute
    ) : MangaReaderPresentationEffect

    data object PartialOfflineBoundaryBlocked : MangaReaderPresentationEffect
    data object SeriesBoundaryReached : MangaReaderPresentationEffect
    data object ChapterRouteUnavailable : MangaReaderPresentationEffect
}

class MangaReaderPresentationCoordinator(
    private val navigationResolver: MangaChapterNavigationResolver
) {
    fun onUiEffect(
        presentation: MangaReadyPresentation,
        effect: MangaReaderUiEffect
    ): List<MangaReaderPresentationEffect> = when (effect) {
        is MangaReaderUiEffect.SnapshotChanged -> emptyList()

        is MangaReaderUiEffect.ChapterBoundaryRequested -> {
            when (
                val result = navigationResolver.resolve(
                    current = presentation,
                    boundary = effect.boundary
                )
            ) {
                is MangaChapterNavigationResult.Target -> listOf(
                    MangaReaderPresentationEffect.NavigateToChapter(result.route)
                )

                MangaChapterNavigationResult.BlockedByPartialOffline -> listOf(
                    MangaReaderPresentationEffect.PartialOfflineBoundaryBlocked
                )

                MangaChapterNavigationResult.SeriesBoundary -> listOf(
                    MangaReaderPresentationEffect.SeriesBoundaryReached
                )

                MangaChapterNavigationResult.CurrentChapterNotFound -> listOf(
                    MangaReaderPresentationEffect.ChapterRouteUnavailable
                )
            }
        }
    }
}
