package com.example.yogaapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FigurePlate = Color(0xFFF4F5F7)

/**
 * Студийное фото позы из `assets/poses/{id}.jpg` с «живой» анимацией:
 *  - плавный переход между позами (fade + scale),
 *  - заметное дыхание / Ken-Burns (зум + мягкое покачивание; у силовых `str_*` ритм бодрее),
 *  - пульсирующее свечение-ореол в ритм вдоха-выдоха на средних/крупных карточках.
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
    val isCompact = size <= 56.dp
    val isStrength = pose.id.startsWith("str_")
    val cycleMs = if (isStrength) 2200 else 4200

    val transition = rememberInfiniteTransition(label = "posePhotoLife")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val auraPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "auraPulse",
    )

    val angle = phase * 2.0 * PI
    val zoomAmp = when {
        !animated -> 0f
        isCompact -> 0.025f
        isStrength -> 0.055f
        else -> 0.042f
    }
    val baseZoom = if (animated) 1.045f else 1f
    val scale = baseZoom + sin(angle).toFloat() * zoomAmp
    val shiftX = if (animated && !isCompact) cos(angle).toFloat() * 4.5f else 0f
    val shiftY = if (animated && !isCompact) sin(angle).toFloat() * -5.5f else 0f
    val tiltDeg = if (animated && !isCompact) sin(angle).toFloat() * 1.1f else 0f

    val corner = if (isCompact) 8.dp else 24.dp
    val shape = RoundedCornerShape(corner)
    val primary = MaterialTheme.colorScheme.primary
    val borderAlpha = if (animated && !isCompact) 0.22f + auraPulse * 0.48f else 0.15f
    val borderWidth = if (isCompact) 1.dp else (1.5f + auraPulse * 1.5f).dp

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (!isCompact && animated) {
                    Modifier
                        .border(
                            width = borderWidth,
                            color = primary.copy(alpha = borderAlpha),
                            shape = shape,
                        )
                        .padding(3.dp)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(FigurePlate)
            .semantics { contentDescription = "Иллюстрация позы: ${pose.ruName}" },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = pose.id,
            transitionSpec = {
                (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.92f)) togetherWith
                    (fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 1.06f))
            },
            label = "poseCrossfade",
        ) { targetPoseId ->
            PoseAssetBitmap(
                poseId = targetPoseId,
                scale = scale,
                shiftX = shiftX,
                shiftY = shiftY,
                tiltDeg = tiltDeg,
            )
        }

        // Soft breathing light vignette on medium/large figures.
        if (animated && !isCompact) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glowAlpha = 0.06f + auraPulse * 0.14f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            primary.copy(alpha = glowAlpha),
                        ),
                        center = Offset(this.size.width * 0.5f, this.size.height * 0.45f),
                        radius = this.size.minDimension * 0.68f,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PoseAssetBitmap(
    poseId: String,
    scale: Float,
    shiftX: Float,
    shiftY: Float,
    tiltDeg: Float,
) {
    val context = LocalContext.current
    val bytes by produceState<ByteArray?>(initialValue = null, poseId) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("poses/$poseId.jpg").use { it.readBytes() }
            }.getOrNull()
        }
    }
    val request = remember(bytes) {
        bytes?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(180)
                .build()
        }
    }
    if (request != null) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = shiftX
                    translationY = shiftY
                    rotationZ = tiltDeg
                },
        )
    }
}
