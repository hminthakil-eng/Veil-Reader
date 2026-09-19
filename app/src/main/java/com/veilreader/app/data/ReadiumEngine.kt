package com.veilreader.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Size
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.allAreHtml
import org.readium.r2.shared.publication.services.coverFitting
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
        var cachedCoverPath: String? = null
        try {
            val localFile = requireNotNull(localUri.path).let(::File)
            val fingerprint = sha256(localFile)
            val publication = openPublication(localUri, allowUserInteraction = false)
            try {
                val format = formatOf(publication)
                val metadata = publication.metadata
                val author = metadata.authors
                    .firstOrNull { it.name.isNotBlank() }
                    ?.name
                    ?: "Unknown author"
                val title = metadata.title
                    ?.takeIf { it.isNotBlank() }
                    ?: displayName(uri)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    ?: "Untitled"
                val series = metadata.belongsToSeries.firstOrNull()
                val bookId = UUID.randomUUID().toString()
                cachedCoverPath = cacheCover(publication, bookId)

                Book(
                    id = bookId,
                    title = title,
                    author = author,
                    progress = 0f,
                    totalPages = metadata.numberOfPages ?: 0,
                    format = format,
                    sourceUri = localUri.toString(),
                    mediaType = when (format) {
                        BookFormat.EPUB -> "application/epub+zip"
                        BookFormat.PDF -> "application/pdf"
                        else -> "application/octet-stream"
                    },
                    coverCachePath = cachedCoverPath,
                    contentFingerprint = fingerprint,
                    seriesName = series?.name?.trim()?.takeIf { it.isNotEmpty() },
                    seriesIndex = series?.position?.takeIf { it.isFinite() },
                    language = metadata.languages.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                )
            } finally {
                publication.close()
            }
        } catch (t: Throwable) {
            localUri.path?.let(::File)?.takeIf { it.exists() }?.delete()
            cachedCoverPath?.takeIf { it.isNotBlank() }?.let(::File)?.delete()
            throw t
        }
    }

    suspend fun openBook(
        book: Book,
        persistedLocatorJsons: Set<String> = emptySet()
    ): Result<OpenedPublication> = runCatching {
        val uri = requireNotNull(book.sourceUri) { "This is a demo book. Import an EPUB or PDF first." }
            .let(Uri::parse)
        val publication = openPublication(uri, allowUserInteraction = true)
        try {
            val format = formatOf(publication)
            val allLocatorJsons = buildSet {
                book.locatorJson?.let(::add)
                addAll(persistedLocatorJsons)
            }
            val locatorMigrations = if (format == BookFormat.PDF) {
                buildMap {
                    for (json in allLocatorJsons) {
                        val locator = runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
                            ?: continue
                        val migrated = publication.migrateVeilLegacyPdfiumLocator(locator)
                        val migratedJson = migrated.toJSON().toString()
                        if (migratedJson != json) put(json, migratedJson)
                    }
                }
            } else {
                emptyMap()
            }
            val initialLocator = book.locatorJson?.let { json ->
                val persistedJson = locatorMigrations[json] ?: json
                runCatching { Locator.fromJSON(JSONObject(persistedJson)) }.getOrNull()
            }

            OpenedPublication(
                book = book,
                publication = publication,
                format = format,
                initialLocator = initialLocator,
                locatorMigrations = locatorMigrations
            )
        } catch (error: Throwable) {
            publication.close()
            throw error
        }
    }

    /**
     * Regenerates a derived cover thumbnail for an existing imported publication.
     * The empty string is a successful terminal result meaning Readium found no usable cover.
     */
    suspend fun extractAndCacheCover(book: Book): Result<String> = runCatching {
        if (!book.isImported) return@runCatching ""
        book.coverCachePath?.let { cached ->
            if (cached.isBlank() || File(cached).isFile) return@runCatching cached
        }

        val uri = requireNotNull(book.sourceUri).let(Uri::parse)
        val publication = openPublication(uri, allowUserInteraction = false)
        try {
            cacheCover(publication, book.id)
        } finally {
            publication.close()
        }
    }

    /** Computes SHA-256 for an existing app-private publication without reopening Readium. */
    suspend fun computeContentFingerprint(book: Book): Result<String> = runCatching {
        if (!book.isImported) return@runCatching ""
        val uri = requireNotNull(book.sourceUri).let(Uri::parse)
        require(uri.scheme == "file") { "Only app-private publication files can be fingerprinted." }
        val file = File(requireNotNull(uri.path)).canonicalFile
        val publicationsRoot = File(appContext.filesDir, "publications").canonicalFile
        require(file.toPath().startsWith(publicationsRoot.toPath()) && file.isFile) {
            "The imported publication file is missing."
        }
        sha256(file)
    }

    private suspend fun cacheCover(publication: Publication, bookId: String): String {
        val bitmap = runCatching { publication.coverFitting(COVER_MAX_SIZE) }.getOrNull()
            ?: return ""
        val coversDir = File(appContext.filesDir, "covers").apply { mkdirs() }
        val safeName = UUID.nameUUIDFromBytes(
            "veil-cover:$bookId".toByteArray(StandardCharsets.UTF_8)
        ).toString()
        val target = File(coversDir, "$safeName.jpg")
        val temporary = File(coversDir, "$safeName.tmp")

        return withContext(Dispatchers.IO) {
            try {
                temporary.outputStream().buffered().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_JPEG_QUALITY, output)) {
                        "Android could not encode the publication cover."
                    }
                }
                check(temporary.length() > 0L) { "The generated cover thumbnail is empty." }
                if (target.exists() && !target.delete()) {
                    error("Could not replace the existing cover thumbnail.")
                }
                check(temporary.renameTo(target)) { "Could not install the cover thumbnail." }
                target.absolutePath
            } catch (_: Throwable) {
                temporary.delete()
                ""
            }
        }
    }

    private suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
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

    companion object {
        private val COVER_MAX_SIZE = Size(600, 900)
        private const val COVER_JPEG_QUALITY = 88
    }
}

data class OpenedPublication(
    val book: Book,
    val publication: Publication,
    val format: BookFormat,
    val initialLocator: Locator?,
    val locatorMigrations: Map<String, String> = emptyMap()
) : AutoCloseable {
    override fun close() = publication.close()
}

class ReaderException(message: String) : IllegalStateException(message)
