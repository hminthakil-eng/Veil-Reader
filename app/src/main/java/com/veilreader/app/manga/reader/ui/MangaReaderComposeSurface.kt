package com.veilreader.app.manga.reader.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.veilreader.app.manga.reader.HorizontalGesture
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.MangaReaderPosition
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Renderer-agnostic Compose shell.
 *
 * The slot receives only an item index and modifier. Image loading/decoding can therefore come from
 * online source pages, offline cache or another mature image pipeline without coupling transport to
 * the reader UI.
 */
@Composable
fun MangaReaderSurface(
    state: MangaReaderUiState,
    onIntent: (MangaReaderUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    pageContent: @Composable (pageIndex: Int, modifier: Modifier) -> Unit
) {
    when (state.reader.mode) {
        MangaReaderMode.PAGED -> MangaPagedReaderSurface(
            state = state,
            onIntent = onIntent,
            modifier = modifier,
            pageContent = pageContent
        )

        MangaReaderMode.WEBTOON -> MangaWebtoonReaderSurface(
            state = state,
            onIntent = onIntent,
            modifier = modifier,
            pageContent = pageContent
        )
    }
}

@Composable
private fun MangaPagedReaderSurface(
    state: MangaReaderUiState,
    onIntent: (MangaReaderUiIntent) -> Unit,
    modifier: Modifier,
    pageContent: @Composable (pageIndex: Int, modifier: Modifier) -> Unit
) {
    val index = state.reader.position.itemIndex
    val zoom = state.reader.zoom

    Box(
        modifier = modifier
            .fillMaxSize()
            .readerTapAndZoomGestures(state, onIntent)
            .pagedSwipeGestures(state, onIntent)
    ) {
        pageContent(
            index,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom.scale.toFloat()
                    scaleY = zoom.scale.toFloat()
                    transformOrigin = TransformOrigin(
                        pivotFractionX = zoom.centerXFraction.toFloat(),
                        pivotFractionY = zoom.centerYFraction.toFloat()
                    )
                }
        )
    }
}

@Composable
private fun MangaWebtoonReaderSurface(
    state: MangaReaderUiState,
    onIntent: (MangaReaderUiIntent) -> Unit,
    modifier: Modifier,
    pageContent: @Composable (pageIndex: Int, modifier: Modifier) -> Unit
) {
    val pageCount = state.reader.pageCount ?: 0
    val restored = state.reader.position as MangaReaderPosition.Webtoon
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = restored.itemIndex.coerceAtLeast(0)
    )

    LaunchedEffect(state.reader.chapter, pageCount) {
        if (pageCount <= 0) return@LaunchedEffect
        val index = restored.itemIndex.coerceIn(0, pageCount - 1)
        listState.scrollToItem(index)

        val itemSize = snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == index }
                ?.size
                ?: 0
        }
            .filter { it > 0 }
            .first()

        listState.scrollToItem(
            index = index,
            scrollOffset = (restored.offsetFraction * itemSize).roundToInt()
        )
    }

    LaunchedEffect(listState, state.reader.chapter, pageCount) {
        if (pageCount <= 0) return@LaunchedEffect

        snapshotFlow {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            first?.let {
                Triple(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset,
                    it.size
                )
            }
        }
            .filterNotNull()
            .map { (index, offset, size) ->
                index to MangaReaderViewportMath.webtoonOffsetFraction(offset, size)
            }
            .distinctUntilChanged()
            .collect { (index, offsetFraction) ->
                onIntent(
                    MangaReaderUiIntent.WebtoonPositionChanged(
                        itemIndex = index,
                        offsetFraction = offsetFraction
                    )
                )
            }
    }

    val zoom = state.reader.zoom

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .readerTapAndZoomGestures(state, onIntent)
    ) {
        items(
            count = pageCount,
            key = { index -> index }
        ) { index ->
            val pageModifier = if (index == state.reader.position.itemIndex) {
                Modifier.graphicsLayer {
                    scaleX = zoom.scale.toFloat()
                    scaleY = zoom.scale.toFloat()
                    transformOrigin = TransformOrigin(
                        pivotFractionX = zoom.centerXFraction.toFloat(),
                        pivotFractionY = zoom.centerYFraction.toFloat()
                    )
                }
            } else {
                Modifier
            }
            pageContent(index, pageModifier)
        }
    }
}

private fun Modifier.readerTapAndZoomGestures(
    state: MangaReaderUiState,
    onIntent: (MangaReaderUiIntent) -> Unit
): Modifier = this
    .pointerInput(state.reader.chapter, state.reader.zoom.scale) {
        detectTapGestures(
            onTap = { offset ->
                val width = size.width.coerceAtLeast(1)
                onIntent(
                    MangaReaderUiIntent.Tap(
                        xFraction = offset.x.toDouble() / width.toDouble()
                    )
                )
            },
            onDoubleTap = { offset ->
                val width = size.width.coerceAtLeast(1)
                val height = size.height.coerceAtLeast(1)
                onIntent(
                    MangaReaderUiIntent.DoubleTap(
                        xFraction = offset.x.toDouble() / width.toDouble(),
                        yFraction = offset.y.toDouble() / height.toDouble()
                    )
                )
            }
        )
    }
    .pointerInput(state.reader.chapter, state.reader.zoom) {
        detectTransformGestures { centroid, _, zoomChange, _ ->
            if (abs(zoomChange - 1f) < 0.001f) return@detectTransformGestures
            val width = size.width.coerceAtLeast(1)
            val height = size.height.coerceAtLeast(1)
            onIntent(
                MangaReaderUiIntent.TransformZoom(
                    scale = state.reader.zoom.scale * zoomChange.toDouble(),
                    centerXFraction = centroid.x.toDouble() / width.toDouble(),
                    centerYFraction = centroid.y.toDouble() / height.toDouble()
                )
            )
        }
    }

private fun Modifier.pagedSwipeGestures(
    state: MangaReaderUiState,
    onIntent: (MangaReaderUiIntent) -> Unit
): Modifier = pointerInput(
    state.reader.chapter,
    state.reader.direction,
    state.reader.zoom.scale
) {
    var horizontalDistance = 0f
    detectHorizontalDragGestures(
        onDragStart = { horizontalDistance = 0f },
        onDragCancel = { horizontalDistance = 0f },
        onHorizontalDrag = { _, dragAmount ->
            horizontalDistance += dragAmount
        },
        onDragEnd = {
            if (state.reader.zoom.scale > 1.01) {
                horizontalDistance = 0f
                return@detectHorizontalDragGestures
            }

            val threshold = size.width.coerceAtLeast(1) * 0.12f
            if (abs(horizontalDistance) >= threshold) {
                onIntent(
                    MangaReaderUiIntent.Swipe(
                        if (horizontalDistance < 0f) {
                            HorizontalGesture.SWIPE_LEFT
                        } else {
                            HorizontalGesture.SWIPE_RIGHT
                        }
                    )
                )
            }
            horizontalDistance = 0f
        }
    )
}
