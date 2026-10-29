package com.example.yogaapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class BurstSpec(
    val xFraction: Float,
    val yFraction: Float,
    val phaseOffset: Float,
    val color: Color,
    val rays: Int = 18,
)

private val FIREWORK_COLORS = listOf(
    Color(0xFFFFD54F), // warm gold
    Color(0xFFFF8A65), // coral
    Color(0xFFE1BEE7), // soft lavender
    Color(0xFFFFAB91), // peach
    Color(0xFFFFF176), // bright lemon
    Color(0xFFF48FB1), // rose
)

/**
 * Full-screen fireworks bursts Canvas overlay for the session completion screen.
 */
@Composable
fun FireworksOverlay(
    modifier: Modifier = Modifier,
) {
    val bursts = remember {
        listOf(
            BurstSpec(0.22f, 0.22f, 0.00f, FIREWORK_COLORS[0], 20),
            BurstSpec(0.78f, 0.20f, 0.33f, FIREWORK_COLORS[1], 18),
            BurstSpec(0.50f, 0.14f, 0.66f, FIREWORK_COLORS[5], 22),
            BurstSpec(0.18f, 0.45f, 0.50f, FIREWORK_COLORS[3], 16),
            BurstSpec(0.82f, 0.42f, 0.18f, FIREWORK_COLORS[4], 18),
            BurstSpec(0.50f, 0.30f, 0.82f, FIREWORK_COLORS[2], 20),
        )
    }
    val transition = rememberInfiniteTransition(label = "fireworks")
    val master by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fireworksPhase",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val maxRadius = size.minDimension * 0.28f
        bursts.forEach { b ->
            val t = (master + b.phaseOffset) % 1f
            // Expand quickly then fade with slight gravity drop.
            val ease = 1f - (1f - t) * (1f - t)
            val radius = maxRadius * ease
            val alpha = (1f - t).coerceIn(0f, 1f)
            val cx = size.width * b.xFraction
            val cy = size.height * b.yFraction + (t * t * 36f)

            for (i in 0 until b.rays) {
                val angle = (2.0 * PI * i / b.rays) + (b.phaseOffset * 2f)
                val ringScale = if (i % 2 == 0) 1f else 0.62f
                val px = cx + (cos(angle) * radius * ringScale).toFloat()
                val py = cy + (sin(angle) * radius * ringScale).toFloat()
                val tail = radius * ringScale * 0.72f
                val tx = cx + (cos(angle) * tail).toFloat()
                val ty = cy + (sin(angle) * tail).toFloat()

                drawLine(
                    color = b.color.copy(alpha = alpha * 0.75f),
                    start = Offset(tx, ty),
                    end = Offset(px, py),
                    strokeWidth = 4f,
                )
                drawCircle(
                    color = b.color.copy(alpha = alpha),
                    radius = (6f * (1f - t * 0.5f)).coerceAtLeast(2f),
                    center = Offset(px, py),
                )
            }
        }
    }
}

/**
 * Floating / pulsing congratulation banner («Ты Большой молодец !»).
 */
@Composable
fun FloatingPraiseBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "praiseFloat")
    val bobDp by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .offset { IntOffset(0, bobDp.dp.roundToPx()) }
            .scale(pulse),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
