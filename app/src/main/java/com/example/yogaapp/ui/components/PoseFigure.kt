package com.example.yogaapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.yogaapp.domain.model.PoseSpec
import com.example.yogaapp.domain.model.YogaPose
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val FigurePlate = Color(0xFFEEF3F8)
private val Skin = Color(0xFFF3C7A4)
private val SkinShade = Color(0xFFE0B08C)
private val Hair = Color(0xFF6B3F2A)
private val Shirt = Color(0xFF3C9BE8)
private val ShirtShade = Color(0xFF2E86D4)
private val Pants = Color(0xFF2A3D72)
private val Eye = Color(0xFF3E2C24)
private val Smile = Color(0xFFC4846A)

/**
 * Иллюстрация позы: объёмный персонаж (как на референсе — футболка, брюки, лицо),
 * а не палочная схема. Углы берутся из [PoseSpec], поэтому одна функция рисует все асаны.
 *
 * Анимация: дыхание (покачивание корпуса) и плавный переход углов при смене позы.
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
    val spec = pose.spec
    val motion = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)
    val torsoTilt by animateFloatAsState(spec.torsoTilt, motion, label = "tilt")
    val upperLeg by animateFloatAsState(spec.upperLegAngle, motion, label = "upperLeg")
    val lowerLeg by animateFloatAsState(spec.lowerLegAngle, motion, label = "lowerLeg")
    val upperArm by animateFloatAsState(spec.upperArmAngle, motion, label = "upperArm")
    val lowerArm by animateFloatAsState(spec.lowerArmAngle, motion, label = "lowerArm")

    val transition = rememberInfiniteTransition(label = "poseLife")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breath",
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blink",
    )
    val breathPhase = if (animated) breath else 0.35f
    val eyeOpen = if (animated && blink > 0.94f) 0.18f else 1f
    val sway = if (animated) sin(breathPhase * 2.0 * PI).toFloat() * 3.5f else 0f
    val drawn = spec.copy(
        torsoTilt = torsoTilt,
        upperLegAngle = upperLeg,
        lowerLegAngle = lowerLeg,
        upperArmAngle = upperArm + sway,
        lowerArmAngle = lowerArm,
    )

    Box(
        modifier = modifier
            .size(size)
            .background(color = FigurePlate, shape = RoundedCornerShape(24.dp))
            .semantics { contentDescription = "Иллюстрация позы: ${pose.ruName}" },
    ) {
        Canvas(modifier = Modifier.size(size)) {
            drawIllustratedPose(drawn, breathPhase, eyeOpen)
        }
    }
}

private fun DrawScope.drawIllustratedPose(spec: PoseSpec, breath: Float, eyeOpen: Float) {
    val width = size.width
    val height = size.height
    val tilt = spec.torsoTilt * PI.toFloat() / 180f
    val cosT = cos(tilt)
    val sinT = sin(tilt)
    fun rot(v: Offset): Offset = Offset(
        v.x * cosT + v.y * sinT,
        -v.x * sinT + v.y * cosT,
    )

    val bob = sin(breath * 2.0 * PI).toFloat() * height * 0.012f
    val hip = Offset(width / 2f, height * 0.58f + bob)
    val headR = height * 0.074f
    val torsoLen = height * 0.23f
    val upperArmLen = height * 0.125f
    val lowerArmLen = height * 0.11f
    val upperLegLen = height * 0.15f
    val lowerLegLen = height * 0.14f
    val limb = headR * 0.58f
    val pant = headR * 0.92f
    val shirtW = headR * 2.05f
    val spread = height * 0.04f

    drawOval(
        color = Color(0x33000000),
        topLeft = Offset(width * 0.26f, height * 0.8f),
        size = Size(width * 0.48f, height * 0.05f),
    )

    val neck = hip + rot(Offset(0f, -torsoLen))
    val head = neck + rot(Offset(0f, -headR * 2.05f))
    val hipL = hip + rot(Offset(-spread, 0f))
    val hipR = hip + rot(Offset(spread, 0f))

    fun end(origin: Offset, angleDeg: Float, length: Float, mirror: Float): Offset {
        val rad = angleDeg * PI.toFloat() / 180f
        return origin + rot(Offset(cos(rad) * length * mirror, sin(rad) * length))
    }

    val shoulderL = neck + rot(Offset(-spread * 0.35f, torsoLen * 0.14f))
    val shoulderR = neck + rot(Offset(spread * 0.35f, torsoLen * 0.14f))
    val armAng = 90f + spec.upperArmAngle

    var elbowL: Offset
    var elbowR: Offset
    var wristL: Offset
    var wristR: Offset
    if (spec.armsOverhead) {
        elbowL = shoulderL + rot(Offset(-headR * 0.2f, -upperArmLen))
        elbowR = shoulderR + rot(Offset(headR * 0.2f, -upperArmLen))
        wristL = elbowL + rot(Offset(-headR * 0.05f, -lowerArmLen))
        wristR = elbowR + rot(Offset(headR * 0.05f, -lowerArmLen))
    } else {
        elbowL = end(shoulderL, armAng, upperArmLen, 1f)
        elbowR = end(shoulderR, armAng, upperArmLen, -1f)
        val fore = armAng + spec.lowerArmAngle
        wristL = end(elbowL, fore, lowerArmLen, 1f)
        wristR = end(elbowR, fore, lowerArmLen, -1f)
    }
    if (spec.armsInPrayer) {
        val prayer = neck + rot(Offset(0f, torsoLen * 0.32f))
        wristL = prayer
        wristR = prayer
        elbowL = shoulderL + rot(Offset(headR * 0.35f, torsoLen * 0.2f))
        elbowR = shoulderR + rot(Offset(-headR * 0.35f, torsoLen * 0.2f))
    }

    val legAng = 90f + spec.upperLegAngle
    var kneeL = end(hipL, legAng, upperLegLen, 1f)
    var kneeR = end(hipR, legAng, upperLegLen, -1f)
    val lowAng = if (spec.kneeling) -20f else legAng + spec.lowerLegAngle
    val lowMirror = if (spec.kneeling) -1f else 1f
    var footL = end(kneeL, lowAng, lowerLegLen, lowMirror)
    var footR = end(kneeR, lowAng, lowerLegLen, -lowMirror)
    if (spec.legsCrossed) {
        kneeL = hip + rot(Offset(-upperLegLen * 0.95f, upperLegLen * 0.42f))
        kneeR = hip + rot(Offset(upperLegLen * 0.95f, upperLegLen * 0.42f))
        footL = hip + rot(Offset(upperLegLen * 0.28f, upperLegLen * 0.12f))
        footR = hip + rot(Offset(-upperLegLen * 0.28f, upperLegLen * 0.12f))
        if (!spec.armsOverhead && !spec.armsInPrayer) {
            wristL = kneeL + rot(Offset(0f, -headR * 0.2f))
            wristR = kneeR + rot(Offset(0f, -headR * 0.2f))
            elbowL = shoulderL + (wristL - shoulderL) * 0.55f + rot(Offset(0f, -headR * 0.35f))
            elbowR = shoulderR + (wristR - shoulderR) * 0.55f + rot(Offset(0f, -headR * 0.35f))
        }
    }

    drawLine(Pants, hipL, kneeL, pant, StrokeCap.Round)
    drawLine(Pants, kneeL, footL, pant * 0.82f, StrokeCap.Round)
    drawLine(Pants, hipR, kneeR, pant, StrokeCap.Round)
    drawLine(Pants, kneeR, footR, pant * 0.82f, StrokeCap.Round)
    drawCircle(Pants, headR * 0.26f, footL)
    drawCircle(Pants, headR * 0.26f, footR)

    drawLine(Skin, shoulderL, elbowL, limb, StrokeCap.Round)
    drawLine(Skin, elbowL, wristL, limb * 0.88f, StrokeCap.Round)
    drawLine(Skin, shoulderR, elbowR, limb, StrokeCap.Round)
    drawLine(Skin, elbowR, wristR, limb * 0.88f, StrokeCap.Round)
    val hand = headR * 0.22f
    if (wristL == wristR) {
        drawCircle(SkinShade, hand * 1.35f, wristL)
    } else {
        drawCircle(Skin, hand, wristL)
        drawCircle(Skin, hand, wristR)
    }

    val shirtTop = neck + rot(Offset(0f, headR * 0.2f))
    drawLine(ShirtShade, shirtTop, hip, shirtW, StrokeCap.Round)
    drawLine(Shirt, shirtTop, hip + rot(Offset(0f, -torsoLen * 0.12f)), shirtW * 0.7f, StrokeCap.Round)
    drawLine(Skin, head, shirtTop, limb * 0.72f, StrokeCap.Round)

    rotate(degrees = spec.torsoTilt, pivot = head) {
        drawArc(
            color = Hair,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = true,
            topLeft = Offset(head.x - headR * 1.12f, head.y - headR * 1.35f),
            size = Size(headR * 2.24f, headR * 1.9f),
        )
    }
    drawCircle(Skin, headR, head)
    rotate(degrees = spec.torsoTilt, pivot = head) {
        drawArc(
            color = Hair,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = true,
            topLeft = Offset(head.x - headR * 1.02f, head.y - headR * 1.22f),
            size = Size(headR * 2.04f, headR * 1.05f),
        )
        val eyeRy = headR * 0.12f * eyeOpen.coerceIn(0.15f, 1f)
        val eyeRx = headR * 0.09f
        drawOval(
            color = Eye,
            topLeft = Offset(head.x - headR * 0.38f - eyeRx, head.y - headR * 0.08f - eyeRy),
            size = Size(eyeRx * 2f, eyeRy * 2f),
        )
        drawOval(
            color = Eye,
            topLeft = Offset(head.x + headR * 0.16f, head.y - headR * 0.08f - eyeRy),
            size = Size(eyeRx * 2f, eyeRy * 2f),
        )
        drawArc(
            color = Smile,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(head.x - headR * 0.28f, head.y + headR * 0.08f),
            size = Size(headR * 0.56f, headR * 0.4f),
            style = Stroke(width = (headR * 0.07f).coerceAtLeast(1.4f), cap = StrokeCap.Round),
        )
    }
}
