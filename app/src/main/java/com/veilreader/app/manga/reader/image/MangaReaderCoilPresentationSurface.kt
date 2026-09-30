package com.veilreader.app.manga.reader.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import com.veilreader.app.R
import com.veilreader.app.manga.reader.presentation.MangaPresentationError
import com.veilreader.app.manga.reader.presentation.MangaPresentationErrorKind
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationState
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationSurface
import com.veilreader.app.manga.reader.ui.MangaReaderUiIntent
import com.veilreader.app.manga.reader.ui.MangaReaderUiState
import java.io.File

/**
 * Ready-to-wire presentation renderer. ImageLoader is injected so the app can keep one reader
 * loader for its lifecycle instead of rebuilding caches per recomposition/screen.
 */
@Composable
fun MangaReaderCoilPresentationSurface(
    presentation: MangaReaderPresentationState,
    uiState: MangaReaderUiState,
    cacheRoot: File,
    imageLoader: ImageLoader,
    onIntent: (MangaReaderUiIntent) -> Unit,
    onRetryChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolver = remember(cacheRoot) {
        MangaPageAssetResolver(
            MangaLocalPageVerifier(cacheRoot)
        )
    }

    MangaReaderPresentationSurface(
        presentation = presentation,
        uiState = uiState,
        onIntent = onIntent,
        onRetry = onRetryChapter,
        modifier = modifier,
        loadingContent = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        errorContent = { error, retry ->
            DefaultChapterError(error, retry)
        },
        partialOfflineContent = {
            Text(stringResource(R.string.manga_reader_partial_offline_notice))
        },
        pageContent = { asset, pageModifier ->
            MangaCoilPage(
                asset = asset,
                mode = uiState.reader.mode,
                resolver = resolver,
                imageLoader = imageLoader,
                modifier = pageModifier
            )
        }
    )
}

@Composable
private fun DefaultChapterError(
    error: MangaPresentationError,
    retry: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(error.kind.toUiMessageRes()))
            if (error.retryable) {
                Button(onClick = retry) {
                    Text(stringResource(R.string.manga_reader_retry))
                }
            }
        }
    }
}


private fun MangaPresentationErrorKind.toUiMessageRes(): Int = when (this) {
    MangaPresentationErrorKind.OFFLINE_UNAVAILABLE ->
        R.string.manga_reader_error_offline_unavailable
    MangaPresentationErrorKind.SOURCE_FAILURE ->
        R.string.manga_reader_error_source_failure
    MangaPresentationErrorKind.EMPTY_CHAPTER ->
        R.string.manga_reader_error_empty_chapter
    MangaPresentationErrorKind.INVALID_PAGE_SET ->
        R.string.manga_reader_error_invalid_pages
    MangaPresentationErrorKind.REQUEST_MISMATCH ->
        R.string.manga_reader_error_request_mismatch
}
