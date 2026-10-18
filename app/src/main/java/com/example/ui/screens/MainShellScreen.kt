package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Match
import com.example.service.GeminiAiService
import com.example.service.PreferencesManager
import com.example.service.SportsRepository
import com.example.ui.components.AiFab
import com.example.ui.components.Rocket3D
import com.example.ui.theme.AppColors

enum class MainNavSection(val title: String, val subtitle: String, val icon: ImageVector) {
    ANALYSIS("التحليل", "تحليلات الذكاء الاصطناعي", Icons.Rounded.AutoAwesome),
    MATCHES("المباريات", "النتائج وموجز المباريات", Icons.Rounded.SportsSoccer),
    LEAGUES("الدوريات", "مستكشف 50+ بطولة", Icons.Rounded.EmojiEvents),
    PROFILE("الملف الشخصي", "تفضيلاتك وإعداداتك", Icons.Rounded.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    repository: SportsRepository,
    preferencesManager: PreferencesManager,
    aiService: GeminiAiService,
    authManager: com.example.service.AuthManager? = null,
    notificationCenter: com.example.service.NotificationCenter? = null,
    modifier: Modifier = Modifier
) {
    var currentSection by remember { mutableStateOf(MainNavSection.MATCHES) }
    var selectedMatch by remember { mutableStateOf<Match?>(null) }
    var isAiAssistantOpen by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val matches by repository.matches.collectAsState()
    val leagues by repository.leagues.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val refreshNotification by repository.refreshNotification.collectAsState()
    val connectionQuality by repository.connectionMonitor?.quality?.collectAsState() ?: remember { mutableStateOf(null) }
    val unreadNotifsCount by notificationCenter?.unreadCount?.collectAsState() ?: remember { mutableIntStateOf(2) }

    // Sub-screens conditional rendering
    if (selectedMatch != null) {
        MatchDetailScreen(
            match = selectedMatch!!,
            aiService = aiService,
            onBack = { selectedMatch = null },
            onToggleFavorite = {
                preferencesManager.toggleFavoriteMatch(selectedMatch!!.id)
                repository.refreshMatches()
                selectedMatch = repository.getMatchById(selectedMatch!!.id)
            }
        )
        return
    }

    if (isAiAssistantOpen) {
        AiAssistantScreen(
            aiService = aiService,
            matches = matches,
            onBack = { isAiAssistantOpen = false }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.Bg,
        topBar = {
            // Main Top Bar
            Surface(
                color = AppColors.BgElevated.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 3D Mini Rocket Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppColors.SurfaceSoft)
                                .border(1.dp, AppColors.NeonBlue.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Rocket3D(size = 32.dp, showFlame = false, enableTilt = false)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // App Title & Current Section Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "roket_ro",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AppColors.NeonBlue.copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = currentSection.title,
                                        color = AppColors.NeonBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = currentSection.subtitle,
                                color = AppColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        // 60fps & Network Health Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.Surface)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(connectionQuality?.indicatorColorHex ?: 0xFF4FE3C1))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "60fps",
                                    color = AppColors.NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Top Bar Action Buttons
                        IconButton(
                            onClick = { repository.refreshMatches() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "تحديث",
                                tint = AppColors.NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Notifications Bell Button with Unread Badge
                        Box {
                            IconButton(
                                onClick = { showNotificationsDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Surface)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = "الإشعارات",
                                    tint = AppColors.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (unreadNotifsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(AppColors.NeonRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$unreadNotifsCount",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { isAiAssistantOpen = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppColors.NeonBlue.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "مساعد AI",
                                tint = AppColors.NeonBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Main Bottom Navigation Bar: Analysis, Matches, Profile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = AppColors.NeonBlue.copy(alpha = 0.3f),
                            ambientColor = Color.Black
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(AppColors.BgElevated.copy(alpha = 0.96f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainNavSection.entries.forEach { section ->
                            val isSelected = currentSection == section
                            MainBottomNavItem(
                                section = section,
                                isSelected = isSelected,
                                onClick = { currentSection = section }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentSection,
                label = "section_transition"
            ) { section ->
                when (section) {
                    MainNavSection.ANALYSIS -> {
                        AnalysisTab(
                            matches = matches,
                            aiService = aiService,
                            onMatchClick = { selectedMatch = it },
                            onOpenAiChat = { isAiAssistantOpen = true }
                        )
                    }
                    MainNavSection.MATCHES -> {
                        // Scrollable List Area for displaying Match Summaries
                        MatchesTab(
                            matches = matches,
                            isLoading = isLoading,
                            onRefresh = { repository.refreshMatches() },
                            onMatchClick = { selectedMatch = it },
                            onToggleFavorite = { match ->
                                preferencesManager.toggleFavoriteMatch(match.id)
                                repository.refreshMatches()
                            },
                            refreshNotification = refreshNotification
                        )
                    }
                    MainNavSection.LEAGUES -> {
                        LeaguesTab(leagues = leagues)
                    }
                    MainNavSection.PROFILE -> {
                        ProfileTab(
                            preferencesManager = preferencesManager,
                            authManager = authManager,
                            onOpenAiChat = { isAiAssistantOpen = true }
                        )
                    }
                }
            }

            // Floating AI Rocket FAB positioned on bottom right above navigation bar
            AiFab(
                onClick = { isAiAssistantOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 6.dp)
            )
        }

        if (showNotificationsDialog) {
            NotificationsCenterDialog(
                notificationCenter = notificationCenter,
                onDismiss = { showNotificationsDialog = false }
            )
        }
    }
}

@Composable
fun MainBottomNavItem(
    section: MainNavSection,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val iconBgModifier = if (isSelected) {
            Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(AppColors.NeonBlue, AppColors.NeonCyan)
                ),
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .then(iconBgModifier)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = section.title,
                tint = if (isSelected) Color.Black else AppColors.TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = section.title,
            color = if (isSelected) AppColors.NeonBlue else AppColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
        )
    }
}

@Composable
fun NotificationsCenterDialog(
    notificationCenter: com.example.service.NotificationCenter? = null,
    onDismiss: () -> Unit
) {
    val liveNotifs by notificationCenter?.notifications?.collectAsState() ?: remember {
        mutableStateOf(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgElevated,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = AppColors.NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مركز الإشعارات الحية",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                if (liveNotifs.isNotEmpty()) {
                    TextButton(onClick = { notificationCenter?.markAllAsRead() }) {
                        Text("قراءة الكل", color = AppColors.NeonCyan, fontSize = 11.sp)
                    }
                }
            }
        },
        text = {
            if (liveNotifs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد إشعارات حالياً", color = AppColors.TextMuted, fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    liveNotifs.take(5).forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.Surface)
                                .border(1.dp, item.accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(text = item.title, color = item.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(text = item.message, color = AppColors.TextPrimary, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = AppColors.NeonBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}
