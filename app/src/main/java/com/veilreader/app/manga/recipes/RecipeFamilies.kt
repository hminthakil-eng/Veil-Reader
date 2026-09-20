package com.veilreader.app.manga.recipes

import java.net.URLEncoder

object MadaraRecipeDefaults {
    fun parsers(): HtmlRecipeParsers = HtmlRecipeParsers(
        browse = HtmlBrowseRules(
            itemSelector = "div.page-item-detail, .c-tabs-item__content",
            titleSelector = ".post-title a",
            coverSelector = "img",
            nextPageSelector = ".pagination .next, a.next"
        ),
        details = HtmlDetailsRules(
            titleSelector = ".post-title h1, .post-title h3, #manga-title h1",
            authorSelector = ".author-content a, .manga-authors a",
            artistSelector = ".artist-content a",
            descriptionSelector = ".description-summary .summary__content, .manga-excerpt",
            statusSelector = ".post-status .summary-content, .summary-heading:contains(Status) + div",
            tagSelector = ".genres-content a, .tags-content a",
            coverSelector = ".summary_image img"
        ),
        chapters = HtmlChapterRules(
            itemSelector = "li.wp-manga-chapter",
            linkSelector = "a"
        ),
        pages = HtmlPageRules(
            imageSelector = ".reading-content img, .page-break img"
        )
    )

    fun adminAjaxChapters(
        baseUrl: String,
        mangaPostId: String,
        headers: Map<String, String> = emptyMap()
    ): MangaHttpPlan.PostForm = MangaHttpPlan.PostForm(
        url = baseUrl.trimEnd('/') + "/wp-admin/admin-ajax.php",
        fields = mapOf(
            "action" to "manga_get_chapters",
            "manga" to mangaPostId
        ),
        headers = headers + mapOf("X-Requested-With" to "XMLHttpRequest")
    )

    fun mangaAjaxChapters(
        baseUrl: String,
        mangaPath: String,
        headers: Map<String, String> = emptyMap()
    ): MangaHttpPlan.PostForm = MangaHttpPlan.PostForm(
        url = baseUrl.trimEnd('/') + "/" +
            mangaPath.trim('/') + "/ajax/chapters/",
        fields = emptyMap(),
        headers = headers + mapOf("X-Requested-With" to "XMLHttpRequest")
    )
}

object MangaThemesiaRecipeDefaults {
    fun parsers(): HtmlRecipeParsers = HtmlRecipeParsers(
        browse = HtmlBrowseRules(
            itemSelector = ".listupd .bsx, .utao .uta .imgu",
            titleSelector = "a",
            titleAttribute = "title",
            coverSelector = "img",
            nextPageSelector = ".pagination .next, .hpage .r"
        ),
        details = HtmlDetailsRules(
            titleSelector = ".entry-title, .ts-breadcrumb li:last-child span",
            authorSelector = ".infotable tr:contains(Author) td:last-child, .tsinfo .imptdt:contains(Author) i",
            artistSelector = ".infotable tr:contains(Artist) td:last-child, .tsinfo .imptdt:contains(Artist) i",
            descriptionSelector = ".desc, .entry-content[itemprop=description]",
            statusSelector = ".infotable tr:contains(Status) td:last-child, .tsinfo .imptdt:contains(Status) i",
            tagSelector = ".mgen a, .seriestugenre a",
            coverSelector = ".infomanga img, .thumb img"
        ),
        chapters = HtmlChapterRules(
            itemSelector = "#chapterlist li, .bxcl li, .cl li",
            linkSelector = "a",
            titleSelector = ".chapternum, .lch a, a"
        ),
        pages = HtmlPageRules(
            imageSelector = "#readerarea img"
        )
    )

    fun permanentSlug(rawSlug: String): String =
        rawSlug.trim('/').substringAfterLast('/').replaceFirst(DYNAMIC_SLUG_PREFIX, "")

    private val DYNAMIC_SLUG_PREFIX = Regex("""^\d+-""")
}

internal fun encodedQuery(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name())

internal fun pageNumber(cursor: String?): Int =
    cursor?.toIntOrNull()?.coerceAtLeast(1) ?: 1
