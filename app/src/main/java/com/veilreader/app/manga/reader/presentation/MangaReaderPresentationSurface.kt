package com.veilreader.app.manga.reader.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.ui.MangaReaderGestureOwner
import com.veilreader.app.manga.reader.ui.MangaReaderSurface
import com.veilreader.app.manga.reader.ui.MangaReaderUiIntent
import com.veilreader.app.manga.reader.ui.MangaReaderUiState

/**
 * Presentation wrapper over the renderer-agnostic reader surface.
 *
 * The app decides how Local/Remote assets are rendered. This wrapper only selects the correct page,
 * keeps pageCount synchronized, and exposes loading/error/partial-offline UX slots.
 */
@Composable
fun MangaReaderPresentationSurface(
    presentation: MangaReaderPresentationState,
    uiState: MangaReaderUiState,
    onIntent: (MangaReaderUiIntent) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    gestureOwner: MangaReaderGestureOwner = MangaReaderGestureOwner.VEIL_READER,
    loadingContent: @Composable (MangaReaderChapterRef) -> Unit,
    errorContent: @Composable (MangaPresentationError, onRetry: () -> Unit) -> Unit,
    partialOfflineContent: @Composable () -> Unit = {},
    pageContent: @Composable (MangaPageAsset, Modifier) -> Unit
) {
    when (presentation) {
        is MangaReaderPresentationState.Loading -> {
            Box(modifier.fillMaxSize()) {
                loadingContent(presentation.chapter)
            }
        }

        is MangaReaderPresentationState.Error -> {
            Box(modifier.fillMaxSize()) {
                errorContent(
                    presentation.error,
                    if (presentation.error.retryable) onRetry else ({})
                )
            }
        }

        is MangaReaderPresentationState.Ready -> {
            val ready = presentation.value

            // Never render newly loaded assets through stale reader state from another chapter.
            // The owner must initialize/restore MangaReaderState for the new route first.
            if (!uiState.reader.chapter.sameLogicalChapter(ready.chapter)) {
                Box(modifier.fillMaxSize()) {
                    loadingContent(ready.chapter)
                }
                return
            }

            LaunchedEffect(ready.chapter, ready.pageCount) {
                if (uiState.reader.pageCount != ready.pageCount) {
                    onIntent(MangaReaderUiIntent.PageCountResolved(ready.pageCount))
                }
            }

            Box(modifier.fillMaxSize()) {
                MangaReaderSurface(
                    state = uiState,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxSize(),
                    gestureOwner = gestureOwner
                ) { index, pageModifier ->
                    val asset = ready.page(index)
                    if (asset != null) {
                        pageContent(asset, pageModifier)
                    }
                }

                if (!ready.complete) {
                    partialOfflineContent()
                }
            }
        }
    }
}
