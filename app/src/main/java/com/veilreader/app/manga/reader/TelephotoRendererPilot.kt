package com.veilreader.app.manga.reader

import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.veilreader.app.manga.core.MangaResourceRequest
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage

/**
 * Controlled renderer pilot only.
 *
 * This is intentionally not wired into [MangaReaderScreen]. It compiles Telephoto against
 * Veil's real Coil 3 [coil3.request.ImageRequest] path (including request headers and file URIs)
 * while keeping the production renderer unchanged until physical-device gates pass.
 */
@Keep
@Composable
internal fun TelephotoMangaPagePilot(
    resource: MangaResourceRequest,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ZoomableAsyncImage(
        model = mangaImageRequest(context, resource),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
