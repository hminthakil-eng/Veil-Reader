package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shockwave.pdfium.PdfiumCore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/** Real native fixture checks; compilation is not represented as device execution. */
@RunWith(AndroidJUnit4::class)
class ReaderPdfNativeFixtureTest {
    @Test
    fun embeddedAnnotationsRenderOnlyWhenRequestedAndLinksUseDocumentIndices() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bytes = instrumentation.context.assets.open("pdf/veil-links-annotations.pdf")
            .use { it.readBytes() }
        val core = PdfiumCore(instrumentation.targetContext)
        val document = core.newDocument(bytes)
        val plain = Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888)
        val annotated = Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888)
        try {
            assertEquals(3, core.getPageCount(document))
            core.openPage(document, 0)
            val link = core.getPageLinks(document, 0).single()
            assertEquals(2, link.destPageIdx)
            val origin = Locator(
                href = requireNotNull(Url("fixture.pdf")), mediaType = MediaType.PDF,
                locations = Locator.Locations(fragments = listOf("page=1"), position = 1)
            )
            val target = requireNotNull(pdfInternalLinkLocator(origin, link.destPageIdx, 3))
            assertEquals(3, pdfPageNumber(target))
            core.renderPageBitmap(document, plain, 0, 0, 0, 612, 792, false)
            core.renderPageBitmap(document, annotated, 0, 0, 0, 612, 792, true)
            assertNotEquals("Embedded appearance did not render", plain.getPixel(200, 82), annotated.getPixel(200, 82))
            assertEquals("Unannotated page content changed", plain.getPixel(400, 400), annotated.getPixel(400, 400))
        } finally {
            plain.recycle()
            annotated.recycle()
            core.closeDocument(document)
        }
    }
}
