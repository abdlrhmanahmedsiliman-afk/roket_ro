package com.example.core.performance

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppColors

/**
 * High-performance 60fps Shimmer Modifier for skeleton screens
 * Eliminates spinner jank and provides immediate visual feedback.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerColors = listOf(
        AppColors.Surface.copy(alpha = 0.6f),
        AppColors.SurfaceSoft.copy(alpha = 0.95f),
        AppColors.NeonBlue.copy(alpha = 0.15f),
        AppColors.SurfaceSoft.copy(alpha = 0.95f),
        AppColors.Surface.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim.value - 300f, y = translateAnim.value - 300f),
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    background(brush)
}

@Composable
fun ShimmerMatchCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.BgElevated.copy(alpha = 0.7f))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header shimmer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .shimmerEffect()
                )
            }

            // Teams and Score row shimmer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home team
                Row {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 75.dp, height = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )
                }

                // Score pill
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )

                // Away team
                Row {
                    Box(
                        modifier = Modifier
                            .size(width = 75.dp, height = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect()
                    )
                }
            }

            // Bottom prediction bar shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shimmerEffect()
            )
        }
    }
}
