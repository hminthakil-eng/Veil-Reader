package com.veilreader.app.data.manga

import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaRef
import org.json.JSONArray
import org.json.JSONObject

object MangaBackupCodec {
    private const val MAX_WORKS = 20_000
    private const val MAX_SOURCE_LINKS_PER_WORK = 128
    private const val MAX_PROGRESS = 20_000

    fun toJson(snapshot: MangaBackupSnapshot): JSONObject = JSONObject().apply {
        put("works", JSONArray().apply {
            snapshot.works.forEach { manga ->
                put(JSONObject().apply {
                    put("id", manga.id.value)
                    put("title", manga.title)
                    put("alternativeTitles", JSONArray(manga.alternativeTitles.sorted()))
                    put("createdAtEpochMs", manga.createdAtEpochMs)
                    put("sourceRefs", JSONArray().apply {
                        manga.sourceRefs.values
                            .sortedBy { it.sourceId.value }
                            .forEach { ref ->
                                put(JSONObject().apply {
                                    put("sourceId", ref.sourceId.value)
                                    put("key", ref.key)
                                    put("publicUrl", ref.publicUrl ?: JSONObject.NULL)
                                })
                            }
                    })
                })
            }
        })
        put("progress", JSONArray().apply {
            snapshot.progress.forEach { progress ->
                put(JSONObject().apply {
                    put("mangaId", progress.mangaId.value)
                    put("chapter", anchorToJson(progress.chapter))
                    put("pageIndex", progress.pageIndex)
                    put("pageCount", progress.pageCount ?: JSONObject.NULL)
                    put("chapterProgression", progress.chapterProgression)
                    put("updatedAtEpochMs", progress.updatedAtEpochMs)
                })
            }
        })
    }

    fun fromJson(json: JSONObject): MangaBackupSnapshot {
        val worksJson = json.optJSONArray("works") ?: JSONArray()
        val progressJson = json.optJSONArray("progress") ?: JSONArray()
        require(worksJson.length() <= MAX_WORKS) { "Manga backup contains too many works" }
        require(progressJson.length() <= MAX_PROGRESS) { "Manga backup contains too many progress rows" }

        val works = buildList {
            for (index in 0 until worksJson.length()) {
                val value = worksJson.getJSONObject(index)
                val refsJson = value.optJSONArray("sourceRefs") ?: JSONArray()
                require(refsJson.length() <= MAX_SOURCE_LINKS_PER_WORK) {
                    "Manga backup work contains too many source links"
                }

                val refs = linkedMapOf<SourceId, SourceMangaRef>()
                for (refIndex in 0 until refsJson.length()) {
                    val refJson = refsJson.getJSONObject(refIndex)
                    val sourceId = SourceId(refJson.getString("sourceId"))
                    require(sourceId !in refs) { "Duplicate Manga backup source link" }
                    refs[sourceId] = SourceMangaRef(
                        sourceId = sourceId,
                        key = refJson.getString("key"),
                        publicUrl = refJson.optString("publicUrl")
                            .takeIf { it.isNotBlank() && it != "null" }
                    )
                }

                val alternativeTitlesJson = value.optJSONArray("alternativeTitles") ?: JSONArray()
                val alternatives = buildSet {
                    for (titleIndex in 0 until alternativeTitlesJson.length()) {
                        alternativeTitlesJson.optString(titleIndex)
                            .trim()
                            .takeIf(String::isNotEmpty)
                            ?.let(::add)
                    }
                }

                add(
                    CanonicalManga(
                        id = CanonicalMangaId(value.getString("id")),
                        title = value.getString("title"),
                        alternativeTitles = alternatives,
                        sourceRefs = refs,
                        createdAtEpochMs = value.getLong("createdAtEpochMs")
                    )
                )
            }
        }

        val knownIds = works.map { it.id }.toSet()
        val progress = buildList {
            for (index in 0 until progressJson.length()) {
                val value = progressJson.getJSONObject(index)
                val mangaId = CanonicalMangaId(value.getString("mangaId"))
                require(mangaId in knownIds) {
                    "Manga backup progress references an unknown work"
                }
                add(
                    MangaReadingProgress(
                        mangaId = mangaId,
                        chapter = anchorFromJson(value.getJSONObject("chapter")),
                        pageIndex = value.getInt("pageIndex"),
                        pageCount = value.optIntOrNull("pageCount"),
                        chapterProgression = value.getDouble("chapterProgression"),
                        updatedAtEpochMs = value.getLong("updatedAtEpochMs")
                    )
                )
            }
        }

        require(progress.map { it.mangaId }.distinct().size == progress.size) {
            "Manga backup contains duplicate progress rows"
        }

        return MangaBackupSnapshot(
            works = works,
            progress = progress
        )
    }

    private fun anchorToJson(anchor: MangaChapterAnchor): JSONObject = JSONObject().apply {
        put("volume", anchor.volume ?: JSONObject.NULL)
        put("number", anchor.number ?: JSONObject.NULL)
        put("languageTag", anchor.languageTag ?: JSONObject.NULL)
        put("normalizedTitle", anchor.normalizedTitle ?: JSONObject.NULL)
        put("providerChapterKeyHint", anchor.providerChapterKeyHint ?: JSONObject.NULL)
    }

    private fun anchorFromJson(json: JSONObject): MangaChapterAnchor =
        MangaChapterAnchor(
            volume = json.optDoubleOrNull("volume"),
            number = json.optDoubleOrNull("number"),
            languageTag = json.optNullableString("languageTag"),
            normalizedTitle = json.optNullableString("normalizedTitle"),
            providerChapterKeyHint = json.optNullableString("providerChapterKeyHint")
        )

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else getDouble(key)

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else getInt(key)
}
