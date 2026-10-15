package com.veilreader.app.data

import android.net.Uri
import com.veilreader.app.domain.Book
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReadiumEngineCancellationTest {
    @Test
    fun `cancelled document stream aborts import instead of returning an ordinary failure`() = runBlocking<Unit> {
        val cancellation = CancellationException("document selection cancelled")
        val uri = registerStream("cancelled.epub", failingStream(cancellation))
        val before = importFiles()
        try {
            ReadiumEngine(RuntimeEnvironment.getApplication()).inspectAndCreateBook(uri)
            fail("Cancellation must propagate to the owning import coroutine")
        } catch (actual: CancellationException) {
            assertEquals("document selection cancelled", actual.message)
        }
        assertTrue(before == importFiles())
    }

    @Test
    fun `cancelled reader request propagates cancellation instead of returning a result`() = runBlocking<Unit> {
        val engine = ReadiumEngine(RuntimeEnvironment.getApplication())
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext().cancel()
            try {
                engine.openBook(Book(id = "cancelled", title = "Cancelled", author = "Test"))
                fail("A cancelled open request must not run validation or deliver a Result")
            } catch (_: CancellationException) {
                // The cancelled owner remains cancelled; this is not a corrupt-book error.
            }
        }
        request.join()
        assertTrue(request.isCancelled)
    }

    @Test
    fun `unreadable document returns failure without creating a book or leaving a partial file`() = runBlocking<Unit> {
        val missing = FileNotFoundException("source unavailable")
        val uri = registerStream("missing.epub", failingStream(missing))
        val before = importFiles()
        val result = ReadiumEngine(RuntimeEnvironment.getApplication()).inspectAndCreateBook(uri)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileNotFoundException)
        assertEquals("source unavailable", result.exceptionOrNull()?.message)
        assertTrue(before == importFiles())
    }

    @Test
    fun `cancellation at the end of copying removes the uncommitted publication`() = runBlocking<Unit> {
        val before = importFiles()
        lateinit var owner: Job
        val stream = object : ByteArrayInputStream("source bytes".toByteArray()) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                val count = super.read(buffer, offset, length)
                if (count < 0) owner.cancel()
                return count
            }
        }
        val uri = registerStream("cancel-at-eof.epub", stream)
        val engine = ReadiumEngine(RuntimeEnvironment.getApplication())
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            owner = requireNotNull(currentCoroutineContext()[Job])
            try {
                engine.inspectAndCreateBook(uri)
                fail("Cancelled copy must not deliver a book or ordinary failure")
            } catch (_: CancellationException) {
                // Cancellation arrives after the last read, before dispatcher handoff completes.
            }
        }
        request.join()
        assertTrue(request.isCancelled)
        assertTrue("Uncommitted files must be cleaned on dispatcher handoff cancellation", before == importFiles())
    }

    private fun failingStream(failure: Exception): InputStream = object : InputStream() {
        override fun read(): Int = throw failure
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = throw failure
    }

    private fun registerStream(name: String, stream: InputStream): Uri {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.fromFile(File(context.cacheDir, name))
        shadowOf(context.contentResolver).registerInputStream(uri, stream)
        return uri
    }

    private fun importFiles(): Set<String> =
        File(RuntimeEnvironment.getApplication().filesDir, "publications")
            .listFiles().orEmpty().map { it.name }.toSet()
}
