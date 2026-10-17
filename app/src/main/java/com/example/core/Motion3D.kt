package com.example.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.math.sqrt

class SensorMotionState {
    var rawTiltX by mutableFloatStateOf(0f)
    var rawTiltY by mutableFloatStateOf(0f)
    var touchOffsetX by mutableFloatStateOf(0f)
    var touchOffsetY by mutableFloatStateOf(0f)
}

@Composable
fun rememberSensorMotionState(
    enableShake: Boolean = false,
    onShake: (() -> Unit)? = null
): SensorMotionState {
    val context = LocalContext.current
    val motionState = remember { SensorMotionState() }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        var lastShakeTimestamp = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val ax = event.values[0]
                        val ay = event.values[1]
                        val az = event.values[2]

                        // Invert coordinates for natural phone tilt feel
                        val targetTiltY = (-ax / 9.81f).coerceIn(-1f, 1f) * 18f
                        val targetTiltX = (ay / 9.81f).coerceIn(-1f, 1f) * 18f

                        // Smooth dampening
                        motionState.rawTiltX = motionState.rawTiltX * 0.82f + targetTiltX * 0.18f
                        motionState.rawTiltY = motionState.rawTiltY * 0.82f + targetTiltY * 0.18f

                        // Shake detection
                        if (enableShake) {
                            val mag = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
                            val now = System.currentTimeMillis()
                            if (abs(mag - 9.81f) > 5.5f && (now - lastShakeTimestamp) > 900) {
                                lastShakeTimestamp = now
                                triggerHaptic(context)
                                onShake?.invoke()
                            }
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        val gx = event.values[0]
                        val gy = event.values[1]
                        // Gyroscope delta boost
                        motionState.rawTiltX += (gx * 1.5f).coerceIn(-4f, 4f)
                        motionState.rawTiltY += (gy * 1.5f).coerceIn(-4f, 4f)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    return motionState
}

fun triggerHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    } catch (_: Exception) {}
}

@Composable
fun MotionTilt3DBox(
    modifier: Modifier = Modifier,
    motionState: SensorMotionState = rememberSensorMotionState(),
    maxTiltDegrees: Float = 16f,
    depthCameraDistance: Float = 16f,
    interactiveTouch: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current.density

    // Combined tilt from sensor + touch drag
    val combinedTiltX = (motionState.rawTiltX + motionState.touchOffsetY).coerceIn(-maxTiltDegrees, maxTiltDegrees)
    val combinedTiltY = (motionState.rawTiltY + motionState.touchOffsetX).coerceIn(-maxTiltDegrees, maxTiltDegrees)

    val animatedRotationX by animateFloatAsState(
        targetValue = combinedTiltX,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rotX"
    )

    val animatedRotationY by animateFloatAsState(
        targetValue = combinedTiltY,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rotY"
    )

    Box(
        modifier = modifier
            .pointerInput(interactiveTouch) {
                if (interactiveTouch) {
                    detectDragGestures(
                        onDragEnd = {
                            motionState.touchOffsetX = 0f
                            motionState.touchOffsetY = 0f
                        },
                        onDragCancel = {
                            motionState.touchOffsetX = 0f
                            motionState.touchOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            motionState.touchOffsetX =
                                (motionState.touchOffsetX + dragAmount.x * 0.08f).coerceIn(-maxTiltDegrees, maxTiltDegrees)
                            motionState.touchOffsetY =
                                (motionState.touchOffsetY - dragAmount.y * 0.08f).coerceIn(-maxTiltDegrees, maxTiltDegrees)
                        }
                    )
                }
            }
            .graphicsLayer {
                rotationX = animatedRotationX
                rotationY = animatedRotationY
                cameraDistance = depthCameraDistance * density
                transformOrigin = TransformOrigin.Center
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
