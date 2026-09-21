package com.veilreader.app.manga.reader

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaResourceRequest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MangaReaderScreen(
    pages: List<MangaPage>,
    mode: MangaReaderMode,
    onModeChange: (MangaReaderMode) -> Unit,
    onProgress: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    readingDirection: MangaReadingDirection = MangaReadingDirection.RIGHT_TO_LEFT
) {
    require(pages.map(MangaPage::index).distinct().size == pages.size) {
        "Manga reader pages must have unique indices."
    }

    val orderedPages = remember(pages) { pages.sortedBy(MangaPage::index) }
    var anchorPage by rememberSaveable(orderedPages) {
        mutableIntStateOf(
            if (orderedPages.isEmpty()) 0 else initialPage.coerceIn(0, orderedPages.lastIndex)
        )
    }
    val context = LocalContext.current
    val imageLoader = context.imageLoader

    LaunchedEffect(anchorPage, orderedPages) {
        MangaPrefetchWindow.indices(
            center = anchorPage,
            total = orderedPages.size,
            radius = MangaRendererSafety.prefetchRadius(mode)
        ).forEach { pageIndex ->
            imageLoader.enqueue(mangaImageRequest(context, orderedPages[pageIndex].image))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (orderedPages.isEmpty()) {
            Text(
                text = "No pages available",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        } else when (mode) {
            MangaReaderMode.PAGED -> {
                val pagerState = rememberPagerState(
                    initialPage = anchorPage,
                    pageCount = { orderedPages.size }
                )

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }
                        .distinctUntilChanged()
                        .collect { page ->
                            anchorPage = page
                            onProgress(page)
                        }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    // Pilot safety cap: keep pager retention aligned with prefetch policy.
                    beyondViewportPageCount =
                        MangaRendererSafety.beyondViewportPageCount(orderedPages.size),
                    reverseLayout = readingDirection == MangaReadingDirection.RIGHT_TO_LEFT,
                    key = { page -> orderedPages[page].index }
                ) { page ->
                    AsyncImage(
                        model = mangaImageRequest(context, orderedPages[page].image),
                        contentDescription = "Manga page ${page + 1} of ${orderedPages.size}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            MangaReaderMode.WEBTOON -> {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = anchorPage
                )

                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemIndex }
                        .distinctUntilChanged()
                        .collect { page ->
                            anchorPage = page
                            onProgress(page)
                        }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = orderedPages,
                        key = { _, page -> page.index }
                    ) { index, page ->
                        AsyncImage(
                            model = mangaImageRequest(context, page.image),
                            contentDescription = "Manga page ${index + 1} of ${orderedPages.size}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                    }
                }
            }
        }

        MangaReaderControls(
            mode = mode,
            currentPage = anchorPage,
            totalPages = orderedPages.size,
            onModeChange = onModeChange,
            onClose = onClose,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
        )
    }
}

@Composable
private fun MangaReaderControls(
    mode: MangaReaderMode,
    currentPage: Int,
    totalPages: Int,
    onModeChange: (MangaReaderMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.72f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            TextButton(onClick = onClose) {
                Text("Close", color = Color.White)
            }
            Text(
                text = if (totalPages == 0) "0 / 0" else "${currentPage + 1} / $totalPages",
                color = Color.White
            )
            TextButton(
                onClick = { onModeChange(MangaReaderMode.PAGED) },
                enabled = mode != MangaReaderMode.PAGED
            ) {
                Text("Paged")
            }
            TextButton(
                onClick = { onModeChange(MangaReaderMode.WEBTOON) },
                enabled = mode != MangaReaderMode.WEBTOON
            ) {
                Text("Webtoon")
            }
        }
    }
}

internal fun mangaImageRequest(
    context: Context,
    resource: MangaResourceRequest
): ImageRequest {
    val builder = ImageRequest.Builder(context).data(resource.url)
    if (resource.headers.isNotEmpty()) {
        val headers = NetworkHeaders.Builder().apply {
            resource.headers.forEach { (name, value) -> set(name, value) }
        }.build()
        builder.httpHeaders(headers)
    }
    return builder.build()
}