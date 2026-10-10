@file:OptIn(
    org.readium.r2.shared.ExperimentalReadiumApi::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class
)

package com.veilreader.app.ui.reader

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.SearchTry
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

class ReaderPublicationSearchTest {
    private fun locator(position: Int): Locator =
        Locator(
            href = Url("chapter.xhtml")!!,
            mediaType = MediaType.XHTML,
            locations = Locator.Locations(position = position)
        )

    private class FakeCursor(
        private val steps: List<SearchTry<LocatorCollection?>>
    ) : SearchIterator {
        var nextCalls = 0
        var closeCalls = 0
        override suspend fun next(): SearchTry<LocatorCollection?> {
            val index = nextCalls++
            return steps.getOrElse(index) { Try.success(null) }
        }
        override fun close() { closeCalls++ }
    }

    @Test
    fun invalidTextDoesNotOpenSearchOrProcessEntireUntrustedPublication() = runTest {
        var openCalls = 0
        for (text in listOf("", " \n ", "bad\u0000text", "x".repeat(129))) {
            val result = openReaderBookSearch(text) {
                openCalls++
                null
            }
            assertEquals(ReaderBookSearchOpen.InvalidQuery, result)
        }
        assertEquals(0, openCalls)
    }

    @Test
    fun unsupportedPublicationFailsClosedAndSanitizesQuery() = runTest {
        var request: String? = null
        val unsupported = openReaderBookSearch("  سلام دنیا  ") {
            request = it
            null
        }
        assertEquals("سلام دنیا", request)
        assertEquals(ReaderBookSearchOpen.Unsupported, unsupported)
    }

    @Test
    fun largeReadiumResultsArePaginatedWithoutLosingAnyLocator() = runTest {
        val cursor = FakeCursor(listOf(
            Try.success(LocatorCollection(locators = listOf(locator(1), locator(2), locator(3)))),
            Try.success(LocatorCollection(locators = listOf(locator(4), locator(5)))),
            Try.success(null)
        ))
        val opened = openReaderBookSearch("constellation") { cursor }
        assertTrue(opened is ReaderBookSearchOpen.Ready)
        val session = (opened as ReaderBookSearchOpen.Ready).session
        val first = session.nextPage(limit = 2) as ReaderBookSearchPage.Hits
        assertEquals(listOf(1, 2), first.locators.map { it.locations.position })
        assertFalse(first.isLast)
        assertEquals(1, cursor.nextCalls)

        val second = session.nextPage(limit = 2) as ReaderBookSearchPage.Hits
        assertEquals(listOf(3, 4), second.locators.map { it.locations.position })
        assertFalse(second.isLast)
        assertEquals(2, cursor.nextCalls)

        val third = session.nextPage(limit = 2) as ReaderBookSearchPage.Hits
        assertEquals(listOf(5), third.locators.map { it.locations.position })
        assertTrue(third.isLast)
        assertEquals(3, cursor.nextCalls)
        assertEquals(1, cursor.closeCalls)
        assertEquals(ReaderBookSearchPage.Closed, session.nextPage())
    }

    @Test
    fun closingSearchIsIdempotentAndDiscardsFutureQueries() = runTest {
        val cursor = FakeCursor(listOf(Try.success(LocatorCollection(locators = listOf(locator(1))))))
        val session = (openReaderBookSearch("dream") { cursor } as ReaderBookSearchOpen.Ready).session
        session.close()
        session.close()
        assertEquals(1, cursor.closeCalls)
        assertEquals(ReaderBookSearchPage.Closed, session.nextPage())
        assertEquals(0, cursor.nextCalls)
    }

    @Test
    fun cursorFailureIsExplicitAndClosesResources() = runTest {
        val cursor = object : SearchIterator {
            var closed = 0
            override suspend fun next(): SearchTry<LocatorCollection?> =
                throw IllegalStateException("broken EPUB entry")
            override fun close() { closed++ }
        }
        val session = (openReaderBookSearch("book") { cursor } as ReaderBookSearchOpen.Ready).session
        assertEquals(ReaderBookSearchPage.Failed, session.nextPage())
        assertEquals(1, cursor.closed)
        assertEquals(ReaderBookSearchPage.Closed, session.nextPage())
    }

    @Test
    fun cancelledReadingSearchClosesCursorAndDoesNotProduceStaleHits() = runTest {
        val cursor = object : SearchIterator {
            var closed = 0
            override suspend fun next(): SearchTry<LocatorCollection?> =
                suspendCancellableCoroutine { }
            override fun close() { closed++ }
        }
        val session = (openReaderBookSearch("word") { cursor } as ReaderBookSearchOpen.Ready).session
        val pending = async { session.nextPage() }
        runCurrent()
        pending.cancelAndJoin()
        assertEquals(1, cursor.closed)
        assertEquals(ReaderBookSearchPage.Closed, session.nextPage())
    }
}
