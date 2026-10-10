package com.veilreader.app.debug

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.eschao.android.widget.pageflip.OnPageFlipListener
import com.eschao.android.widget.pageflip.PageFlip
import com.eschao.android.widget.pageflip.PageFlipState
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.withLock

/**
 * Open-source engine A/B: PageFlip by eschao (Apache-2.0).
 *
 * This deliberately contains ONLY fictional synthetic paper artwork. It is
 * independent of Readium, preferences, persistence and all actual books.
 * Paper must not be promoted from this lab to production without #408.
 *
 * Upstream: https://github.com/eschao/android-PageFlip
 */
class PageFlipPremiumLabActivity : Activity() {
    private var engine: PremiumFlipSurface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(28, 33, 45)) }
        val hud = TextView(this).apply {
            textSize = 12.5f
            setTextColor(Color.rgb(226, 197, 137))
            setBackgroundColor(Color.argb(222, 18, 26, 39))
            setPadding(dp(14), dp(9), dp(12), dp(9))
            gravity = Gravity.CENTER_VERTICAL
            text = "PAGEFLIP · PREMIUM PHYSICS LAB\n" +
                "OpenGL 3D | fold + base shadows | drag from the RIGHT edge"
            isFocusable = false
            isClickable = false
        }
        val surface = PremiumFlipSurface(
            activity = this,
            report = { index, error ->
                runOnUiThread {
                    hud.text = if (error != null) {
                        "PAGEFLIP LAB — ERROR (no fallback)\n$error"
                    } else {
                        "PAGEFLIP · PREMIUM PHYSICS LAB  |  ${index + 1} / ${PremiumFlipSurface.PAGE_COUNT}\n" +
                            "Drag right→left. Reverse and cancel to test real folded paper."
                    }
                }
            }
        )
        engine = surface
        root.addView(surface, FrameLayout.LayoutParams(-1, -1))
        root.addView(hud, FrameLayout.LayoutParams(-1, dp(76), Gravity.TOP))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        engine?.onResume()
    }

    override fun onPause() {
        engine?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        engine = null
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private class PremiumFlipSurface(
        private val activity: Activity,
        private val report: (index: Int, error: String?) -> Unit
    ) : GLSurfaceView(activity), GLSurfaceView.Renderer, OnPageFlipListener {
        companion object {
            const val PAGE_COUNT = 8
            private const val TAG = "VeilPageFlipArena"
            private const val FULL = 0
            private const val MOVING = 1
            private const val ANIMATING = 2
        }

        private val guard = ReentrantLock()
        private val flip = PageFlip(activity).apply {
            // High quality configuration: denser mesh, smaller bend radius,
            // distinct paper contact and crease shadow gradients.
            enableAutoPage(false)
            enableClickToFlip(false)
            setPixelsOfMesh(6)
            setSemiPerimeterRatio(0.65f)
            setShadowWidthOfFoldEdges(5f, 76f, 0.36f)
            setShadowWidthOfFoldBase(6f, 98f, 0.46f)
            setShadowColorOfFoldEdges(0.10f, 0.34f, 0.30f, 0f)
            setShadowColorOfFoldBase(0.04f, 0.34f, 0.25f, 0f)
            setListener(this@PremiumFlipSurface)
        }

        private var pageIndex = 0
        private var drawCommand = FULL
        private var surfaceReady = false
        private var touchStarted = false
        private var failed = false

        init {
            setEGLContextClientVersion(2)
            setRenderer(this)
            renderMode = RENDERMODE_WHEN_DIRTY
        }

        override fun canFlipForward() = pageIndex < PAGE_COUNT - 1

        override fun canFlipBackward(): Boolean {
            if (pageIndex <= 0) return false
            // Mirror upstream's single-page backward ownership: texture 1
            // becomes the second, then previous is uploaded as the first.
            flip.firstPage?.setSecondTextureWithFirst()
            return true
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // No Readium WebView is involved in this isolated comparison.
            // Guard both UI and GL access to PageFlip's mutable state.
            if (failed) return false
            guard.withLock {
                if (!surfaceReady) return false
                val x = event.x
                val y = event.y
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (flip.isAnimating) return true
                        touchStarted = true
                        flip.onFingerDown(x, y)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!touchStarted || flip.isAnimating) return true
                        if (flip.onFingerMove(x, y)) {
                            drawCommand = MOVING
                            requestRender()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!touchStarted) return true
                        touchStarted = false
                        flip.onFingerUp(x, y, 450)
                        if (flip.isAnimating) {
                            drawCommand = ANIMATING
                            requestRender()
                        } else {
                            drawCommand = FULL
                            requestRender()
                        }
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        touchStarted = false
                        flip.abortAnimating()
                        drawCommand = FULL
                        requestRender()
                        return true
                    }
                }
            }
            return true
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            guard.withLock {
                try {
                    flip.onSurfaceCreated()
                } catch (e: Exception) {
                    fail("OpenGL init: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            guard.withLock {
                try {
                    flip.onSurfaceChanged(width, height)
                    // A resized texture must come from exactly this viewport.
                    flip.firstPage?.deleteAllTextures()
                    surfaceReady = true
                    drawCommand = FULL
                    requestRender()
                } catch (e: Exception) {
                    fail("OpenGL resize: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            guard.withLock {
                if (!surfaceReady || failed) return
                try {
                    flip.deleteUnusedTextures()
                    val page = flip.firstPage ?: return
                    if (drawCommand == FULL) {
                        if (!page.isFirstTextureSet) {
                            uploadPaper(pageIndex, page.width(), page.height(), page::setFirstTexture)
                        }
                        flip.drawPageFrame()
                    } else {
                        val state = flip.flipState
                        if (state == PageFlipState.FORWARD_FLIP && !page.isSecondTextureSet) {
                            uploadPaper(pageIndex + 1, page.width(), page.height(), page::setSecondTexture)
                        } else if (state == PageFlipState.BACKWARD_FLIP && !page.isFirstTextureSet) {
                            pageIndex = (pageIndex - 1).coerceAtLeast(0)
                            uploadPaper(pageIndex, page.width(), page.height(), page::setFirstTexture)
                        }
                        flip.drawFlipFrame()
                    }
                    // The renderer loop is driven only by actual animation,
                    // never by an unbounded busy-loop on the UI thread.
                    post { onFrameFinished() }
                } catch (e: Exception) {
                    fail("Render: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }

        private fun onFrameFinished() {
            guard.withLock {
                if (failed || drawCommand != ANIMATING) return
                if (flip.animating()) {
                    requestRender()
                    return
                }
                if (flip.flipState == PageFlipState.END_WITH_FORWARD) {
                    flip.firstPage?.setFirstTextureWithSecond()
                    pageIndex = (pageIndex + 1).coerceAtMost(PAGE_COUNT - 1)
                }
                // During backwards, the first-page bitmap was already updated
                // on the rendering thread; a cancelled turn must not advance.
                drawCommand = FULL
                report(pageIndex, null)
                requestRender()
            }
        }

        private fun fail(message: String) {
            failed = true
            surfaceReady = false
            Log.e(TAG, message)
            post { report(pageIndex, message) }
        }

        /**
         * PageFlip uses synchronous GLUtils.texImage2D and does not retain a
         * bitmap lease. Always recycle the transient ARGB fixture after upload;
         * otherwise repeated turns leak ~7 MiB per page.
         */
        private inline fun uploadPaper(
            index: Int,
            pageWidth: Float,
            pageHeight: Float,
            upload: (Bitmap) -> Unit
        ) {
            val texture = createPaper(index, pageWidth, pageHeight)
            try {
                upload(texture)
            } finally {
                texture.recycle()
            }
        }

        private fun createPaper(index: Int, viewWidth: Float, viewHeight: Float): Bitmap {
            // Fixed ceiling for this *debug* experiment only; production
            // Readium pages must use keyed bounded immutable snapshot leases.
            val w = viewWidth.toInt().coerceIn(96, 1050)
            val h = viewHeight.toInt().coerceIn(96, 1820)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(250, 241, 221))
            val scaleX = w / 800f
            val scaleY = h / 1280f
            canvas.save()
            canvas.scale(scaleX, scaleY)
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(169, 140, 97)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(RectF(38f, 39f, 762f, 1242f), border)
            val brass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(147, 104, 48)
                typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
                textSize = 31f
            }
            val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(45, 42, 43)
                typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
                textSize = 29f
            }
            canvas.drawText("V E I L   ·   T H E   A R C H I V E", 68f, 117f, brass)
            border.style = Paint.Style.FILL
            canvas.drawRect(69f, 140f, 732f, 142f, border)
            ink.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
            ink.textSize = 51f
            canvas.drawText("THE WEIGHT OF PAPER", 68f, 236f, ink)
            ink.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
            ink.textSize = 28f

            val rows = listOf(
                "A page is not a sliding glass pane.",
                "Lift the corner and bend the ink.",
                "Watch the shadow shift with touch.",
                "The fold should resist and release.",
                "Reverse the turn before it commits.",
                "A real material has two faces.",
                "The world behind it stays still.",
                "A reader should feel the page.",
                "Slow fingers reveal every flaw.",
                "Every turn deserves precision."
            )
            for (i in 0..9) {
                canvas.drawText(rows[(index.coerceAtLeast(0) + i) % rows.size], 69f, 347f + i * 78f, ink)
                canvas.drawRect(68f, 363f + i * 78f, 732f, 364f + i * 78f, border)
            }
            brass.textSize = 34f
            canvas.drawText("— ${index.coerceIn(0, PAGE_COUNT - 1) + 1} —", 342f, 1191f, brass)
            canvas.restore()
            return bitmap
        }
    }
}
