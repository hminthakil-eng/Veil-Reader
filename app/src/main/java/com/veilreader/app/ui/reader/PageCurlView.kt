package com.veilreader.app.ui.reader

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Opt-in page curl for the real EPUB viewport. Readium still owns layout, locators and selection.
 * One bounded reusable snapshot is bent over the live destination; no page text is re-rendered.
 * Missing snapshots or failed turns fall back to the navigator, never to a synthetic book page.
 */
class PageCurlView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var snapshot: Bitmap? = null
    private val mesh = FloatArray((COLUMNS + 1) * 4)
    private val shades = IntArray((COLUMNS + 1) * 2)
    private var animator: ValueAnimator? = null
    private var fraction = 0f
    private var towardLeft = true
    private var sourceLocator: String? = null
    private var destinationScheduled = false
    var isTurning: Boolean = false
        private set
    private val expiry = Runnable { cancelTurn() }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
        visibility = INVISIBLE
        // This temporary mesh is small; software Canvas has consistent bitmap-mesh behavior.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun turn(source: View, locator: String, toLeft: Boolean, navigate: () -> Boolean): Boolean {
        if (isTurning) return true
        if (source.width <= 0 || source.height <= 0 || width <= 0 || height <= 0) return navigate()
        val scale = min(1f, MAX_EDGE.toFloat() / maxOf(source.width, source.height))
        val sw = (source.width * scale).toInt().coerceAtLeast(1)
        val sh = (source.height * scale).toInt().coerceAtLeast(1)
        try {
            val previous = snapshot
            if (previous == null || previous.width != sw || previous.height != sh) {
                snapshot = null
                previous?.recycle()
                snapshot = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            }
            val target = requireNotNull(snapshot)
            target.eraseColor(Color.TRANSPARENT)
            Canvas(target).apply { scale(scale, scale); source.draw(this) }
        } catch (_: OutOfMemoryError) {
            release()
            return navigate()
        } catch (_: RuntimeException) {
            cancelTurn()
            return navigate()
        }
        sourceLocator = locator
        towardLeft = toLeft
        fraction = 0f
        isTurning = true
        destinationScheduled = false
        visibility = VISIBLE
        invalidate()
        if (!navigate()) {
            cancelTurn()
            return false
        }
        // A slow/corrupt resource can never leave a stale screenshot covering the reader.
        postDelayed(expiry, 1800L)
        return true
    }

    fun onLocationChanged(locator: String) {
        if (!isTurning || locator == sourceLocator || destinationScheduled) return
        destinationScheduled = true
        // Locators may be emitted before the WebView has submitted its next frame.
        postOnAnimation { postOnAnimation { if (isTurning) animateTurn() } }
    }

    private fun animateTurn() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 460L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { fraction = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { cancelTurn() }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = snapshot ?: return
        if (!isTurning || bitmap.isRecycled) return
        val w = width.toFloat()
        val h = height.toFloat()
        val fold = w * (1f - 2f * fraction)
        val arcLength = w * .34f
        val radius = arcLength / PI.toFloat()
        for (row in 0..1) for (column in 0..COLUMNS) {
            val u = column.toFloat() / COLUMNS
            val x = if (towardLeft) w * u else w * (1f - u)
            val distance = (x - fold).coerceAtLeast(0f)
            val angle = min(PI.toFloat(), distance / radius)
            val projected = if (x <= fold) x else fold + radius * sin(angle) - (distance - arcLength).coerceAtLeast(0f)
            val depth = radius * (1f - cos(angle))
            val perspective = 1f / (1f + depth / (w * 2.5f))
            val index = row * (COLUMNS + 1) + column
            mesh[index * 2] = if (towardLeft) projected else w - projected
            mesh[index * 2 + 1] = h / 2 + (row * h - h / 2) * perspective
            val shade = (255 * (1f - .32f * sin(angle / 2))).toInt().coerceIn(0, 255)
            shades[index] = Color.rgb(shade, shade, shade)
        }
        val edge = if (towardLeft) fold else w - fold
        val shadowLeft = if (towardLeft) edge else edge - 30f
        shadowPaint.shader = LinearGradient(shadowLeft, 0f, shadowLeft + 30f, 0f,
            if (towardLeft) intArrayOf(0x44000000, 0x00000000) else intArrayOf(0x00000000, 0x44000000), null, Shader.TileMode.CLAMP)
        canvas.drawRect(shadowLeft, 0f, shadowLeft + 30f, h, shadowPaint)
        canvas.drawBitmapMesh(bitmap, COLUMNS, 1, mesh, 0, shades, 0, paint)
    }

    fun cancelTurn() {
        removeCallbacks(expiry)
        val running = animator
        animator = null
        running?.removeAllListeners()
        running?.cancel()
        isTurning = false
        sourceLocator = null
        destinationScheduled = false
        visibility = INVISIBLE
    }

    fun release() {
        cancelTurn()
        snapshot?.recycle()
        snapshot = null
    }

    override fun onDetachedFromWindow() { release(); super.onDetachedFromWindow() }

    private companion object { const val MAX_EDGE = 1400; const val COLUMNS = 48 }
}
