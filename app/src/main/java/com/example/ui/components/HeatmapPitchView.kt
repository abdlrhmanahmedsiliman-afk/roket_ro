package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors

@Composable
fun HeatmapPitchView(
    homeTeam: String,
    awayTeam: String,
    modifier: Modifier = Modifier
) {
    var selectedTeamTab by remember { mutableIntStateOf(0) } // 0: Home Team, 1: Away Team

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0C1914))
            .border(1.dp, Color(0x334FE3C1), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "خريطة النشاط والحرارة (Heatmap)",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Selector Tab
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Surface)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTeamTab == 0) AppColors.NeonBlue else Color.Transparent)
                        .clickable { selectedTeamTab = 0 }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = homeTeam,
                        color = if (selectedTeamTab == 0) Color.Black else AppColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTeamTab == 1) AppColors.Coral else Color.Transparent)
                        .clickable { selectedTeamTab = 1 }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = awayTeam,
                        color = if (selectedTeamTab == 1) Color.Black else AppColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Soccer Pitch with Heatmap Gradients
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF10251E))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val lineColor = Color(0x33FFFFFF)
                val lineStroke = Stroke(width = 1.2f)

                // Field Markings
                drawRect(color = lineColor, topLeft = Offset(6f, 6f), size = Size(w - 12f, h - 12f), style = lineStroke)
                drawLine(color = lineColor, start = Offset(w / 2f, 6f), end = Offset(w / 2f, h - 6f), strokeWidth = 1.2f)
                drawCircle(color = lineColor, radius = h * 0.22f, center = Offset(w / 2f, h / 2f), style = lineStroke)

                // Goal Areas
                drawRect(color = lineColor, topLeft = Offset(6f, h * 0.25f), size = Size(w * 0.15f, h * 0.5f), style = lineStroke)
                drawRect(color = lineColor, topLeft = Offset(w - 6f - w * 0.15f, h * 0.25f), size = Size(w * 0.15f, h * 0.5f), style = lineStroke)

                // Draw Heatmap Zones
                val heatColor = if (selectedTeamTab == 0) AppColors.NeonBlue else AppColors.Coral
                val heatColorHot = if (selectedTeamTab == 0) AppColors.NeonCyan else AppColors.NeonRed

                val homeHeatCenters = listOf(
                    Offset(w * 0.68f, h * 0.32f) to 48f, // Right wing deep
                    Offset(w * 0.72f, h * 0.65f) to 54f, // Penalty box danger zone
                    Offset(w * 0.52f, h * 0.45f) to 62f, // Central midfield pivot
                    Offset(w * 0.35f, h * 0.75f) to 40f  // Left back support
                )

                val awayHeatCenters = listOf(
                    Offset(w * 0.30f, h * 0.25f) to 52f, // Left wing counter
                    Offset(w * 0.26f, h * 0.55f) to 46f, // Box edge
                    Offset(w * 0.48f, h * 0.52f) to 60f, // Center circle fight
                    Offset(w * 0.65f, h * 0.20f) to 38f  // Defensive transition
                )

                val activeCenters = if (selectedTeamTab == 0) homeHeatCenters else awayHeatCenters

                activeCenters.forEach { (center, radius) ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                heatColorHot.copy(alpha = 0.65f),
                                heatColor.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(AppColors.NeonCyan))
                Spacer(modifier = Modifier.width(4.dp))
                Text("منطقة تمركز مكثف", color = AppColors.TextSoft, fontSize = 10.sp)
            }
            Text(
                text = "Field Tilt: ${if (selectedTeamTab == 0) "58% هجومي" else "42% هجومي"}",
                color = if (selectedTeamTab == 0) AppColors.NeonBlue else AppColors.Coral,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
