package com.veilreader.app.manga.reader.ui

import androidx.lifecycle.SavedStateHandle
import com.veilreader.app.manga.reader.MangaReaderSnapshot

/**
 * Android bridge for process-death state.
 *
 * Only primitive String values are stored in SavedStateHandle. No provider object, network request,
 * image loader or Kotlin data-class serialization crosses the process boundary.
 */
class MangaReaderSavedStateHandleAdapter(
    private val handle: SavedStateHandle,
    private val prefix: String = DEFAULT_PREFIX
) {
    init {
        require(prefix.isNotBlank()) { "SavedState prefix cannot be blank" }
    }

    fun save(snapshot: MangaReaderSnapshot) {
        val encoded = MangaReaderSavedStateCodec.encode(snapshot)
        MangaReaderSavedStateCodec.keys.forEach { key ->
            handle[stateKey(key)] = encoded[key]
        }
    }

    fun load(): MangaReaderSnapshot? {
        val values = buildMap {
            MangaReaderSavedStateCodec.keys.forEach { key ->
                handle.get<String>(stateKey(key))?.let { value ->
                    put(key, value)
                }
            }
        }
        return MangaReaderSavedStateCodec.decode(values)
    }

    fun clear() {
        MangaReaderSavedStateCodec.keys.forEach { key ->
            handle.remove<String>(stateKey(key))
        }
    }

    private fun stateKey(key: String): String = "$prefix.$key"

    companion object {
        const val DEFAULT_PREFIX = "manga_reader"
    }
}
