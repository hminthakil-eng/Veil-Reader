package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.TextContentTokenizer
import org.readium.r2.shared.publication.services.content.content
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.tokenizer.TextUnit

internal data class ReaderTtsUtterance(
    val text: String,
    val languageTag: String?,
    val locator: Locator
)

internal interface ReaderTtsContent {
    suspend fun next(): ReaderTtsUtterance?
}

/**
 * Real Readium content/locators only.
 *
 * Readium's ICU-aware sentence tokenizer is reused before the Android-length safety split. This
 * preserves authored language/locator semantics and gives native TTS sentence-sized utterances
 * instead of hand-rolled paragraph chunks.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class ReadiumTtsContent private constructor(
    private val iterator: Content.Iterator,
    defaultLanguage: Language?,
    private val maximumLength: Int
) : ReaderTtsContent {
    private val sentenceTokenizer = TextContentTokenizer(
        language = defaultLanguage,
        unit = TextUnit.Sentence,
        overrideContentLanguage = false
    )

    private var element: Content.TextElement? = null
    private var segmentIndex = 0
    private var characterOffset = 0

    override suspend fun next(): ReaderTtsUtterance? = withContext(Dispatchers.IO) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val current = element
            val segment = current?.segments?.getOrNull(segmentIndex)

            if (segment != null && characterOffset < segment.text.length) {
                val end = ttsChunkEnd(
                    text = segment.text,
                    start = characterOffset,
                    maximum = maximumLength
                )
                val text = segment.text.substring(characterOffset, end)
                characterOffset = end
                if (text.isNotBlank()) {
                    return@withContext ReaderTtsUtterance(
                        text = text,
                        languageTag = (segment.language ?: current.language)?.code,
                        locator = segment.locator
                    )
                }
            } else if (segment != null) {
                segmentIndex += 1
                characterOffset = 0
            } else {
                element = null
                val next = iterator.nextOrNull() ?: return@withContext null
                if (next is Content.TextElement) {
                    val tokenized = sentenceTokenizer
                        .tokenize(next)
                        .filterIsInstance<Content.TextElement>()
                        .firstOrNull()
                    if (tokenized != null) {
                        element = tokenized
                        segmentIndex = 0
                        characterOffset = 0
                    }
                }
            }
        }

        @Suppress("UNREACHABLE_CODE")
        null
    }

    companion object {
        fun isAvailable(publication: Publication): Boolean =
            !publication.isRestricted &&
                !publication.isProtected &&
                publication.content() != null

        fun create(
            publication: Publication,
            start: Locator,
            maximumLength: Int = 1000
        ): ReaderTtsContent? {
            if (!isAvailable(publication)) return null

            // Readium ignores progression for a mid-resource content start. Current-position
            // controls must request VisualNavigator.firstVisibleElementLocator(), never silently
            // feed an ordinary paginated currentLocator and restart the chapter.
            val selector =
                start.locations.otherLocations["cssSelector"] as? String
            val progression = start.locations.progression
            if (
                progression != null &&
                (!progression.isFinite() || progression !in 0.0..1.0)
            ) {
                return null
            }
            if (
                progression != null &&
                progression > 0.0 &&
                progression < 1.0 &&
                selector.isNullOrBlank()
            ) {
                return null
            }

            val defaultLanguage = publication.metadata.languages
                .firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let(::Language)

            return publication.content(start)?.iterator()?.let {
                ReadiumTtsContent(
                    iterator = it,
                    defaultLanguage = defaultLanguage,
                    maximumLength = maximumLength.coerceIn(2, 4000)
                )
            }
        }
    }
}
