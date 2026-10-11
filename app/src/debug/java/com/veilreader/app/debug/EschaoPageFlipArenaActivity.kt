package com.veilreader.app.debug

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.eschao.android.widget.pageflip.OnPageFlipListener
import com.eschao.android.widget.pageflip.PageFlip
import com.eschao.android.widget.pageflip.PageFlipException
import com.eschao.android.widget.pageflip.PageFlipState
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min

/**
 * Independent 3D engine candidate for the Veil Reader arena.
 *
 * Uses original Apache-2.0 eschao/android-PageFlip core in this debug source set.
 * This is NOT a working EPUB/Readium navigator. Eight fictional parchment pages
 * exercise actual ES2 fold mesh, moving finger, independent underside and shadows.
 * Never promote based on a synthetic visual specimen alone.
 */
class EschaoPageFlipArenaActivity : Activity() {
    private var paper: EschaoPageFlipSurface? = null
    private var label: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frame = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(17, 22, 31))
        }
        val sheet = EschaoPageFlipSurface(this) { page ->
            label?.text = "VEIL PAPER ARENA  ·  ESCHAO OPENGL\n" +
                "Physically curved mesh  •  Sheet ${page + 1} / 8  •  Drag and reverse"
        }
        paper = sheet
        frame.addView(sheet, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        val hud = TextView(this).apply {
            setBackgroundColor(Color.argb(225, 14, 21, 31))
            setTextColor(Color.rgb(226, 194, 129))
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18.dp(), 6.dp(), 14.dp(), 6.dp())
            text = "VEIL PAPER ARENA  ·  ESCHAO OPENGL\n" +
                "Sheet 1 / 8  •  Drag the corner; reverse or release."
            isClickable = false
            isFocusable = false
        }
        label = hud
        frame.addView(hud, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 76.dp(), Gravity.TOP
        ))
        setContentView(frame)
    }

    override fun onResume() {
        super.onResume()
        paper?.onResume()
    }

    override fun onPause() {
        paper?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        paper?.dispose()
        label = null
        paper = null
        super.onDestroy()
    }

    private fun Int.dp() = (this * resources.displayMetrics.density + 0.5f).toInt()
}

private class EschaoPageFlipSurface(
    context: Context,
    private val onChanged: (Int) -> Unit
) : GLSurfaceView(context), GLSurfaceView.Renderer, OnPageFlipListener {
    companion object {
        private const val PAGE_COUNT = 8
        private const val FRAME_FULL = 0
        private const val FRAME_MOVING = 1
        private const val FRAME_ANIMATING = 2
        private const val FINISH_MS = 420
    }

    private val lock = ReentrantLock()
    private val uiHandler = Handler(Looper.getMainLooper())
    private val flip = PageFlip(context).apply {
        // Pin portrait single-sheet mode for device-to-device comparison.
        enableAutoPage(false)
        enableClickToFlip(false)
        setPixelsOfMesh(5)
        setSemiPerimeterRatio(0.70f)
        setMaskAlphaOfFold(72)
        setShadowColorOfFoldEdges(0.07f, 0.28f, 0.26f, 0.80f)
        setShadowColorOfFoldBase(0.03f, 0.18f, 0.20f, 0.68f)
        setShadowWidthOfFoldEdges(5f, 42f, 0.30f)
        setShadowWidthOfFoldBase(7f, 64f, 0.42f)
    }
    private var pageNumber = 1
    private var mode = FRAME_FULL
    private var closed = false
    private var surfaceReady = false
    // A canceled or restored fold must never change the logical page.
    // Texture reset is requested on the UI thread, but actually executed
    // ONLY in the GL renderer while the GL context is current.
    private var gestureStartPage = 1
    private var gestureStartX = 0f
    private var cancelPending = false
    private var invalidatePaperTextures = false

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        flip.setListener(this)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
        isClickable = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        lock.withLock {
            try {
                flip.onSurfaceCreated()
            } catch (error: PageFlipException) {
                surfaceReady = false
                Log.e("VeilEschao", "Cannot initialize ES2 fold shaders", error)
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        lock.withLock {
            if (width <= 0 || height <= 0) return
            try {
                flip.onSurfaceChanged(width, height)
                mode = FRAME_FULL
                surfaceReady = true
                requestRender()
            } catch (error: PageFlipException) {
                surfaceReady = false
                Log.e("VeilEschao", "Cannot prepare paper fold viewport", error)
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        var nextMode = FRAME_FULL
        lock.withLock {
            if (closed || !surfaceReady) return
            try {
                flip.deleteUnusedTextures()
                val page = flip.getFirstPage() ?: return
                if (invalidatePaperTextures) {
                    page.deleteAllTextures()
                    invalidatePaperTextures = false
                }
                when (mode) {
                    FRAME_FULL -> {
                        if (!page.isFirstTextureSet) {
                            val bitmap = createPage(pageNumber, false, page.width(), page.height())
                            try { page.setFirstTexture(bitmap) } finally { bitmap.recycle() }
                        }
                        if (!page.isBackTextureSet) {
                            val back = createPage(pageNumber, true, page.width(), page.height())
                            try { page.setBackTexture(back) } finally { back.recycle() }
                        }
                        flip.drawPageFrame()
                    }
                    else -> {
                        if (flip.flipState == PageFlipState.FORWARD_FLIP &&
                            !page.isSecondTextureSet) {
                            val next = createPage(min(pageNumber + 1, PAGE_COUNT), false, page.width(), page.height())
                            try { page.setSecondTexture(next) } finally { next.recycle() }
                        } else if (flip.flipState == PageFlipState.BACKWARD_FLIP &&
                            !page.isFirstTextureSet) {
                            pageNumber = max(1, pageNumber - 1)
                            val previous = createPage(pageNumber, false, page.width(), page.height())
                            try { page.setFirstTexture(previous) } finally { previous.recycle() }
                            // A backward turn still gets its own paper underside.
                            page.setBackTexture(null)
                            val back = createPage(pageNumber, true, page.width(), page.height())
                            try { page.setBackTexture(back) } finally { back.recycle() }
                        }
                        flip.drawFlipFrame()
                    }
                }
                nextMode = mode
            } catch (error: Exception) {
                Log.e("VeilEschao", "Paper renderer error; candidate NOT ACCEPTED", error)
                mode = FRAME_FULL
            }
        }
        // Upstream animation intentionally advances off the GL thread, then
        // requests the next frame; rendering and touch share the same lock.
        if (nextMode == FRAME_ANIMATING) {
            uiHandler.post { onFinishedFrame() }
        }
    }

    private fun onFinishedFrame() {
        var nextPage: Int? = null
        lock.withLock {
            if (closed || mode != FRAME_ANIMATING) return
            if (flip.animating()) {
                requestRender()
            } else {
                val state = flip.flipState
                if (cancelPending || state == PageFlipState.END_WITH_RESTORE) {
                    // A backward preview may have decremented pageNumber
                    // before the turn is committed. Restore its original page
                    // and rebuild the correct front/back GLSL textures.
                    pageNumber = gestureStartPage
                    invalidatePaperTextures = true
                    cancelPending = false
                } else if (state == PageFlipState.END_WITH_FORWARD) {
                    flip.getFirstPage()?.let { page ->
                        page.setFirstTextureWithSecond()
                        page.setBackTexture(null)
                    }
                    pageNumber = min(PAGE_COUNT, pageNumber + 1)
                    nextPage = pageNumber - 1
                } else if (state == PageFlipState.END_WITH_BACKWARD) {
                    pageNumber = max(1, gestureStartPage - 1)
                    nextPage = pageNumber - 1
                }
                mode = FRAME_FULL
                requestRender()
            }
        }
        nextPage?.let(onChanged)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (closed) return false
        lock.withLock {
            if (!surfaceReady || flip.getFirstPage() == null) return true
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (!flip.isAnimating) {
                        gestureStartPage = pageNumber
                        gestureStartX = event.x
                        cancelPending = false
                        flip.onFingerDown(event.x, event.y)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!flip.isAnimating) {
                        if (flip.canAnimate(event.x, event.y)) {
                            release(event.x, event.y)
                        } else if (flip.onFingerMove(event.x, event.y)) {
                            mode = FRAME_MOVING
                            requestRender()
                        }
                    }
                }
                MotionEvent.ACTION_UP -> release(event.x, event.y)
                MotionEvent.ACTION_CANCEL -> {
                    cancelPending = true
                    // Ask upstream to return towards original edge rather
                    // than interpreting CANCEL as a normal release. Force
                    // restore by transaction guard even if its fold threshold
                    // would otherwise finish in the forward direction.
                    if (flip.isAnimating) {
                        // Existing animation will finish, but MUST NOT commit.
                        mode = FRAME_ANIMATING
                        requestRender()
                    } else {
                        release(gestureStartX, event.y)
                        if (mode != FRAME_ANIMATING) {
                            pageNumber = gestureStartPage
                            invalidatePaperTextures = true
                            cancelPending = false
                            mode = FRAME_FULL
                            requestRender()
                        }
                    }
                }
            }
        }
        return true
    }

    private fun release(x: Float, y: Float) {
        if (!flip.isAnimating) {
            flip.onFingerUp(x, y, FINISH_MS)
            if (flip.animating()) {
                mode = FRAME_ANIMATING
                requestRender()
            }
        }
    }

    override fun canFlipForward(): Boolean = pageNumber < PAGE_COUNT
    override fun canFlipBackward(): Boolean {
        if (pageNumber <= 1) return false
        // Preserve current page as the second texture for a reverse fold.
        flip.getFirstPage()?.setSecondTextureWithFirst()
        return true
    }

    fun dispose() {
        lock.withLock {
            closed = true
            flip.setListener(null)
            uiHandler.removeCallbacksAndMessages(null)
        }
    }

    /**
     * Render a front and back with distinct text/ink, not a static translated
     * bitmap. The upstream ES2 engine then bends these textures as a true mesh.
     * This is deliberately synthetic; Readium snapshot ownership is separate.
     */
    private fun createPage(index: Int, back: Boolean, width: Float, height: Float): Bitmap {
        val w = width.toInt().coerceIn(128, 1080)
        val h = height.toInt().coerceIn(128, 1800)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (back) Color.rgb(90, 84, 78) else Color.rgb(48, 45, 46)
            typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        val sx = w / 800f
        val sy = h / 1280f
        canvas.save()
        canvas.scale(sx, sy)
        val ground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 800f, 1280f,
                if (back) Color.rgb(239, 225, 198) else Color.rgb(252, 245, 226),
                if (back) Color.rgb(215, 201, 174) else Color.rgb(236, 223, 193),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, 800f, 1280f, ground)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(174, 145, 93)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(35f, 40f, 765f, 1234f), 5f, 5f, border)
        ink.typeface = Typeface.create("serif", Typeface.BOLD)
        ink.color = Color.rgb(120, 87, 47)
        ink.textSize = 24f
        canvas.drawText("V E I L  /  T H E  G R A Y F O G  A R C H I V E", 67f, 123f, ink)
        canvas.drawLine(65f, 145f, 735f, 145f, border)
        ink.color = Color.rgb(47, 44, 46)
        ink.textSize = 46f
        canvas.drawText(if (back) "THE OTHER SIDE" else "THE FOLDING PAGE", 66f, 239f, ink)
        ink.typeface = Typeface.create("serif", Typeface.NORMAL)
        ink.textSize = 30f
        val lines = if (back) listOf(
            "The back of this paper has its own ink.",
            "A crease should change the light itself.",
            "Weight draws the sheet against a finger.",
            "Drag backwards to return to the beginning.",
            "There is no live EPUB in this experiment.",
            "Judge the bend, shadow, and underside."
        ) else listOf(
            "A good page is a physical object.",
            "An inked sheet rises from its corner.",
            "The fold follows the moving fingertip.",
            "The base shadow reveals the distance.",
            "The opposite side has a separate texture.",
            "Try a slow drag, then a sudden release."
        )
        for (row in 0 until 10) {
            ink.color = if (row % 2 == 0) Color.rgb(57, 52, 48) else Color.rgb(75, 65, 56)
            canvas.drawText(lines[(row + index) % lines.size], 69f, 341f + 76f * row, ink)
            canvas.drawLine(69f, 359f + 76f * row, 720f, 359f + 76f * row, border)
        }
        ink.color = Color.rgb(135, 94, 46)
        ink.typeface = Typeface.create("serif", Typeface.BOLD)
        ink.textSize = 34f
        canvas.drawText("—  ${index.coerceIn(1, PAGE_COUNT)}  —", 335f, 1171f, ink)
        canvas.restore()
        return bitmap
    }
}
