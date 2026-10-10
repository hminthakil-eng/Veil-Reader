package com.veilreader.app.debug

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.irurueta.android.gl.curl.CurlPage
import com.irurueta.android.gl.curl.CurlTextureView
import kotlin.math.min

/**
 * Isolated visual/gesture evaluation of a READY-MADE native curl engine.
 *
 * Apache-2.0 library: com.irurueta:irurueta-android-gl-curl:1.1.6
 * The fixtures are fictional bitmaps, not Readium publication pages.
 *
 * NEVER advertise this as a working EPUB reader or route the user's Reader
 * gestures here without a real current/next/previous Readium snapshot bridge,
 * durable locator ownership and physical QA. The app's live Paper mode is untouched.
 */
class ExternalCurlArenaActivity : Activity() {
    private var curl: CurlTextureView? = null
    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this).apply { setBackgroundColor(Color.rgb(24, 29, 38)) }
        val engine = CurlTextureView(this).apply {
            viewMode = CurlTextureView.SHOW_ONE_PAGE
            renderLeftPage = false
            // Higher-than-default curl polygon resolution: validate actual feel
            // on physical 60/120 Hz hardware before choosing a production value.
            maxCurlSplitsInMesh = 20
            drawShadowInMesh = true
            drawTextureInMesh = true
            enableTouchPressure = false
            animationDurationTime = 380
            pageProvider = object : CurlTextureView.PageProvider {
                override val pageCount: Int = FIXTURE_PAGE_COUNT

                override fun updatePage(
                    page: CurlPage,
                    width: Int,
                    height: Int,
                    index: Int,
                    backIndex: Int?
                ) {
                    // CurlPage TAKES OWNERSHIP and recycles each Bitmap after GPU upload.
                    // Never share a bitmap between slots or recycle it ourselves.
                    page.setTexture(
                        makeFictionalPage(width, height, index, isBack = false),
                        CurlPage.SIDE_FRONT
                    )
                    page.setTexture(
                        makeFictionalPage(width, height, backIndex ?: index, isBack = true),
                        CurlPage.SIDE_BACK
                    )
                    page.setColor(Color.rgb(245, 234, 207), CurlPage.SIDE_BACK)
                }
            }
        }
        curl = engine

        frame.addView(
            engine,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val hud = TextView(this).apply {
            setBackgroundColor(Color.argb(227, 19, 26, 36))
            setTextColor(Color.rgb(230, 204, 145))
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            val h = dp(14)
            setPadding(h, dp(10), h, dp(10))
            text = "CURL ENGINE LAB  ·  external 3D mesh\n" +
                "Drag from right edge to left; reverse mid-turn. Fixture 1 / $FIXTURE_PAGE_COUNT"
            // Decorative overlay must not take over the page drag.
            isClickable = false
            isFocusable = false
        }
        status = hud
        frame.addView(
            hud,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(84),
                Gravity.TOP
            )
        )
        engine.currentIndexChangedListener =
            object : CurlTextureView.CurrentIndexChangedListener {
                override fun onCurrentIndexChanged(view: CurlTextureView, currentIndex: Int) {
                    status?.text = "CURL ENGINE LAB  ·  external 3D mesh\n" +
                        "Drag / reverse / cancel  ·  Fixture ${(currentIndex + 1).coerceAtMost(FIXTURE_PAGE_COUNT)} / $FIXTURE_PAGE_COUNT"
                }
            }
        setContentView(frame)
    }

    override fun onDestroy() {
        status = null
        curl = null
        super.onDestroy()
    }

    private fun makeFictionalPage(width: Int, height: Int, index: Int, isBack: Boolean): Bitmap {
        // Bound debug fixture allocations; the upstream engine creates extra
        // power-of-two GL textures. This is not a production EPUB snapshot cache.
        val bitmapWidth = width.coerceIn(64, 900)
        val bitmapHeight = height.coerceIn(64, 1600)
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (isBack) Color.rgb(240, 230, 207) else Color.rgb(250, 242, 222))
        val sx = bitmapWidth / 800f
        val sy = bitmapHeight / 1280f
        canvas.save()
        canvas.scale(sx, sy)

        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(44, 40, 43) }
        val brass = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(143, 110, 52) }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(197, 177, 137)
            strokeWidth = 2f
        }
        canvas.drawRect(RectF(35f, 35f, 765f, 1245f), line.apply { style = Paint.Style.STROKE })
        line.style = Paint.Style.FILL

        brass.textSize = 26f
        brass.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        canvas.drawText("V E I L   ·   T H E   A R C H I V E", 68f, 115f, brass)
        canvas.drawLine(65f, 145f, 735f, 145f, line)

        ink.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        ink.textSize = 55f
        canvas.drawText(if (isBack) "THE OTHER SIDE" else "THE PAPER TEST", 68f, 248f, ink)

        ink.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
        ink.textSize = 29f
        val paragraphs = listOf(
            "A page should fold, not slide.",
            "The corners lift as the fingers move.",
            "Ink and shadow follow the paper.",
            "Each side has its own texture.",
            "Reverse the gesture to cancel.",
            "Watch the edge, crease and weight.",
            "No Readium content is used here.",
            "This is an independent curl engine.",
            "Test slowly, then rapidly.",
            "Judge the material, not only speed."
        )
        for ((row, text) in paragraphs.withIndex()) {
            val shifted = (index.coerceAtLeast(0) + row) % paragraphs.size
            canvas.drawText(paragraphs[shifted], 69f, 352f + row * 76f, ink)
            canvas.drawLine(68f, 369f + row * 76f, 732f, 369f + row * 76f, line)
        }
        brass.textSize = 38f
        brass.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        canvas.drawText("— ${index.coerceIn(0, FIXTURE_PAGE_COUNT - 1) + 1} —", 342f, 1190f, brass)
        canvas.restore()
        return bitmap
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        const val FIXTURE_PAGE_COUNT = 8
    }
}
