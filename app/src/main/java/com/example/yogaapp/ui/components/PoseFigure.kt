package com.example.yogaapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.yogaapp.domain.model.YogaPose
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FigurePlate = Color(0xFFF4F5F7)

/**
 * Студийное фото позы из `assets/poses/{id}.jpg` (стиль референса).
 * Лёгкая анимация дыхания масштабом.
 */
@Composable
fun PoseFigure(
    pose: YogaPose,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    @Suppress("UNUSED_PARAMETER") strokeColor: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") fillColor: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") strokeWidth: Dp = 6.dp,
    animated: Boolean = true,
) {
    val context = LocalContext.current
    val bytes by produceState<ByteArray?>(initialValue = null, pose.id) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("poses/${pose.id}.jpg").use { it.readBytes() }
            }.getOrNull()
        }
    }
    val request = remember(bytes) {
        bytes?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(220)
                .build()
        }
    }

    val transition = rememberInfiniteTransition(label = "posePhotoLife")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breath",
    )
    val scale = if (animated) {
        1f + sin(breath * 2.0 * PI).toFloat() * 0.012f
    } else {
        1f
    }

    val corner = if (size <= 56.dp) 8.dp else 24.dp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(FigurePlate)
            .semantics { contentDescription = "Иллюстрация позы: ${pose.ruName}" },
        contentAlignment = Alignment.Center,
    ) {
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale),
            )
        }
    }
}
