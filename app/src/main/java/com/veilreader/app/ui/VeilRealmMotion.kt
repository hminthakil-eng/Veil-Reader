package com.veilreader.app.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotionClass
import com.veilreader.app.ui.theme.effectiveMotionDurationMs
import com.veilreader.app.ui.theme.motionBudgetFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

internal val LocalVeilSharedTransitionScope =
    compositionLocalOf<SharedTransitionScope?> { null }

internal val LocalVeilAnimatedVisibilityScope =
    compositionLocalOf<AnimatedVisibilityScope?> { null }

internal fun hallSharedBoundsKey(route: String): String {
    val clean = route.trim()
    require(clean.isNotEmpty()) { "Shared realm route must not be blank." }
    return "hall:$clean"
}

internal data class VeilRealmMotionPolicy(
    val enterDurationMs: Int,
    val exitDurationMs: Int,
    val sharedBoundsDurationMs: Int,
    val predictiveScaleAtCommit: Float,
    val predictiveAlphaAtCommit: Float,
    val predictiveTranslationFractionAtCommit: Float
)

internal fun veilRealmMotionPolicy(
    reducedMotion: Boolean
): VeilRealmMotionPolicy =
    if (reducedMotion) {
        VeilRealmMotionPolicy(
            enterDurationMs = effectiveMotionDurationMs(
                VeilMotionClass.REALM,
                reducedMotion = true
            ),
            exitDurationMs = effectiveMotionDurationMs(
                VeilMotionClass.SPATIAL,
                reducedMotion = true
            ),
            sharedBoundsDurationMs = 0,
            predictiveScaleAtCommit = 1f,
            predictiveAlphaAtCommit = 1f,
            predictiveTranslationFractionAtCommit = 0f
        )
    } else {
        VeilRealmMotionPolicy(
            enterDurationMs = motionBudgetFor(VeilMotionClass.REALM).targetDurationMs,
            exitDurationMs = motionBudgetFor(VeilMotionClass.SPATIAL).targetDurationMs,
            sharedBoundsDurationMs = motionBudgetFor(VeilMotionClass.SPATIAL).targetDurationMs,
            predictiveScaleAtCommit = 0.955f,
            predictiveAlphaAtCommit = 0.34f,
            predictiveTranslationFractionAtCommit = 0.075f
        )
    }

@Composable
fun Modifier.veilSharedBounds(
    key: String
): Modifier {
    val reducedMotion = LocalVeilReducedMotion.current
    val sharedScope = LocalVeilSharedTransitionScope.current
    val visibilityScope = LocalVeilAnimatedVisibilityScope.current

    if (reducedMotion || sharedScope == null || visibilityScope == null) {
        return this
    }

    val policy = remember { veilRealmMotionPolicy(reducedMotion = false) }

    return with(sharedScope) {
        this@veilSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
            enter = fadeIn(tween(policy.sharedBoundsDurationMs)),
            exit = fadeOut(tween(policy.sharedBoundsDurationMs)),
            boundsTransform = BoundsTransform { _, _ ->
                tween(policy.sharedBoundsDurationMs)
            }
        )
    }
}

/**
 * Owns motion between the normal world shell and a full-screen world chamber.
 *
 * Reader/Readium is intentionally not hosted here. Reader remains a controlled handoff because
 * shared-element interop does not cross the Compose/View boundary safely.
 */
@Composable
fun VeilRealmMotionHost(
    activeChamber: String?,
    onCloseChamber: () -> Unit,
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
    chamberContent: @Composable (String) -> Unit
) {
    val reducedMotion = LocalVeilReducedMotion.current
    val policy = remember(reducedMotion) {
        veilRealmMotionPolicy(reducedMotion)
    }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackDirection by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(activeChamber) {
        if (activeChamber == null) {
            predictiveBackProgress = 0f
            predictiveBackDirection = 1f
        }
    }

    PredictiveBackHandler(enabled = activeChamber != null) { progress ->
        try {
            progress.collect { event ->
                predictiveBackProgress = event.progress.coerceIn(0f, 1f)
                predictiveBackDirection =
                    if (event.swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
            }
            predictiveBackProgress = 1f
            onCloseChamber()
        } catch (cancelled: CancellationException) {
            predictiveBackProgress = 0f
            predictiveBackDirection = 1f
            throw cancelled
        }
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalVeilSharedTransitionScope provides this
        ) {
            AnimatedContent(
                targetState = activeChamber,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(
                            tween(policy.enterDurationMs)
                        ) togetherWith fadeOut(
                            tween(policy.exitDurationMs)
                        ) using SizeTransform(clip = false)
                    } else {
                        (
                            fadeIn(tween(policy.enterDurationMs)) +
                                scaleIn(
                                    animationSpec = tween(policy.enterDurationMs),
                                    initialScale = 0.985f
                                )
                            ) togetherWith (
                            fadeOut(tween(policy.exitDurationMs)) +
                                scaleOut(
                                    animationSpec = tween(policy.exitDurationMs),
                                    targetScale = 0.992f
                                )
                            ) using SizeTransform(clip = false)
                    }
                },
                label = "veil-realm-host"
            ) { chamber ->
                CompositionLocalProvider(
                    LocalVeilAnimatedVisibilityScope provides this
                ) {
                    val progress =
                        if (chamber == null || reducedMotion) 0f
                        else predictiveBackProgress
                    val scale =
                        1f - progress * (1f - policy.predictiveScaleAtCommit)
                    val alpha =
                        1f - progress * (1f - policy.predictiveAlphaAtCommit)

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (
                            chamber != null &&
                            !reducedMotion &&
                            predictiveBackProgress > 0f
                        ) {
                            val previewProgress = predictiveBackProgress
                            CompositionLocalProvider(
                                LocalVeilSharedTransitionScope provides null,
                                LocalVeilAnimatedVisibilityScope provides null
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clearAndSetSemantics { }
                                        .graphicsLayer {
                                            val previewScale =
                                                0.985f + previewProgress * 0.015f
                                            scaleX = previewScale
                                            scaleY = previewScale
                                            this.alpha =
                                                0.30f + previewProgress * 0.70f
                                        }
                                ) {
                                    mainContent()
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX =
                                        predictiveBackDirection *
                                            progress *
                                            size.width *
                                            policy.predictiveTranslationFractionAtCommit
                                    this.alpha = alpha
                                }
                        ) {
                            if (chamber == null) {
                                mainContent()
                            } else {
                                chamberContent(chamber)
                            }
                        }
                    }
                }
            }
        }
    }
}
