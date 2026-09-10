package com.veilreader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenHeader(eyebrow: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            eyebrow.uppercase(),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        subtitle?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MysteryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun BookCover(title: String, modifier: Modifier = Modifier) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF4D315F), Color(0xFF17101E), Color(0xFF0A090C))
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .padding(12.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            maxLines = 3
        )
    }
}
