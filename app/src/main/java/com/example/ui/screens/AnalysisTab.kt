package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Match
import com.example.service.GeminiAiService
import com.example.ui.components.GlassCard
import com.example.ui.components.ProbabilityBar
import com.example.ui.components.Rocket3D
import com.example.ui.components.StatComparisonRow
import com.example.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun AnalysisTab(
    matches: List<Match>,
    aiService: GeminiAiService,
    onMatchClick: (Match) -> Unit,
    onOpenAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val topMatch = matches.firstOrNull { it.isLive } ?: matches.firstOrNull()

    var quickQuestion by remember { mutableStateOf("") }
    var quickAnswer by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp)
    ) {
        // Hero AI Analytical Hub Banner with 3D Rocket
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                borderBrush = Brush.linearGradient(
                    colors = listOf(AppColors.NeonBlue, AppColors.Coral, AppColors.NeonPurple)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.NeonBlue.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "AI ENGINE",
                                    color = AppColors.NeonBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "التحليل الذكي التنبؤي",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "تحويل الأرقام والإحصائيات المعقدة إلى قراءة تكتيكية ونسب احتمالية إحصائية دقيقة.",
                            color = AppColors.TextSoft,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onOpenAiChat,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.NeonBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحدث مع roket_ro AI", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Rocket3D(size = 90.dp, showFlame = true, enableTilt = true)
                    }
                }
            }
        }

        // Quick AI Question Input Bar
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "استشر الذكاء الاصطناعي في أي مواجهة",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.SurfaceSoft)
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = quickQuestion,
                            onValueChange = { quickQuestion = it },
                            placeholder = { Text("مثال: ما هي فرص ريال مدريد؟", color = AppColors.TextMuted, fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = AppColors.TextPrimary,
                                unfocusedTextColor = AppColors.TextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (quickQuestion.isNotBlank() && !isAnalyzing) {
                                    coroutineScope.launch {
                                        isAnalyzing = true
                                        quickAnswer = aiService.askAssistant(quickQuestion, matches)
                                        isAnalyzing = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppColors.NeonBlue)
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Analyze", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (isAnalyzing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = AppColors.NeonBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ تحليل البيانات الإحصائية والـ xG...", color = AppColors.TextMuted, fontSize = 11.sp)
                        }
                    } else if (quickAnswer != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.NeonBlue.copy(alpha = 0.08f))
                                .border(1.dp, AppColors.NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = quickAnswer!!, color = AppColors.TextPrimary, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        // Featured Match Deep Analysis Card
        if (topMatch != null) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تحليل قمة اليوم بالأرقام والـ xG",
                    color = AppColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onMatchClick(topMatch) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(topMatch.league, color = AppColors.TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.Coral.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("تحليل مباشر", color = AppColors.Coral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(topMatch.homeTeam, color = AppColors.NeonBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${topMatch.homeScore} - ${topMatch.awayScore}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text(topMatch.awayTeam, color = AppColors.Coral, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // xG and Possession Stats preview
                        StatComparisonRow(
                            title = "معدل الأهداف المتوقعة (xG)",
                            homeValueStr = "${topMatch.stats.xgHome}",
                            awayValueStr = "${topMatch.stats.xgAway}",
                            homeRatio = topMatch.stats.xgHome,
                            awayRatio = topMatch.stats.xgAway
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ProbabilityBar(
                            homeTeam = topMatch.homeTeam,
                            awayTeam = topMatch.awayTeam,
                            homeProb = topMatch.homeWinProb,
                            drawProb = topMatch.drawProb,
                            awayProb = topMatch.awayWinProb
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = topMatch.aiPreview.ifBlank { topMatch.aiLiveTactical },
                            color = AppColors.TextSoft,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Analytical Predictions List for upcoming matches
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "توقعات ونسب الذكاء الاصطناعي للمباريات",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(matches.take(6)) { match ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { onMatchClick(match) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(match.league, color = AppColors.TextMuted, fontSize = 11.sp)
                        Text(if (match.isLive) "د${match.minute}'" else match.kickoffTime, color = AppColors.NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(match.homeTeam, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("vs", color = AppColors.TextMuted, fontSize = 11.sp)
                        Text(match.awayTeam, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
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
}
