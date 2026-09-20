package com.veilreader.app.manga.recipes

import com.veilreader.app.manga.core.MangaProviderId
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaSearchRequest
import com.veilreader.app.manga.core.MangaSourceCapability
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.core.MangaStatus
import com.veilreader.app.manga.core.MangaUpdateOptions
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.MangaHttpResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlSourceRecipeTest {
    private val descriptor = MangaSourceDescriptor(
        id = MangaSourceId("fixture.en"),
        providerId = MangaProviderId("fixture"),
        version = 1,
        name = "Fixture",
        language = "en",
        capabilities = setOf(
            MangaSourceCapability.SEARCH,
            MangaSourceCapability.POPULAR,
            MangaSourceCapability.LATEST,
            MangaSourceCapability.URL_RESOLUTION
        )
    )

    @Test
    fun browse_parsesRelativeKeysLazyCoversAndNextCursor() = runBlocking {
        val http = RecordingHttpClient(
            getBodies = mapOf("https://reader.example/search?q=veil&page=1" to BROWSE_HTML)
        )
        val source = HtmlRecipeSourceProvider(recipe(), http)

        val result = source.search(MangaSearchRequest("veil"))

        assertEquals(2, result.items.size)
        assertEquals("/series/veil-knight/", result.items[0].ref.key)
        assertEquals("Veil Knight", result.items[0].title)
        assertEquals(
            "https://reader.example/covers/veil.jpg",
            result.items[0].cover?.url
        )
        assertEquals("2", result.nextCursor)
        assertTrue(result.items.none { it.title == "Foreign" })
    }

    @Test
    fun sharedUpdate_fetchesOnceAndParsesDetailsAndChapters() = runBlocking {
        val http = RecordingHttpClient(
            getBodies = mapOf("https://reader.example/series/veil-knight/" to UPDATE_HTML)
        )
        val source = HtmlRecipeSourceProvider(recipe(), http)
        val ref = MangaRef(descriptor.id, "/series/veil-knight/")

        val update = source.fetchUpdate(
            ref = ref,
            existingChapters = emptyList(),
            options = MangaUpdateOptions()
        )

        assertEquals(1, http.getCalls.size)
        assertEquals("Veil Knight", update.details?.title)
        assertEquals(MangaStatus.ONGOING, update.details?.status)
        assertEquals(listOf("Author One", "Artist Two"), update.details?.authors)
        assertEquals(listOf("Action", "Fantasy"), update.details?.tags)
        assertEquals(2, update.chapters?.size)
        assertEquals(12.5, update.chapters?.first()?.chapterNumber ?: 0.0, 0.0)
        assertEquals("/series/veil-knight/chapter-12-5/", update.chapters?.first()?.ref?.key)
    }

    @Test
    fun pages_useLazyAttributesAndRemoveDuplicateImages() = runBlocking {
        val http = RecordingHttpClient(
            getBodies = mapOf(
                "https://reader.example/series/veil-knight/chapter-12-5/" to PAGES_HTML
            )
        )
        val source = HtmlRecipeSourceProvider(recipe(), http)
        val manga = MangaRef(descriptor.id, "/series/veil-knight/")

        val pages = source.pages(
            com.veilreader.app.manga.core.MangaChapterRef(
                manga,
                "/series/veil-knight/chapter-12-5/"
            )
        )

        assertEquals(listOf(0, 1), pages.map { it.index })
        assertEquals("https://cdn.reader.example/p1.jpg", pages[0].image.url)
        assertEquals("https://reader.example/pages/p2.jpg", pages[1].image.url)
    }

    @Test
    fun urlResolution_rejectsForeignHostsByDefault() = runBlocking {
        val source = HtmlRecipeSourceProvider(recipe(), RecordingHttpClient())

        assertEquals(
            "/series/veil-knight/",
            source.resolveUrl("https://reader.example/series/veil-knight/")?.key
        )
        assertNull(source.resolveUrl("https://evil.example/series/veil-knight/"))
    }

    @Test
    fun recipe_canUseFormPostWithoutOwningNetworkStack() = runBlocking {
        val http = RecordingHttpClient(
            postBodies = mapOf("https://reader.example/ajax/search" to BROWSE_HTML)
        )
        val postRecipe = recipe().copy(
            endpoints = recipe().endpoints.copy(
                search = { request ->
                    MangaHttpPlan.PostForm(
                        url = "https://reader.example/ajax/search",
                        fields = mapOf(
                            "q" to request.query,
                            "page" to pageNumber(request.cursor).toString()
                        ),
                        headers = mapOf("X-Requested-With" to "XMLHttpRequest")
                    )
                }
            )
        )
        val source = HtmlRecipeSourceProvider(postRecipe, http)

        source.search(MangaSearchRequest("veil"))

        assertEquals(1, http.postCalls.size)
        assertEquals("veil", http.postCalls.single().fields["q"])
        assertEquals("XMLHttpRequest", http.postCalls.single().headers["X-Requested-With"])
    }

    @Test
    fun requestPlan_rejectsUnapprovedNetworkHostBeforeHttpCall() = runBlocking {
        val http = RecordingHttpClient()
        val unsafe = recipe().copy(
            endpoints = recipe().endpoints.copy(
                search = {
                    MangaHttpPlan.Get("https://collector.example/search")
                }
            )
        )
        val source = HtmlRecipeSourceProvider(unsafe, http)

        val error = runCatching {
            source.search(MangaSearchRequest("veil"))
        }.exceptionOrNull()

        assertTrue(error is com.veilreader.app.manga.core.MangaSourceException.Blocked)
        assertTrue(http.getCalls.isEmpty())
    }

    @Test
    fun pageParser_ignoresInlinePlaceholderImages() = runBlocking {
        val html = """
            <html><body>
              <img class="page" src="data:image/gif;base64,AAAA">
              <img class="page" data-src="/pages/real.jpg" src="data:image/gif;base64,BBBB">
            </body></html>
        """.trimIndent()
        val http = RecordingHttpClient(
            getBodies = mapOf(
                "https://reader.example/series/veil-knight/chapter-12-5/" to html
            )
        )
        val source = HtmlRecipeSourceProvider(recipe(), http)
        val manga = MangaRef(descriptor.id, "/series/veil-knight/")

        val pages = source.pages(
            com.veilreader.app.manga.core.MangaChapterRef(
                manga,
                "/series/veil-knight/chapter-12-5/"
            )
        )

        assertEquals(1, pages.size)
        assertEquals("https://reader.example/pages/real.jpg", pages.single().image.url)
    }

    @Test
    fun familyHelpers_normalizeDynamicSlugsAndMadaraAjaxPlan() {
        assertEquals("veil-knight", MangaThemesiaRecipeDefaults.permanentSlug("2381-veil-knight"))

        val plan = MadaraRecipeDefaults.adminAjaxChapters(
            baseUrl = "https://reader.example",
            mangaPostId = "991"
        )

        assertEquals("https://reader.example/wp-admin/admin-ajax.php", plan.url)
        assertEquals("manga_get_chapters", plan.fields["action"])
        assertEquals("991", plan.fields["manga"])
    }

    private fun recipe(): HtmlSourceRecipe = HtmlSourceRecipe(
        family = HtmlRecipeFamily.CUSTOM,
        descriptor = descriptor,
        baseUrl = "https://reader.example/",
        endpoints = HtmlRecipeEndpoints(
            search = { request ->
                MangaHttpPlan.Get(
                    "https://reader.example/search?q=${encodedQuery(request.query)}&page=" +
                        pageNumber(request.cursor)
                )
            },
            popular = { request ->
                MangaHttpPlan.Get(
                    "https://reader.example/popular?page=" + pageNumber(request.cursor)
                )
            },
            latest = { request ->
                MangaHttpPlan.Get(
                    "https://reader.example/latest?page=" + pageNumber(request.cursor)
                )
            },
            update = { ref ->
                HtmlUpdatePlan.Shared(
                    MangaHttpPlan.Get("https://reader.example" + ref.key)
                )
            },
            pages = { ref ->
                MangaHttpPlan.Get("https://reader.example" + ref.key)
            }
        ),
        parsers = HtmlRecipeParsers(
            browse = HtmlBrowseRules(
                itemSelector = ".card",
                titleSelector = ".title",
                linkSelector = ".title",
                coverSelector = "img",
                nextPageSelector = ".next"
            ),
            details = HtmlDetailsRules(
                titleSelector = "h1",
                authorSelector = ".author",
                artistSelector = ".artist",
                descriptionSelector = ".description",
                statusSelector = ".status",
                tagSelector = ".tag",
                coverSelector = ".cover img"
            ),
            chapters = HtmlChapterRules(
                itemSelector = ".chapter",
                linkSelector = "a",
                scanlatorSelector = ".group"
            ),
            pages = HtmlPageRules(
                imageSelector = ".page"
            )
        )
    )

    private data class PostCall(
        val url: String,
        val headers: Map<String, String>,
        val fields: Map<String, String>
    )

    private class RecordingHttpClient(
        private val getBodies: Map<String, String> = emptyMap(),
        private val postBodies: Map<String, String> = emptyMap()
    ) : MangaHttpClient {
        val getCalls = mutableListOf<String>()
        val postCalls = mutableListOf<PostCall>()

        override suspend fun get(
            url: String,
            headers: Map<String, String>
        ): MangaHttpResponse {
            getCalls += url
            return MangaHttpResponse(
                code = 200,
                body = getBodies[url] ?: error("Unexpected GET: $url")
            )
        }

        override suspend fun postForm(
            url: String,
            headers: Map<String, String>,
            fields: Map<String, String>
        ): MangaHttpResponse {
            postCalls += PostCall(url, headers, fields)
            return MangaHttpResponse(
                code = 200,
                body = postBodies[url] ?: error("Unexpected POST: $url")
            )
        }
    }

    private companion object {
        val BROWSE_HTML = """
            <html><body>
              <article class="card">
                <a class="title" href="/series/veil-knight/">Veil Knight</a>
                <img data-src="/covers/veil.jpg">
              </article>
              <article class="card">
                <a class="title" href="/series/moon/">Moon Archive</a>
                <img src="/covers/moon.jpg">
              </article>
              <article class="card">
                <a class="title" href="https://evil.example/series/x/">Foreign</a>
              </article>
              <a class="next" href="/search?page=2">Next</a>
            </body></html>
        """.trimIndent()

        val UPDATE_HTML = """
            <html><body>
              <section class="details">
                <h1>Veil Knight</h1>
                <a class="author">Author One</a>
                <a class="artist">Artist Two</a>
                <div class="description">A hidden city under glass.</div>
                <div class="status">Ongoing</div>
                <a class="tag">Action</a>
                <a class="tag">Fantasy</a>
                <div class="cover"><img data-lazy-src="/covers/veil-large.jpg"></div>
              </section>
              <ul>
                <li class="chapter">
                  <a href="/series/veil-knight/chapter-12-5/">Chapter 12.5 - Arrival</a>
                  <span class="group">Group A</span>
                </li>
                <li class="chapter">
                  <a href="/series/veil-knight/chapter-13/">Chapter 13</a>
                </li>
              </ul>
            </body></html>
        """.trimIndent()

        val PAGES_HTML = """
            <html><body>
              <img class="page" data-src="//cdn.reader.example/p1.jpg">
              <img class="page" src="/pages/p2.jpg">
              <img class="page" data-original="//cdn.reader.example/p1.jpg">
            </body></html>
        """.trimIndent()
    }
}
