package com.veilreader.app.manga.reader.screen

import androidx.lifecycle.viewmodel.testing.viewModelScenario
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaProgressStore
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.presentation.MangaChapterAvailability
import com.veilreader.app.manga.reader.presentation.MangaChapterPresentationLoader
import com.veilreader.app.manga.reader.presentation.MangaChapterRoute
import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationState
import com.veilreader.app.manga.reader.presentation.MangaReadyPresentation
import com.veilreader.app.manga.reader.ui.MangaReaderUiIntent
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MangaReaderDurableCloseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun successfulClosePersistsLatestProgressBeforeLockingFurtherInput() =
        runTest(mainDispatcherRule.dispatcher) {
            val store = RecordingProgressStore()
            val scenario = scenario(store)

            scenario.use {
                val viewModel = it.viewModel
                advanceUntilIdle()
                store.lastSaved = null

                assertTrue(viewModel.persistForClose())
                assertNotNull(store.lastSaved)
                val persisted = requireNotNull(store.lastSaved)
                assertEquals(MANGA_ID, persisted.mangaId)
                assertEquals(0, persisted.pageIndex)
                assertEquals(PAGE_COUNT, persisted.pageCount)

                val modeAtClose = viewModel.state.value.readerUi.reader.mode
                viewModel.onIntent(MangaReaderUiIntent.SetMode(MangaReaderMode.WEBTOON))
                assertEquals(modeAtClose, viewModel.state.value.readerUi.reader.mode)
            }
        }

    @Test
    fun failedCloseKeepsReaderOpenAndCanRetryDurably() =
        runTest(mainDispatcherRule.dispatcher) {
            val store = RecordingProgressStore()
            val scenario = scenario(store)

            scenario.use {
                val viewModel = it.viewModel
                advanceUntilIdle()
                store.failWrites = true

                assertFalse(viewModel.persistForClose())
                assertEquals(
                    MangaReaderScreenMessage.ProgressSaveFailed,
                    viewModel.state.value.message
                )

                store.failWrites = false
                assertTrue(viewModel.persistForClose())
                assertNotNull(store.lastSaved)
            }
        }

    private fun scenario(store: RecordingProgressStore) =
        viewModelScenario<MangaReaderScreenViewModel>(
            factory = MangaReaderScreenViewModel.factory(
                session = testSession(),
                loader = testLoader(),
                progressStore = store
            )
        )

    private fun testSession(): MangaReaderSession {
        val sourceId = SourceId("local.test")
        val chapter = SourceChapter(
            sourceId = sourceId,
            mangaKey = "work",
            chapterKey = "chapter-1",
            title = "Chapter 1",
            number = 1.0,
            languageTag = "en"
        )
        val readerChapter = MangaReaderChapterRef(
            mangaId = MANGA_ID,
            anchor = MangaChapterAnchor(
                number = 1.0,
                languageTag = "en",
                normalizedTitle = "Chapter 1",
                providerChapterKeyHint = chapter.chapterKey
            )
        )
        return MangaReaderSession(
            entriesInReadingOrder = listOf(
                MangaReaderChapterEntry(
                    route = MangaChapterRoute(
                        readerChapter = readerChapter,
                        sourceChapter = chapter
                    ),
                    provider = null
                )
            )
        )
    }

    private fun testLoader(): MangaChapterPresentationLoader =
        MangaChapterPresentationLoader { request ->
            MangaReaderPresentationState.Ready(
                MangaReadyPresentation(
                    chapter = request.chapter,
                    pages = List(PAGE_COUNT) { index ->
                        MangaPageAsset.Local(
                            index = index,
                            relativePath = "page-$index.jpg",
                            byteSize = 1L
                        )
                    },
                    availability = MangaChapterAvailability.COMPLETE_OFFLINE,
                    complete = true
                )
            )
        }

    private class RecordingProgressStore : MangaProgressStore {
        var lastSaved: MangaReadingProgress? = null
        var failWrites: Boolean = false

        override suspend fun load(mangaId: CanonicalMangaId): MangaReadingProgress? = null

        override suspend fun save(progress: MangaReadingProgress) {
            if (failWrites) error("simulated durable write failure")
            lastSaved = progress
        }

        override suspend fun delete(mangaId: CanonicalMangaId) = Unit
    }

    private companion object {
        val MANGA_ID = CanonicalMangaId("work")
        const val PAGE_COUNT = 5
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
