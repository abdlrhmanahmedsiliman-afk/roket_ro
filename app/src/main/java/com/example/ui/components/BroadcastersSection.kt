package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Broadcaster
import com.example.model.RegionZone
import com.example.model.StreamFormat
import com.example.ui.theme.AppColors

@Composable
fun BroadcastersSection(
    broadcasters: List<Broadcaster>,
    modifier: Modifier = Modifier
) {
    var selectedRegion by remember { mutableStateOf(RegionZone.ALL) }
    val context = LocalContext.current

    val filteredBroadcasters = remember(broadcasters, selectedRegion) {
        if (selectedRegion == RegionZone.ALL) broadcasters
        else broadcasters.filter { it.region == selectedRegion || it.region == RegionZone.INTERNATIONAL }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legal compliance header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.NeonCyan.copy(alpha = 0.10f))
                .border(1.dp, AppColors.NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = "Verified",
                tint = AppColors.NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "دليل القنوات الناقلة الرسمية والمرخصة 100% (خالٍ من المراهنات والقرصنة)",
                color = AppColors.NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Region Selector Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RegionZone.entries) { region ->
                val isSelected = selectedRegion == region
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AppColors.NeonBlue else AppColors.Surface)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedRegion = region }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = region.flag, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = region.nameAr,
                            color = if (isSelected) Color.Black else AppColors.TextSoft,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredBroadcasters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد قنوات ناقلة مسجلة لهذه المنطقة حاليًا",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredBroadcasters.forEach { broadcaster ->
                    BroadcasterItemCard(
                        broadcaster = broadcaster,
                        onOpenPlatform = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(broadcaster.officialPlatformUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BroadcasterItemCard(
    broadcaster: Broadcaster,
    onOpenPlatform: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppColors.SurfaceSoft)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = "TV",
                            tint = AppColors.NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = broadcaster.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${broadcaster.region.flag} ${broadcaster.region.nameAr} • ${broadcaster.channelNumberOrPackage}",
                            color = AppColors.TextSoft,
                            fontSize = 11.sp
                        )
                    }
                }

                // Format badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(broadcaster.format.badgeColor).copy(alpha = 0.18f))
                        .border(1.dp, Color(broadcaster.format.badgeColor).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = broadcaster.format.labelAr,
                        color = Color(broadcaster.format.badgeColor),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Commentators & Audio Language
            if (broadcaster.commentators.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = "Commentator",
                        tint = AppColors.TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "التعليق: ${broadcaster.commentators.joinToString("، ")} (${broadcaster.language})",
                        color = AppColors.TextPrimary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Open Official Stream Platform
            ElevatedButton(
                onClick = onOpenPlatform,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = AppColors.NeonBlue.copy(alpha = 0.16f),
                    contentColor = AppColors.NeonBlue
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = "Watch",
                        tint = AppColors.NeonBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فتح المنصة الرسمية للبث",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
