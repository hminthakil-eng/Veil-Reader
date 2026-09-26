package com.veilreader.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.ui.VeilBrandMark
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing

@Composable
fun WelcomeScreen(
    onEnter: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(VeilPalette.Ink)
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VeilPalette.Ink.copy(alpha = 0.18f),
                            VeilPalette.Ink.copy(alpha = 0.34f),
                            VeilPalette.Ink.copy(alpha = 0.96f)
                        )
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            VeilPalette.Ink.copy(alpha = 0.44f),
                            Color.Transparent,
                            VeilPalette.Ink.copy(alpha = 0.30f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = VeilSpacing.xl, vertical = VeilSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            VeilBrandMark()
            Spacer(Modifier.height(VeilSpacing.md))
            Text(
                "VEIL READER",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.4.sp),
                color = VeilPalette.Brass
            )
            BrassRule(Modifier.width(92.dp), strong = true)
            Spacer(Modifier.height(VeilSpacing.sm))
            Text(
                "The Library Beyond Time",
                style = MaterialTheme.typography.displayLarge,
                color = VeilPalette.Moon,
                textAlign = TextAlign.Center
            )
            Text(
                "Books are gateways. Keep them private, local, and always ready to reopen.",
                style = MaterialTheme.typography.bodyLarge,
                color = VeilPalette.Moon.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp)
            )
            Spacer(Modifier.height(VeilSpacing.xl))

            Button(
                onClick = onEnter,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .heightIn(min = 54.dp)
                    .border(
                        BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.74f)),
                        RoundedCornerShape(5.dp)
                    ),
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VeilPalette.Ink.copy(alpha = 0.86f),
                    contentColor = VeilPalette.Moon
                )
            ) {
                Text("Enter the Library")
            }
        }
    }
}
