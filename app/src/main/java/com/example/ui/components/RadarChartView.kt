package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import kotlin.math.cos
import kotlin.math.sin

data class PlayerRadarStats(
    val playerName: String,
    val pace: Float,       // [0 - 100]
    val shooting: Float,
    val passing: Float,
    val dribbling: Float,
    val defense: Float,
    val physical: Float
)

@Composable
fun RadarChartView(
    playerA: PlayerRadarStats,
    playerB: PlayerRadarStats,
    modifier: Modifier = Modifier
) {
    val labels = listOf("السرعة", "التسديد", "التمرير", "المراوغة", "الدفاع", "البدنية")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AppColors.NeonBlue)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = playerA.playerName,
                    color = AppColors.NeonBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "مقارنة الرادار (Radar Chart)",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playerB.playerName,
                    color = AppColors.Coral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AppColors.Coral)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width * 0.42f
                val count = 6
                val angleStep = (2 * Math.PI / count).toFloat()

                // Draw Polygon concentric web lines (20%, 40%, 60%, 80%, 100%)
                for (step in 1..5) {
                    val r = maxRadius * (step / 5f)
                    val webPath = Path()
                    for (i in 0 until count) {
                        val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        if (i == 0) webPath.moveTo(x, y) else webPath.lineTo(x, y)
                    }
                    webPath.close()
                    drawPath(
                        path = webPath,
                        color = Color.White.copy(alpha = 0.09f),
                        style = Stroke(width = 1f)
                    )
                }

                // Draw axis spokes
                for (i in 0 until count) {
                    val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                    val x = center.x + maxRadius * cos(angle)
                    val y = center.y + maxRadius * sin(angle)
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = center,
                        end = Offset(x, y),
                        strokeWidth = 1f
                    )
                }

                // Player A Polygon (Neon Blue)
                val valuesA = listOf(
                    playerA.pace / 100f,
                    playerA.shooting / 100f,
                    playerA.passing / 100f,
                    playerA.dribbling / 100f,
                    playerA.defense / 100f,
                    playerA.physical / 100f
                )

                val pathA = Path()
                for (i in 0 until count) {
                    val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                    val r = maxRadius * valuesA[i]
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (i == 0) pathA.moveTo(x, y) else pathA.lineTo(x, y)
                }
                pathA.close()

                drawPath(
                    path = pathA,
                    color = AppColors.NeonBlue.copy(alpha = 0.35f)
                )
                drawPath(
                    path = pathA,
                    color = AppColors.NeonBlue,
                    style = Stroke(width = 2.2f)
                )

                // Player B Polygon (Coral)
                val valuesB = listOf(
                    playerB.pace / 100f,
                    playerB.shooting / 100f,
                    playerB.passing / 100f,
                    playerB.dribbling / 100f,
                    playerB.defense / 100f,
                    playerB.physical / 100f
                )

                val pathB = Path()
                for (i in 0 until count) {
                    val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                    val r = maxRadius * valuesB[i]
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (i == 0) pathB.moveTo(x, y) else pathB.lineTo(x, y)
                }
                pathB.close()

                drawPath(
                    path = pathB,
                    color = AppColors.Coral.copy(alpha = 0.35f)
                )
                drawPath(
                    path = pathB,
                    color = AppColors.Coral,
                    style = Stroke(width = 2.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stat Badges Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(labels[0], color = AppColors.TextMuted, fontSize = 10.sp)
                Text("${playerA.pace.toInt()} / ${playerB.pace.toInt()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(labels[1], color = AppColors.TextMuted, fontSize = 10.sp)
                Text("${playerA.shooting.toInt()} / ${playerB.shooting.toInt()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(labels[2], color = AppColors.TextMuted, fontSize = 10.sp)
                Text("${playerA.passing.toInt()} / ${playerB.passing.toInt()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(labels[3], color = AppColors.TextMuted, fontSize = 10.sp)
                Text("${playerA.dribbling.toInt()} / ${playerB.dribbling.toInt()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(labels[4], color = AppColors.TextMuted, fontSize = 10.sp)
                Text("${playerA.defense.toInt()} / ${playerB.defense.toInt()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(labels[5], color = AppColors.TextMuted, fontSize = 10.sp)
                Text("${playerA.physical.toInt()} / ${playerB.physical.toInt()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
