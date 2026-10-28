package com.example.yogaapp.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yogaapp.R
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private val SplashInk = Color(0xFF1B1612)
private val PetalPink = Color(0xFFE8D8D0)
private val PetalRose = Color(0xFFEDDDD0)
private val PetalGold = Color(0xFFD4C4B5)
private val PetalCream = Color(0xFFF9F7EE)
private val GlowGold = Color(0x665B9090)

private val HandwrittenFont = FontFamily(Font(R.font.caveat_regular))

/**
 * Заставка: лотос раскрывается из центра, затем проявляется подпись рукописным шрифтом.
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
) {
    val bloom = remember { Animatable(0f) }
    val breath by rememberInfiniteTransition(label = "lotusBreath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breath",
    )
    LaunchedEffect(Unit) {
        bloom.animateTo(1f, tween(durationMillis = 1700, easing = FastOutSlowInEasing))
        delay(HOLD_AFTER_BLOOM_MS)
        onTimeout()
    }

    val titleAlpha = ((bloom.value - 0.42f) / 0.58f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashInk),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LotusMark(
                bloom = bloom.value,
                breath = breath,
                modifier = Modifier.size(260.dp),
            )
            Text(
                text = stringResource(R.string.splash_brand),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = HandwrittenFont,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    translationY = (1f - titleAlpha) * 20f
                },
            )
        }
    }
}

@Composable
private fun LotusMark(
    bloom: Float,
    breath: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val sway = sin(breath * 2.0 * PI).toFloat()
        val breathScale = 1f + sway * 0.018f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GlowGold, Color.Transparent),
                center = center,
                radius = size.minDimension * 0.48f,
            ),
            radius = size.minDimension * 0.48f,
            center = center,
        )
        drawPetalRing(
            center = center,
            count = 8,
            length = size.minDimension * 0.34f,
            width = size.minDimension * 0.09f,
            rotationOffset = sway * 2.2f,
            color = PetalPink.copy(alpha = 0.92f),
            open = ease(bloom, start = 0f),
            breathScale = breathScale,
        )
        drawPetalRing(
            center = center,
            count = 8,
            length = size.minDimension * 0.27f,
            width = size.minDimension * 0.075f,
            rotationOffset = 22.5f - sway * 1.4f,
            color = PetalRose,
            open = ease(bloom, start = 0.12f),
            breathScale = breathScale,
        )
        drawPetalRing(
            center = center,
            count = 6,
            length = size.minDimension * 0.18f,
            width = size.minDimension * 0.055f,
            rotationOffset = 12f + sway,
            color = PetalGold,
            open = ease(bloom, start = 0.28f),
            breathScale = breathScale,
        )
        val core = ease(bloom, start = 0.45f)
        drawOval(
            color = PetalCream.copy(alpha = core),
            topLeft = Offset(
                center.x - size.minDimension * 0.045f * core,
                center.y - size.minDimension * 0.045f * core,
            ),
            size = Size(
                size.minDimension * 0.09f * core.coerceAtLeast(0.001f),
                size.minDimension * 0.09f * core.coerceAtLeast(0.001f),
            ),
        )
        drawCircle(
            color = Color(0xFFE8C56B).copy(alpha = core),
            radius = size.minDimension * 0.018f * core.coerceAtLeast(0.001f),
            center = center,
        )
    }
}

private fun DrawScope.drawPetalRing(
    center: Offset,
    count: Int,
    length: Float,
    width: Float,
    rotationOffset: Float,
    color: Color,
    open: Float,
    breathScale: Float,
) {
    if (open <= 0.01f) return
    val petal = petalPath(length, width)
    val scale = (0.2f + 0.8f * open) * breathScale
    repeat(count) { index ->
        val degrees = rotationOffset + index * (360f / count)
        withTransform({
            translate(center.x, center.y)
            rotate(degrees = degrees, pivot = Offset.Zero)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            drawPath(path = petal, color = color.copy(alpha = color.alpha * open))
        }
    }
}

private fun petalPath(length: Float, width: Float): Path = Path().apply {
    moveTo(0f, 0f)
    cubicTo(
        width, -length * 0.18f,
        width * 0.62f, -length * 0.72f,
        0f, -length,
    )
    cubicTo(
        -width * 0.62f, -length * 0.72f,
        -width, -length * 0.18f,
        0f, 0f,
    )
    close()
}

private fun ease(progress: Float, start: Float): Float {
    val t = ((progress - start) / (1f - start)).coerceIn(0f, 1f)
    return 1f - (1f - t) * (1f - t)
}

private const val HOLD_AFTER_BLOOM_MS = 900L
