package com.veilreader.app.ui.station

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veilreader.app.MainActivity
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilTheme
import io.github.sceneview.SceneView
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Debug-source-set-only true 3D experiment.
 *
 * Never enable in the shipping manifest, never start ReaderScreen or move
 * book text into GPU. SceneView owns native rendering and releases its scene
 * with the Activity lifecycle; the real device must still verify context loss.
 */
class Station3DLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VeilTheme(AppThemeMode.DARK) {
                Station3DLabScreen(
                    onClose = { finish() },
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
internal fun Station3DLabScreen(
    onClose: () -> Unit,
    onOpenApp: () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = LocalVeilReducedMotion.current
    val highContrast = LocalVeilHighContrast.current
    val powerManager = remember(context) {
        context.getSystemService(PowerManager::class.java)
    }
    val powerSaving = powerManager?.isPowerSaveMode == true
    val allowGpu = !reducedMotion && !highContrast && !powerSaving

    var requestedRotation by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = if (allowGpu) requestedRotation else 0f,
        animationSpec = tween(durationMillis = if (allowGpu) 380 else 0),
        label = "station3d-plaque-turn-once"
    )

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "VEIL READER — STATION 3D LAB",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "An original 3D enamel railway sign. Swipe the object to inspect depth. " +
                    "This is a debug-only technology test; it does not contain your books.",
                style = MaterialTheme.typography.bodyMedium
            )

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (allowGpu) {
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    SceneView(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        cameraManipulator = rememberCameraManipulator()
                    ) {
                        rememberModelInstance(modelLoader, "models/veil_victorian_sign.gltf")?.let { instance ->
                            ModelNode(
                                modelInstance = instance,
                                scaleToUnits = 2.3f,
                                rotation = Rotation(y = rotation),
                                autoAnimate = false
                            )
                        }
                    }
                } else {
                    Text(
                        text = "3D paused for reduced motion, high contrast, or power saving. " +
                            "The main reader and library remain available.",
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { requestedRotation = if (requestedRotation == 0f) 20f else 0f },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = allowGpu
                ) { Text("Tilt sign") }
                OutlinedButton(
                    onClick = { requestedRotation = 0f },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = allowGpu
                ) { Text("Reset") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("Close lab") }
                Button(
                    onClick = onOpenApp,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("Open reader") }
            }
            Text(
                text = "Technology preview • No performance or visual approval yet",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
