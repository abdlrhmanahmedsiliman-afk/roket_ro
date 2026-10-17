package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import kotlin.math.sin

@Composable
fun MomentumGraphView(
    homeTeam: String,
    awayTeam: String,
    currentMinute: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "momentum_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.NeonBlue)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = homeTeam,
                    color = AppColors.NeonBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "مؤشر ضغط واستحواذ المباراة (Momentum)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = awayTeam,
                    color = AppColors.Coral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.Coral)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceSoft)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Draw center baseline
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.5f
                )

                // Grid interval vertical lines (15', 30', 45', 60', 75', 90')
                for (m in 15..90 step 15) {
                    val x = (m / 90f) * w
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                }

                // Wave Path for Momentum
                val homePath = Path()
                val awayPath = Path()
                val linePath = Path()

                val steps = 90
                val activeMinute = currentMinute.coerceIn(1, 90)
                val activeWidth = (activeMinute / 90f) * w

                homePath.moveTo(0f, midY)
                awayPath.moveTo(0f, midY)
                linePath.moveTo(0f, midY)

                for (i in 0..activeMinute) {
                    val x = (i / 90f) * w
                    // Procedural momentum curve based on game phases
                    val val1 = sin(i * 0.22) * 0.45f
                    val val2 = sin(i * 0.08 + 1.2) * 0.35f
                    val val3 = sin(i * 0.45) * 0.2f
                    val momentum = ((val1 + val2 + val3) * pulse).toFloat().coerceIn(-1f, 1f)
                    val y = midY - (momentum * (midY * 0.85f))

                    linePath.lineTo(x, y)
                    if (y <= midY) {
                        homePath.lineTo(x, y)
                        awayPath.lineTo(x, midY)
                    } else {
                        awayPath.lineTo(x, y)
                        homePath.lineTo(x, midY)
                    }
                }

                homePath.lineTo(activeWidth, midY)
                homePath.close()

                awayPath.lineTo(activeWidth, midY)
                awayPath.close()

                // Draw Home Advantage area (Upper fill: NeonBlue)
                drawPath(
                    path = homePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(AppColors.NeonBlue.copy(alpha = 0.45f), Color.Transparent),
                        startY = 0f,
                        endY = midY
                    )
                )

                // Draw Away Advantage area (Lower fill: Coral)
                drawPath(
                    path = awayPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, AppColors.Coral.copy(alpha = 0.45f)),
                        startY = midY,
                        endY = h
                    )
                )

                // Draw Momentum Stroke line
                drawPath(
                    path = linePath,
                    color = AppColors.NeonCyan,
                    style = Stroke(width = 2.5f)
                )

                // Draw Current Minute pulse point
                val currentX = activeWidth
                val currentMomentum = sin(activeMinute * 0.22) * 0.45f + sin(activeMinute * 0.08 + 1.2) * 0.35f
                val currentY = midY - (currentMomentum.toFloat() * (midY * 0.85f))

                drawCircle(
                    color = AppColors.NeonCyan,
                    radius = 4.5f * pulse,
                    center = Offset(currentX, currentY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = Offset(currentX, currentY)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0'", color = AppColors.TextMuted, fontSize = 10.sp)
            Text("15'", color = AppColors.TextMuted, fontSize = 10.sp)
            Text("30'", color = AppColors.TextMuted, fontSize = 10.sp)
            Text("45' (شوط 1)", color = AppColors.TextMuted, fontSize = 10.sp)
            Text("60'", color = AppColors.TextMuted, fontSize = 10.sp)
            Text("75'", color = AppColors.TextMuted, fontSize = 10.sp)
            Text("90'", color = AppColors.TextMuted, fontSize = 10.sp)
        }
    }
}
