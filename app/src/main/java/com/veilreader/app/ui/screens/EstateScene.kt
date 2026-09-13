package com.veilreader.app.ui.screens

import com.veilreader.app.ui.theme.LocalVeilMotion
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.veilreader.app.domain.*

/** Vector artwork drawn from durable construction state, with no network or bitmap cache. */
@Composable
fun EstateScene(estate: EstateState, modifier: Modifier = Modifier, reduceMotion: Boolean = false) {
    val motion = LocalVeilMotion.current
    Crossfade(
        targetState = estate,
        animationSpec = tween(if (reduceMotion) 0 else motion.duration(650)),
        label = "estate-construction",
        modifier = modifier.fillMaxWidth().aspectRatio(1.36f).semantics {
            contentDescription = "${estate.name}: ${EstateCampaign.stages[estate.stage].name}, ${estate.palette.label}, ${estate.grounds.label}, ${estate.sky.label}"
        }
    ) { state ->
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.36f)) {
            withTransform({ scale(size.width / 400f, size.height / 294f, Offset.Zero) }) {
                drawEstate(state)
            }
        }
    }
}

private fun DrawScope.drawEstate(state: EstateState) {
    val dawn = state.sky == EstateSky.DAWN
    val mist = state.sky == EstateSky.MIST
    val skyTop = if (dawn) Color(0xFF69638B) else if (mist) Color(0xFF354C61) else Color(0xFF101A31)
    val skyBottom = if (dawn) Color(0xFFE7B394) else Color(0xFF45526B)
    drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom), endY = 280f), size = Size(400f, 294f))
    val moon = if (dawn) Color(0xFFFFD9A2) else Color(0xFFF5E8BC)
    drawCircle(moon.copy(alpha = .06f), 58f, Offset(315f, 62f))
    drawCircle(moon.copy(alpha = .08f), 42f, Offset(315f, 62f))
    drawCircle(moon, if (dawn) 22f else 19f, Offset(315f, 62f))
    if (!dawn) {
        if (!mist) repeat(37) { i ->
            val x = ((i * 79 + 19) % 393).toFloat()
            val y = ((i * 43 + 13) % 150).toFloat()
            drawCircle(Color(0xFFE8DABF).copy(alpha = .25f + (i % 3) * .15f), if (i % 6 == 0) 1.2f else .65f, Offset(x, y))
        }
        drawCircle(skyTop, 18f, Offset(322f, 55f))
    }
    polygon(Color(0xFF3A465B), 0f, 173f, 58f, 111f, 105f, 155f, 175f, 91f, 248f, 158f, 292f, 128f, 400f, 175f, 400f, 294f, 0f, 294f)
    polygon(Color(0xFF2B3D4C), 0f, 194f, 51f, 167f, 124f, 184f, 226f, 154f, 302f, 183f, 355f, 156f, 400f, 181f, 400f, 294f, 0f, 294f)
    repeat(12) { i -> pine(10f + i * 37f, 220f, 32f + (i % 4) * 8f, Color(0xFF1C3440)) }
    drawOval(Color(0xFF243D43), Offset(-45f, 197f), Size(490f, 146f))
    drawOval(Color(0xFF354E49), Offset(45f, 211f), Size(308f, 53f))
    polygon(Color(0xFF9F9583).copy(alpha = .7f), 183f, 241f, 218f, 241f, 262f, 294f, 137f, 294f)
    val stone = when (state.palette) {
        EstatePalette.MOONSTONE -> Color(0xFFA5B0BE)
        EstatePalette.AMBER -> Color(0xFFC2AA8C)
        EstatePalette.WISTERIA -> Color(0xFFAEA0BD)
    }
    val shadow = when (state.palette) {
        EstatePalette.MOONSTONE -> Color(0xFF667589)
        EstatePalette.AMBER -> Color(0xFF8A705A)
        EstatePalette.WISTERIA -> Color(0xFF7D6D91)
    }
    val roof = when (state.palette) {
        EstatePalette.MOONSTONE -> Color(0xFF273D58)
        EstatePalette.AMBER -> Color(0xFF63444C)
        EstatePalette.WISTERIA -> Color(0xFF4D385F)
    }
    val light = Color(0xFFFFD38A)
    val tier = state.stage
    drawOval(Color.Black.copy(alpha = .22f), Offset(82f, 232f), Size(240f, 22f))
    if (tier == 0) {
        drawRect(Color(0xFF665954), Offset(147f, 178f), Size(105f, 66f))
        drawRect(Color(0xFF867569), Offset(147f, 178f), Size(83f, 66f))
        repeat(7) { i -> drawLine(Color(0xFF514A49), Offset(147f, 181f + i * 9), Offset(230f, 181f + i * 9), 1f) }
        polygon(Color(0xFF3F464E), 133f, 182f, 183f, 144f, 254f, 169f, 265f, 183f)
        drawLine(Color(0xFF9A8B75), Offset(147f, 176f), Offset(180f, 151f), 3f)
        drawLine(Color(0xFFB3A08A), Offset(222f, 174f), Offset(238f, 181f), 4f)
        door(187f, 208f, 21f, 36f, Color(0xFF3C3335))
        window(158f, 194f, 16f, 19f, light)
        drawRect(Color(0xFF7E6960), Offset(240f, 162f), Size(9f, 16f))
        drawLine(Color(0xFF988974), Offset(228f, 239f), Offset(242f, 218f), 4f)
    } else {
        if (tier >= 4) tower(if (tier >= 5) 95f else 253f, if (tier == 6) 83f else 112f, 47f, 131f, stone, shadow, roof, light, tier >= 5)
        if (tier >= 5) tower(267f, 101f, 43f, 143f, stone, shadow, roof, light, true)
        if (tier >= 6) tower(175f, 67f, 47f, 126f, stone, shadow, roof, light, false)
        if (tier >= 3) {
            house(111f, 181f, 70f, 62f, stone, shadow, roof, light)
            house(234f, 180f, 66f, 63f, stone, shadow, roof, light)
        }
        val top = if (tier >= 2) 144f else 182f
        house(148f, top, 105f, 244f - top, stone, shadow, roof, light)
        door(189f, 210f, 25f, 35f, Color(0xFF443C43))
        drawCircle(Color(0xFFC8AD73), 1.5f, Offset(207f, 231f))
        if (tier >= 2) { window(164f, 160f, 17f, 24f, light); window(217f, 160f, 17f, 24f, light) }
        if (tier >= 3) {
            repeat(5) { i -> drawCircle(Color(0xFF7F889C), 5f, Offset(148f, 174f + i * 10)) }
            repeat(8) { i -> drawCircle(Color(0xFFACA0C3), 2.5f, Offset(142f + (i % 2) * 7, 181f + i * 6)) }
        }
        if (tier >= 5) {
            drawRect(shadow, Offset(120f, 221f), Size(160f, 24f))
            repeat(13) { i -> drawRect(stone, Offset(120f + i * 12f, 216f), Size(8f, 12f)) }
            door(181f, 218f, 37f, 30f, Color(0xFF263A40))
            drawLine(Color(0xFFDAC199), Offset(182f, 222f), Offset(182f, 245f), 2f)
        }
        drawRect(shadow, Offset(218f, top - 28f), Size(13f, 23f))
        drawRect(stone, Offset(215f, top - 29f), Size(19f, 5f))
    }
    when (state.grounds) {
        EstateGrounds.WILDFLOWERS -> repeat(25) { i ->
            val x = if (i % 2 == 0) 82f + (i * 13) % 62 else 267f + (i * 7) % 49
            val y = 251f + (i * 11) % 26
            drawLine(Color(0xFF6B886C), Offset(x, y), Offset(x, y - 5), 1f)
            drawCircle(if (i % 3 == 0) Color(0xFFECD5A8) else Color(0xFFAAADD1), 1.8f, Offset(x, y - 6))
        }
        EstateGrounds.LANTERNS -> repeat(6) { i ->
            val row = i / 2
            val x = if (i % 2 == 0) 175f - row * 18 else 226f + row * 18
            val y = 252f + row * 13
            drawLine(Color(0xFF23303B), Offset(x, y), Offset(x, y - 19), 2f)
            drawCircle(light.copy(alpha = .08f), 13f, Offset(x, y - 17))
            drawRect(light, Offset(x - 2.5f, y - 22), Size(5f, 8f))
            polygon(Color(0xFF2A3640), x - 5, y - 22, x, y - 26, x + 5, y - 22)
        }
        EstateGrounds.FOUNTAIN -> {
            drawOval(shadow, Offset(265f, 253f), Size(52f, 17f))
            drawOval(Color(0xFF86ADB9), Offset(270f, 254f), Size(42f, 10f))
            drawRect(stone, Offset(288f, 235f), Size(6f, 25f))
            drawOval(stone, Offset(280f, 233f), Size(23f, 6f))
            drawLine(Color(0xFFCEE7E8).copy(alpha = .8f), Offset(291f, 222f), Offset(291f, 235f), 1.5f)
        }
    }
    // Foreground framing and quiet fog add depth without an always-running animation.
    pine(21f, 300f, 103f, Color(0xFF142E33)); pine(378f, 308f, 122f, Color(0xFF173337))
    if (mist) {
        drawOval(Color(0xFFA4BCCB).copy(alpha = .16f), Offset(-90f, 189f), Size(335f, 35f))
        drawOval(Color(0xFFA4BCCB).copy(alpha = .15f), Offset(216f, 209f), Size(300f, 35f))
    }
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0F202B).copy(alpha = .42f)), startY = 248f, endY = 294f), size = Size(400f, 294f))
}

private fun DrawScope.polygon(color: Color, vararg points: Float) {
    val path = Path().apply { moveTo(points[0], points[1]); for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1]); close() }
    drawPath(path, color)
}
private fun DrawScope.pine(x: Float, base: Float, height: Float, color: Color) {
    drawLine(color, Offset(x, base), Offset(x, base - height), 3f)
    repeat(4) { i -> val y = base - height + i * height * .18f; val w = height * (.13f + i * .035f)
        polygon(color, x, y, x - w, y + height * .38f, x + w, y + height * .38f)
    }
}
private fun DrawScope.window(x: Float, y: Float, w: Float, h: Float, light: Color) {
    drawCircle(light.copy(alpha = .055f), w * 1.2f, Offset(x + w / 2, y + h / 2))
    drawRect(Color(0xFF3A3A47), Offset(x - 2, y - 2), Size(w + 4, h + 4))
    drawRect(light, Offset(x, y), Size(w, h))
    drawLine(Color(0xFF7F756A), Offset(x + w / 2, y), Offset(x + w / 2, y + h), 1.6f)
    drawLine(Color(0xFF7F756A), Offset(x, y + h / 2), Offset(x + w, y + h / 2), 1.6f)
}
private fun DrawScope.door(x: Float, y: Float, w: Float, h: Float, color: Color) {
    drawRect(color, Offset(x, y + w / 2), Size(w, h - w / 2))
    drawArc(color, 180f, 180f, true, Offset(x, y), Size(w, w))
}
private fun DrawScope.house(x: Float, y: Float, w: Float, h: Float, stone: Color, shadow: Color, roof: Color, light: Color) {
    drawRect(stone, Offset(x, y), Size(w, h))
    drawRect(shadow, Offset(x + w * .78f, y), Size(w * .22f, h))
    repeat((h / 11).toInt()) { i -> drawLine(shadow.copy(alpha = .35f), Offset(x, y + 5 + i * 11), Offset(x + w, y + 5 + i * 11), .8f) }
    polygon(roof, x - 10, y + 2, x + w / 2, y - 31, x + w + 10, y + 2)
    drawLine(stone.copy(alpha = .65f), Offset(x - 10, y + 2), Offset(x + w / 2, y - 31), 1.5f)
    window(x + 13, y + h - 42, 15f, 20f, light)
    if (w > 80) window(x + w - 33, y + h - 42, 15f, 20f, light)
}
private fun DrawScope.tower(x: Float, y: Float, w: Float, h: Float, stone: Color, shadow: Color, roof: Color, light: Color, banner: Boolean) {
    drawRect(stone, Offset(x, y), Size(w, h))
    drawRect(shadow, Offset(x + w * .68f, y), Size(w * .32f, h))
    polygon(roof, x - 8, y + 1, x + w / 2, y - 37, x + w + 8, y + 1)
    repeat(3) { i -> window(x + w * .35f, y + 18 + i * 33, 10f, 18f, light) }
    repeat(6) { i -> drawLine(shadow.copy(alpha = .35f), Offset(x, y + 15 + i * 20), Offset(x + w, y + 15 + i * 20), 1f) }
    if (banner) {
        drawLine(Color(0xFFDFCA9A), Offset(x + w / 2, y - 34), Offset(x + w / 2, y - 57), 1.5f)
        polygon(Color(0xFFB993BB), x + w / 2, y - 57, x + w / 2 + 22, y - 52, x + w / 2, y - 45)
    }
}
