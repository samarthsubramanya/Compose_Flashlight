package com.thingsenz.flashlight.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thingsenz.flashlight.ui.theme.BeamAmber
import com.thingsenz.flashlight.ui.theme.BeamAmberBright
import com.thingsenz.flashlight.ui.theme.BodySteel
import com.thingsenz.flashlight.ui.theme.BodySteelDark
import com.thingsenz.flashlight.ui.theme.OutlineDim
import kotlin.math.ceil

/**
 * A hand-drawn, animated torch. Tapping toggles the beam; the glow, press feedback
 * and idle "breathing" are all driven by Compose animation APIs rather than static art.
 */
@Composable
fun TorchIllustration(
    isOn: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press-scale"
    )
    val glowTarget by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "glow"
    )
    val infinite = rememberInfiniteTransition(label = "breathe")
    val breathe by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val lensColor by animateColorAsState(
        targetValue = if (isOn) BeamAmberBright else BodySteelDark,
        animationSpec = tween(120),
        label = "lens-color"
    )

    Canvas(
        modifier
            .size(220.dp, 320.dp)
            .scale(pressScale)
            .semantics {
                role = Role.Switch
                contentDescription = if (isOn) "Flashlight, on" else "Flashlight, off"
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onToggle() }
                )
            }
    ) {
        drawTorch(glow = glowTarget * breathe, lensColor = lensColor)
    }
}

private fun DrawScope.drawTorch(glow: Float, lensColor: Color) {
    val w = size.width
    val h = size.height
    val lensRadius = w * 0.40f
    val lensCenter = Offset(w / 2f, lensRadius + h * 0.04f)

    if (glow > 0.01f) {
        val beamTop = 0f
        val beamHalfWidthTop = lensRadius * 1.7f
        val beamPath = Path().apply {
            moveTo(lensCenter.x - lensRadius * 0.75f, lensCenter.y)
            lineTo(lensCenter.x - beamHalfWidthTop, beamTop)
            lineTo(lensCenter.x + beamHalfWidthTop, beamTop)
            lineTo(lensCenter.x + lensRadius * 0.75f, lensCenter.y)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    BeamAmberBright.copy(alpha = 0.5f * glow),
                    BeamAmber.copy(alpha = 0.12f * glow),
                    Color.Transparent
                ),
                startY = lensCenter.y,
                endY = beamTop
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BeamAmberBright.copy(alpha = 0.4f * glow), Color.Transparent),
                center = lensCenter,
                radius = lensRadius * 2.3f
            ),
            radius = lensRadius * 2.3f,
            center = lensCenter
        )
    }

    val bodyWidth = w * 0.46f
    val bodyTop = lensCenter.y + lensRadius * 0.5f
    val bodyBottom = h * 0.90f
    val corner = 22.dp.toPx()
    val bodyPath = Path().apply {
        moveTo(lensCenter.x - lensRadius * 0.78f, lensCenter.y + lensRadius * 0.35f)
        lineTo(lensCenter.x - bodyWidth / 2f, bodyTop)
        lineTo(lensCenter.x - bodyWidth / 2f, bodyBottom - corner)
        quadraticTo(
            lensCenter.x - bodyWidth / 2f, bodyBottom,
            lensCenter.x - bodyWidth / 2f + corner, bodyBottom
        )
        lineTo(lensCenter.x + bodyWidth / 2f - corner, bodyBottom)
        quadraticTo(
            lensCenter.x + bodyWidth / 2f, bodyBottom,
            lensCenter.x + bodyWidth / 2f, bodyBottom - corner
        )
        lineTo(lensCenter.x + bodyWidth / 2f, bodyTop)
        lineTo(lensCenter.x + lensRadius * 0.78f, lensCenter.y + lensRadius * 0.35f)
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
            colors = listOf(BodySteel, BodySteelDark),
            startY = bodyTop,
            endY = bodyBottom
        )
    )
    drawPath(path = bodyPath, color = OutlineDim, style = Stroke(width = 2.dp.toPx()))

    val ridgeStart = bodyTop + (bodyBottom - bodyTop) * 0.32f
    repeat(4) { i ->
        val y = ridgeStart + i * 18.dp.toPx()
        drawLine(
            color = OutlineDim,
            start = Offset(lensCenter.x - bodyWidth / 2f + 10.dp.toPx(), y),
            end = Offset(lensCenter.x + bodyWidth / 2f - 10.dp.toPx(), y),
            strokeWidth = 2.dp.toPx()
        )
    }

    drawCircle(
        color = OutlineDim,
        radius = lensRadius,
        center = lensCenter,
        style = Stroke(width = 6.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(lensColor, lensColor.copy(alpha = 0.75f)),
            center = lensCenter,
            radius = lensRadius * 0.92f
        ),
        radius = lensRadius * 0.86f,
        center = lensCenter
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        radius = lensRadius * 0.28f,
        center = lensCenter - Offset(lensRadius * 0.26f, lensRadius * 0.26f)
    )
}

/**
 * A vertical rail of glowing segments beside the torch: tap or drag to pick the
 * flashlight strength, with a continuous fill animation between levels.
 */
@Composable
fun PowerLevelRail(
    level: Int,
    maxLevel: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onLevelChange: (Int) -> Unit,
) {
    if (maxLevel <= 1) return

    val progress by animateFloatAsState(
        targetValue = level.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "level-progress"
    )

    fun levelFromOffsetY(y: Float, height: Float): Int {
        val fraction = 1f - (y / height).coerceIn(0f, 1f)
        return ceil(fraction * maxLevel).toInt().coerceIn(1, maxLevel)
    }

    Canvas(
        modifier
            .width(28.dp)
            .height(220.dp)
            .semantics {
                contentDescription = "Flashlight power level $level of $maxLevel"
            }
            .pointerInput(enabled, maxLevel) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onLevelChange(levelFromOffsetY(offset.y, size.height.toFloat()))
                }
            }
            .pointerInput(enabled, maxLevel) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures { change, _ ->
                    onLevelChange(levelFromOffsetY(change.position.y, size.height.toFloat()))
                }
            }
    ) {
        val gap = 6.dp.toPx()
        val segmentHeight = (size.height - gap * (maxLevel - 1)) / maxLevel
        for (i in 0 until maxLevel) {
            val bottomY = size.height - i * (segmentHeight + gap)
            val topY = bottomY - segmentHeight
            val fill = (progress - i).coerceIn(0f, 1f)
            val color = lerp(OutlineDim, BeamAmber, fill)
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, topY),
                size = androidx.compose.ui.geometry.Size(size.width, segmentHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
}
