/*
 * Paper curl geometry adapted from oleksandrbalan/pagecurl (Apache-2.0).
 * Veil-specific changes: external gesture driving, reader overlay use,
 * paper edge highlight, theme-aware back sheet and cleanup.
 */
package com.veilreader.app.ui.screens

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max

internal data class PaperCurlVisualConfig(
    val backPageColor: Color,
    val backPageContentAlpha: Float = 0.11f,
    val shadowColor: Color = Color.Black,
    val shadowAlpha: Float = 0.36f,
    val shadowRadius: Dp = 28.dp,
    val shadowOffset: DpOffset = DpOffset((-5).dp, 2.dp),
    val edgeHighlight: Color = Color.White,
    val creaseHighlightAlpha: Float = 0.26f,
    val creaseShadowAlpha: Float = 0.20f,
    val backPageShadeAlpha: Float = 0.16f,
    val contactShadowAlpha: Float = 0.18f
)

internal fun Modifier.paperCurl(
    config: PaperCurlVisualConfig,
    edgeProvider: () -> PaperCurlEdge
): Modifier = drawWithCache {
    val edge = edgeProvider()
    val posA = edge.top
    val posB = edge.bottom

    if (posA == size.toRect().topLeft &&
        posB == size.toRect().bottomLeft
    ) {
        return@drawWithCache drawNothing()
    }

    if (posA == size.toRect().topRight &&
        posB == size.toRect().bottomRight
    ) {
        return@drawWithCache drawOnlyContent()
    }
    val topIntersection = paperLineIntersection(
        Offset.Zero,
        Offset(size.width, 0f),
        posA,
        posB
    )
    val bottomIntersection = paperLineIntersection(
        Offset(0f, size.height),
        Offset(size.width, size.height),
        posA,
        posB
    )

    if (topIntersection == null || bottomIntersection == null) {
        return@drawWithCache drawOnlyContent()
    }

    val topCurl = Offset(max(0f, topIntersection.x), topIntersection.y)
    val bottomCurl = Offset(max(0f, bottomIntersection.x), bottomIntersection.y)

    val drawClippedContent = prepareClippedContent(
        topCurl,
        bottomCurl
    )
    val centerX = (topCurl.x + bottomCurl.x) * 0.5f
    val progress = (1f - centerX / size.width).coerceIn(0f, 1f)
    val foldLift = paperFoldLift(progress)
    val crease = paperCreaseIntensity(progress)
    val contactShadow = paperContactShadowIntensity(progress)
    val drawCurl = prepareCurl(
        config,
        topCurl,
        bottomCurl,
        foldLift
    )
    onDrawWithContent {
        drawClippedContent()
        drawCurl()

        if (crease > 0.001f) {
            val lightAlpha = (config.creaseHighlightAlpha * crease).coerceIn(0f, 0.34f)
            val darkAlpha = (config.creaseShadowAlpha * crease).coerceIn(0f, 0.30f)

            drawLine(
                color = config.edgeHighlight.copy(alpha = lightAlpha),
                start = topCurl - Offset(0.75.dp.toPx(), 0f),
                end = bottomCurl - Offset(0.75.dp.toPx(), 0f),
                strokeWidth = 1.35.dp.toPx()
            )
            drawLine(
                color = config.shadowColor.copy(alpha = darkAlpha),
                start = topCurl + Offset(1.5.dp.toPx(), 0f),
                end = bottomCurl + Offset(1.5.dp.toPx(), 0f),
                strokeWidth = 1.15.dp.toPx()
            )
        }

        if (contactShadow > 0.001f) {
            drawLine(
                color = config.shadowColor.copy(
                    alpha = (config.contactShadowAlpha * contactShadow).coerceIn(0f, 0.24f)
                ),
                start = topCurl + Offset(5.dp.toPx(), 0f),
                end = bottomCurl + Offset(5.dp.toPx(), 0f),
                strokeWidth = (3.5f + contactShadow * 4f).dp.toPx()
            )
        }
    }
}

private fun CacheDrawScope.drawOnlyContent(): DrawResult =
    onDrawWithContent { drawContent() }

private fun CacheDrawScope.drawNothing(): DrawResult =
    onDrawWithContent { }

private fun CacheDrawScope.prepareClippedContent(
    topCurl: Offset,
    bottomCurl: Offset
): ContentDrawScope.() -> Unit {
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(topCurl.x, topCurl.y)
        lineTo(bottomCurl.x, bottomCurl.y)
        lineTo(0f, size.height)
        close()
    }
    return result@{
        clipPath(path) {
            this@result.drawContent()
        }
    }
}

private fun CacheDrawScope.prepareCurl(
    config: PaperCurlVisualConfig,
    topCurl: Offset,
    bottomCurl: Offset,
    foldLift: Float
): ContentDrawScope.() -> Unit {
    val polygon = PaperCurlPolygon(
        sequence {
            suspend fun SequenceScope<Offset>.yieldRightIntercept() {
                val offset = paperLineIntersection(
                    topCurl,
                    bottomCurl,
                    Offset(size.width, 0f),
                    Offset(size.width, size.height)
                ) ?: return
                yield(offset)
                yield(offset)
            }

            if (topCurl.x < size.width) {
                yield(topCurl)
                yield(Offset(size.width, topCurl.y))
            } else {
                yieldRightIntercept()
            }
            if (bottomCurl.x < size.width) {
                yield(Offset(size.width, size.height))
                yield(bottomCurl)
            } else {
                yieldRightIntercept()
            }
        }.toList()
    )

    val lineVector = topCurl - bottomCurl
    val angle = PI.toFloat() -
        atan2(lineVector.y, lineVector.x) * 2f
    val drawShadow = prepareShadow(
        config,
        polygon,
        angle,
        foldLift
    )

    return result@{
        withTransform({
            scale(-1f, 1f, pivot = bottomCurl)
            rotateRad(angle, pivot = bottomCurl)
        }) {
            this@result.drawShadow()
            clipPath(polygon.toPath()) {
                this@result.drawContent()
                val visibleBackContentAlpha =
                    (config.backPageContentAlpha + foldLift * 0.06f)
                        .coerceIn(0f, 0.32f)
                val overlayAlpha =
                    (1f - visibleBackContentAlpha)
                        .coerceIn(0f, 1f)
                drawRect(
                    config.backPageColor.copy(alpha = overlayAlpha)
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            config.edgeHighlight.copy(alpha = 0.05f + foldLift * 0.08f),
                            Color.Transparent,
                            config.shadowColor.copy(
                                alpha = (config.backPageShadeAlpha * foldLift).coerceIn(0f, 0.22f)
                            )
                        ),
                        startX = 0f,
                        endX = size.width
                    )
                )
            }
        }
    }
}
private fun CacheDrawScope.prepareShadow(
    config: PaperCurlVisualConfig,
    polygon: PaperCurlPolygon,
    angle: Float,
    foldLift: Float
): ContentDrawScope.() -> Unit {
    val lift = foldLift.coerceIn(0f, 1f)
    if (config.shadowAlpha == 0f ||
        config.shadowRadius == 0.dp ||
        lift <= 0.001f
    ) {
        return { }
    }

    val radius = config.shadowRadius.toPx() * (0.55f + lift * 0.45f)
    val dynamicShadowAlpha = config.shadowAlpha * lift
    val shadowColor = config.shadowColor
        .copy(alpha = dynamicShadowAlpha)
        .toArgb()
    val transparent = config.shadowColor
        .copy(alpha = 0f)
        .toArgb()
    val offsetScale = 0.65f + lift * 0.35f
    val shadowOffset = Offset(
        -config.shadowOffset.x.toPx() * offsetScale,
        config.shadowOffset.y.toPx() * offsetScale
    ).paperRotate(2f * PI.toFloat() - angle)

    val paint = Paint().apply {
        asFrameworkPaint().apply {
            color = transparent
            setShadowLayer(
                radius,
                shadowOffset.x,
                shadowOffset.y,
                shadowColor
            )
        }
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        prepareShadowApi28(
            radius,
            paint,
            polygon
        )
    } else {
        prepareShadowLegacy(
            radius = radius,
            color = config.shadowColor,
            alpha = dynamicShadowAlpha,
            polygon = polygon
        )
    }
}

private fun prepareShadowApi28(
    radius: Float,
    paint: Paint,
    polygon: PaperCurlPolygon
): ContentDrawScope.() -> Unit = {
    drawIntoCanvas {
        it.nativeCanvas.drawPath(
            polygon
                .offset(radius)
                .toPath()
                .asAndroidPath(),
            paint.asFrameworkPaint()
        )
    }
}

private fun prepareShadowLegacy(
    radius: Float,
    color: Color,
    alpha: Float,
    polygon: PaperCurlPolygon
): ContentDrawScope.() -> Unit = {
    drawPath(
        path = polygon.offset(radius * 0.35f).toPath(),
        color = color.copy(alpha = alpha * 0.18f)
    )
    drawPath(
        path = polygon.offset(radius * 0.70f).toPath(),
        color = color.copy(alpha = alpha * 0.10f)
    )
    drawPath(
        path = polygon.offset(radius).toPath(),
        color = color.copy(alpha = alpha * 0.05f)
    )
}
