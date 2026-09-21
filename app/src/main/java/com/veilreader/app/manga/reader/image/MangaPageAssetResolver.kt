package com.veilreader.app.manga.reader.image

import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import java.io.File

sealed interface MangaResolvedPage {
    val index: Int

    data class Local(
        override val index: Int,
        val file: File
    ) : MangaResolvedPage

    class Remote(
        override val index: Int,
        val url: String,
        val headers: Map<String, String>
    ) : MangaResolvedPage {
        override fun toString(): String =
            "Remote(index=$index, url=<redacted>, headers=<redacted>)"
    }
}

sealed interface MangaPageResolveResult {
    data class Ready(val page: MangaResolvedPage) : MangaPageResolveResult
    data class Error(val message: String) : MangaPageResolveResult
}

class MangaPageAssetResolver(
    private val localVerifier: MangaLocalPageVerifier
) {
    suspend fun resolve(asset: MangaPageAsset): MangaPageResolveResult = when (asset) {
        is MangaPageAsset.Local -> {
            when (val local = localVerifier.resolve(asset)) {
                is LocalPageResolution.Ready -> MangaPageResolveResult.Ready(
                    MangaResolvedPage.Local(asset.index, local.file)
                )
                is LocalPageResolution.Invalid -> MangaPageResolveResult.Error(local.reason)
            }
        }

        is MangaPageAsset.Remote -> {
            when (val remote = MangaRemotePagePolicy.resolve(asset)) {
                is RemotePageResolution.Ready -> MangaPageResolveResult.Ready(
                    MangaResolvedPage.Remote(
                        index = asset.index,
                        url = remote.url,
                        headers = remote.headers
                    )
                )
                is RemotePageResolution.Invalid -> MangaPageResolveResult.Error(remote.reason)
            }
        }
    }
}
