package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.content
import org.readium.r2.shared.publication.services.content.DefaultContentService
import org.readium.r2.shared.publication.services.content.iterators.HtmlResourceContentIterator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.CompositeContainer
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.shared.util.resource.StringResource
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The actual pinned Readium iterator, not a fake queue or independent HTML parser. */
@OptIn(ExperimentalReadiumApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReadiumTtsContentTest {
    private val firstUrl = requireNotNull(Url("chapter-one.xhtml"))
    private val secondUrl = requireNotNull(Url("chapter-two.xhtml"))
    private val targetText = "متن فارسی English العربية 中文 😀 پایان"

    private fun publication(): Publication = Publication.Builder(
        manifest = Manifest(
            metadata = Metadata(languages = listOf("fa")),
            readingOrder = listOf(Link(firstUrl, MediaType.XHTML), Link(secondUrl, MediaType.XHTML))
        ),
        container = CompositeContainer(
            SingleResourceContainer(firstUrl, StringResource("""
                <html xmlns="http://www.w3.org/1999/xhtml"><body>
                <p id="intro">Earlier paragraph</p>
                <p id="target" lang="fa">$targetText</p>
                </body></html>
            """.trimIndent())),
            SingleResourceContainer(secondUrl, StringResource("""
                <html xmlns="http://www.w3.org/1999/xhtml"><body>
                <p lang="en">Next chapter paragraph</p>
                </body></html>
            """.trimIndent()))
        ),
        servicesBuilder = Publication.ServicesBuilder(content = DefaultContentService.createFactory(
            listOf(HtmlResourceContentIterator.Factory())
        ))
    ).build()

    @Test
    fun selectorStartPreservesRealSourceLocatorsUnicodeAndChapterOrder() = runBlocking {
        val publication = publication()
        try {
            val start = Locator(firstUrl, MediaType.XHTML, locations = Locator.Locations(
                progression = 0.4, otherLocations = mapOf("cssSelector" to "#target")
            ))
            val source = requireNotNull(ReadiumTtsContent.create(publication, start, maximumLength = 7))
            val chunks = buildList {
                repeat(100) {
                    val next = source.next() ?: return@buildList
                    add(next)
                }
                fail("Content did not terminate")
            }
            assertEquals(targetText, chunks.filter { it.locator.href == firstUrl }.joinToString("") { it.text })
            assertEquals("Next chapter paragraph", chunks.filter { it.locator.href == secondUrl }.joinToString("") { it.text })
            assertTrue(chunks.all { it.text.length <= 7 })
            assertTrue(chunks.filter { it.locator.href == firstUrl }.all { it.languageTag == "fa" })
            assertTrue(chunks.all { (it.locator.locations.otherLocations["cssSelector"] as? String)?.isNotBlank() == true })
            assertNull(source.next())
        } finally { publication.close() }
    }

    @Test
    fun readiumSentenceTokenizerCreatesNaturalUtteranceBoundaries() = runBlocking {
        val url = requireNotNull(Url("sentences.xhtml"))
        val publication = Publication.Builder(
            manifest = Manifest(
                metadata = Metadata(languages = listOf("en")),
                readingOrder = listOf(Link(url, MediaType.XHTML))
            ),
            container = SingleResourceContainer(
                url,
                StringResource(
                    """
                    <html xmlns="http://www.w3.org/1999/xhtml"><body>
                    <p id="sentences" lang="en">First sentence. Second sentence! Third sentence?</p>
                    </body></html>
                    """.trimIndent()
                )
            ),
            servicesBuilder = Publication.ServicesBuilder(
                content = DefaultContentService.createFactory(
                    listOf(HtmlResourceContentIterator.Factory())
                )
            )
        ).build()

        try {
            val start = Locator(
                url,
                MediaType.XHTML,
                locations = Locator.Locations(
                    otherLocations = mapOf("cssSelector" to "#sentences")
                )
            )
            val source = requireNotNull(
                ReadiumTtsContent.create(
                    publication = publication,
                    start = start,
                    maximumLength = 1000
                )
            )
            val utterances = buildList {
                while (true) {
                    add(source.next() ?: break)
                }
            }

            assertEquals(
                listOf("First sentence.", "Second sentence!", "Third sentence?"),
                utterances.map { it.text }
            )
            assertTrue(utterances.all { it.languageTag == "en" })
            assertTrue(
                utterances.all {
                    (it.locator.locations.otherLocations["cssSelector"] as? String)
                        ?.isNotBlank() == true
                }
            )
        } finally {
            publication.close()
        }
    }

    @Test
    fun progressionWithoutSelectorHasOnlyResourceBoundaryPrecision() = runBlocking {
        val publication = publication()
        try {
            val locator = Locator(firstUrl, MediaType.XHTML, locations = Locator.Locations(progression = 0.4))
            val upstream = requireNotNull(publication.content(locator)).iterator().nextOrNull()
            assertEquals("Earlier paragraph", (upstream as Content.TextElement).text)
            assertNull(ReadiumTtsContent.create(publication, locator))
        } finally { publication.close() }
    }

    @Test
    fun malformedProgressionFailsWithoutAChapterRestart() = runBlocking {
        val publication = publication()
        try {
            listOf(Double.NaN, Double.POSITIVE_INFINITY, -0.1, 1.1).forEach { progression ->
                assertNull(ReadiumTtsContent.create(publication, Locator(firstUrl, MediaType.XHTML,
                    locations = Locator.Locations(progression = progression))))
            }
        } finally { publication.close() }
    }

    @Test
    fun speechNeverMutatesTheRequestedReaderLocator() = runBlocking {
        val publication = publication()
        try {
            val locator = Locator(firstUrl, MediaType.XHTML, locations = Locator.Locations(
                progression = 0.4, otherLocations = mapOf("cssSelector" to "#target")
            ))
            val before = locator.toJSON().toString()
            requireNotNull(ReadiumTtsContent.create(publication, locator)).next()
            assertEquals(before, locator.toJSON().toString())
        } finally { publication.close() }
    }
}
