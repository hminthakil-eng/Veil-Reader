package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Vector-only portrait fallback. Not a 3D engine or 3D preview.
 * Built-in customization choices share the stable catalog index with RagsToRichesAvatar.
 */
@Composable
internal fun PremiumAvatarPortrait(
    hairStyle: Int,
    hairColor: Int,
    skinTone: Int,
    outfitStyle: Int,
    accentColor: Int,
    modifier: Modifier = Modifier
) {
    val style = hairStyle.coerceIn(0, 3)
    val hair = when (hairColor.coerceIn(0, 3)) {
        0 -> Color(0xFF24242E)
        1 -> Color(0xFFB2ADBA)
        2 -> Color(0xFF7D3F32)
        else -> Color(0xFFC99B50)
    }
    val skin = when (skinTone.coerceIn(0, 3)) {
        0 -> Color(0xFFEAC6A8)
        1 -> Color(0xFFD7A17F)
        2 -> Color(0xFFA87957)
        else -> Color(0xFF704835)
    }
    val coat = when (outfitStyle.coerceIn(0, 3)) {
        0 -> Color(0xFF50453E)
        1 -> Color(0xFF273A50)
        2 -> Color(0xFF373242)
        else -> Color(0xFF172938)
    }
    val trim = when (accentColor.coerceIn(0, 3)) {
        0 -> Color(0xFFBA9354)
        1 -> Color(0xFF7DAEB0)
        2 -> Color(0xFFB36A72)
        else -> Color(0xFFD9CBB1)
    }
    val hairPath = remember { Path() }
    val facePath = remember { Path() }
    val coatPath = remember { Path() }
    val detailPath = remember { Path() }
    Canvas(modifier.semantics { hideFromAccessibility() }) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val s = min(w, h)
        val line = (s * 0.006f).coerceAtLeast(1.dp.toPx())
        val cx = w * 0.5f

        drawCircle(
            Brush.radialGradient(
                colors = listOf(trim.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(cx, h * 0.45f),
                radius = s * 0.53f
            ),
            radius = s * 0.53f,
            center = Offset(cx, h * 0.45f)
        )
        drawCircle(trim.copy(alpha = 0.38f), radius = s * 0.40f,
            center = Offset(cx, h * 0.48f), style = Stroke(width = line * 0.7f))

        // Hair at back: visible on the shoulders for long and braided silhouettes.
        if (style == 1 || style == 3) {
            hairPath.reset()
            hairPath.moveTo(w * 0.31f, h * 0.29f)
            hairPath.cubicTo(w * 0.18f, h * 0.38f, w * 0.21f, h * 0.75f, w * 0.27f, h * 0.87f)
            hairPath.lineTo(w * 0.73f, h * 0.87f)
            hairPath.cubicTo(w * 0.80f, h * 0.67f, w * 0.84f, h * 0.37f, w * 0.68f, h * 0.29f)
            hairPath.close()
            drawPath(hairPath, hair)
        }

        // Shoulder silhouette, jacket and layered collar.
        coatPath.reset()
        coatPath.moveTo(w * 0.39f, h * 0.66f)
        coatPath.cubicTo(w * 0.22f, h * 0.67f, w * 0.12f, h * 0.76f, w * 0.09f, h * 1.02f)
        coatPath.lineTo(w * 0.91f, h * 1.02f)
        coatPath.cubicTo(w * 0.87f, h * 0.77f, w * 0.78f, h * 0.68f, w * 0.61f, h * 0.66f)
        coatPath.close()
        drawPath(coatPath, Brush.verticalGradient(
            listOf(coat.copy(alpha = 0.96f), coat.copy(alpha = 0.96f), Color(0xFF111922)),
            startY = h * 0.66f, endY = h
        ))
        drawPath(coatPath, trim.copy(alpha = 0.5f), style = Stroke(width = line))
        if (outfitStyle.coerceIn(0, 3) == 3) {
            drawCircle(trim.copy(alpha = 0.55f), radius = s * 0.085f,
                center = Offset(w * 0.19f, h * 0.78f))
            drawCircle(trim.copy(alpha = 0.55f), radius = s * 0.085f,
                center = Offset(w * 0.81f, h * 0.78f))
        }

        // Neck and ears under the face.
        drawRoundRect(skin.copy(alpha = 0.87f),
            topLeft = Offset(w * 0.44f, h * 0.60f), size = Size(w * 0.12f, h * 0.13f),
            cornerRadius = CornerRadius(s * 0.025f))
        drawOval(skin.copy(alpha = 0.82f), topLeft = Offset(w * 0.276f, h * 0.43f),
            size = Size(w * 0.09f, h * 0.13f))
        drawOval(skin.copy(alpha = 0.82f), topLeft = Offset(w * 0.634f, h * 0.43f),
            size = Size(w * 0.09f, h * 0.13f))
        // Gemini peer-design review: taper the chin for an anime portrait silhouette.
        facePath.reset()
        facePath.moveTo(cx, h * 0.246f)
        facePath.cubicTo(w * 0.70f, h * 0.246f, w * 0.69f, h * 0.49f, w * 0.62f, h * 0.585f)
        facePath.cubicTo(w * 0.55f, h * 0.68f, w * 0.45f, h * 0.68f, w * 0.38f, h * 0.585f)
        facePath.cubicTo(w * 0.31f, h * 0.49f, w * 0.30f, h * 0.246f, cx, h * 0.246f)
        facePath.close()
        drawPath(facePath, Brush.verticalGradient(
            listOf(skin.copy(alpha = 0.95f), skin, skin.copy(alpha = 0.86f)),
            startY = h * 0.26f, endY = h * 0.68f
        ))

        // Anime eye design: liner, irises and small reflection, quiet expression.
        for (side in 0..1) {
            val eyeX = if (side == 0) w * 0.407f else w * 0.593f
            drawLine(Color(0xFF28252D), Offset(eyeX - w * 0.045f, h * 0.466f),
                Offset(eyeX + w * 0.045f, h * 0.463f), line * 1.65f)
            drawOval(Color(0xFF394654), Offset(eyeX - w * 0.024f, h * 0.466f),
                Size(w * 0.048f, h * 0.040f))
            drawCircle(Color(0xFF151720), s * 0.009f,
                Offset(eyeX, h * 0.483f))
            drawCircle(Color.White.copy(alpha = 0.84f), s * 0.005f,
                Offset(eyeX - w * 0.010f, h * 0.475f))
            drawLine(hair.copy(alpha = 0.75f), Offset(eyeX - w * 0.040f, h * 0.435f),
                Offset(eyeX + w * 0.035f, h * 0.430f), line * 0.9f)
        }
        drawLine(Color(0xFF824F4A).copy(alpha = 0.56f),
            Offset(w * 0.475f, h * 0.587f), Offset(w * 0.527f, h * 0.586f), line)

        // Eyebrow and forehead hair vary with style; side-part leaves a larger open forehead.
        hairPath.reset()
        hairPath.moveTo(w * 0.31f, h * 0.455f)
        hairPath.cubicTo(w * 0.23f, h * 0.14f, w * 0.78f, h * 0.16f, w * 0.69f, h * 0.458f)
        if (style == 2) {
            hairPath.cubicTo(w * 0.63f, h * 0.40f, w * 0.46f, h * 0.37f, w * 0.31f, h * 0.455f)
        } else if (style == 0) {
            hairPath.cubicTo(w * 0.58f, h * 0.37f, w * 0.46f, h * 0.40f, w * 0.31f, h * 0.455f)
        } else {
            hairPath.cubicTo(w * 0.64f, h * 0.36f, w * 0.39f, h * 0.42f, w * 0.31f, h * 0.455f)
        }
        hairPath.close()
        drawPath(hairPath, hair)
        drawPath(hairPath, trim.copy(alpha = 0.18f), style = Stroke(width = line * 0.6f))

        // Distinctive lapels, embroidered seams and clasp, stable at any screen size.
        detailPath.reset()
        detailPath.moveTo(w * 0.38f, h * 0.68f)
        detailPath.lineTo(w * 0.49f, h * 0.89f)
        detailPath.lineTo(w * 0.30f, h * 0.75f)
        detailPath.moveTo(w * 0.62f, h * 0.68f)
        detailPath.lineTo(w * 0.51f, h * 0.89f)
        detailPath.lineTo(w * 0.70f, h * 0.75f)
        drawPath(detailPath, trim.copy(alpha = 0.81f), style = Stroke(width = line * 1.5f))
        drawCircle(trim, radius = s * 0.022f, center = Offset(cx, h * 0.89f))
        drawCircle(Color(0xFF23323E), radius = s * 0.010f, center = Offset(cx, h * 0.89f))
        drawLine(trim.copy(alpha = 0.50f), Offset(w * 0.25f, h * 0.92f),
            Offset(w * 0.75f, h * 0.92f), line * 0.7f)
    }
}
