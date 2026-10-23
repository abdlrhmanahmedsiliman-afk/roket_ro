package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.provider.DataResult
import com.example.data.provider.FootballDataOrgProvider
import com.example.model.League
import com.example.model.LeagueTier
import com.example.model.PlayerLineup
import com.example.model.StandingRow
import com.example.model.TopScorer
import com.example.ui.components.GlassCard
import com.example.ui.components.Player3DBottomSheet
import com.example.ui.components.PlayerAvatarView
import com.example.ui.components.TeamDetailDialog
import com.example.data.provider.PlayerDatabaseProvider
import com.example.ui.components.TeamHubDialog
import com.example.ui.theme.AppColors

@Composable
fun LeaguesTab(
    leagues: List<League>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf<LeagueTier?>(null) }
    var selectedLeagueId by remember { mutableStateOf(leagues.firstOrNull()?.id ?: "") }
    var subTab by remember { mutableIntStateOf(0) } // 0: جدول الترتيب, 1: الهدافون

    var selectedTeamDetail by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedPlayer3D by remember { mutableStateOf<PlayerLineup?>(null) }

    val filteredLeagues = remember(leagues, searchQuery, selectedTier) {
        leagues.filter { league ->
            val matchesTier = selectedTier == null || league.tier == selectedTier
            val matchesSearch = searchQuery.isBlank() ||
                    league.name.contains(searchQuery, ignoreCase = true) ||
                    league.country.contains(searchQuery, ignoreCase = true) ||
                    league.sport.contains(searchQuery, ignoreCase = true)
            matchesTier && matchesSearch
        }
    }

    val currentLeague = remember(filteredLeagues, selectedLeagueId) {
        filteredLeagues.firstOrNull { it.id == selectedLeagueId }
            ?: filteredLeagues.firstOrNull()
            ?: leagues.firstOrNull()
    }

    val fdoCode = remember(currentLeague) {
        currentLeague?.let { FootballDataOrgProvider.getCompetitionCode(it.name.ifBlank { it.id }) }
    }

    val fdoStandingsState = produceState<DataResult<List<StandingRow>>?>(initialValue = null, fdoCode) {
        value = if (fdoCode != null) {
            FootballDataOrgProvider.fetchStandings(fdoCode)
        } else {
            null
        }
    }

    val fdoScorersState = produceState<DataResult<List<TopScorer>>?>(initialValue = null, fdoCode) {
        value = if (fdoCode != null) {
            FootballDataOrgProvider.fetchTopScorers(fdoCode, limit = 20)
        } else {
            null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 120.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "Leagues",
                    tint = AppColors.Gold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "مستكشف البطولات والدوريات العالمية",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${leagues.size} دوري وبطولة عبر 4 مستويات وتصنيفات رياضية",
                        color = AppColors.TextSoft,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في 50+ دوري وبطولة، دولة، أو رياضة...", color = AppColors.TextMuted, fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = AppColors.TextSoft, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.NeonBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = AppColors.SurfaceSoft,
                    unfocusedContainerColor = AppColors.Surface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Tier Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isAll = selectedTier == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAll) AppColors.NeonCyan else AppColors.Surface)
                            .clickable { selectedTier = null }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "جميع المستويات (${leagues.size})",
                            color = if (isAll) Color.Black else AppColors.TextSoft,
                            fontSize = 11.sp,
                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                items(LeagueTier.entries) { tier ->
                    val isSelected = selectedTier == tier
                    val count = leagues.count { it.tier == tier }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(tier.badgeColor) else AppColors.Surface)
                            .clickable { selectedTier = tier }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${tier.labelAr} ($count)",
                            color = if (isSelected) Color.Black else AppColors.TextSoft,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Horizontal League Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLeagues) { league ->
                    val isSelected = currentLeague?.id == league.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AppColors.NeonBlue else AppColors.Surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedLeagueId = league.id }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = league.name,
                            color = if (isSelected) Color.Black else AppColors.TextSoft,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (currentLeague != null) {
            // Selected League Header Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 14.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentLeague.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentLeague.country} • الموسم ${currentLeague.season}",
                                    color = AppColors.TextSoft,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(currentLeague.tier.badgeColor).copy(alpha = 0.18f))
                                    .border(1.dp, Color(currentLeague.tier.badgeColor).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentLeague.tier.labelAr,
                                    color = Color(currentLeague.tier.badgeColor),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Sub Tab: Standings vs Top Scorers
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Surface)
                        .padding(4.dp)
                ) {
                    listOf("جدول الترتيب", "قائمة الهدافين").forEachIndexed { idx, title ->
                        val isSelected = subTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppColors.NeonBlue else Color.Transparent)
                                .clickable { subTab = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.Black else AppColors.TextSoft,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (subTab == 0) {
                // Standings Tab
                val standingsResult = fdoStandingsState.value
                val isFdo = fdoCode != null

                // Status Banner
                item {
                    val statusText = when {
                        isFdo && standingsResult == null -> "جارٍ استرداد الترتيب المباشر من football-data.org ($fdoCode)..."
                        isFdo && standingsResult is DataResult.Success -> "المصدر: football-data.org (مباشر • كاش 30 دقيقة)"
                        isFdo && standingsResult is DataResult.Empty -> "جدول الترتيب: غير متاح (${standingsResult.message})"
                        isFdo && standingsResult is DataResult.Error -> "football-data.org: ${standingsResult.message}"
                        else -> "المصدر: قاعدة بيانات البطولة (${currentLeague.name})"
                    }
                    val statusColor = when {
                        isFdo && standingsResult is DataResult.Success -> AppColors.NeonCyan
                        isFdo && standingsResult is DataResult.Error -> AppColors.Warning
                        else -> AppColors.TextMuted
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isFdo && standingsResult == null) {
                            CircularProgressIndicator(
                                color = AppColors.NeonBlue,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Table Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.SurfaceSoft)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(22.dp))
                        Text("الفريق", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("لعب", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Text("فاز", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                        Text("تعادل", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                        Text("خسر", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                        Text("فارق", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        Text("نقاط", color = AppColors.NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val activeStandings = when {
                    isFdo && standingsResult is DataResult.Success -> standingsResult.data
                    isFdo && standingsResult is DataResult.Error && currentLeague.standings.isNotEmpty() -> currentLeague.standings
                    !isFdo -> currentLeague.standings
                    else -> emptyList()
                }

                if (activeStandings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFdo && standingsResult is DataResult.Error) {
                                    "غير متاح: ${standingsResult.message}"
                                } else {
                                    "جدول الترتيب غير متاح لهذه البطولة حالياً."
                                },
                                color = AppColors.TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(activeStandings) { row ->
                        val isChampions = row.rank <= 4
                        val isEuropa = row.rank in 5..6
                        val isRelegation = activeStandings.size >= 6 && row.rank >= (activeStandings.size - 2)
                        val rankColor = when {
                            isChampions -> AppColors.NeonBlue
                            isEuropa -> AppColors.NeonPurple
                            isRelegation -> AppColors.NeonRed
                            else -> AppColors.TextMuted
                        }
                        val rowBg = when {
                            isChampions -> AppColors.NeonBlue.copy(alpha = 0.08f)
                            isRelegation -> AppColors.NeonRed.copy(alpha = 0.08f)
                            else -> Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(rowBg)
                                .clickable { selectedTeamDetail = row.teamName to currentLeague.name }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(rankColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${row.rank}",
                                    color = rankColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            if (row.logo.isNotBlank()) {
                                AsyncImage(
                                    model = row.logo,
                                    contentDescription = row.teamName,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.teamName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (row.form.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        row.form.takeLast(5).forEach { badge ->
                                            val (bg, textColor) = when (badge) {
                                                "W" -> AppColors.Success to Color.Black
                                                "D" -> AppColors.Warning to Color.Black
                                                "L" -> AppColors.NeonRed to Color.White
                                                else -> AppColors.SurfaceSoft to AppColors.TextSoft
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .clip(CircleShape)
                                                    .background(bg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = badge,
                                                    color = textColor,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Text("${row.played}", color = AppColors.TextSoft, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("${row.won}", color = AppColors.TextSoft, fontSize = 12.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            Text("${row.drawn}", color = AppColors.TextSoft, fontSize = 12.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            Text("${row.lost}", color = AppColors.TextSoft, fontSize = 12.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            Text(
                                text = if (row.goalDiff > 0) "+${row.goalDiff}" else "${row.goalDiff}",
                                color = if (row.goalDiff >= 0) AppColors.NeonCyan else AppColors.NeonRed,
                                fontSize = 11.sp,
                                modifier = Modifier.width(30.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${row.points}",
                                color = AppColors.NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(34.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.NeonBlue))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دوري الأبطال / النخبة", color = AppColors.TextMuted, fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.NeonPurple))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الدوري الأوروبي / الاتحاد", color = AppColors.TextMuted, fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.NeonRed))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("منطقة الهبوط", color = AppColors.TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            } else {
                // Top Scorers Tab
                val scorersResult = fdoScorersState.value
                val isFdo = fdoCode != null

                // Status Banner
                item {
                    val statusText = when {
                        isFdo && scorersResult == null -> "جارٍ استرداد قائمة الهدافين الرسمية من football-data.org ($fdoCode)..."
                        isFdo && scorersResult is DataResult.Success -> "قائمة الهدافين الرسمية: football-data.org (مباشر • كاش 30 دقيقة)"
                        isFdo && scorersResult is DataResult.Empty -> "قائمة الهدافين: غير متاح (${scorersResult.message})"
                        isFdo && scorersResult is DataResult.Error -> "football-data.org: ${scorersResult.message}"
                        else -> "قائمة الهدافين: ${currentLeague.name}"
                    }
                    val statusColor = when {
                        isFdo && scorersResult is DataResult.Success -> AppColors.NeonCyan
                        isFdo && scorersResult is DataResult.Error -> AppColors.Warning
                        else -> AppColors.TextMuted
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isFdo && scorersResult == null) {
                            CircularProgressIndicator(
                                color = AppColors.NeonBlue,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val activeScorers = when {
                    isFdo && scorersResult is DataResult.Success -> scorersResult.data
                    isFdo && scorersResult is DataResult.Error && currentLeague.topScorers.isNotEmpty() -> currentLeague.topScorers
                    !isFdo -> currentLeague.topScorers
                    else -> emptyList()
                }

                if (activeScorers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFdo && scorersResult is DataResult.Error) {
                                    "غير متاح: ${scorersResult.message}"
                                } else {
                                    "قائمة الهدافين: غير متاح لهذه البطولة في المصادر الحالية."
                                },
                                color = AppColors.TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(activeScorers) { scorer ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = 10.dp,
                            onClick = { selectedTeamDetail = scorer.teamName to currentLeague.name }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${scorer.rank}",
                                    color = if (scorer.rank == 1) AppColors.Gold else AppColors.NeonBlue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.width(28.dp)
                                )
                                PlayerAvatarView(
                                    photoUrl = scorer.playerPhoto.ifBlank { null },
                                    playerName = scorer.playerName,
                                    playerNumber = scorer.penalties,
                                    teamColor = if (scorer.rank == 1) AppColors.Gold else AppColors.NeonCyan,
                                    size = 40.dp,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable {
                                        selectedPlayer3D = PlayerLineup(
                                            number = scorer.rank,
                                            name = scorer.playerName,
                                            position = "FWD",
                                            rating = 8.5f,
                                            photoUrl = scorer.playerPhoto
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scorer.playerName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (scorer.teamLogo.isNotBlank()) {
                                            AsyncImage(
                                                model = scorer.teamLogo,
                                                contentDescription = scorer.teamName,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = scorer.teamName,
                                            color = AppColors.TextSoft,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${scorer.goals}",
                                            color = AppColors.NeonCyan,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "أهداف",
                                            color = AppColors.TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${scorer.assists}",
                                            color = AppColors.NeonBlue,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "صناعة",
                                            color = AppColors.TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    if (scorer.penalties > 0) {
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${scorer.penalties}",
                                                color = AppColors.Gold,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "جزاء",
                                                color = AppColors.TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Complete Interconnection: Team Profile Dialog with Squad, Transfers, and Top Scorers
    val teamToView = selectedTeamDetail
    if (teamToView != null) {
        TeamHubDialog(
            teamName = teamToView.first,
            leagueName = teamToView.second,
            onDismiss = { selectedTeamDetail = null },
            onPlayerClick = { player ->
                selectedPlayer3D = player
            }
        )
    }

    // Complete Interconnection: 3D Player Interactive Sheet
    val playerToView = selectedPlayer3D
    if (playerToView != null) {
        Player3DBottomSheet(
            player = playerToView,
            teamName = selectedTeamDetail?.first ?: "",
            teamColor = AppColors.NeonBlue,
            playerCondition = null,
            onDismiss = { selectedPlayer3D = null }
        )
    }
}
