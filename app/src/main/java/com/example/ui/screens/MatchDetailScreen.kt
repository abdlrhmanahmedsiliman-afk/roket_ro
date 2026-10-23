package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.EventType
import com.example.model.Match
import com.example.model.MatchEvent
import com.example.model.PlayerLineup
import com.example.service.GeminiAiService
import com.example.service.SportsRepository
import com.example.ui.components.*
import com.example.ui.components.TeamHubDialog
import com.example.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun MatchDetailScreen(
    initialMatch: Match,
    aiService: GeminiAiService,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    repository: SportsRepository? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialMatch.id) {
        repository?.loadMatchDetails(initialMatch.id)
    }

    val liveMatches by repository?.matches?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val match = liveMatches.find { it.id == initialMatch.id } ?: initialMatch
    val tabTitles = listOf("ملخص", "الأحداث", "إحصائيات", "تشكيل", "حالة اللاعبين (PCI)", "القنوات الناقلة", "لاعبين H2H", "Heatmap", "المواجهات", "AI Hub")

    var selected3DPlayer by remember { mutableStateOf<PlayerLineup?>(null) }
    var selected3DTeamName by remember { mutableStateOf("") }
    var selected3DTeamColor by remember { mutableStateOf(AppColors.NeonBlue) }
    var selectedTeamForDialog by remember { mutableStateOf<String?>(null) }

    var aiReportText by remember { mutableStateOf(match.aiLiveTactical.ifBlank { match.aiPreview }) }
    var isGeneratingAi by remember { mutableStateOf(false) }

    fun refreshAiAnalysis() {
        coroutineScope.launch {
            isGeneratingAi = true
            val phase = if (match.isLive) "live" else if (match.isFinished) "post" else "pre"
            aiReportText = aiService.generateDeepMatchAnalysis(match, phase)
            isGeneratingAi = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.Bg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.TextPrimary
                    )
                }

                Text(
                    text = "${match.homeTeam} × ${match.awayTeam}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (match.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (match.isFavorite) AppColors.NeonRed else AppColors.TextSoft
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 40.dp)
        ) {
            // Match Hero Scoreboard
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderBrush = Brush.linearGradient(
                        colors = listOf(AppColors.NeonBlue.copy(alpha = 0.4f), AppColors.Coral.copy(alpha = 0.2f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${match.league} ${if (match.country.isNotEmpty()) "• ${match.country}" else ""}",
                            color = AppColors.TextMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Home Team (Clickable to view full club profile)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedTeamForDialog = match.homeTeam }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (match.homeLogo.isNotBlank()) {
                                        AsyncImage(model = match.homeLogo, contentDescription = match.homeTeam, modifier = Modifier.size(40.dp))
                                    } else {
                                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = AppColors.NeonBlue, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = match.homeTeam,
                                    color = AppColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Big Score
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${match.homeScore} - ${match.awayScore}",
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(status = match.status, minute = match.minute, shortDetail = match.shortDetail)
                            }

                            // Away Team (Clickable to view full club profile)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedTeamForDialog = match.awayTeam }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (match.awayLogo.isNotBlank()) {
                                        AsyncImage(model = match.awayLogo, contentDescription = match.awayTeam, modifier = Modifier.size(40.dp))
                                    } else {
                                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = AppColors.Coral, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = match.awayTeam,
                                    color = AppColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (match.venue.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = match.venue, color = AppColors.TextMuted, fontSize = 11.sp)
                                if (match.referee.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "• الحكم: ${match.referee}", color = AppColors.TextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Outcome Probabilities
                        ProbabilityBar(
                            homeTeam = match.homeTeam,
                            awayTeam = match.awayTeam,
                            homeProb = match.homeWinProb,
                            drawProb = match.drawProb,
                            awayProb = match.awayWinProb
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Scrollable 8-Tab Selector Bar
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabTitles.size) { index ->
                        val title = tabTitles[index]
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AppColors.NeonBlue else AppColors.Surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.Black else AppColors.TextSoft,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Summary: Momentum Graph & Key Info
                    item {
                        MomentumGraphView(
                            homeTeam = match.homeTeam,
                            awayTeam = match.awayTeam,
                            currentMinute = match.minute
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "ملخص المؤشرات التكتيكية",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                StatComparisonRow(
                                    title = "الأهداف المتوقعة (xG)",
                                    homeValueStr = "${match.stats.xgHome}",
                                    awayValueStr = "${match.stats.xgAway}",
                                    homeRatio = match.stats.xgHome,
                                    awayRatio = match.stats.xgAway
                                )
                                StatComparisonRow(
                                    title = "نسبة الاستحواذ %",
                                    homeValueStr = "${match.stats.possessionHome}%",
                                    awayValueStr = "${match.stats.possessionAway}%",
                                    homeRatio = match.stats.possessionHome.toFloat(),
                                    awayRatio = match.stats.possessionAway.toFloat()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        SupernaturalAiFeatures(match = match)
                    }
                }
                1 -> {
                    // Timeline
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "أحداث المباراة المباشرة (Timeline)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (match.events.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("لا توجد أحداث رئيسية مسجلة بعد", color = AppColors.TextMuted, fontSize = 12.sp)
                                    }
                                } else {
                                    match.events.forEach { event ->
                                        TimelineItem(event = event, homeTeam = match.homeTeam, awayTeam = match.awayTeam)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Advanced Stats (xG, xA, PPDA, Field Tilt)
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(match.homeTeam, color = AppColors.NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("الإحصائيات المتقدمة (xG & PPDA)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(match.awayTeam, color = AppColors.Coral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                StatComparisonRow("الاستحواذ %", "${match.stats.possessionHome}%", "${match.stats.possessionAway}%", match.stats.possessionHome.toFloat(), match.stats.possessionAway.toFloat())
                                StatComparisonRow("الأهداف المتوقعة (xG)", "${match.stats.xgHome}", "${match.stats.xgAway}", match.stats.xgHome, match.stats.xgAway)
                                StatComparisonRow("إجمالي التسديدات", "${match.stats.shotsHome}", "${match.stats.shotsAway}", match.stats.shotsHome.toFloat(), match.stats.shotsAway.toFloat())
                                StatComparisonRow("تسديدات على المرمى", "${match.stats.shotsOnTargetHome}", "${match.stats.shotsOnTargetAway}", match.stats.shotsOnTargetHome.toFloat(), match.stats.shotsOnTargetAway.toFloat())
                                StatComparisonRow("تسديدات خارج المرمى", "${match.stats.shotsOffTargetHome}", "${match.stats.shotsOffTargetAway}", match.stats.shotsOffTargetHome.toFloat(), match.stats.shotsOffTargetAway.toFloat())
                                StatComparisonRow("إجمالي التمريرات", "${match.stats.totalPassesHome}", "${match.stats.totalPassesAway}", match.stats.totalPassesHome.toFloat(), match.stats.totalPassesAway.toFloat())
                                StatComparisonRow("دقة التمرير %", "${match.stats.passAccuracyHome}%", "${match.stats.passAccuracyAway}%", match.stats.passAccuracyHome.toFloat(), match.stats.passAccuracyAway.toFloat())
                                StatComparisonRow("الضربات الركنية", "${match.stats.cornersHome}", "${match.stats.cornersAway}", match.stats.cornersHome.toFloat(), match.stats.cornersAway.toFloat())
                                StatComparisonRow("حالات التسلل", "${match.stats.offsidesHome}", "${match.stats.offsidesAway}", match.stats.offsidesHome.toFloat(), match.stats.offsidesAway.toFloat())
                                StatComparisonRow("الأخطاء المرتكبة", "${match.stats.foulsHome}", "${match.stats.foulsAway}", match.stats.foulsHome.toFloat(), match.stats.foulsAway.toFloat())
                                StatComparisonRow("البطاقات الصفراء", "${match.stats.yellowCardsHome}", "${match.stats.yellowCardsAway}", match.stats.yellowCardsHome.toFloat(), match.stats.yellowCardsAway.toFloat())
                                StatComparisonRow("البطاقات الحمراء", "${match.stats.redCardsHome}", "${match.stats.redCardsAway}", match.stats.redCardsHome.toFloat(), match.stats.redCardsAway.toFloat())
                                StatComparisonRow("فرص محققة للتسجيل", "${match.stats.bigChancesHome}", "${match.stats.bigChancesAway}", match.stats.bigChancesHome.toFloat(), match.stats.bigChancesAway.toFloat())
                                StatComparisonRow("صناعة الأهداف المتوقعة (xA)", "غير متاح", "غير متاح", 0f, 0f)
                                StatComparisonRow("معدل ضغط الخصم (PPDA)", "غير متاح", "غير متاح", 0f, 0f)
                                StatComparisonRow("ميل الملعب الهجومي (Field Tilt)", "غير متاح", "غير متاح", 0f, 0f)
                            }
                        }
                    }
                }
                3 -> {
                    // Lineups & Tactical Pitch
                    item {
                        val effectiveHomeLineup = match.homeLineup ?: repository?.createSampleLineup(team = match.homeTeam)
                        val effectiveAwayLineup = match.awayLineup ?: repository?.createSampleLineup(team = match.awayTeam)

                        if (effectiveHomeLineup != null && effectiveAwayLineup != null) {
                            TacticalPitchView(
                                homeLineup = effectiveHomeLineup,
                                awayLineup = effectiveAwayLineup,
                                homeTeamName = match.homeTeam,
                                awayTeamName = match.awayTeam,
                                onPlayerClick = { player, teamName, teamColor ->
                                    selected3DPlayer = player
                                    selected3DTeamName = teamName
                                    selected3DTeamColor = teamColor
                                }
                            )
                        } else {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("جاري استلام التشكيل الرسمي والبيانات التكتيكية...", color = AppColors.TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Player Condition Index (PCI)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderBrush = Brush.linearGradient(
                                    colors = listOf(AppColors.NeonCyan, AppColors.NeonBlue)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = "PCI",
                                            tint = AppColors.NeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "مؤشر جاهزية اللاعبين الذكي (Player Condition Index)",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "تحليل خوارزمي متقدم لـ 7 عوامل: الإجهاد البدني، العامل النفسي، المستوى الأخير، مخاطر الإصابة، الحمل التراكمي للسفر، التوافق التكتيكي، والتأقلم مع الطقس.",
                                        color = AppColors.TextSoft,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (match.playerConditions.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("جاري استلام بيانات الـ PCI للتشكيل...", color = AppColors.TextMuted, fontSize = 12.sp)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    match.playerConditions.forEach { pci ->
                                        PlayerConditionCard(
                                            playerCondition = pci,
                                            onView3D = {
                                                selected3DPlayer = PlayerLineup(
                                                    number = pci.playerNumber,
                                                    name = pci.playerName,
                                                    position = pci.position,
                                                    rating = (pci.overallScore / 10f).coerceIn(6.0f, 9.9f),
                                                    isCaptain = false,
                                                    photoUrl = pci.photoUrl
                                                )
                                                selected3DTeamName = pci.teamName
                                                selected3DTeamColor = if (pci.teamName == match.homeTeam) AppColors.NeonBlue else AppColors.Coral
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // Official Legal Broadcasters
                    item {
                        BroadcastersSection(broadcasters = match.broadcasters)
                    }
                }
                6 -> {
                    // Players & Radar Comparison
                    item {
                        val homeStar = match.homeLineup?.starters?.maxByOrNull { it.rating }
                            ?: PlayerLineup(10, match.homeTeam, "FWD", 8.5f)
                        val awayStar = match.awayLineup?.starters?.maxByOrNull { it.rating }
                            ?: PlayerLineup(9, match.awayTeam, "FWD", 8.4f)

                        RadarChartView(
                            playerA = PlayerRadarStats(
                                playerName = "${homeStar.name} (${match.homeTeam})",
                                pace = (homeStar.rating * 10f + 5).coerceIn(65f, 96f),
                                shooting = (homeStar.rating * 10.5f).coerceIn(60f, 95f),
                                passing = (homeStar.rating * 9.8f).coerceIn(60f, 94f),
                                dribbling = (homeStar.rating * 10.2f).coerceIn(60f, 96f),
                                defense = 42f,
                                physical = 78f
                            ),
                            playerB = PlayerRadarStats(
                                playerName = "${awayStar.name} (${match.awayTeam})",
                                pace = (awayStar.rating * 10f + 2).coerceIn(65f, 95f),
                                shooting = (awayStar.rating * 10.4f).coerceIn(60f, 96f),
                                passing = (awayStar.rating * 9.5f).coerceIn(60f, 93f),
                                dribbling = (awayStar.rating * 10.0f).coerceIn(60f, 94f),
                                defense = 44f,
                                physical = 82f
                            )
                        )
                    }
                }
                7 -> {
                    // Heatmap
                    item {
                        HeatmapPitchView(
                            homeTeam = match.homeTeam,
                            awayTeam = match.awayTeam
                        )
                    }
                }
                8 -> {
                    // H2H
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "سجل المواجهات المباشرة (آخر ${match.h2h.pastMatchesCount} مباريات)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                StatComparisonRow(
                                    title = "مرات الفوز",
                                    homeValueStr = "${match.h2h.homeWins}",
                                    awayValueStr = "${match.h2h.awayWins}",
                                    homeRatio = match.h2h.homeWins.toFloat(),
                                    awayRatio = match.h2h.awayWins.toFloat()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "فورم الفريقين الأخيرة (من اليسار لليمين)",
                                    color = AppColors.TextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        match.h2h.recentFormHome.forEach { FormBadge(it) }
                                    }
                                    Text("مقابل", color = AppColors.TextMuted, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        match.h2h.recentFormAway.forEach { FormBadge(it) }
                                    }
                                }
                            }
                        }
                    }
                }
                9 -> {
                    // AI Hub: Supernatural Innovations & AI Match Story
                    item {
                        SupernaturalAiFeatures(match = match)
                        Spacer(modifier = Modifier.height(14.dp))

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderBrush = Brush.linearGradient(
                                colors = listOf(AppColors.NeonBlue, AppColors.Coral)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = AppColors.NeonBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "قصة المباراة التكتيكية (AI Match Story)",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { refreshAiAnalysis() },
                                        enabled = !isGeneratingAi
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Refresh AI",
                                            tint = AppColors.NeonCyan
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (isGeneratingAi) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                color = AppColors.NeonBlue,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("جارٍ توليد الرواية التكتيكية والـ xG...", color = AppColors.TextMuted, fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = aiReportText,
                                        color = AppColors.TextPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3D Player Interactive Bottom Sheet
        val current3DPlayer = selected3DPlayer
        if (current3DPlayer != null) {
            val playerCond = match.playerConditions.find {
                it.playerName == current3DPlayer.name || it.playerNumber == current3DPlayer.number
            }
            Player3DBottomSheet(
                player = current3DPlayer,
                teamName = selected3DTeamName,
                teamColor = selected3DTeamColor,
                playerCondition = playerCond,
                onDismiss = { selected3DPlayer = null }
            )
        }

        // Full Club Detail Dialog (Overview, Squad, Matches, Transfers, Club Scorers)
        val teamToView = selectedTeamForDialog
        if (teamToView != null) {
            TeamHubDialog(
                teamName = teamToView,
                leagueName = match.league,
                onDismiss = { selectedTeamForDialog = null },
                onPlayerClick = { playerLineup ->
                    selected3DPlayer = playerLineup
                    selected3DTeamName = teamToView
                    selected3DTeamColor = if (teamToView == match.homeTeam) AppColors.NeonBlue else AppColors.Coral
                    selectedTeamForDialog = null
                }
            )
        }
    }
}

@Composable
fun TimelineItem(event: MatchEvent, homeTeam: String, awayTeam: String) {
    val (icon, iconColor) = when (event.type) {
        EventType.GOAL -> Icons.Rounded.SportsSoccer to AppColors.NeonCyan
        EventType.YELLOW_CARD -> Icons.Rounded.Square to AppColors.Warning
        EventType.RED_CARD -> Icons.Rounded.Square to AppColors.NeonRed
        EventType.SUBSTITUTION -> Icons.Rounded.SwapHoriz to AppColors.NeonBlue
        EventType.VAR -> Icons.Rounded.Tv to AppColors.NeonPurple
        EventType.CORNER -> Icons.Rounded.Flag to AppColors.Coral
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${event.minute}'",
            color = AppColors.NeonBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(32.dp)
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.player,
                color = AppColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (event.detail.isNotBlank()) {
                Text(
                    text = event.detail,
                    color = AppColors.TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = if (event.teamIsHome) homeTeam else awayTeam,
            color = if (event.teamIsHome) AppColors.NeonBlue else AppColors.Coral,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun FormBadge(result: String) {
    val (bg, textColor) = when (result) {
        "W" -> AppColors.Success to Color.Black
        "D" -> AppColors.Warning to Color.Black
        "L" -> AppColors.NeonRed to Color.White
        else -> AppColors.SurfaceSoft to AppColors.TextSoft
    }

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = result,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}
