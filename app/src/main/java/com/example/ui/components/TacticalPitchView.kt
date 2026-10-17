package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerLineup
import com.example.model.TeamLineup
import com.example.ui.theme.AppColors

@Composable
fun TacticalPitchView(
    homeLineup: TeamLineup,
    awayLineup: TeamLineup,
    homeTeamName: String,
    awayTeamName: String,
    modifier: Modifier = Modifier,
    onPlayerClick: ((PlayerLineup, String, Color) -> Unit)? = null
) {
    var selectedTeamTab by remember { mutableIntStateOf(0) } // 0: كلاهما على الملعب, 1: قائمة الفريقين والبدلاء

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0C1914)) // Deep tactical pitch dark green
            .border(1.dp, Color(0x334FE3C1), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Formation & Coach Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = homeTeamName,
                    color = AppColors.NeonBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "خطة: ${homeLineup.formation}",
                    color = AppColors.TextSoft,
                    fontSize = 11.sp
                )
                Text(
                    text = "المدرب: ${homeLineup.coach}",
                    color = AppColors.TextMuted,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.NeonCyan.copy(alpha = 0.15f))
                    .border(1.dp, AppColors.NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ViewInAr,
                        contentDescription = "3D",
                        tint = AppColors.NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "لاعب 3D تفاعلي",
                        color = AppColors.NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = awayTeamName,
                    color = AppColors.Coral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "خطة: ${awayLineup.formation}",
                    color = AppColors.TextSoft,
                    fontSize = 11.sp
                )
                Text(
                    text = "المدرب: ${awayLineup.coach}",
                    color = AppColors.TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hint Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ViewInAr,
                contentDescription = "Tap",
                tint = AppColors.TextSoft,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "المس أي لاعب لفتح مجسم 3D التفاعلي 360° مع مؤشرات الجاهزية (PCI)",
                color = AppColors.TextSoft,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tactical 2D Field with Interactive Players
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF10271F),
                            Color(0xFF0B1F17),
                            Color(0xFF10271F)
                        )
                    )
                )
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
        ) {
            // Field Lines Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val lineColor = Color(0x35FFFFFF)
                val lineStroke = Stroke(width = 1.5f)

                // Outer boundary
                drawRect(
                    color = lineColor,
                    topLeft = Offset(12f, 12f),
                    size = Size(w - 24f, h - 24f),
                    style = lineStroke
                )

                // Halfway line
                drawLine(
                    color = lineColor,
                    start = Offset(12f, h / 2f),
                    end = Offset(w - 12f, h / 2f),
                    strokeWidth = 1.5f
                )

                // Center circle
                drawCircle(
                    color = lineColor,
                    radius = w * 0.16f,
                    center = Offset(w / 2f, h / 2f),
                    style = lineStroke
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.5f,
                    center = Offset(w / 2f, h / 2f)
                )

                // Top Penalty Box (Away goal)
                val boxWidth = w * 0.54f
                val boxHeight = h * 0.15f
                drawRect(
                    color = lineColor,
                    topLeft = Offset((w - boxWidth) / 2f, 12f),
                    size = Size(boxWidth, boxHeight),
                    style = lineStroke
                )

                // Bottom Penalty Box (Home goal)
                drawRect(
                    color = lineColor,
                    topLeft = Offset((w - boxWidth) / 2f, h - 12f - boxHeight),
                    size = Size(boxWidth, boxHeight),
                    style = lineStroke
                )
            }

            // Draw Players on Pitch
            // Away Team: Upper half (y from 0.06 to 0.44)
            awayLineup.starters.forEach { player ->
                PlayerBadgeOnPitch(
                    player = player,
                    teamColor = AppColors.Coral,
                    fieldX = player.x,
                    fieldY = 0.44f - (player.y * 0.38f),
                    onClick = {
                        onPlayerClick?.invoke(player, awayTeamName, AppColors.Coral)
                    }
                )
            }

            // Home Team: Lower half (y from 0.54 to 0.94)
            homeLineup.starters.forEach { player ->
                PlayerBadgeOnPitch(
                    player = player,
                    teamColor = AppColors.NeonBlue,
                    fieldX = player.x,
                    fieldY = 0.54f + (player.y * 0.38f),
                    onClick = {
                        onPlayerClick?.invoke(player, homeTeamName, AppColors.NeonBlue)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Substitutions & Bench Section
        Text(
            text = "دكة البدلاء والتبديلات (Substitutions)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Home Subs Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "بدلاء $homeTeamName (${homeLineup.subs.size})",
                    color = AppColors.NeonBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                homeLineup.subs.forEach { sub ->
                    BenchPlayerRow(
                        player = sub,
                        teamColor = AppColors.NeonBlue,
                        onClick = { onPlayerClick?.invoke(sub, homeTeamName, AppColors.NeonBlue) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Away Subs Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "بدلاء $awayTeamName (${awayLineup.subs.size})",
                    color = AppColors.Coral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                awayLineup.subs.forEach { sub ->
                    BenchPlayerRow(
                        player = sub,
                        teamColor = AppColors.Coral,
                        onClick = { onPlayerClick?.invoke(sub, awayTeamName, AppColors.Coral) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayerBadgeOnPitch(
    player: PlayerLineup,
    teamColor: Color,
    fieldX: Float,
    fieldY: Float,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val posX = (maxWidth.value * fieldX.coerceIn(0.08f, 0.92f) - 24).dp
        val posY = (maxHeight.value * fieldY.coerceIn(0.04f, 0.94f) - 24).dp

        Column(
            modifier = Modifier
                .offset(x = posX, y = posY)
                .width(48.dp)
                .clickable { onClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(teamColor)
                    .border(1.2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${player.number}",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = player.name,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = player.position,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BenchPlayerRow(
    player: PlayerLineup,
    teamColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(teamColor.copy(alpha = 0.2f))
                    .border(1.dp, teamColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${player.number}",
                    color = teamColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = player.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = player.position,
                    color = AppColors.TextMuted,
                    fontSize = 9.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${player.rating}",
                color = AppColors.NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ViewInAr,
                contentDescription = "3D",
                tint = AppColors.TextMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
