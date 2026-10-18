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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PreferencesManager
import com.example.ui.components.GlassCard
import com.example.ui.components.Rocket3D
import com.example.ui.theme.AppColors

@Composable
fun ProfileTab(
    preferencesManager: PreferencesManager,
    authManager: com.example.service.AuthManager? = null,
    onOpenAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteTeams by preferencesManager.favoriteTeams.collectAsState()
    val userProfile by authManager?.userProfile?.collectAsState() ?: remember {
        mutableStateOf(com.example.service.UserProfile())
    }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundAlertsEnabled by remember { mutableStateOf(true) }
    var sensor3dEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp)
    ) {
        // User Profile Header Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                borderBrush = Brush.linearGradient(
                    colors = listOf(AppColors.NeonBlue.copy(alpha = 0.5f), AppColors.NeonCyan.copy(alpha = 0.2f))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(AppColors.NeonGradient)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "User Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile.displayName,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AppColors.NeonCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (userProfile.isGoogleUser) "GOOGLE VERIFIED" else "PRO VIP",
                                        color = AppColors.NeonCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = userProfile.email,
                                color = AppColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!userProfile.isGoogleUser) {
                        Button(
                            onClick = {
                                authManager?.signInWithGoogle("مستخدم Google الرسمي", "user@gmail.com")
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ربط الحساب عبر Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { authManager?.signOut() },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تسجيل الخروج", color = AppColors.NeonRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Quick Stats row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileStatBadge("الفرق المفضلة", "${favoriteTeams.size}", AppColors.NeonBlue, Modifier.weight(1f))
                ProfileStatBadge("المباريات المتابعة", "12", AppColors.Coral, Modifier.weight(1f))
                ProfileStatBadge("دقة التنبؤ الإحصائي", "84%", AppColors.Success, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Favorite Teams Manager
        item {
            Text(
                text = "الفرق الرياضية المفضلة",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (favoriteTeams.isEmpty()) {
                        Text(
                            text = "لم تقم بإضافة أي فريق للمفضلة بعد. يمكنك النقر على القلب في أي بطاقة مباراة.",
                            color = AppColors.TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(favoriteTeams.toList()) { team ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.NeonRed.copy(alpha = 0.15f))
                                        .border(1.dp, AppColors.NeonRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { preferencesManager.toggleFavoriteTeam(team) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = AppColors.NeonRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = team, color = AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = AppColors.TextMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Settings & Toggles
        item {
            Text(
                text = "الإعدادات والتفضيلات",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingToggleRow(
                        icon = Icons.Rounded.Notifications,
                        title = "إشعارات الأهداف والأحداث المباشرة",
                        subtitle = "تنبيه لحظي عند تسجيل الأهداف والبطاقات الحمراء",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingToggleRow(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        title = "المؤثرات الصوتية والاهتزاز التوربيني",
                        subtitle = "اهتزاز تفاعلي عند هز الجهاز وتحديث النتائج",
                        checked = soundAlertsEnabled,
                        onCheckedChange = { soundAlertsEnabled = it }
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingToggleRow(
                        icon = Icons.Rounded.ViewInAr,
                        title = "تأثير الصاروخ ثلاثي الأبعاد بالحساسات (3D)",
                        subtitle = "استخدام الجيروسكوب ومقياس التسارع لإمالة الصاروخ",
                        checked = sensor3dEnabled,
                        onCheckedChange = { sensor3dEnabled = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // About roket_ro App
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Rocket3D(size = 70.dp, showFlame = false, enableTilt = false)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "roket_ro v3.0.0",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "المنصة الذكية الأولى للنتائج المباشرة والتحليل الرياضي التنبؤي",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onOpenAiChat,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.NeonBlue)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AppColors.NeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مساعد الذكاء الاصطناعي", color = AppColors.NeonBlue, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatBadge(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, color = AppColors.TextMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.NeonBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = AppColors.TextMuted, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AppColors.NeonBlue,
                uncheckedThumbColor = AppColors.TextMuted,
                uncheckedTrackColor = AppColors.SurfaceSoft
            )
        )
    }
}
