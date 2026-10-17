package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Match
import com.example.model.SportType
import com.example.ui.components.MatchCard
import com.example.ui.theme.AppColors

@Composable
fun SportsTab(
    matches: List<Match>,
    onMatchClick: (Match) -> Unit,
    onToggleFavorite: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSport by remember { mutableStateOf<SportType>(SportType.ALL) }

    val filteredMatches = remember(matches, selectedSport) {
        if (selectedSport == SportType.ALL) matches
        else matches.filter { it.sport == selectedSport.id }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = "Sports",
                    tint = AppColors.NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "كل الرياضات",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Horizontal Sport Chips Grid
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(SportType.entries) { sport ->
                    val isSelected = selectedSport == sport
                    val sportMatchCount = if (sport == SportType.ALL) matches.size
                    else matches.count { it.sport == sport.id }

                    Column(
                        modifier = Modifier
                            .width(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) AppColors.NeonBlue.copy(alpha = 0.22f)
                                else AppColors.Surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AppColors.NeonBlue else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { selectedSport = sport }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = sport.emoji, fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sport.labelAr,
                            color = if (isSelected) AppColors.NeonBlue else AppColors.TextSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$sportMatchCount مباريات",
                            color = AppColors.TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Sport Title & Count
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedSport.emoji} ${selectedSport.labelAr}",
                    color = AppColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${filteredMatches.size} مباراة)",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (filteredMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(selectedSport.emoji, fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد مباريات جارية أو مجدولة لهذه الرياضة حالياً",
                            color = AppColors.TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(filteredMatches, key = { it.id }) { match ->
                MatchCard(
                    match = match,
                    onClick = { onMatchClick(match) },
                    onToggleFavorite = { onToggleFavorite(match) }
                )
            }
        }
    }
}
