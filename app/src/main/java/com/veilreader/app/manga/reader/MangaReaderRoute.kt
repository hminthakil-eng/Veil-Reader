package com.veilreader.app.manga.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Route-level adapter that owns lifecycle durability for the manga reader.
 *
 * The renderer stays stateless; this layer binds it to [MangaReaderViewModel] and guarantees a
 * progress flush when the host pauses/stops or leaves composition.
 */
@Composable
fun MangaReaderRoute(
    viewModel: MangaReaderViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val failure = state.failure
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.flushProgress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.flushProgress()
        }
    }

    when {
        failure == null && state.pages.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading chapter…")
            }
        }

        failure != null && state.pages.isEmpty() -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(failure.message)
                if (failure.retryable) {
                    TextButton(onClick = viewModel::retry) {
                        Text("Retry")
                    }
                }
                if (failure.recoveryChapterId != null) {
                    TextButton(onClick = viewModel::recoverNearbyChapter) {
                        Text("Open nearby chapter")
                    }
                }
                TextButton(
                    onClick = {
                        viewModel.flushProgress()
                        onClose()
                    }
                ) {
                    Text("Close")
                }
            }
        }

        else -> {
            MangaReaderScreen(
                pages = state.pages,
                mode = state.preferences.layout.toScreenMode(),
                onModeChange = { mode ->
                    val layout = when (mode) {
                        MangaReaderMode.PAGED -> MangaReaderLayout.PAGED
                        MangaReaderMode.WEBTOON -> MangaReaderLayout.WEBTOON
                    }
                    viewModel.setPreferences(state.preferences.copy(layout = layout))
                },
                onProgress = viewModel::onPageSettled,
                canGoPreviousChapter = state.canGoPreviousChapter && !state.loading,
                canGoNextChapter = state.canGoNextChapter && !state.loading,
                onPreviousChapter = viewModel::goToPreviousChapter,
                onNextChapter = viewModel::goToNextChapter,
                onClose = {
                    viewModel.flushProgress()
                    onClose()
                },
                modifier = modifier,
                initialPage = state.pageIndex,
                readingDirection = state.preferences.direction
            )
        }
    }
}

internal fun MangaReaderLayout.toScreenMode(): MangaReaderMode = when (this) {
    MangaReaderLayout.PAGED,
    MangaReaderLayout.VERTICAL_PAGER -> MangaReaderMode.PAGED

    MangaReaderLayout.WEBTOON,
    MangaReaderLayout.CONTINUOUS_VERTICAL -> MangaReaderMode.WEBTOON
}
