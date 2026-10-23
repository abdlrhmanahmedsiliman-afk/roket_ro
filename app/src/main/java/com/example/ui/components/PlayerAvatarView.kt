package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.provider.PlayerDatabaseProvider
import com.example.ui.theme.AppColors

@Composable
fun PlayerAvatarView(
    photoUrl: String?,
    playerName: String,
    playerNumber: Int = 0,
    teamColor: Color = AppColors.NeonCyan,
    size: Dp = 40.dp,
    fontSize: TextUnit = 12.sp,
    borderColor: Color = Color.White.copy(alpha = 0.85f),
    borderWidth: Dp = 1.5.dp,
    modifier: Modifier = Modifier
) {
    val cleanUrl = remember(photoUrl) {
        if (!photoUrl.isNullOrBlank() && !photoUrl.equals("null", ignoreCase = true) && !photoUrl.contains("/null")) {
            photoUrl.trim()
        } else {
            null
        }
    }

    val initial = remember(playerName) {
        playerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
    }

    @Composable
    fun FallbackBadge() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (initial.isNotEmpty()) {
                if (playerNumber > 0 && size >= 36.dp) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = (fontSize.value * 0.95f).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "#$playerNumber",
                            color = teamColor,
                            fontSize = (fontSize.value * 0.7f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (playerNumber > 0) {
                Text(
                    text = "$playerNumber",
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = playerName,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(teamColor.copy(alpha = 0.35f))
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!cleanUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cleanUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = playerName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { FallbackBadge() },
                error = { FallbackBadge() }
            )
        } else {
            FallbackBadge()
        }
    }
}
