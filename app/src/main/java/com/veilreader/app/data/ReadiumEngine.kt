package com.veilreader.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.allAreHtml
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Owns the Readium parsing stack used by imports and reader sessions. */
@OptIn(ExperimentalReadiumApi::class)
class ReadiumEngine(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(appContext.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = appContext,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = PdfiumDocumentFactory(appContext)
        )
    )

    /**
     * Copies the selected publication into app-private storage before inspecting it.
     * This makes imports survive process restarts even when the source app only granted a
     * temporary content-URI permission (for example, opening an EPUB from a messaging app).
     */
    suspend fun inspectAndCreateBook(uri: Uri): Result<Book> = runCatching {
        val localUri = materializeImport(uri)
        try {
            val publication = openPublication(localUri, allowUserInteraction = false)
            try {
                val format = formatOf(publication)
                val author = publication.metadata.authors
                    .firstOrNull { it.name.isNotBlank() }
                    ?.name
                    ?: "Unknown author"
                val title = publication.metadata.title
                    ?.takeIf { it.isNotBlank() }
                    ?: displayName(uri)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    ?: "Untitled"

                Book(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    author = author,
                    progress = 0f,
                    format = format,
                    sourceUri = localUri.toString(),
                    mediaType = when (format) {
                        BookFormat.EPUB -> "application/epub+zip"
                        BookFormat.PDF -> "application/pdf"
                        else -> "application/octet-stream"
                    }
                )
            } finally {
                publication.close()
            }
        } catch (t: Throwable) {
            localUri.path?.let(::File)?.takeIf { it.exists() }?.delete()
            throw t
        }
    }

    suspend fun openBook(book: Book): Result<OpenedPublication> = runCatching {
        val uri = requireNotNull(book.sourceUri) { "This is a demo book. Import an EPUB or PDF first." }
            .let(Uri::parse)
        val publication = openPublication(uri, allowUserInteraction = true)
        val format = formatOf(publication)
        val initialLocator = book.locatorJson
            ?.let { json -> runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull() }

        OpenedPublication(
            book = book,
            publication = publication,
            format = format,
            initialLocator = initialLocator
        )
    }

    private suspend fun materializeImport(source: Uri): Uri = withContext(Dispatchers.IO) {
        val importsDir = File(appContext.filesDir, "publications").apply { mkdirs() }
        val extension = bestExtension(source)
        val target = File(importsDir, "${UUID.randomUUID()}$extension")

        try {
            appContext.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw ReaderException("Android could not read the selected file.")
            if (target.length() == 0L) throw ReaderException("The selected publication is empty.")
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
        Uri.fromFile(target)
    }

    private fun bestExtension(uri: Uri): String {
        val byName = displayName(uri)
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
        if (byName != null) return ".$byName"

        return when (appContext.contentResolver.getType(uri)?.lowercase()) {
            "application/epub+zip" -> ".epub"
            "application/pdf" -> ".pdf"
            else -> ".book"
        }
    }

    private fun displayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
        }.getOrNull()
    }

    private suspend fun openPublication(uri: Uri, allowUserInteraction: Boolean): Publication {
        val url = requireNotNull(uri.toAbsoluteUrl()) { "Unsupported document URI." }
        val asset = assetRetriever.retrieve(url).getOrElse {
            throw ReaderException("Cannot access this file: $it")
        }
        return publicationOpener.open(asset, allowUserInteraction = allowUserInteraction).getOrElse {
            throw ReaderException("Cannot open this publication: $it")
        }.also { publication ->
            if (publication.isRestricted) {
                publication.close()
                throw ReaderException(publication.protectionError?.toString() ?: "This publication is protected and locked.")
            }
        }
    }

    private fun formatOf(publication: Publication): BookFormat = when {
        publication.conformsTo(Publication.Profile.EPUB) || publication.readingOrder.allAreHtml -> BookFormat.EPUB
        publication.conformsTo(Publication.Profile.PDF) -> BookFormat.PDF
        else -> throw ReaderException("Veil Reader currently supports EPUB and PDF files.")
    }
}

data class OpenedPublication(
    val book: Book,
    val publication: Publication,
    val format: BookFormat,
    val initialLocator: Locator?
) : AutoCloseable {
    override fun close() = publication.close()
}

class ReaderException(message: String) : IllegalStateException(message)
