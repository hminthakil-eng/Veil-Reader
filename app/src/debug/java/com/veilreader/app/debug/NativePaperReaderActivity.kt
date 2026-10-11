package com.veilreader.app.debug

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.ReadiumEngine
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.ui.screens.ReaderScreen
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Isolated test entry using the production importer, Readium and ReaderScreen. */
class NativePaperReaderActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val library = LocalLibraryRepository(applicationContext)
            val engine = ReadiumEngine(applicationContext)
            val game = GameRepository(applicationContext)
            val opened = withContext(Dispatchers.IO) {
                val book = engine.inspectAndCreateBook(Uri.fromFile(fixture())).getOrThrow()
                val imported = library.addImportedBook(book).book
                library.flushWrites()
                engine.openBook(imported.copy(locatorJson = null, progress = 0f, pagesRead = 0)).getOrThrow()
            }
            val session = "native-paper-device-${UUID.randomUUID()}"
            setContent {
                val appearance = remember { mutableStateOf(ReaderAppearance(pageTurnStyle = PageTurnStyle.PAPER)) }
                VeilTheme {
                    ReaderScreen(opened = opened, readerSessionInstanceId = session,
                        library = library, game = game, readerAppearance = appearance.value,
                        onReaderAppearanceChange = { appearance.value = it }, onClose = ::finish)
                }
            }
        }
    }

    private fun fixture(): File {
        val target = File(cacheDir, "native-paper-real-reader.epub")
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            fun entry(path: String, text: String, stored: Boolean = false) {
                val bytes = text.toByteArray(Charsets.UTF_8)
                val entry = ZipEntry(path)
                if (stored) {
                    entry.method = ZipEntry.STORED
                    entry.size = bytes.size.toLong()
                    entry.compressedSize = entry.size
                    entry.crc = CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(entry); zip.write(bytes); zip.closeEntry()
            }
            entry("mimetype", "application/epub+zip", true)
            entry("META-INF/container.xml", """<?xml version="1.0"?><container xmlns="urn:oasis:names:tc:opendocument:xmlns:container" version="1.0"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            entry("OEBPS/content.opf", """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">veil-native-paper-qa</dc:identifier><dc:title>Native Paper Real Book</dc:title><dc:language>en</dc:language><meta property="dcterms:modified">2026-10-11T00:00:00Z</meta></metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/><item id="c1" href="c1.xhtml" media-type="application/xhtml+xml"/><item id="c2" href="c2.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="c1"/><itemref idref="c2"/></spine></package>""")
            entry("OEBPS/nav.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Contents</title></head><body><nav epub:type="toc"><ol><li><a href="c1.xhtml">Chapter One</a></li><li><a href="c2.xhtml">Chapter Two</a></li></ol></nav></body></html>""")
            for (chapter in 1..2) {
                val paragraphs = (1..90).joinToString("") { index ->
                    "<p>Chapter $chapter, paragraph $index. The archive opens quietly. Ink belongs to this real EPUB page; a finger raises the sheet and its shadow follows the fold. Release commits one reading location, and reversing the pull restores the original page.</p>"
                }
                entry("OEBPS/c$chapter.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>Chapter $chapter</title></head><body><h1>Chapter $chapter</h1>$paragraphs</body></html>""")
            }
        }
        return target
    }
}
