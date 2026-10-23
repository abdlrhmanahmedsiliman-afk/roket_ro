package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsSoccer
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    // Modes:
    // 0: الملعب المشترك (الفريقين معاً)
    // 1: تشكيل الفريق المضيف منفصلاً (تكبير تكتيكي كامل)
    // 2: تشكيل الفريق الضيف منفصلاً
    // 3: قائمة التشكيلة والبدلاء
    var viewMode by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF091410))
            .border(1.dp, Color(0x334FE3C1), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Formation & Coach Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = homeTeamName,
                    color = AppColors.NeonBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "خطة: ${homeLineup.formation}",
                    color = AppColors.TextSoft,
                    fontSize = 11.sp
                )
                Text(
                    text = "المدرب: ${homeLineup.coach}",
                    color = AppColors.TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
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
                        text = "وجه 3D تفاعلي",
                        color = AppColors.NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = awayTeamName,
                    color = AppColors.Coral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "خطة: ${awayLineup.formation}",
                    color = AppColors.TextSoft,
                    fontSize = 11.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "المدرب: ${awayLineup.coach}",
                    color = AppColors.TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4-Perspective Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Surface)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val tabs = listOf(
                "الملعب المشترك",
                homeTeamName.take(8),
                awayTeamName.take(8),
                "قائمة البدلاء"
            )
            tabs.forEachIndexed { idx, title ->
                val isSelected = viewMode == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AppColors.NeonBlue else Color.Transparent)
                        .clickable { viewMode = idx }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.Black else AppColors.TextSoft,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (viewMode) {
            0 -> {
                // Combined Pitch: Both Teams (22 Players)
                Text(
                    text = "💡 اضغط على أي لاعب لفتح مجسم الوجه 3D التفاعلي 360° والمقاييس البيومترية",
                    color = AppColors.NeonCyan,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(490.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF0E2A1F), Color(0xFF091C15), Color(0xFF0E2A1F))
                                )
                            )
                            .border(1.dp, Color(0x334FE3C1), RoundedCornerShape(12.dp))
                    ) {
                        val fieldWidth = maxWidth
                        val fieldHeight = maxHeight

                        // Draw Grass & Markings
                        TacticalFieldCanvas()

                        // Draw Away Starters (Top Half)
                        awayLineup.starters.forEach { player ->
                            val normalizedY = 0.42f - (player.y * 0.35f)
                            val posX = (fieldWidth * player.x.coerceIn(0.12f, 0.88f)) - 38.dp
                            val posY = (fieldHeight * normalizedY.coerceIn(0.05f, 0.43f)) - 26.dp

                            PlayerPitchNode(
                                player = player,
                                teamColor = AppColors.Coral,
                                offsetX = posX,
                                offsetY = posY,
                                onClick = { onPlayerClick?.invoke(player, awayTeamName, AppColors.Coral) }
                            )
                        }

                        // Draw Home Starters (Bottom Half)
                        homeLineup.starters.forEach { player ->
                            val normalizedY = 0.58f + (player.y * 0.35f)
                            val posX = (fieldWidth * player.x.coerceIn(0.12f, 0.88f)) - 38.dp
                            val posY = (fieldHeight * normalizedY.coerceIn(0.57f, 0.93f)) - 26.dp

                            PlayerPitchNode(
                                player = player,
                                teamColor = AppColors.NeonBlue,
                                offsetX = posX,
                                offsetY = posY,
                                onClick = { onPlayerClick?.invoke(player, homeTeamName, AppColors.NeonBlue) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                SubstitutesBenchRow(
                    homeTeamName = homeTeamName,
                    awayTeamName = awayTeamName,
                    homeLineup = homeLineup,
                    awayLineup = awayLineup,
                    onPlayerClick = onPlayerClick
                )
            }

            1 -> {
                // Home Team Full Pitch Zoom (Ample spacing for all 11 players)
                Text(
                    text = "تشكيلة ${homeTeamName} الكاملة (${homeLineup.formation}) • اضغط على أي لاعب لعرض وجهه 3D",
                    color = AppColors.NeonBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(470.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF081B2B), Color(0xFF04101A), Color(0xFF081B2B))
                                )
                            )
                            .border(1.2.dp, AppColors.NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        val fieldWidth = maxWidth
                        val fieldHeight = maxHeight

                        TacticalFieldCanvas()

                        homeLineup.starters.forEach { player ->
                            val posX = (fieldWidth * player.x.coerceIn(0.12f, 0.88f)) - 38.dp
                            val posY = (fieldHeight * player.y.coerceIn(0.08f, 0.90f)) - 26.dp

                            PlayerPitchNode(
                                player = player,
                                teamColor = AppColors.NeonBlue,
                                offsetX = posX,
                                offsetY = posY,
                                onClick = { onPlayerClick?.invoke(player, homeTeamName, AppColors.NeonBlue) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                TeamSubsListOnly(teamName = homeTeamName, subs = homeLineup.subs, teamColor = AppColors.NeonBlue, onPlayerClick = onPlayerClick)
            }

            2 -> {
                // Away Team Full Pitch Zoom
                Text(
                    text = "تشكيلة ${awayTeamName} الكاملة (${awayLineup.formation}) • اضغط على أي لاعب لعرض وجهه 3D",
                    color = AppColors.Coral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(470.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2B1015), Color(0xFF19090C), Color(0xFF2B1015))
                                )
                            )
                            .border(1.2.dp, AppColors.Coral.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        val fieldWidth = maxWidth
                        val fieldHeight = maxHeight

                        TacticalFieldCanvas()

                        awayLineup.starters.forEach { player ->
                            val posX = (fieldWidth * player.x.coerceIn(0.12f, 0.88f)) - 38.dp
                            val posY = (fieldHeight * player.y.coerceIn(0.08f, 0.90f)) - 26.dp

                            PlayerPitchNode(
                                player = player,
                                teamColor = AppColors.Coral,
                                offsetX = posX,
                                offsetY = posY,
                                onClick = { onPlayerClick?.invoke(player, awayTeamName, AppColors.Coral) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                TeamSubsListOnly(teamName = awayTeamName, subs = awayLineup.subs, teamColor = AppColors.Coral, onPlayerClick = onPlayerClick)
            }

            3 -> {
                // List Mode: Detailed Squad & Bench Roster
                Column(modifier = Modifier.fillMaxWidth()) {
                    TeamRosterDetailedSection(
                        teamName = homeTeamName,
                        lineup = homeLineup,
                        teamColor = AppColors.NeonBlue,
                        onPlayerClick = onPlayerClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TeamRosterDetailedSection(
                        teamName = awayTeamName,
                        lineup = awayLineup,
                        teamColor = AppColors.Coral,
                        onPlayerClick = onPlayerClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalFieldCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val lineColor = Color(0x35FFFFFF)
        val stroke = Stroke(width = 1.5f)

        // Outer border
        drawRect(color = lineColor, topLeft = Offset(10f, 10f), size = Size(w - 20f, h - 20f), style = stroke)

        // Grass stripes
        val stripes = 10
        val sH = (h - 20f) / stripes
        for (s in 0 until stripes step 2) {
            drawRect(color = Color(0x08FFFFFF), topLeft = Offset(10f, 10f + s * sH), size = Size(w - 20f, sH))
        }

        // Halfway line & circle
        drawLine(color = lineColor, start = Offset(10f, h / 2f), end = Offset(w - 10f, h / 2f), strokeWidth = 1.5f)
        drawCircle(color = lineColor, radius = w * 0.16f, center = Offset(w / 2f, h / 2f), style = stroke)
        drawCircle(color = lineColor, radius = 3f, center = Offset(w / 2f, h / 2f))

        // Top & Bottom Penalty Boxes
        val boxW = w * 0.52f
        val boxH = h * 0.14f
        drawRect(color = lineColor, topLeft = Offset((w - boxW) / 2f, 10f), size = Size(boxW, boxH), style = stroke)
        drawRect(color = lineColor, topLeft = Offset((w - boxW) / 2f, h - 10f - boxH), size = Size(boxW, boxH), style = stroke)
    }
}

@Composable
private fun PlayerPitchNode(
    player: PlayerLineup,
    teamColor: Color,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .width(76.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar circle with real photo or jersey number
        Box(contentAlignment = Alignment.Center) {
            PlayerAvatarView(
                photoUrl = player.photoUrl,
                playerName = player.name,
                playerNumber = player.number,
                teamColor = teamColor,
                size = 38.dp,
                fontSize = 12.sp,
                borderColor = Color.White,
                borderWidth = 1.8.dp
            )

            if (player.isCaptain) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(AppColors.NeonGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C", color = Color.Black, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // High contrast readable name badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xF0070D18))
                .border(0.9.dp, teamColor.copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.ifBlank { "لاعب ${player.number}" },
                color = Color.White,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // Subtitle badge: Number and Position
        Text(
            text = "${player.number} • ${player.position}",
            color = teamColor,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SubstitutesBenchRow(
    homeTeamName: String,
    awayTeamName: String,
    homeLineup: TeamLineup,
    awayLineup: TeamLineup,
    onPlayerClick: ((PlayerLineup, String, Color) -> Unit)?
) {
    Text(
        text = "دكة البدلاء (Substitutes) • اضغط للعرض 3D",
        color = Color.White,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

@Composable
private fun TeamSubsListOnly(
    teamName: String,
    subs: List<PlayerLineup>,
    teamColor: Color,
    onPlayerClick: ((PlayerLineup, String, Color) -> Unit)?
) {
    Text(
        text = "بدلاء $teamName (${subs.size} لاعباً)",
        color = teamColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    subs.forEach { sub ->
        BenchPlayerRow(
            player = sub,
            teamColor = teamColor,
            onClick = { onPlayerClick?.invoke(sub, teamName, teamColor) }
        )
        Spacer(modifier = Modifier.height(4.dp))
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PlayerAvatarView(
                photoUrl = player.photoUrl,
                playerName = player.name,
                playerNumber = player.number,
                teamColor = teamColor,
                size = 28.dp,
                fontSize = 10.sp,
                borderColor = teamColor,
                borderWidth = 1.2.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = player.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${player.number} • ${player.position}",
                    color = AppColors.TextMuted,
                    fontSize = 9.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.ViewInAr,
                contentDescription = "3D",
                tint = AppColors.NeonCyan,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "3D",
                color = AppColors.NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TeamRosterDetailedSection(
    teamName: String,
    lineup: TeamLineup,
    teamColor: Color,
    onPlayerClick: ((PlayerLineup, String, Color) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .border(1.dp, teamColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تشكيلة $teamName (${lineup.formation})",
                color = teamColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "المدرب: ${lineup.coach}",
                color = AppColors.TextSoft,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "الأساسيون (${lineup.starters.size} لاعباً)",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        lineup.starters.forEach { player ->
            DetailedPlayerCardRow(
                player = player,
                teamName = teamName,
                teamColor = teamColor,
                isStarter = true,
                onClick = { onPlayerClick?.invoke(player, teamName, teamColor) }
            )
            Spacer(modifier = Modifier.height(5.dp))
        }

        if (lineup.subs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "دكة البدلاء (${lineup.subs.size} لاعباً)",
                color = AppColors.TextSoft,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            lineup.subs.forEach { player ->
                DetailedPlayerCardRow(
                    player = player,
                    teamName = teamName,
                    teamColor = teamColor,
                    isStarter = false,
                    onClick = { onPlayerClick?.invoke(player, teamName, teamColor) }
                )
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun DetailedPlayerCardRow(
    player: PlayerLineup,
    teamName: String,
    teamColor: Color,
    isStarter: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PlayerAvatarView(
                photoUrl = player.photoUrl,
                playerName = player.name,
                playerNumber = player.number,
                teamColor = teamColor,
                size = 36.dp,
                fontSize = 12.sp,
                borderColor = teamColor,
                borderWidth = 1.2.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (player.isCaptain) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.NeonGold.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("قائد", color = AppColors.NeonGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "رقم ${player.number} • ${player.position} • تقييم ${player.rating}/10",
                    color = AppColors.TextSoft,
                    fontSize = 10.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.NeonCyan.copy(alpha = 0.15f))
                .border(1.dp, AppColors.NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.ViewInAr,
                    contentDescription = "3D",
                    tint = AppColors.NeonCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "عرض 3D",
                    color = AppColors.NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
