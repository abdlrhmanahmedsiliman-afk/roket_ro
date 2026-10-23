package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.core.performance.ShimmerMatchCard
import com.example.model.Match
import com.example.model.MatchStatus
import com.example.model.SportType
import com.example.model.SportsNewsArticle
import com.example.ui.components.MatchCard
import com.example.ui.components.TeamHubDialog
import com.example.ui.theme.AppColors
import java.util.Calendar
import java.util.Locale

data class GlobalCalendarDay(
    val dayNameAr: String,
    val dayNumber: Int,
    val monthNameAr: String,
    val isToday: Boolean,
    val dateKey: String,
    val offsetDays: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesTab(
    matches: List<Match>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onMatchClick: (Match) -> Unit,
    onToggleFavorite: (Match) -> Unit,
    refreshNotification: String? = null,
    news: List<SportsNewsArticle> = emptyList(),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedSport by remember { mutableStateOf("all") }
    var selectedTeamForDialog by remember { mutableStateOf<String?>(null) }

    // Generate 30 days for Global Calendar (Yesterday, Today, and the next 28 days)
    val calendarDays = remember {
        val list = mutableListOf<GlobalCalendarDay>()
        val arabicDays = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val arabicMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1) // Start from yesterday

        for (i in -1..28) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based for Sunday
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            val monthIdx = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val isToday = i == 0
            val dateKey = "%04d-%02d-%02d".format(year, monthIdx + 1, dayNum)

            list.add(
                GlobalCalendarDay(
                    dayNameAr = if (isToday) "اليوم" else if (i == -1) "أمس" else if (i == 1) "غداً" else arabicDays.getOrElse(dayOfWeek) { "يوم" },
                    dayNumber = dayNum,
                    monthNameAr = arabicMonths.getOrElse(monthIdx) { "" },
                    isToday = isToday,
                    dateKey = dateKey,
                    offsetDays = i
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    var selectedDayIndex by remember { mutableIntStateOf(1) } // Default to Today (index 1)
    val calendarScrollState = rememberLazyListState()

    var isLiveOnly by remember { mutableStateOf(false) }

    val liveCount = remember(matches) {
        matches.count { it.isLive || it.status == MatchStatus.LIVE || it.status == MatchStatus.HALF_TIME }
    }

    // Filter matches based on search, sport, live-only switch, and selected calendar day
    val filteredMatches = remember(matches, searchQuery, selectedSport, selectedDayIndex, isLiveOnly) {
        val selectedDay = calendarDays.getOrNull(selectedDayIndex)
        matches.filter { match ->
            // 1. Live Filter (when button is toggled ON: show only live; when OFF: show all online & offline)
            val matchesLive = if (isLiveOnly) {
                match.isLive || match.status == MatchStatus.LIVE || match.status == MatchStatus.HALF_TIME
            } else {
                true // Shows all matches: offline (finished, upcoming) and online (live)
            }

            // 2. Strict Sport Filter
            val matchesSport = when (selectedSport.lowercase()) {
                "all" -> true
                "football" -> match.sport.lowercase() in listOf("football", "soccer", "sports_soccer")
                "basketball" -> match.sport.lowercase() in listOf("basketball", "nba", "sports_basketball")
                "tennis" -> match.sport.lowercase() in listOf("tennis", "wimbledon", "sports_tennis", "atp", "wta")
                "formula-1", "f1" -> match.sport.lowercase() in listOf("formula-1", "f1", "motorsport")
                "cricket" -> match.sport.lowercase() in listOf("cricket", "sports_cricket")
                "mma" -> match.sport.lowercase() in listOf("mma", "ufc")
                else -> match.sport.equals(selectedSport, ignoreCase = true)
            }

            // 3. Search Filter
            val matchesSearch = searchQuery.isBlank() ||
                    match.homeTeam.contains(searchQuery, ignoreCase = true) ||
                    match.awayTeam.contains(searchQuery, ignoreCase = true) ||
                    match.league.contains(searchQuery, ignoreCase = true)

            // 4. Calendar Day Filter (bypassed if user is filtering strictly for Live)
            val matchesDay = if (isLiveOnly) {
                true
            } else if (selectedDay != null && match.dateKey.isNotBlank()) {
                if (selectedDay.isToday) {
                    match.dateKey == selectedDay.dateKey || match.isLive
                } else {
                    match.dateKey == selectedDay.dateKey
                }
            } else if (selectedDay?.isToday == true) {
                true // Today shows live & upcoming matches
            } else if (selectedDay?.offsetDays == -1) {
                match.isFinished || match.minute > 0
            } else {
                !match.isFinished // Future dates show upcoming scheduled fixtures
            }

            matchesLive && matchesSport && matchesSearch && matchesDay
        }
    }

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        state = pullRefreshState,
        modifier = modifier.fillMaxSize()
    ) {
        // Floating live update notification banner
        androidx.compose.animation.AnimatedVisibility(
            visible = refreshNotification != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.BgElevated)
                    .border(1.dp, AppColors.NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = refreshNotification ?: "",
                    color = AppColors.NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp)
        ) {
            // Header Row with App Branding & Search Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "roket",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "_ro",
                                color = AppColors.NeonCyan,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppColors.NeonRed.copy(alpha = 0.2f))
                                    .border(1.dp, AppColors.NeonRed.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("مباشر", color = AppColors.NeonRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text(
                            text = "تغطية عالمية لجميع الرياضات والمباريات لحظة بلحظة",
                            color = AppColors.TextSoft,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = if (isSearchActive) AppColors.NeonBlue else AppColors.TextSoft,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = AppColors.NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar (if active)
            if (isSearchActive) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن فريق، دوري، أو مباراة...", color = AppColors.TextMuted, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.NeonBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = AppColors.SurfaceSoft,
                            unfocusedContainerColor = AppColors.Surface
                        )
                    )
                }
            }

            // Global Calendar Header & Horizontal Scroll
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = AppColors.NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "التقويم الشامل (Global Calendar)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Quick Jump to Today
                        if (selectedDayIndex != 1) {
                            Text(
                                text = "العودة لليوم ⚡",
                                color = AppColors.NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { selectedDayIndex = 1 }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal 30-Day Scrollable Chips
                    LazyRow(
                        state = calendarScrollState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(calendarDays.size) { index ->
                            val day = calendarDays[index]
                            val isSelected = selectedDayIndex == index

                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) AppColors.NeonBlue
                                        else if (day.isToday) AppColors.NeonCyan.copy(alpha = 0.12f)
                                        else AppColors.Surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent
                                        else if (day.isToday) AppColors.NeonCyan.copy(alpha = 0.6f)
                                        else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedDayIndex = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.dayNameAr,
                                        color = if (isSelected) Color.Black else if (day.isToday) AppColors.NeonCyan else AppColors.TextSoft,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${day.dayNumber}",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = day.monthNameAr,
                                        color = if (isSelected) Color.Black.copy(alpha = 0.7f) else AppColors.TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Match Status Segment: [جميع المباريات (أوفلاين وأونلاين)] vs [🔴 مباشر الآن (LIVE)]
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: All Matches (Online & Offline)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (!isLiveOnly) AppColors.NeonBlue.copy(alpha = 0.22f) else AppColors.Surface)
                            .border(
                                width = 1.dp,
                                color = if (!isLiveOnly) AppColors.NeonBlue else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { isLiveOnly = false }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌐", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الكل (أونلاين وأوفلاين)",
                                color = if (!isLiveOnly) Color.White else AppColors.TextSoft,
                                fontSize = 11.sp,
                                fontWeight = if (!isLiveOnly) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${matches.size}",
                                    color = if (!isLiveOnly) AppColors.NeonCyan else AppColors.TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Option 2: LIVE ONLY Button (with pulsating red dot)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isLiveOnly) AppColors.NeonRed.copy(alpha = 0.25f) else AppColors.Surface)
                            .border(
                                width = 1.dp,
                                color = if (isLiveOnly) AppColors.NeonRed else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { isLiveOnly = true }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.NeonRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مباشر الآن (LIVE)",
                                color = if (isLiveOnly) Color.White else AppColors.TextSoft,
                                fontSize = 11.sp,
                                fontWeight = if (isLiveOnly) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isLiveOnly) AppColors.NeonRed else Color.White.copy(alpha = 0.10f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$liveCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Multi-Sports Horizontal Scrollable Chips Row
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SportType.entries) { sport ->
                        val isSelected = selectedSport == sport.id
                        val count = when (sport.id) {
                            "all" -> matches.size
                            "football" -> matches.count { it.sport.lowercase() in listOf("football", "soccer", "sports_soccer") }
                            "basketball" -> matches.count { it.sport.lowercase() in listOf("basketball", "nba", "sports_basketball") }
                            "tennis" -> matches.count { it.sport.lowercase() in listOf("tennis", "wimbledon", "sports_tennis") }
                            "formula-1" -> matches.count { it.sport.lowercase() in listOf("formula-1", "f1", "motorsport") }
                            else -> matches.count { it.sport.equals(sport.id, ignoreCase = true) }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) AppColors.NeonBlue.copy(alpha = 0.25f) else AppColors.Surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AppColors.NeonBlue else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedSport = sport.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sport.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sport.labelAr,
                                    color = if (isSelected) AppColors.NeonBlue else AppColors.TextSoft,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "($count)",
                                        color = if (isSelected) AppColors.NeonCyan else AppColors.TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Breaking Sports News Live Bar (100% Real Live RSS Feed)
            if (news.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.NeonRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "أخبار رياضية عاجلة ومباشرة",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "مباشر من المصادر",
                                color = AppColors.TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(news.take(10), key = { it.id }) { article ->
                                Box(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.Surface)
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(AppColors.NeonBlue.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = article.source,
                                                    color = AppColors.NeonBlue,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = article.timeAgo,
                                                color = AppColors.TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = article.title,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            // Matches List
            if (isLoading && matches.isEmpty()) {
                items(4) {
                    ShimmerMatchCard(modifier = Modifier.padding(vertical = 6.dp))
                }
            } else if (filteredMatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🗓️", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لا توجد مباريات مسجلة لهذا التاريخ والفلتر",
                                color = AppColors.TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اختر يوماً آخر من التقويم الشامل أعلاه أو غيّر الرياضة",
                                color = AppColors.TextMuted.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredMatches, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        onClick = { onMatchClick(match) },
                        onToggleFavorite = { onToggleFavorite(match) },
                        onTeamClick = { selectedTeamForDialog = it }
                    )
                }
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
