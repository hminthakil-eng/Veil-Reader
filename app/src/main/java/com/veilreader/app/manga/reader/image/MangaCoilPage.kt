package com.veilreader.app.manga.reader.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.presentation.MangaPageAsset

private sealed interface ResolvedUiPage {
    data object Loading : ResolvedUiPage
    data class Ready(val page: MangaResolvedPage) : ResolvedUiPage
    data class Error(val message: String) : ResolvedUiPage
}

@Composable
fun MangaCoilPage(
    asset: MangaPageAsset,
    mode: MangaReaderMode,
    resolver: MangaPageAssetResolver,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    var retryKey by remember(asset) { mutableIntStateOf(0) }

    val resolved by produceState<ResolvedUiPage>(
        initialValue = ResolvedUiPage.Loading,
        asset,
        retryKey,
        resolver
    ) {
        value = when (val result = resolver.resolve(asset)) {
            is MangaPageResolveResult.Ready -> ResolvedUiPage.Ready(result.page)
            is MangaPageResolveResult.Error -> ResolvedUiPage.Error(result.message)
        }
    }

    when (val page = resolved) {
        ResolvedUiPage.Loading -> {
            Box(pageModifier(mode, modifier), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ResolvedUiPage.Error -> {
            MangaImageError(
                message = page.message,
                onRetry = { retryKey += 1 },
                modifier = pageModifier(mode, modifier)
            )
        }

        is ResolvedUiPage.Ready -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val request = remember(page.page, mode) {
                MangaCoilRequestFactory.build(
                    context = context,
                    page = page.page,
                    mode = mode
                )
            }
            var loading by remember(request) { mutableStateOf(true) }
            var failed by remember(request) { mutableStateOf(false) }

            Box(pageModifier(mode, modifier), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = request,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = imageModifier(mode),
                    contentScale = ContentScale.Fit,
                    onLoading = {
                        loading = true
                        failed = false
                    },
                    onSuccess = {
                        loading = false
                        failed = false
                    },
                    onError = {
                        loading = false
                        failed = true
                    }
                )

                if (loading) {
                    CircularProgressIndicator()
                }
                if (failed) {
                    MangaImageError(
                        message = "Page image could not be decoded or fetched.",
                        onRetry = { retryKey += 1 },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaImageError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message)
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

private fun pageModifier(
    mode: MangaReaderMode,
    modifier: Modifier
): Modifier = when (mode) {
    MangaReaderMode.PAGED -> modifier.fillMaxSize()
    MangaReaderMode.WEBTOON -> modifier.fillMaxWidth().wrapContentHeight()
}

private fun imageModifier(mode: MangaReaderMode): Modifier = when (mode) {
    MangaReaderMode.PAGED -> Modifier.fillMaxSize()
    MangaReaderMode.WEBTOON -> Modifier.fillMaxWidth().wrapContentHeight()
}
