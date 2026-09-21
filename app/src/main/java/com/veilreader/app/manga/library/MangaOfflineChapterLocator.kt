package com.veilreader.app.manga.library

import java.security.MessageDigest

/**
 * Builds source-neutral offline chapter identity when possible.
 *
 * Numbered chapters use volume/number/language directly. Unnumbered specials use a normalized title
 * digest so a provider-key change during source replacement does not orphan the cache. Provider key
 * is only a last-resort discriminator when no source-neutral chapter signal exists.
 */
object MangaOfflineChapterLocator {

    fun idFor(
        mangaId: CanonicalMangaId,
        anchor: MangaChapterAnchor
    ): OfflineChapterId? {
        val discriminator = when {
            anchor.number != null -> "main"
            !anchor.normalizedTitle.isNullOrBlank() ->
                "title-" + digest(normalize(anchor.normalizedTitle))
            !anchor.providerChapterKeyHint.isNullOrBlank() ->
                "provider-" + digest(anchor.providerChapterKeyHint)
            else -> return null
        }

        return OfflineChapterId(
            mangaId = mangaId,
            languageTag = anchor.languageTag,
            volume = anchor.volume,
            number = anchor.number,
            discriminator = discriminator
        )
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun digest(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.take(8).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
