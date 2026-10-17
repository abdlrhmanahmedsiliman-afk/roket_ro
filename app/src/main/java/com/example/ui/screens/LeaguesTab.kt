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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.League
import com.example.model.LeagueTier
import com.example.ui.components.GlassCard
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
                // Standings Table Header
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
                        Text("لعب", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        Text("فاز", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        Text("تعادل", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        Text("خسر", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        Text("فارق", color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        Text("نقاط", color = AppColors.NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (currentLeague.standings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("جدول الترتيب قيد التحديث من مزود البيانات...", color = AppColors.TextMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(currentLeague.standings) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (row.rank <= 4) AppColors.Surface.copy(alpha = 0.8f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${row.rank}",
                                color = if (row.rank <= 4) AppColors.NeonBlue else AppColors.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(22.dp)
                            )
                            Text(
                                text = row.teamName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${row.played}", color = AppColors.TextSoft, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                            Text("${row.won}", color = AppColors.TextSoft, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                            Text("${row.drawn}", color = AppColors.TextSoft, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                            Text("${row.lost}", color = AppColors.TextSoft, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                            Text(
                                text = if (row.goalDiff > 0) "+${row.goalDiff}" else "${row.goalDiff}",
                                color = if (row.goalDiff >= 0) AppColors.NeonCyan else AppColors.NeonRed,
                                fontSize = 11.sp,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${row.points}",
                                color = AppColors.NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Top Scorers List
                if (currentLeague.topScorers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("قائمة الهدافين قيد التحديث...", color = AppColors.TextMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(currentLeague.topScorers) { scorer ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = 10.dp
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scorer.playerName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = scorer.teamName,
                                        color = AppColors.TextSoft,
                                        fontSize = 11.sp
                                    )
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
                                    Spacer(modifier = Modifier.width(16.dp))
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
