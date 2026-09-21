package com.veilreader.app.ui.reader

import android.content.Context
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal enum class ReaderFixtureDirection(val opfValue: String, val language: String) {
    LTR("ltr", "en"),
    RTL("rtl", "fa")
}

internal object ReaderQaFixtureFactory {
    fun createEpub(context: Context, direction: ReaderFixtureDirection): File {
        val slug = direction.name.lowercase()
        val target = File(context.cacheDir, "veil-reader-qa-$slug.epub")
        if (target.exists()) target.delete()

        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            writeStored(zip, "mimetype", "application/epub+zip")
            writeText(
                zip,
                "META-INF/container.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="EPUB/package.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """.trimIndent()
            )
            writeText(zip, "EPUB/package.opf", packageDocument(direction))
            writeText(zip, "EPUB/nav.xhtml", navigationDocument(direction))
            repeat(3) { chapterIndex ->
                writeText(
                    zip,
                    "EPUB/chapter${chapterIndex + 1}.xhtml",
                    chapterDocument(direction, chapterIndex + 1)
                )
            }
        }

        check(target.isFile && target.length() > 0L) { "Could not create deterministic EPUB fixture." }
        return target
    }

    private fun packageDocument(direction: ReaderFixtureDirection): String {
        val slug = direction.name.lowercase()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf"
                     version="3.0"
                     unique-identifier="book-id"
                     xml:lang="${direction.language}">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:identifier id="book-id">urn:veil-reader:qa:$slug</dc:identifier>
                <dc:title>Veil QA ${direction.name}</dc:title>
                <dc:creator>Eyad Studio QA</dc:creator>
                <dc:language>${direction.language}</dc:language>
                <meta property="dcterms:modified">2026-09-21T00:00:00Z</meta>
              </metadata>
              <manifest>
                <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                <item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                <item id="c2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
                <item id="c3" href="chapter3.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine page-progression-direction="${direction.opfValue}">
                <itemref idref="c1"/>
                <itemref idref="c2"/>
                <itemref idref="c3"/>
              </spine>
            </package>
        """.trimIndent()
    }

    private fun navigationDocument(direction: ReaderFixtureDirection): String =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE html>
        <html xmlns="http://www.w3.org/1999/xhtml"
              xmlns:epub="http://www.idpf.org/2007/ops"
              lang="${direction.language}"
              dir="${direction.opfValue}">
          <head><title>Contents</title></head>
          <body>
            <nav epub:type="toc">
              <ol>
                <li><a href="chapter1.xhtml">Chapter 1</a></li>
                <li><a href="chapter2.xhtml">Chapter 2</a></li>
                <li><a href="chapter3.xhtml">Chapter 3</a></li>
              </ol>
            </nav>
          </body>
        </html>
        """.trimIndent()

    private fun chapterDocument(direction: ReaderFixtureDirection, chapter: Int): String {
        val dir = direction.opfValue
        val language = direction.language
        val paragraphs = buildString {
            repeat(72) { index ->
                val marker = "QA-${direction.name}-C$chapter-P${index + 1}"
                val sentence = if (direction == ReaderFixtureDirection.RTL) {
                    "این یک متن ثابت برای آزمون کتاب‌خوان وییل است و جایگاه خواندن باید پس از چرخش و بازسازی حفظ شود."
                } else {
                    "This deterministic Veil Reader passage exists to verify pagination, recreation, orientation, and resume continuity."
                }
                append("<p id=\"p${index + 1}\"><strong>$marker</strong> $sentence</p>\n")
            }
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml" lang="$language" dir="$dir">
              <head>
                <title>Chapter $chapter</title>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <style>
                  html { direction: $dir; }
                  body {
                    font-family: serif;
                    line-height: 1.55;
                    margin: 5%;
                    text-align: start;
                  }
                  p { margin: 0 0 1.1em 0; }
                </style>
              </head>
              <body>
                <h1>Veil QA ${direction.name} — Chapter $chapter</h1>
                $paragraphs
              </body>
            </html>
        """.trimIndent()
    }

    private fun writeStored(zip: ZipOutputStream, path: String, text: String) {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val crc = CRC32().apply { update(bytes) }
        val entry = ZipEntry(path).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            this.crc = crc.value
        }
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun writeText(zip: ZipOutputStream, path: String, text: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(text.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}