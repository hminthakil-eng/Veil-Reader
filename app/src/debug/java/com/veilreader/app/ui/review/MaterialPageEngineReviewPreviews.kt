package com.veilreader.app.ui.review

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.veilreader.app.ui.reader.material.MaterialPageEngineState
import com.veilreader.app.ui.reader.material.MaterialPageOverlay
import com.veilreader.app.ui.reader.material.MaterialPageProfile
import com.veilreader.app.ui.reader.material.MaterialPageProfiles
import com.veilreader.app.ui.reader.material.MaterialPageSide
import com.veilreader.app.ui.reader.material.MaterialPageTone
import com.veilreader.app.ui.reader.material.materialPageToneAdjustedArgb
import com.veilreader.app.ui.screens.SlidePageOverlay
import com.veilreader.app.ui.screens.SlidePageState

private data class MaterialReviewSpec(
    val profile: MaterialPageProfile = MaterialPageProfiles.MatteBook,
    val progress: Float = 0f,
    val verticalBias: Float = 0f,
    val side: MaterialPageSide = MaterialPageSide.RIGHT,
    val tone: MaterialPageTone = MaterialPageTone.LIGHT,
    val patina: Float = 0.35f,
    val reducedMotion: Boolean = false,
    val persian: Boolean = false,
    val largeText: Boolean = false
)

@Composable
private fun MaterialPageReview(spec: MaterialReviewSpec) {
    val bitmap = remember(spec) { reviewPageBitmap(spec) }
    val state = remember(spec.profile.preset, spec.side) {
        MaterialPageEngineState(spec.profile)
    }

    SideEffect {
        state.installInspectableFrame(
            bitmap = bitmap,
            progress = spec.progress,
            verticalBias = spec.verticalBias,
            side = spec.side,
            profile = spec.profile,
            reducedMotion = spec.reducedMotion,
            patina = spec.patina,
            tone = spec.tone
        )
    }

    DisposableEffect(state, bitmap) {
        onDispose {
            state.clearImmediately()
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(reviewDestinationColor(spec.tone))
    ) {
        MaterialPageOverlay(
            state = state,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SlideReview(
    offsetFraction: Float,
    rtl: Boolean = false,
    persian: Boolean = false,
    largeText: Boolean = false
) {
    val spec = remember(rtl, persian, largeText) {
        MaterialReviewSpec(
            side = if (rtl) MaterialPageSide.LEFT else MaterialPageSide.RIGHT,
            persian = persian,
            largeText = largeText
        )
    }
    val bitmap = remember(spec) { reviewPageBitmap(spec) }
    val state = remember { SlidePageState() }

    SideEffect {
        val signed = if (rtl) {
            kotlin.math.abs(offsetFraction)
        } else {
            -kotlin.math.abs(offsetFraction)
        }
        state.installInspectableFrame(bitmap, signed)
    }

    DisposableEffect(state, bitmap) {
        onDispose {
            state.clearImmediately()
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFE9E2D5))
    ) {
        SlidePageOverlay(
            state = state,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun reviewPageBitmap(spec: MaterialReviewSpec): Bitmap {
    val width = 720
    val height = 1080
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val front = materialPageToneAdjustedArgb(
        spec.profile.optics.frontArgb,
        spec.tone
    ).toInt()

    canvas.drawColor(front)

    val ink = if (spec.tone == MaterialPageTone.DARK) {
        0xFFEDE6D8.toInt()
    } else {
        0xFF2B2926.toInt()
    }
    val faint = if (spec.tone == MaterialPageTone.DARK) {
        0xFF918B82.toInt()
    } else {
        0xFF7B7469.toInt()
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = if (spec.largeText) 54f else 40f
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.BOLD
        )
        textAlign = if (spec.persian) Paint.Align.RIGHT else Paint.Align.LEFT
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        alpha = 218
        textSize = if (spec.largeText) 38f else 28f
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.NORMAL
        )
        textAlign = if (spec.persian) Paint.Align.RIGHT else Paint.Align.LEFT
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faint
        textSize = 18f
        letterSpacing = 0.08f
        textAlign = if (spec.persian) Paint.Align.RIGHT else Paint.Align.LEFT
    }

    val x = if (spec.persian) width - 74f else 74f
    canvas.drawText(
        if (spec.persian) "فصل دوازدهم" else "CHAPTER TWELVE",
        x,
        116f,
        metaPaint
    )
    canvas.drawText(
        if (spec.persian) "در آستانهٔ مه" else "At the Threshold of Fog",
        x,
        188f,
        titlePaint
    )

    val lines = if (spec.persian) {
        listOf(
            "در سکوت کتابخانه، کاغذ زیر انگشت",
            "وزن خودش را نشان می‌داد؛ نه مثل یک",
            "افکت، بلکه مثل سطحی واقعی و آرام.",
            "نور روی لبه تغییر می‌کرد و صفحه",
            "با مقاومت کوتاهی از بند جدا می‌شد.",
            "متن همیشه مهم‌تر از جلوه باقی می‌ماند."
        )
    } else {
        listOf(
            "The page answers the hand with quiet resistance.",
            "Its edge catches light before the sheet yields.",
            "Nothing theatrical interrupts the act of reading.",
            "Weight, grain and release remain restrained.",
            "The publication stays dominant throughout motion.",
            "A material surface, not a decorative transition."
        )
    }

    val baselineStart = 300f
    val lineGap = if (spec.largeText) 72f else 58f
    lines.forEachIndexed { index, line ->
        canvas.drawText(line, x, baselineStart + index * lineGap, bodyPaint)
    }

    val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faint
        alpha = 72
        strokeWidth = 2f
    }
    canvas.drawLine(74f, 244f, width - 74f, 244f, rulePaint)
    canvas.drawLine(74f, height - 92f, width - 74f, height - 92f, rulePaint)

    return bitmap
}

private fun reviewDestinationColor(tone: MaterialPageTone): Color =
    when (tone) {
        MaterialPageTone.LIGHT -> Color(0xFFEFE7D9)
        MaterialPageTone.SEPIA -> Color(0xFFE5D0A8)
        MaterialPageTone.DARK -> Color(0xFF151318)
    }

@Preview(name = "Material · Idle", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialIdle() = MaterialPageReview(
    MaterialReviewSpec(progress = 0f)
)

@Preview(name = "Material · Slight corner lift", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialSlightLift() = MaterialPageReview(
    MaterialReviewSpec(progress = 0.10f, verticalBias = 0.04f)
)

@Preview(name = "Material · Medium drag", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialMediumDrag() = MaterialPageReview(
    MaterialReviewSpec(progress = 0.36f, verticalBias = 0.06f)
)

@Preview(name = "Material · Deep curl", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialDeepCurl() = MaterialPageReview(
    MaterialReviewSpec(progress = 0.68f, verticalBias = 0.08f)
)

@Preview(name = "Material · Almost complete", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialAlmostComplete() = MaterialPageReview(
    MaterialReviewSpec(progress = 0.91f)
)

@Preview(name = "Material · Complete release", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialCompleteRelease() = MaterialPageReview(
    MaterialReviewSpec(progress = 0.99f)
)

@Preview(name = "Material · Cancel release", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialCancelRelease() = MaterialPageReview(
    MaterialReviewSpec(progress = 0.15f, verticalBias = -0.04f)
)

@Preview(name = "Material · Fast flick", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialFastFlick() = MaterialPageReview(
    MaterialReviewSpec(
        profile = MaterialPageProfiles.Glossy,
        progress = 0.74f,
        verticalBias = 0.02f
    )
)

@Preview(name = "Material · Slow heavy drag", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialSlowHeavyDrag() = MaterialPageReview(
    MaterialReviewSpec(
        profile = MaterialPageProfiles.Parchment,
        progress = 0.44f,
        verticalBias = 0.10f
    )
)

@Preview(name = "Material · Glossy", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialGlossy() = MaterialPageReview(
    MaterialReviewSpec(profile = MaterialPageProfiles.Glossy, progress = 0.52f)
)

@Preview(name = "Material · Matte book", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialMatte() = MaterialPageReview(
    MaterialReviewSpec(profile = MaterialPageProfiles.MatteBook, progress = 0.52f)
)

@Preview(name = "Material · Parchment", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialParchment() = MaterialPageReview(
    MaterialReviewSpec(profile = MaterialPageProfiles.Parchment, progress = 0.52f)
)

@Preview(name = "Material · Papyrus", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialPapyrus() = MaterialPageReview(
    MaterialReviewSpec(profile = MaterialPageProfiles.Papyrus, progress = 0.52f)
)

@Preview(name = "Material · Manuscript", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialManuscript() = MaterialPageReview(
    MaterialReviewSpec(profile = MaterialPageProfiles.Manuscript, progress = 0.52f)
)

@Preview(name = "Material · Aged", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialAged() = MaterialPageReview(
    MaterialReviewSpec(
        profile = MaterialPageProfiles.Parchment,
        progress = 0.52f,
        patina = 1f
    )
)

@Preview(name = "Material · Clean new", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialCleanNew() = MaterialPageReview(
    MaterialReviewSpec(
        profile = MaterialPageProfiles.MatteBook,
        progress = 0.52f,
        patina = 0f
    )
)

@Preview(name = "Material · LTR", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialLtr() = MaterialPageReview(
    MaterialReviewSpec(progress = 0.48f, side = MaterialPageSide.RIGHT)
)

@Preview(name = "Material · RTL", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialRtl() = MaterialPageReview(
    MaterialReviewSpec(progress = 0.48f, side = MaterialPageSide.LEFT)
)

@Preview(name = "Material · Persian", widthDp = 360, heightDp = 640, locale = "fa")
@Composable
private fun MaterialPersian() = MaterialPageReview(
    MaterialReviewSpec(
        progress = 0.50f,
        side = MaterialPageSide.LEFT,
        persian = true
    )
)

@Preview(name = "Material · Dark", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialDark() = MaterialPageReview(
    MaterialReviewSpec(
        progress = 0.50f,
        tone = MaterialPageTone.DARK
    )
)

@Preview(name = "Material · Sepia", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialSepia() = MaterialPageReview(
    MaterialReviewSpec(
        profile = MaterialPageProfiles.Parchment,
        progress = 0.50f,
        tone = MaterialPageTone.SEPIA,
        patina = 0.72f
    )
)

@Preview(name = "Material · Reduced motion", widthDp = 360, heightDp = 640)
@Composable
private fun MaterialReducedMotion() = MaterialPageReview(
    MaterialReviewSpec(
        progress = 0.50f,
        reducedMotion = true
    )
)

@Preview(name = "Slide · Idle", widthDp = 360, heightDp = 640)
@Composable
private fun SlideIdle() = SlideReview(offsetFraction = 0f)

@Preview(name = "Slide · Active", widthDp = 360, heightDp = 640)
@Composable
private fun SlideActive() = SlideReview(offsetFraction = 0.46f)

@Preview(name = "Slide · Completed", widthDp = 360, heightDp = 640)
@Composable
private fun SlideCompleted() = SlideReview(offsetFraction = 0.98f)

@Preview(name = "Slide · RTL", widthDp = 360, heightDp = 640, locale = "ar")
@Composable
private fun SlideRtl() = SlideReview(offsetFraction = 0.46f, rtl = true)

@Preview(name = "Slide · Large text", widthDp = 360, heightDp = 640, fontScale = 1.5f)
@Composable
private fun SlideLargeText() = SlideReview(
    offsetFraction = 0.46f,
    largeText = true
)

@Preview(name = "Slide · Persian", widthDp = 360, heightDp = 640, locale = "fa")
@Composable
private fun SlidePersian() = SlideReview(
    offsetFraction = 0.46f,
    rtl = true,
    persian = true
)
