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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.Favorite
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
import com.example.ui.components.MatchCard
import com.example.ui.components.TeamHubDialog
import com.example.ui.theme.AppColors

@Composable
fun FavoritesTab(
    favoriteTeams: Set<String>,
    matches: List<Match>,
    onToggleFavoriteTeam: (String) -> Unit,
    onMatchClick: (Match) -> Unit,
    onToggleFavoriteMatch: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTeamForDialog by remember { mutableStateOf<String?>(null) }

    val favoriteMatches = matches.filter { match ->
        match.isFavorite || favoriteTeams.contains(match.homeTeam) || favoriteTeams.contains(match.awayTeam)
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
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Favorites",
                    tint = AppColors.NeonRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "المفضلة",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Favorite Teams Chips Row
        item {
            Text(
                text = "الفرق واللاعبين المفضلين",
                color = AppColors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (favoriteTeams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.Surface)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "اضغط على أيقونة النجمة/القلب في أي بطاقة لإضافة فريقك المفضل",
                        color = AppColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favoriteTeams.toList()) { team ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppColors.NeonRed.copy(alpha = 0.15f))
                                .border(1.dp, AppColors.NeonRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable { selectedTeamForDialog = team }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = team,
                                tint = AppColors.NeonRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = team,
                                color = AppColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = AppColors.TextMuted,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onToggleFavoriteTeam(team) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Favorite Matches Header
        item {
            Text(
                text = "مباريات فرقك المفضلة (${favoriteMatches.size})",
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (favoriteMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AppColors.SurfaceSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = AppColors.TextMuted,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد مباريات مجدولة لفرقك المفضلة حالياً",
                            color = AppColors.TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(favoriteMatches, key = { it.id }) { match ->
                MatchCard(
                    match = match,
                    onClick = { onMatchClick(match) },
                    onToggleFavorite = { onToggleFavoriteMatch(match) },
                    onTeamClick = { selectedTeamForDialog = it }
                )
            }
        }
    }

    val teamToView = selectedTeamForDialog
    if (teamToView != null) {
        TeamHubDialog(
            teamName = teamToView,
            onDismiss = { selectedTeamForDialog = null },
            onPlayerClick = { }
        )
    }
}
