package com.veilreader.app.manga.hub

import com.veilreader.app.manga.source.SourceMangaRef

/**
 * Product-level discovery input, independent from source transport.
 *
 * A future curated/recommendation layer can replace this without extending every source parser with
 * a fake "browse" operation. Search remains owned by the Source SDK.
 */
fun interface MangaHubDiscoverySeed {
    fun refs(): List<SourceMangaRef>
}
