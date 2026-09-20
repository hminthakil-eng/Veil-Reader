package com.veilreader.app.manga.recipes

import com.veilreader.app.manga.core.MangaBrowseRequest
import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaDetails
import com.veilreader.app.manga.core.MangaFilterDefinition
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaResourceRequest
import com.veilreader.app.manga.core.MangaResultPage
import com.veilreader.app.manga.core.MangaSearchRequest
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceException
import com.veilreader.app.manga.core.MangaSourceProvider
import com.veilreader.app.manga.core.MangaStatus
import com.veilreader.app.manga.core.MangaSummary
import com.veilreader.app.manga.core.MangaUpdate
import com.veilreader.app.manga.core.MangaUpdateOptions
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.MangaHttpResponse
import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

enum class HtmlRecipeFamily {
    MADARA,
    MANGATHEMESIA,
    CUSTOM
}

sealed interface MangaHttpPlan {
    val url: String
    val headers: Map<String, String>

    data class Get(
        override val url: String,
        override val headers: Map<String, String> = emptyMap()
    ) : MangaHttpPlan

    data class PostForm(
        override val url: String,
        val fields: Map<String, String>,
        override val headers: Map<String, String> = emptyMap()
    ) : MangaHttpPlan
}

sealed interface HtmlUpdatePlan {
    data class Shared(val request: MangaHttpPlan) : HtmlUpdatePlan

    data class Split(
        val details: MangaHttpPlan,
        val chapters: MangaHttpPlan
    ) : HtmlUpdatePlan
}

data class HtmlBrowseRules(
    val itemSelector: String,
    val titleSelector: String,
    val linkSelector: String = titleSelector,
    val linkAttribute: String = "href",
    val titleAttribute: String? = null,
    val coverSelector: String? = null,
    val coverAttributes: List<String> = DEFAULT_IMAGE_ATTRIBUTES,
    val nextPageSelector: String? = null
)

data class HtmlDetailsRules(
    val titleSelector: String,
    val authorSelector: String? = null,
    val artistSelector: String? = null,
    val descriptionSelector: String? = null,
    val statusSelector: String? = null,
    val tagSelector: String? = null,
    val coverSelector: String? = null,
    val coverAttributes: List<String> = DEFAULT_IMAGE_ATTRIBUTES,
    val statusTokens: Map<MangaStatus, List<String>> = DEFAULT_STATUS_TOKENS
)

data class HtmlChapterRules(
    val itemSelector: String,
    val linkSelector: String,
    val titleSelector: String = linkSelector,
    val linkAttribute: String = "href",
    val scanlatorSelector: String? = null,
    val chapterNumberPattern: Regex = DEFAULT_CHAPTER_NUMBER_PATTERN
)

data class HtmlPageRules(
    val imageSelector: String,
    val imageAttributes: List<String> = DEFAULT_IMAGE_ATTRIBUTES
)

data class HtmlRecipeParsers(
    val browse: HtmlBrowseRules,
    val details: HtmlDetailsRules,
    val chapters: HtmlChapterRules,
    val pages: HtmlPageRules
)

data class HtmlRecipeEndpoints(
    val search: (MangaSearchRequest) -> MangaHttpPlan,
    val popular: ((MangaBrowseRequest) -> MangaHttpPlan)? = null,
    val latest: ((MangaBrowseRequest) -> MangaHttpPlan)? = null,
    val update: (MangaRef) -> HtmlUpdatePlan,
    val pages: (MangaChapterRef) -> MangaHttpPlan
)

data class HtmlSourceRecipe(
    val family: HtmlRecipeFamily,
    val descriptor: MangaSourceDescriptor,
    val baseUrl: String,
    val endpoints: HtmlRecipeEndpoints,
    val parsers: HtmlRecipeParsers,
    val filters: List<MangaFilterDefinition> = emptyList(),
    val allowCrossHostContent: Boolean = false
) {
    init {
        val uri = URI(baseUrl)
        require(uri.scheme == "https" || uri.scheme == "http") {
            "HTML source recipe base URL must use http or https."
        }
        require(!uri.host.isNullOrBlank()) { "HTML source recipe base URL must have a host." }
    }
}

class HtmlRecipeSourceProvider(
    private val recipe: HtmlSourceRecipe,
    private val http: MangaHttpClient
) : MangaSourceProvider {
    override val descriptor: MangaSourceDescriptor = recipe.descriptor

    override suspend fun search(request: MangaSearchRequest): MangaResultPage<MangaSummary> =
        execute(recipe.endpoints.search(request)).let { response ->
            HtmlRecipeParser(recipe).parseBrowse(response.body, request.cursor)
        }

    override suspend fun popular(request: MangaBrowseRequest): MangaResultPage<MangaSummary> {
        val plan = recipe.endpoints.popular
            ?: throw MangaSourceException.Unsupported("Popular browse is not configured.")
        return execute(plan(request)).let { response ->
            HtmlRecipeParser(recipe).parseBrowse(response.body, request.cursor)
        }
    }

    override suspend fun latest(request: MangaBrowseRequest): MangaResultPage<MangaSummary> {
        val plan = recipe.endpoints.latest
            ?: throw MangaSourceException.Unsupported("Latest browse is not configured.")
        return execute(plan(request)).let { response ->
            HtmlRecipeParser(recipe).parseBrowse(response.body, request.cursor)
        }
    }

    override suspend fun filters(): List<MangaFilterDefinition> = recipe.filters

    override suspend fun resolveUrl(url: String): MangaRef? {
        val key = sourceKey(
            baseUrl = recipe.baseUrl,
            rawUrl = url,
            allowCrossHost = recipe.allowCrossHostContent
        ) ?: return null
        return MangaRef(descriptor.id, key)
    }

    override suspend fun fetchUpdate(
        ref: MangaRef,
        existingChapters: List<MangaChapter>,
        options: MangaUpdateOptions
    ): MangaUpdate {
        require(ref.sourceId == descriptor.id) {
            "HTML recipe received a manga owned by another source."
        }
        val parser = HtmlRecipeParser(recipe)
        return when (val plan = recipe.endpoints.update(ref)) {
            is HtmlUpdatePlan.Shared -> {
                val body = execute(plan.request).body
                MangaUpdate(
                    ref = ref,
                    details = if (options.fetchDetails) parser.parseDetails(body, ref) else null,
                    chapters = if (options.fetchChapters) parser.parseChapters(body, ref) else null
                )
            }

            is HtmlUpdatePlan.Split -> {
                val details = if (options.fetchDetails) {
                    parser.parseDetails(execute(plan.details).body, ref)
                } else {
                    null
                }
                val chapters = if (options.fetchChapters) {
                    parser.parseChapters(execute(plan.chapters).body, ref)
                } else {
                    null
                }
                MangaUpdate(ref = ref, details = details, chapters = chapters)
            }
        }
    }

    override suspend fun pages(ref: MangaChapterRef): List<MangaPage> {
        require(ref.manga.sourceId == descriptor.id) {
            "HTML recipe received a chapter owned by another source."
        }
        return HtmlRecipeParser(recipe).parsePages(
            execute(recipe.endpoints.pages(ref)).body
        )
    }

    private suspend fun execute(plan: MangaHttpPlan): MangaHttpResponse = when (plan) {
        is MangaHttpPlan.Get -> http.get(plan.url, plan.headers)
        is MangaHttpPlan.PostForm -> http.postForm(plan.url, plan.headers, plan.fields)
    }
}

class HtmlRecipeParser(
    private val recipe: HtmlSourceRecipe
) {
    fun parseBrowse(
        html: String,
        cursor: String?
    ): MangaResultPage<MangaSummary> {
        val document = document(html)
        val rules = recipe.parsers.browse
        val items = document.select(rules.itemSelector).mapNotNull { item ->
            val link = item.selectFirst(rules.linkSelector) ?: return@mapNotNull null
            val rawHref = link.attr(rules.linkAttribute).takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val key = sourceKey(
                baseUrl = recipe.baseUrl,
                rawUrl = absoluteUrl(link, rules.linkAttribute, rawHref),
                allowCrossHost = recipe.allowCrossHostContent
            ) ?: return@mapNotNull null

            val titleElement = item.selectFirst(rules.titleSelector) ?: return@mapNotNull null
            val title = rules.titleAttribute
                ?.let(titleElement::attr)
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: titleElement.text().trim()
            if (title.isBlank()) return@mapNotNull null

            MangaSummary(
                ref = MangaRef(recipe.descriptor.id, key),
                title = title,
                cover = rules.coverSelector
                    ?.let(item::selectFirst)
                    ?.let { coverRequest(it, rules.coverAttributes) }
            )
        }

        val nextCursor = rules.nextPageSelector
            ?.takeIf { document.selectFirst(it) != null }
            ?.let { nextCursor(cursor) }

        return MangaResultPage(items = items, nextCursor = nextCursor)
    }

    fun parseDetails(
        html: String,
        ref: MangaRef
    ): MangaDetails {
        val document = document(html)
        val rules = recipe.parsers.details
        val title = requiredText(document, rules.titleSelector, "title")
        val authors = buildList {
            rules.authorSelector?.let { selector ->
                addAll(document.select(selector).map(Element::text))
            }
            rules.artistSelector?.let { selector ->
                addAll(document.select(selector).map(Element::text))
            }
        }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

        val description = rules.descriptionSelector
            ?.let(document::selectFirst)
            ?.text()
            ?.trim()
            .orEmpty()

        val tags = rules.tagSelector
            ?.let(document::select)
            .orEmpty()
            .map(Element::text)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

        val statusText = rules.statusSelector
            ?.let(document::selectFirst)
            ?.text()
            .orEmpty()

        return MangaDetails(
            ref = ref,
            title = title,
            description = description,
            authors = authors,
            tags = tags,
            status = parseStatus(statusText, rules.statusTokens),
            cover = rules.coverSelector
                ?.let(document::selectFirst)
                ?.let { coverRequest(it, rules.coverAttributes) }
        )
    }

    fun parseChapters(
        html: String,
        manga: MangaRef
    ): List<MangaChapter> {
        val document = document(html)
        val rules = recipe.parsers.chapters
        return document.select(rules.itemSelector).mapNotNull { item ->
            val link = item.selectFirst(rules.linkSelector) ?: return@mapNotNull null
            val rawHref = link.attr(rules.linkAttribute).takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val key = sourceKey(
                baseUrl = recipe.baseUrl,
                rawUrl = absoluteUrl(link, rules.linkAttribute, rawHref),
                allowCrossHost = recipe.allowCrossHostContent
            ) ?: return@mapNotNull null
            val title = item.selectFirst(rules.titleSelector)?.text()?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: link.text().trim()

            MangaChapter(
                ref = MangaChapterRef(manga, key),
                title = title,
                chapterNumber = rules.chapterNumberPattern
                    .find(title)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toDoubleOrNull(),
                scanlator = rules.scanlatorSelector
                    ?.let(item::selectFirst)
                    ?.text()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            )
        }
    }

    fun parsePages(html: String): List<MangaPage> {
        val document = document(html)
        val rules = recipe.parsers.pages
        return document.select(rules.imageSelector)
            .mapNotNull { image ->
                firstImageUrl(image, rules.imageAttributes)
                    ?.let { absoluteUrl(image, null, it) }
            }
            .filter(String::isNotBlank)
            .distinct()
            .mapIndexed { index, url ->
                MangaPage(index, MangaResourceRequest(url))
            }
    }

    private fun document(html: String): Document = try {
        Jsoup.parse(html, recipe.baseUrl)
    } catch (error: Exception) {
        throw MangaSourceException.ParseFailure(
            message = "HTML source document could not be parsed.",
            cause = error
        )
    }

    private fun coverRequest(
        image: Element,
        attributes: List<String>
    ): MangaResourceRequest? =
        firstImageUrl(image, attributes)
            ?.let { absoluteUrl(image, null, it) }
            ?.takeIf(String::isNotBlank)
            ?.let(::MangaResourceRequest)

    private fun absoluteUrl(
        element: Element,
        attribute: String?,
        rawValue: String
    ): String {
        val abs = attribute?.let(element::absUrl).orEmpty()
        if (abs.isNotBlank()) return abs
        return runCatching {
            URI(recipe.baseUrl).resolve(rawValue).toString()
        }.getOrDefault(rawValue)
    }
}

internal fun sourceKey(
    baseUrl: String,
    rawUrl: String,
    allowCrossHost: Boolean
): String? = runCatching {
    val base = URI(baseUrl)
    val resolved = base.resolve(rawUrl)
    if (!allowCrossHost && !resolved.host.equals(base.host, ignoreCase = true)) {
        return null
    }
    buildString {
        append(resolved.rawPath?.ifBlank { "/" } ?: "/")
        resolved.rawQuery?.takeIf(String::isNotBlank)?.let {
            append('?')
            append(it)
        }
    }
}.getOrNull()

internal fun parseStatus(
    raw: String,
    tokens: Map<MangaStatus, List<String>>
): MangaStatus {
    val normalized = raw.trim().lowercase()
    if (normalized.isBlank()) return MangaStatus.UNKNOWN
    return tokens.entries.firstOrNull { (_, phrases) ->
        phrases.any { phrase -> normalized.contains(phrase.lowercase()) }
    }?.key ?: MangaStatus.UNKNOWN
}

internal fun firstImageUrl(
    element: Element,
    attributes: List<String>
): String? = attributes.firstNotNullOfOrNull { attribute ->
    element.attr(attribute).trim().takeIf(String::isNotEmpty)
}

private fun requiredText(
    document: Document,
    selector: String,
    field: String
): String {
    val value = document.selectFirst(selector)?.text()?.trim().orEmpty()
    if (value.isBlank()) {
        throw MangaSourceException.SourceChanged(
            "HTML source is missing required $field selector: $selector"
        )
    }
    return value
}

private fun nextCursor(cursor: String?): String =
    ((cursor?.toIntOrNull() ?: 1) + 1).toString()

internal val DEFAULT_IMAGE_ATTRIBUTES = listOf(
    "data-src",
    "data-lazy-src",
    "data-original",
    "src"
)

internal val DEFAULT_STATUS_TOKENS = linkedMapOf(
    MangaStatus.COMPLETED to listOf("completed", "complete", "finished"),
    MangaStatus.ONGOING to listOf("ongoing", "on going", "publishing"),
    MangaStatus.HIATUS to listOf("hiatus", "on hold"),
    MangaStatus.CANCELLED to listOf("cancelled", "canceled", "dropped")
)

internal val DEFAULT_CHAPTER_NUMBER_PATTERN =
    Regex("""(?i)(?:chapter|ch\.?)[\s#:_-]*(\d+(?:\.\d+)?)""")
