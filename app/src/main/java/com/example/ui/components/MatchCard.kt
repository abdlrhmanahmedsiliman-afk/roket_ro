package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Match
import com.example.model.MatchStatus
import com.example.ui.theme.AppColors

@Composable
fun MatchCard(
    match: Match,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTeamClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick,
        borderBrush = if (match.isLive) {
            Brush.horizontalGradient(
                colors = listOf(AppColors.NeonRed.copy(alpha = 0.5f), AppColors.Coral.copy(alpha = 0.3f))
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: League & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.league} ${if (match.country.isNotEmpty()) "• ${match.country}" else ""}",
                    color = AppColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                StatusBadge(status = match.status, minute = match.minute, shortDetail = match.shortDetail)

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (match.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (match.isFavorite) AppColors.NeonRed else AppColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teams & Score Center
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                TeamColumn(
                    name = match.homeTeam,
                    logo = match.homeLogo,
                    modifier = Modifier.weight(1f),
                    isHome = true,
                    onTeamClick = onTeamClick
                )

                // Score / Kickoff
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (match.status == MatchStatus.NOT_STARTED) {
                        Text(
                            text = match.kickoffTime,
                            color = AppColors.NeonBlue,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "يبدأ قريباً",
                            color = AppColors.TextMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            color = if (match.isLive) AppColors.TextPrimary else AppColors.TextSoft,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        val timeLabel = when {
                            match.status == MatchStatus.HALF_TIME -> "استراحة"
                            match.status == MatchStatus.FINISHED -> "انتهت"
                            match.shortDetail.isNotBlank() -> match.shortDetail
                            match.isLive -> "${match.minute}'"
                            else -> match.status.labelAr
                        }
                        Text(
                            text = timeLabel,
                            color = if (match.isLive) AppColors.NeonRed else AppColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Away Team
                TeamColumn(
                    name = match.awayTeam,
                    logo = match.awayLogo,
                    modifier = Modifier.weight(1f),
                    isHome = false,
                    onTeamClick = onTeamClick
                )
            }

            // AI Preview Callout
            if (match.aiPreview.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.NeonBlue.copy(alpha = 0.08f))
                        .border(1.dp, AppColors.NeonBlue.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Preview",
                        tint = AppColors.NeonBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = match.aiPreview,
                        color = AppColors.TextSoft,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }
            }

            // Probability Bar Footer
            if (match.homeWinProb > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                ProbabilityBar(
                    homeTeam = match.homeTeam,
                    awayTeam = match.awayTeam,
                    homeProb = match.homeWinProb,
                    drawProb = match.drawProb,
                    awayProb = match.awayWinProb
                )
            }
        }
    }
}

@Composable
private fun TeamColumn(
    name: String,
    logo: String,
    isHome: Boolean,
    onTeamClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = if (onTeamClick != null) {
            modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onTeamClick(name) }
                .padding(4.dp)
        } else modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (logo.isNotBlank()) {
                AsyncImage(
                    model = logo,
                    contentDescription = name,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = name,
                    tint = if (isHome) AppColors.NeonBlue else AppColors.Coral,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = name,
            color = AppColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatusBadge(status: MatchStatus, minute: Int, shortDetail: String = "") {
    val liveText = when {
        shortDetail.isNotBlank() && (shortDetail.contains("'") || shortDetail.contains("+")) -> "مباشر • $shortDetail"
        minute > 0 -> "مباشر • $minute'"
        else -> "مباشر"
    }
    val (badgeColor, label) = when (status) {
        MatchStatus.LIVE -> AppColors.NeonRed to liveText
        MatchStatus.HALF_TIME -> AppColors.Warning to (if (shortDetail.isNotBlank()) shortDetail else "استراحة")
        MatchStatus.EXTRA_TIME -> AppColors.NeonPurple to (if (shortDetail.isNotBlank()) shortDetail else "إضافي • $minute'")
        MatchStatus.PENALTIES -> AppColors.Coral to "ركلات ترجيح"
        MatchStatus.FINISHED -> AppColors.TextMuted to (if (shortDetail.isNotBlank() && shortDetail != "FT") shortDetail else "انتهت")
        MatchStatus.NOT_STARTED -> AppColors.NeonBlue to "قريباً"
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(badgeColor.copy(alpha = 0.16f))
            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status.isLive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = badgeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
