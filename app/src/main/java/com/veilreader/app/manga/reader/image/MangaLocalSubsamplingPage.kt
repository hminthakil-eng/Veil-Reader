package com.veilreader.app.manga.reader.image

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.util.Logger
import com.veilreader.app.manga.reader.MangaReaderMode

/**
 * Isolated mature subsampling renderer for verified local extreme images.
 *
 * IMPORTANT: ZoomImage owns its own zoom/pan gestures. Do not place this under the current reader
 * gesture layer until screen integration explicitly transfers gesture ownership for that page.
 */
@Composable
fun MangaLocalSubsamplingPage(
    page: MangaResolvedPage.Local,
    dimensions: MangaImageDimensions,
    mode: MangaReaderMode,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val request = remember(page.file, mode) {
        MangaCoilRequestFactory.build(
            context = context,
            page = page,
            mode = mode
        )
    }
    // ZoomImage debug logs include request.data. Error level prevents page paths/URLs from being
    // emitted by its debug pipeline while preserving actionable library failures.
    val zoomState = rememberCoilZoomState(logLevel = Logger.Level.Error)

    val imageModifier = when (mode) {
        MangaReaderMode.PAGED -> modifier.fillMaxSize()
        MangaReaderMode.WEBTOON -> modifier
            .fillMaxWidth()
            .aspectRatio(
                dimensions.widthPx.toFloat() /
                    dimensions.heightPx.toFloat()
            )
    }

    CoilZoomAsyncImage(
        model = request,
        contentDescription = null,
        imageLoader = imageLoader,
        modifier = imageModifier,
        contentScale = ContentScale.Fit,
        zoomState = zoomState
    )
}
