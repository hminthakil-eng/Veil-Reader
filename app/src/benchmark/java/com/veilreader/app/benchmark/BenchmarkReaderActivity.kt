package com.veilreader.app.benchmark

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.ExperimentalReadiumApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.ReadiumEngine
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.diagnostics.ReaderJankMonitor
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
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

/**
 * Performance-only entry point that opens a deterministic local EPUB in the real Readium
 * reader. It is compiled and declared only by the explicit benchmark build; normal debug,
 * release, and Baseline Profile target variants never include this test-only entry point.
 */
@OptIn(ExperimentalReadiumApi::class)
class BenchmarkReaderActivity : FragmentActivity() {
    private lateinit var readerJankMonitor: ReaderJankMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readerJankMonitor = ReaderJankMonitor(window)
        readerJankMonitor.install()
        MaterialPageEngineRollout.setDebugOverride(true)

        val benchmarkReaderSessionId = "benchmark-reader-session-${UUID.randomUUID()}"

        // Test-only observation of the actual Readium locator, independent of gesture delivery.
        // No navigation is driven by this probe. Normal app variants never include it.
        lifecycleScope.launch {
            var navigator: Navigator? = null
            while (navigator == null) {
                navigator = findNavigator(supportFragmentManager)
                if (navigator == null) delay(50)
            }
            navigator.currentLocator.collect { locator ->
                val progression = locator.locations.progression ?: return@collect
                window.decorView.contentDescription = "benchmark-reader-locator:${locator.href}|$progression"
            }
        }

        lifecycleScope.launch {
            val context = applicationContext
            val library = LocalLibraryRepository(context)
            val engine = ReadiumEngine(context)
            val game = GameRepository(context)

            val opened = withContext(Dispatchers.IO) {
                val inspected = engine.inspectAndCreateBook(Uri.fromFile(ensureFixture())).getOrThrow()
                val committed = library.addImportedBook(inspected).book
                library.flushWrites()

                // Macrobenchmark repeats the same deterministic fixture inside one app data
                // directory. The real Reader correctly persists progress between Activity launches,
                // which would otherwise make later benchmark iterations resume near the end of the
                // book and produce zero-frame GfxInfo samples. Keep durable app behavior untouched:
                // only the benchmark-owned OpenedPublication starts from a transient clean locator.
                val benchmarkBook = committed.copy(
                    progress = 0f,
                    pagesRead = 0,
                    locatorJson = null,
                    finished = false
                )
                engine.openBook(benchmarkBook).getOrThrow()
            }

            setContent {
                val readerAppearance = remember {
                    mutableStateOf(ReaderAppearance(pageTurnStyle = PageTurnStyle.PAPER))
                }
                VeilTheme {
                    ReaderScreen(
                        opened = opened,
                        readerSessionInstanceId = benchmarkReaderSessionId,
                        library = library,
                        game = game,
                        readerAppearance = readerAppearance.value,
                        onReaderAppearanceChange = { readerAppearance.value = it },
                        onClose = ::finish
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::readerJankMonitor.isInitialized) readerJankMonitor.resume()
    }

    override fun onPause() {
        if (::readerJankMonitor.isInitialized) readerJankMonitor.pause()
        super.onPause()
    }

    private fun findNavigator(manager: FragmentManager): Navigator? {
        for (fragment in manager.fragments) {
            (fragment as? Navigator)?.let { return it }
            findNavigator(fragment.childFragmentManager)?.let { return it }
        }
        return null
    }

    private fun ensureFixture(): File {
        val svgCover = intent.getBooleanExtra("benchmark_svg_cover", false)
        val target = File(cacheDir, if (svgCover) "veil-reader-benchmark-svg-cover-v1.epub" else "veil-reader-benchmark-cover-v3.epub")
        if (target.isFile && target.length() > 0L) return target

        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            zip.writeEntry("mimetype", "application/epub+zip", stored = true)
            zip.writeEntry(
                "META-INF/container.xml",
                """<?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>""".trimIndent()
            )

            val chapters = (1..12).joinToString("") { index ->
                """<item id="c$index" href="c$index.xhtml" media-type="application/xhtml+xml"/>"""
            }
            val spine = (1..12).joinToString("") { index ->
                """<itemref idref="c$index"/>"""
            }
            val navEntries = (1..12).joinToString("") { index ->
                """<li><a href="c$index.xhtml">Chapter $index</a></li>"""
            }

            zip.writeEntry(
                "OEBPS/content.opf",
                """<?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:identifier id="id">veil-reader-benchmark</dc:identifier>
                    <dc:title>Veil Benchmark Book</dc:title>
                    <dc:creator>Eyad Studio</dc:creator>
                    <dc:language>en</dc:language>
                    <meta property="dcterms:modified">2026-09-18T00:00:00Z</meta>
                  </metadata>
                  <manifest>
                    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                    <item id="cover" href="cover.xhtml" media-type="application/xhtml+xml"/>
                    <item id="coverImage" href="cover.svg" media-type="image/svg+xml" properties="cover-image"/>
                    $chapters
                  </manifest>
                  <spine><itemref idref="cover"/>$spine</spine>
                </package>""".trimIndent()
            )

            zip.writeEntry(
                "OEBPS/nav.xhtml",
                """<?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                  <head><title>Contents</title></head>
                  <body><nav epub:type="toc"><ol>$navEntries</ol></nav></body>
                </html>""".trimIndent()
            )

            zip.writeEntry(
                "OEBPS/cover.xhtml",
                if (svgCover) {
                    // Same container geometry as the reported book, using only our test artwork.
                    """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>SVG cover</title></head>
                    <body style="margin:0;background:#000"><div>
                    <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
                    width="100%" height="100%" viewBox="0 0 600 800" preserveAspectRatio="xMidYMid">
                    <image width="600" height="800" xlink:href="cover.svg"/></svg>
                    </div></body></html>"""
                } else {
                    """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>Cover</title></head>
                    <body style="margin:0;background:#000"><img src="cover.svg" alt="Veil test cover"
                    style="display:block;width:90%;height:auto;margin:5% auto"/></body></html>"""
                }
            )
            zip.writeEntry(
                "OEBPS/cover.svg",
                """<svg xmlns="http://www.w3.org/2000/svg" width="600" height="800" viewBox="0 0 600 800">
                <rect width="600" height="800" fill="#222"/>
                <rect x="20" y="20" width="560" height="760" fill="none" stroke="#aaa"/>
                <text x="300" y="380" text-anchor="middle" fill="#eee" font-size="40">Veil test cover</text>
                </svg>"""
            )

            repeat(12) { zeroBased ->
                val index = zeroBased + 1
                val paragraph = buildString {
                    repeat(40) {
                        append("Veil Reader performance fixture. Turn the page smoothly through the archive and continue reading. ")
                    }
                }
                zip.writeEntry(
                    "OEBPS/c$index.xhtml",
                    """<?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                      <head><title>Chapter $index</title></head>
                      <body><h1>Chapter $index</h1><p>$paragraph</p></body>
                    </html>""".trimIndent()
                )
            }
        }
        return target
    }
}

private fun ZipOutputStream.writeEntry(path: String, text: String, stored: Boolean = false) {
    val bytes = text.toByteArray(Charsets.UTF_8)
    val entry = ZipEntry(path)
    if (stored) {
        val crc = CRC32().apply { update(bytes) }
        entry.method = ZipEntry.STORED
        entry.size = bytes.size.toLong()
        entry.compressedSize = bytes.size.toLong()
        entry.crc = crc.value
    }
    putNextEntry(entry)
    write(bytes)
    closeEntry()
}
