package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.provider.PlayerDatabaseProvider
import com.example.model.PlayerLineup
import com.example.model.TeamDetail
import com.example.model.TeamPlayerStat
import com.example.ui.theme.AppColors

@Composable
fun TeamDetailDialog(
    teamName: String,
    leagueName: String = "",
    onDismiss: () -> Unit,
    onPlayerClick: (PlayerLineup) -> Unit
) {
    val teamDetail = remember(teamName) {
        PlayerDatabaseProvider.getTeamDetail(teamName, leagueName)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("نظرة عامة", "قائمة اللاعبين", "المباريات", "الانتقالات", "الهدافون")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            color = AppColors.BgElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "الملف التعريفي للنادي",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.NeonBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = teamDetail.country,
                            color = AppColors.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Team Hero Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Team Logo with Fallback
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.5.dp, AppColors.NeonBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (teamDetail.logo.isNotBlank()) {
                                AsyncImage(
                                    model = teamDetail.logo,
                                    contentDescription = teamDetail.teamName,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text("⚽", fontSize = 28.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = teamDetail.teamName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "المدرب: ${teamDetail.coach}",
                                color = AppColors.NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "الملعب: ${teamDetail.stadium}",
                                color = AppColors.TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Form Chips
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "آخر النتائج",
                                color = AppColors.TextMuted,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                teamDetail.recentForm.take(5).forEach { result ->
                                    val bg = when (result) {
                                        "W" -> AppColors.Success
                                        "D" -> AppColors.Warning
                                        else -> AppColors.NeonRed
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(bg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(result, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tabs Navigation Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = AppColors.BgElevated,
                    contentColor = AppColors.NeonCyan,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) AppColors.NeonCyan else AppColors.TextSoft,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Content Body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Overview & League Stats
                            item {
                                Text(
                                    text = "إحصائيات الموسم في الدوري",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatBox("المركز", "#${teamDetail.rank}", AppColors.NeonBlue, Modifier.weight(1f))
                                    StatBox("النقاط", "${teamDetail.points}", AppColors.NeonCyan, Modifier.weight(1f))
                                    StatBox("لعب", "${teamDetail.played}", Color.White, Modifier.weight(1f))
                                    StatBox("فاز", "${teamDetail.won}", AppColors.Success, Modifier.weight(1f))
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatBox("تعادل", "${teamDetail.drawn}", AppColors.Warning, Modifier.weight(1f))
                                    StatBox("خسر", "${teamDetail.lost}", AppColors.NeonRed, Modifier.weight(1f))
                                    StatBox("له", "${teamDetail.goalsFor}", AppColors.Success, Modifier.weight(1f))
                                    StatBox("عليه", "${teamDetail.goalsAgainst}", AppColors.NeonRed, Modifier.weight(1f))
                                }
                            }
                        }
                        1 -> {
                            // Full Squad List
                            item {
                                Text(
                                    text = "قائمة لاعبي الفريق (${teamDetail.squad.size} لاعباً) • اضغط على أي لاعب لعرض ملفه",
                                    color = AppColors.NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(teamDetail.squad) { player ->
                                SquadPlayerRow(
                                    player = player,
                                    onClick = {
                                        onPlayerClick(
                                            PlayerLineup(
                                                number = player.number,
                                                name = player.name,
                                                position = player.position,
                                                rating = player.rating,
                                                photoUrl = player.photoUrl,
                                                athleteId = player.athleteId
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        2 -> {
                            // Recent Results & Upcoming
                            item {
                                Text(
                                    text = "جدول المباريات والنتائج",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            item {
                                MatchScheduleRow(
                                    matchTitle = "${teamDetail.teamName} × منافس الجولة",
                                    resultOrTime = "2 - 1",
                                    status = "انتهت",
                                    competition = teamDetail.league,
                                    isWin = true
                                )
                            }
                            item {
                                MatchScheduleRow(
                                    matchTitle = "ديربي العاصمة × ${teamDetail.teamName}",
                                    resultOrTime = "0 - 3",
                                    status = "انتهت",
                                    competition = teamDetail.league,
                                    isWin = true
                                )
                            }
                            item {
                                MatchScheduleRow(
                                    matchTitle = "${teamDetail.teamName} × قمة الجولة القادمة",
                                    resultOrTime = "21:00",
                                    status = "قادمة",
                                    competition = teamDetail.league,
                                    isWin = null
                                )
                            }
                        }
                        3 -> {
                            // Transfers
                            item {
                                Text(
                                    text = "سوق الانتقالات الصيفية والشتوية (2024/2025)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(teamDetail.transfers) { transfer ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.Surface)
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(if (transfer.isIncoming) AppColors.Success.copy(alpha = 0.2f) else AppColors.NeonRed.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (transfer.isIncoming) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = if (transfer.isIncoming) AppColors.Success else AppColors.NeonRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = transfer.playerName,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${transfer.type} • ${transfer.otherClub}",
                                                    color = AppColors.TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = transfer.fee,
                                            color = AppColors.NeonCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                        4 -> {
                            // Top Scorers in Club
                            item {
                                Text(
                                    text = "قائمة هدافي النادي هذا الموسم",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            val clubScorers = teamDetail.squad.filter { it.goals > 0 }.sortedByDescending { it.goals }
                            items(clubScorers) { scorer ->
                                SquadPlayerRow(
                                    player = scorer,
                                    onClick = {
                                        onPlayerClick(
                                            PlayerLineup(
                                                number = scorer.number,
                                                name = scorer.name,
                                                position = scorer.position,
                                                rating = scorer.rating,
                                                photoUrl = scorer.photoUrl
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = AppColors.TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun SquadPlayerRow(
    player: TeamPlayerStat,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Jersey Number Badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.NeonBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${player.number}",
                    color = AppColors.NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Player Avatar
            PlayerAvatarView(
                photoUrl = player.photoUrl,
                playerName = player.name,
                playerNumber = player.number,
                teamColor = AppColors.NeonCyan,
                size = 42.dp,
                fontSize = 14.sp,
                borderColor = AppColors.NeonCyan.copy(alpha = 0.5f),
                borderWidth = 1.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${player.position} • ${player.nationality} • ${player.age} سنة",
                    color = AppColors.TextMuted,
                    fontSize = 11.sp
                )
            }

            // Rating & Stats Badge
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.NeonBlue.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ ${player.rating}",
                        color = AppColors.NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (player.goals > 0 || player.assists > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⚽ ${player.goals}  👟 ${player.assists}",
                        color = AppColors.TextSoft,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MatchScheduleRow(
    matchTitle: String,
    resultOrTime: String,
    status: String,
    competition: String,
    isWin: Boolean?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = matchTitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = competition,
                    color = AppColors.TextMuted,
                    fontSize = 10.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (isWin) {
                            true -> AppColors.Success.copy(alpha = 0.2f)
                            false -> AppColors.NeonRed.copy(alpha = 0.2f)
                            else -> AppColors.NeonBlue.copy(alpha = 0.15f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$resultOrTime ($status)",
                    color = when (isWin) {
                        true -> AppColors.Success
                        false -> AppColors.NeonRed
                        else -> AppColors.NeonCyan
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
