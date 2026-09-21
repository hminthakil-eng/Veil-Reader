package com.veilreader.app.manga.hub.verification

import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaPageImage
import com.veilreader.app.manga.source.MangaPublicationStatus
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.PagedSourceResult
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceMangaSummary
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRequestContext
import com.veilreader.app.manga.source.SourceSearchRequest

class FixtureMangaSourceProvider : MangaSourceProvider {

    override val descriptor = MangaSourceDescriptor(
        id = SOURCE_ID,
        displayName = "Veil Fixture Source",
        domains = listOf("fixture.veil.local"),
        localeTags = setOf("en"),
        contentTypes = setOf(MangaContentType.MANGA, MangaContentType.WEBTOON)
    )

    override val capabilities = setOf(
        MangaSourceCapability.SEARCH,
        MangaSourceCapability.DETAILS,
        MangaSourceCapability.CHAPTERS,
        MangaSourceCapability.PAGES
    )

    private val works = listOf(
        FixtureWork(
            key = "ink-dragon",
            title = "Ink Dragon",
            alternativeTitles = setOf("The Dragon in Ink"),
            description = "A quiet archivist discovers that every brush stroke can wake a sleeping dragon.",
            tags = setOf("fantasy", "adventure"),
            contentType = MangaContentType.MANGA,
            chapterCount = 3
        ),
        FixtureWork(
            key = "midnight-pharmacy",
            title = "Midnight Pharmacy",
            alternativeTitles = setOf("The Pharmacy After Dark"),
            description = "A night-shift pharmacist finds that some prescriptions arrive from impossible places.",
            tags = setOf("mystery", "slice-of-life"),
            contentType = MangaContentType.WEBTOON,
            chapterCount = 4
        )
    )

    override suspend fun search(
        request: SourceSearchRequest,
        context: SourceRequestContext
    ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> {
        val query = request.query.trim().lowercase()
        val matches = works.filter { work ->
            work.title.lowercase().contains(query) ||
                work.alternativeTitles.any { it.lowercase().contains(query) } ||
                work.tags.any { it.contains(query) }
        }.take(request.limit)

        return SourceOutcome.Success(
            PagedSourceResult(matches.map(::summary))
        )
    }

    override suspend fun details(
        manga: SourceMangaRef,
        context: SourceRequestContext
    ): SourceOutcome<SourceMangaDetails> {
        val work = works.firstOrNull { it.key == manga.key }
            ?: return notFound("Unknown fixture Manga")
        return SourceOutcome.Success(
            SourceMangaDetails(
                summary = summary(work),
                description = work.description,
                authors = setOf("Veil Fixture Team"),
                status = MangaPublicationStatus.ONGOING,
                tags = work.tags
            )
        )
    }

    override suspend fun chapters(
        manga: SourceMangaRef,
        context: SourceRequestContext
    ): SourceOutcome<List<SourceChapter>> {
        val work = works.firstOrNull { it.key == manga.key }
            ?: return notFound("Unknown fixture Manga")

        return SourceOutcome.Success(
            (1..work.chapterCount).map { number ->
                SourceChapter(
                    sourceId = SOURCE_ID,
                    mangaKey = work.key,
                    chapterKey = work.key + "-chapter-" + number,
                    title = "Chapter " + number,
                    number = number.toDouble(),
                    languageTag = "en"
                )
            }
        )
    }

    override suspend fun pages(
        chapter: SourceChapter,
        context: SourceRequestContext
    ): SourceOutcome<List<MangaPageImage>> {
        val work = works.firstOrNull { it.key == chapter.mangaKey }
            ?: return notFound("Unknown fixture Manga")
        val belongs = chapter.chapterKey.startsWith(work.key + "-chapter-")
        if (!belongs) return notFound("Unknown fixture chapter")

        return SourceOutcome.Success(
            (0 until 4).map { index ->
                MangaPageImage(
                    index = index,
                    imageUrl = "https://" + context.domain + "/" +
                        chapter.mangaKey + "/" + chapter.chapterKey + "/" + index + ".png"
                )
            }
        )
    }

    override suspend fun resolvePublicUrl(
        url: String,
        context: SourceRequestContext
    ): SourceOutcome<SourceMangaRef?> {
        val prefix = "https://" + context.domain + "/title/"
        if (!url.startsWith(prefix)) return SourceOutcome.Success(null)
        val key = url.removePrefix(prefix).substringBefore("/")
        val work = works.firstOrNull { it.key == key } ?: return SourceOutcome.Success(null)
        return SourceOutcome.Success(ref(work))
    }

    fun discoveryRefs(): List<SourceMangaRef> = works.map(::ref)

    private fun summary(work: FixtureWork): SourceMangaSummary =
        SourceMangaSummary(
            ref = ref(work),
            title = work.title,
            alternativeTitles = work.alternativeTitles,
            coverUrl = null,
            languageTag = "en",
            contentType = work.contentType
        )

    private fun ref(work: FixtureWork): SourceMangaRef =
        SourceMangaRef(
            sourceId = SOURCE_ID,
            key = work.key,
            publicUrl = "https://" + descriptor.primaryDomain + "/title/" + work.key
        )

    private fun <T> notFound(message: String): SourceOutcome<T> =
        SourceOutcome.Failure(
            SourceFailure(
                kind = SourceFailureKind.NOT_FOUND,
                message = message
            )
        )

    private data class FixtureWork(
        val key: String,
        val title: String,
        val alternativeTitles: Set<String>,
        val description: String,
        val tags: Set<String>,
        val contentType: MangaContentType,
        val chapterCount: Int
    )

    companion object {
        val SOURCE_ID = SourceId("fixture.local")
    }
}
