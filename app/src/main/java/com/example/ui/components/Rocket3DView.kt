package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.MotionTilt3DBox
import com.example.core.rememberSensorMotionState
import com.example.ui.theme.AppColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Rocket3D(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    showFlame: Boolean = true,
    enableTilt: Boolean = true,
    interactiveBoost: Boolean = true
) {
    var isBoosted by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "rocket_fx")
    val flamePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    val boostScale by animateFloatAsState(
        targetValue = if (isBoosted) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "boost_scale"
    )

    val motionState = rememberSensorMotionState(
        enableShake = true,
        onShake = {
            isBoosted = true
        }
    )

    LaunchedEffect(isBoosted) {
        if (isBoosted) {
            kotlinx.coroutines.delay(1200)
            isBoosted = false
        }
    }

    val content = @Composable {
        Box(
            modifier = Modifier
                .size(size)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (interactiveBoost) isBoosted = !isBoosted
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val effectiveBoost = if (isBoosted) 1.4f else 1.0f
                val flameIntensity = flamePulse * effectiveBoost

                // 1. Atmosphere Halo Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.NeonBlue.copy(alpha = 0.22f * haloPulse),
                            AppColors.NeonPurple.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, h * 0.45f),
                        radius = w * 0.65f * haloPulse
                    ),
                    radius = w * 0.65f * haloPulse,
                    center = Offset(w / 2f, h * 0.45f)
                )

                // 2. Drop Shadow on bottom
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(AppColors.NeonBlue.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(w / 2f, h * 0.92f),
                        radius = w * 0.35f
                    ),
                    topLeft = Offset(w * 0.18f, h * 0.88f),
                    size = Size(w * 0.64f, h * 0.08f)
                )

                // 3. Flame thruster plume
                if (showFlame) {
                    drawRocketFlame(w, h, flameIntensity, isBoosted)
                }

                // 4. Fins (Back & Side wings)
                drawRocketFins(w, h)

                // 5. Main Rocket Body (Futuristic metallic fuselage)
                drawRocketBody(w, h, floatOffset)

                // 6. Visor / Porthole Window
                drawRocketVisor(w, h, floatOffset)

                // 7. Glass Specular highlight
                drawRocketGlassHighlight(w, h, floatOffset)
            }
        }
    }

    if (enableTilt) {
        MotionTilt3DBox(
            modifier = modifier.graphicsLayer {
                scaleX = boostScale
                scaleY = boostScale
            },
            motionState = motionState,
            maxTiltDegrees = 18f
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier.graphicsLayer {
                scaleX = boostScale
                scaleY = boostScale
            }
        ) {
            content()
        }
    }
}

private fun DrawScope.drawRocketFlame(w: Float, h: Float, intensity: Float, isBoosted: Boolean) {
    val flameHeight = h * 0.42f * intensity
    val flameWidth = w * 0.26f * (if (isBoosted) 1.25f else 1.0f)
    val startY = h * 0.72f
    val centerX = w / 2f

    // Outer flame plume
    val outerFlame = Path().apply {
        moveTo(centerX - flameWidth * 0.5f, startY)
        quadraticTo(
            centerX - flameWidth * 0.8f, startY + flameHeight * 0.45f,
            centerX, startY + flameHeight
        )
        quadraticTo(
            centerX + flameWidth * 0.8f, startY + flameHeight * 0.45f,
            centerX + flameWidth * 0.5f, startY
        )
        close()
    }

    drawPath(
        path = outerFlame,
        brush = Brush.verticalGradient(
            colors = listOf(
                AppColors.Coral,
                AppColors.NeonRed,
                AppColors.NeonBlue.copy(alpha = 0.3f),
                Color.Transparent
            ),
            startY = startY,
            endY = startY + flameHeight
        )
    )

    // Inner hot plasma core
    val innerFlame = Path().apply {
        moveTo(centerX - flameWidth * 0.25f, startY)
        quadraticTo(
            centerX - flameWidth * 0.35f, startY + flameHeight * 0.35f,
            centerX, startY + flameHeight * 0.65f
        )
        quadraticTo(
            centerX + flameWidth * 0.35f, startY + flameHeight * 0.35f,
            centerX + flameWidth * 0.25f, startY
        )
        close()
    }

    drawPath(
        path = innerFlame,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, AppColors.Gold, Color.Transparent),
            startY = startY,
            endY = startY + flameHeight * 0.7f
        )
    )

    // Thruster ring sparks
    val sparkY = startY + flameHeight * 0.85f
    drawCircle(
        color = AppColors.NeonCyan.copy(alpha = 0.7f),
        radius = 4f * intensity,
        center = Offset(centerX - flameWidth * 0.3f, sparkY)
    )
    drawCircle(
        color = AppColors.Coral.copy(alpha = 0.7f),
        radius = 5f * intensity,
        center = Offset(centerX + flameWidth * 0.3f, sparkY)
    )
}

private fun DrawScope.drawRocketFins(w: Float, h: Float) {
    val leftFin = Path().apply {
        moveTo(w * 0.32f, h * 0.52f)
        lineTo(w * 0.12f, h * 0.75f)
        quadraticTo(w * 0.18f, h * 0.78f, w * 0.33f, h * 0.72f)
        close()
    }

    val rightFin = Path().apply {
        moveTo(w * 0.68f, h * 0.52f)
        lineTo(w * 0.88f, h * 0.75f)
        quadraticTo(w * 0.82f, h * 0.78f, w * 0.67f, h * 0.72f)
        close()
    }

    val finBrush = Brush.verticalGradient(
        colors = listOf(AppColors.Coral, AppColors.NeonRed, AppColors.NeonPurple),
        startY = h * 0.5f,
        endY = h * 0.78f
    )

    drawPath(leftFin, finBrush)
    drawPath(rightFin, finBrush)

    // Neon edge glow on fins
    drawPath(
        path = leftFin,
        color = AppColors.NeonCyan.copy(alpha = 0.45f),
        style = Stroke(width = 2.5f)
    )
    drawPath(
        path = rightFin,
        color = AppColors.NeonCyan.copy(alpha = 0.45f),
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawRocketBody(w: Float, h: Float, floatY: Float) {
    val top = h * 0.12f + floatY
    val bottom = h * 0.74f + floatY
    val bodyWidth = w * 0.44f
    val centerX = w / 2f

    val bodyPath = Path().apply {
        moveTo(centerX, top)
        // Right flank curve
        cubicTo(
            centerX + bodyWidth * 0.55f, top + (bottom - top) * 0.28f,
            centerX + bodyWidth * 0.55f, bottom - (bottom - top) * 0.25f,
            centerX + bodyWidth * 0.44f, bottom
        )
        // Thruster base curve
        quadraticTo(
            centerX, bottom + (bottom - top) * 0.05f,
            centerX - bodyWidth * 0.44f, bottom
        )
        // Left flank curve
        cubicTo(
            centerX - bodyWidth * 0.55f, bottom - (bottom - top) * 0.25f,
            centerX - bodyWidth * 0.55f, top + (bottom - top) * 0.28f,
            centerX, top
        )
        close()
    }

    // High-tech metallic hull shader
    val bodyShader = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF1E283C),
            Color(0xFF455573),
            Color(0xFFE4E9F5),
            Color(0xFF8B9CB8),
            Color(0xFF161E2E)
        ),
        startX = centerX - bodyWidth * 0.6f,
        endX = centerX + bodyWidth * 0.6f
    )

    drawPath(bodyPath, bodyShader)

    // Sleek aerodynamic accent panel seams
    val seamPaint = Color(0x33FFFFFF)
    drawLine(
        color = seamPaint,
        start = Offset(centerX - bodyWidth * 0.38f, top + (bottom - top) * 0.42f),
        end = Offset(centerX + bodyWidth * 0.38f, top + (bottom - top) * 0.42f),
        strokeWidth = 1.8f
    )
    drawLine(
        color = AppColors.NeonCyan.copy(alpha = 0.7f),
        start = Offset(centerX - bodyWidth * 0.36f, top + (bottom - top) * 0.68f),
        end = Offset(centerX + bodyWidth * 0.36f, top + (bottom - top) * 0.68f),
        strokeWidth = 2.4f
    )
}

private fun DrawScope.drawRocketVisor(w: Float, h: Float, floatY: Float) {
    val centerX = w / 2f
    val centerY = h * 0.36f + floatY
    val radius = w * 0.10f

    // Outer ring
    drawCircle(
        color = AppColors.SurfaceSoft,
        radius = radius + 4f,
        center = Offset(centerX, centerY)
    )

    // Inner glowing glass
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, AppColors.NeonCyan, AppColors.NeonBlue, Color(0xFF0D172B)),
            center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
            radius = radius * 1.2f
        ),
        radius = radius,
        center = Offset(centerX, centerY)
    )

    // Glass rim highlight
    drawCircle(
        color = AppColors.NeonBlue.copy(alpha = 0.6f),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.2f)
    )
}

private fun DrawScope.drawRocketGlassHighlight(w: Float, h: Float, floatY: Float) {
    val top = h * 0.18f + floatY
    val highlightHeight = h * 0.32f
    val left = w * 0.34f

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.0f)),
            startY = top,
            endY = top + highlightHeight
        ),
        topLeft = Offset(left, top),
        size = Size(w * 0.05f, highlightHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
    )
}
