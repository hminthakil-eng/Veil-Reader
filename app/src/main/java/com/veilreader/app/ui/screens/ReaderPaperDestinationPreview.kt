package com.veilreader.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.core.graphics.createBitmap
import com.veilreader.app.diagnostics.ReaderTrace
import com.veilreader.app.ui.reader.material.MaterialPageSnapshotCapture
import com.veilreader.app.ui.reader.material.ViewDrawImmediateMaterialPageSnapshotProvider
import com.veilreader.app.ui.reader.material.awaitMaterialPageSourceVisualReady
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import com.veilreader.app.ui.reader.material.nextMaterialPageSnapshotRevision
import kotlin.math.abs
import org.json.JSONTokener

internal data class PaperDestinationSnapshot(
    val bitmap: Bitmap,
    val sourceRevision: Long,
    val origin: Locator,
    val destination: Locator
)

/** Two expendable adjacent captures and one pinned gesture; never the reverse of the leaf. */
@Stable
internal class PaperDestinationPreviewState {
    private val prepared = mutableMapOf<PaperTurnDirection, PaperDestinationSnapshot>()
    var activeSnapshot: PaperDestinationSnapshot? by mutableStateOf(null)
        private set

    fun publish(direction: PaperTurnDirection, snapshot: PaperDestinationSnapshot) {
        prepared[direction] = snapshot
    }

    fun prepared(direction: PaperTurnDirection): PaperDestinationSnapshot? = prepared[direction]

    fun unpreparedDirections(origin: Locator, revision: Long, width: Int, height: Int): List<PaperTurnDirection> =
        PaperTurnDirection.entries.filter { direction ->
            val snapshot = prepared[direction]
            snapshot == null || revision <= 0L || width <= 0 || height <= 0 ||
                snapshot.sourceRevision != revision || snapshot.bitmap.isRecycled ||
                snapshot.bitmap.width != width || snapshot.bitmap.height != height ||
                !paperPreviewSamePage(snapshot.origin, origin)
        }

    fun begin(direction: PaperTurnDirection, revision: Long, width: Int, height: Int) {
        activeSnapshot = prepared[direction]?.takeIf {
            it.sourceRevision == revision && !it.bitmap.isRecycled &&
                it.bitmap.width == width && it.bitmap.height == height
        }
        ReaderTrace.event("paper_destination_selected",
            details = "revision=$revision direction=$direction prepared=${activeSnapshot != null}")
    }

    fun invalidate() { prepared.clear() }
    fun end() { activeSnapshot = null }
    fun release() { invalidate(); end() }
    // Published bitmaps are immutable by contract. Drop references instead of recycling
    // pixels which Compose/RenderThread may still be reading from a display list.
}

internal fun paperPreviewSamePage(first: Locator, second: Locator): Boolean =
    first.href == second.href && first.locations.progression != null &&
        second.locations.progression != null &&
        abs(first.locations.progression!! - second.locations.progression!!) < 0.00001

/** Separate Readium pagination, no input listeners, progress collector or publication ownership. */
@OptIn(ExperimentalReadiumApi::class)
internal class PaperPreviewNavigator {
    private data class RenderedOrigin(val bitmap: Bitmap, val sourceRevision: Long,
        val locator: Locator, val forwardDestination: Locator, val settings: EpubSettings)
    private var renderedOrigin: RenderedOrigin? = null
    fun releaseHistory() { renderedOrigin = null }
    var navigator: EpubNavigatorFragment? by mutableStateOf(null)
    private val pages = MutableStateFlow(0L)
    val paginationListener = object : EpubNavigatorFragment.PaginationListener {
        override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
            pages.value += 1L
        }
    }

    private suspend fun awaitSelectedDocument(preview: EpubNavigatorFragment, revision: Long): Boolean {
        // This API awaits the current Readium page's load. Match that document
        // URL to the visible child before asking for its visual-state callback.
        // A chapter locator alone can precede the ViewPager's actual layout.
        val value = preview.evaluateJavascript(
            "document.readyState === 'complete' && !!document.body && " +
                "(!document.fonts || document.fonts.status === 'loaded') ? document.location.href : null"
        ) ?: return false
        val url = JSONTokener(value).nextValue() as? String ?: return false
        return awaitMaterialPageSourceVisualReady(preview.publicationView, revision, 1_000L,
            expectedWebViewUrl = url)
    }

    suspend fun prepare(
        source: EpubNavigatorFragment,
        preferences: EpubPreferences,
        state: PaperCurlState,
        isCurrent: () -> Boolean
    ) {
        val preview = navigator ?: return
        val origin = source.currentLocator.value
        val settingsAtRequest = source.settings.value
        val revision = state.snapshotSourceRevision
        val width = source.publicationView.width
        val height = source.publicationView.height
        fun valid(): Boolean = isCurrent() && navigator === preview && !state.active &&
            revision == state.snapshotSourceRevision &&
            source.settings.value == settingsAtRequest &&
            paperPreviewSamePage(origin, source.currentLocator.value) &&
            width == source.publicationView.width && height == source.publicationView.height &&
            width > 0 && height > 0 && preview.view?.width == width && preview.view?.height == height

        try { withTimeoutOrNull(5_000L) {
            if (!valid()) return@withTimeoutOrNull
            // Stylesheets can finish loading before web fonts. A late font
            // changes pagination without changing the logical source locator.
            // Both navigators must have their actual font set before caching ink.
            val documentReady = "document.readyState === 'complete' && !!document.body && " +
                "(!document.fonts || document.fonts.status === 'loaded')"
            while (preview.evaluateJavascript(documentReady) != "true" ||
                source.evaluateJavascript(documentReady) != "true") {
                if (!valid()) return@withTimeoutOrNull
                delay(32L)
            }
            // Reversing/canceling a fold does not change its source. The effect
            // restarts when active becomes false, but its immutable adjacent ink
            // is still valid. Avoid Readium jumps, WebView paint and full-size
            // bitmap allocations for directions which are already prepared.
            val pending = state.destinationPreview.unpreparedDirections(origin, revision, width, height)
            if (pending.isEmpty()) {
                ReaderTrace.event("paper_destination_cache_hit", details = "revision=$revision")
                return@withTimeoutOrNull
            }
            // Readium signals settings before all visual work is complete. Also require a
            // pagination notification when settings change, then a WebView visual barrier.
            if (preview.settings.value != settingsAtRequest) {
                val before = pages.value
                preview.submitPreferences(preferences)
                preview.settings.first { it == settingsAtRequest }
                pages.first { it > before }
            }
            // After one forward commit, the previously captured source IS the
            // previous page. Reuse immutable ink instead of repaginating/capturing
            // it again. Extra invalidations (annotations/reflow) reject this cache.
            val previous = renderedOrigin?.takeIf {
                nextMaterialPageSnapshotRevision(it.sourceRevision) == revision &&
                    paperPreviewSamePage(it.forwardDestination, origin) && it.settings == settingsAtRequest &&
                    !it.bitmap.isRecycled && it.bitmap.width == width && it.bitmap.height == height
            }
            if (PaperTurnDirection.BACKWARD in pending && previous != null && valid()) {
                state.destinationPreview.publish(PaperTurnDirection.BACKWARD,
                    PaperDestinationSnapshot(previous.bitmap, revision, origin, previous.locator))
                ReaderTrace.event("paper_destination_reused_origin", details = "revision=$revision")
            }
            for (direction in pending) {
                if (direction == PaperTurnDirection.BACKWARD && previous != null) continue
                if (!valid()) return@withTimeoutOrNull
                if (!paperPreviewSamePage(preview.currentLocator.value, origin)) {
                    val before = pages.value
                    if (!preview.go(origin, animated = false)) return@withTimeoutOrNull
                    pages.first { it > before && paperPreviewSamePage(preview.currentLocator.value, origin) }
                }
                // A locator notification is not evidence of paint. Wait before navigation
                // so a newly loaded resource cannot use its provisional pagination.
                if (!awaitSelectedDocument(preview, revision)) {
                    return@withTimeoutOrNull
                }
                if (!valid()) return@withTimeoutOrNull
                val before = pages.value
                val accepted = when (direction) {
                    PaperTurnDirection.FORWARD -> preview.goForward(animated = false)
                    PaperTurnDirection.BACKWARD -> preview.goBackward(animated = false)
                }
                if (!accepted) continue
                // Readium can return true at the end of a reflowable book. Absence of an
                // actual changed locator is a boundary, never a duplicate-page preview.
                val reached = withTimeoutOrNull(1_200L) {
                    pages.first { it > before && !paperPreviewSamePage(preview.currentLocator.value, origin) }
                } != null
                if (!reached || !valid()) continue
                val destination = preview.currentLocator.value
                if (!awaitSelectedDocument(preview, revision)) continue
                if (!valid() || !paperPreviewSamePage(destination, preview.currentLocator.value)) continue
                val bitmap = try { createBitmap(width, height, Bitmap.Config.ARGB_8888) }
                catch (_: OutOfMemoryError) {
                    releaseHistory()
                    state.destinationPreview.invalidate()
                    ReaderTrace.event("paper_destination_memory_pressure", details = "revision=$revision")
                    return@withTimeoutOrNull
                }
                val result = ViewDrawImmediateMaterialPageSnapshotProvider.capture(
                    preview.publicationView, bitmap, revision
                )
                if (result is MaterialPageSnapshotCapture.Ready && valid()) {
                    state.destinationPreview.publish(direction,
                        PaperDestinationSnapshot(bitmap, revision, origin, destination))
                    if (direction == PaperTurnDirection.FORWARD) {
                        renderedOrigin = state.materialEngine.copyPreparedOrigin(revision, width, height)?.let {
                            RenderedOrigin(it, revision, origin, destination, settingsAtRequest)
                        }
                    }
                    ReaderTrace.event("paper_destination_prepared",
                        details = "revision=$revision direction=$direction width=$width height=$height")
                } else {
                    bitmap.recycle() // Never published or submitted to a renderer.
                }
            }
        } } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ReaderTrace.event("paper_destination_prepare_failed", details = "type=${error::class.java.simpleName}")
        }
    }
}

/** Remains laid out and attached for WebView paint, but is invisible and owns no input/focus. */
internal class PaperPreviewContainer @JvmOverloads constructor(
    context: Context,
    val previews: PaperDestinationPreviewState = PaperDestinationPreviewState()
) :
    FrameLayout(context) {
    init {
        alpha = 0f
        isFocusable = false
        isFocusableInTouchMode = false
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
}

@OptIn(ExperimentalReadiumApi::class, DelicateReadiumApi::class)
@Composable
internal fun ReaderPaperDestinationPreviewHost(
    activity: FragmentActivity,
    publication: Publication,
    initialLocator: Locator?,
    initialPreferences: EpubPreferences,
    owner: PaperPreviewNavigator,
    state: PaperDestinationPreviewState,
    tag: String,
    modifier: Modifier = Modifier
) {
    val id = remember(tag) { View.generateViewId() }
    val factory = remember(tag) {
        EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = initialLocator,
            initialPreferences = initialPreferences,
            paginationListener = owner.paginationListener,
            configuration = EpubNavigatorFragment.Configuration {
                useReadiumCssFontSize = false
                disablePageTurnsWhileScrolling = false
                decorationTemplates = HtmlDecorationTemplates.defaultTemplates(
                    alpha = READER_HIGHLIGHT_ALPHA,
                    experimentalPositioning = true
                )
            }
        )
    }
    AndroidView(factory = { context ->
        PaperPreviewContainer(context, state).apply {
            addView(FragmentContainerView(context).apply { this.id = id },
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
    }, modifier = modifier)
    DisposableEffect(activity, tag, id) {
        val manager = activity.supportFragmentManager
        val fragment = factory.instantiate(activity.classLoader, EpubNavigatorFragment::class.java.name)
        manager.beginTransaction().add(id, fragment, tag).commitNow()
        owner.navigator = fragment as EpubNavigatorFragment
        onDispose {
            owner.navigator = null
            owner.releaseHistory()
            state.invalidate()
            if (fragment.isAdded) manager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
        }
    }
}
