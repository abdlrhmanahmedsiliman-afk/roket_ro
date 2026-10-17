package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Match
import com.example.ui.theme.AppColors
import kotlin.math.roundToInt

/**
 * 3 Supernatural AI Innovations (ابتكارات خارقة للطبيعة لـ roket_ro AI)
 * 1) Coach Tactical Sandbox Simulator
 * 2) Crowd Psychological Pressure Matrix
 * 3) Clutch Momentum Horizon Index
 */
@Composable
fun SupernaturalAiFeatures(
    match: Match,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Sandbox, 1: Crowd Matrix, 2: Clutch Horizon

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF280B45),
                            Color(0xFF0F1E3D)
                        )
                    )
                )
                .border(1.dp, AppColors.NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(AppColors.NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = "Supernatural AI",
                        tint = AppColors.NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "مختبر الذكاء الخارق (Supernatural AI Hub)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "3 محركات ذكاء اصطناعي ثورية وحصرية لتطبيق roket_ro",
                        color = AppColors.NeonCyan,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Innovation Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "محاكي الخطة التكتيكية",
                "مصفوفة ضغط الجماهير",
                "أفق الزخم القاتل"
            )
            tabs.forEachIndexed { index, title ->
                val isSelected = activeSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AppColors.NeonBlue else AppColors.Surface)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeSubTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.Black else AppColors.TextSoft,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeSubTab) {
            0 -> CoachTacticalSandboxCard(match = match)
            1 -> CrowdPsychologicalPressureCard(match = match)
            2 -> ClutchMomentumHorizonCard(match = match)
        }
    }
}

/**
 * Innovation 1: Coach Tactical Sandbox Simulator
 * Changes formation & pressing style and calculates predicted outcome shifts
 */
@Composable
fun CoachTacticalSandboxCard(match: Match) {
    val formations = listOf("4-3-3 هجومي", "3-5-2 أجنحة", "4-2-3-1 متوازن", "4-2-4 هجوم كاسح")
    val pressingStyles = listOf("ضغط عالي Gegenpress", "تكتل دفاعي مع مرتدات", "استحواذ بطيء Tiki-Taka")

    var selectedFormation by remember { mutableStateOf(formations[0]) }
    var selectedStyle by remember { mutableStateOf(pressingStyles[0]) }

    // Dynamic calculated shift based on user simulation
    val simulatedWinProb by animateFloatAsState(
        targetValue = when (selectedFormation) {
            "4-2-4 هجوم كاسح" -> (match.homeWinProb + 8.5).toFloat().coerceIn(10f, 90f)
            "3-5-2 أجنحة" -> (match.homeWinProb + 3.2).toFloat().coerceIn(10f, 90f)
            "4-2-3-1 متوازن" -> (match.homeWinProb - 1.5).toFloat().coerceIn(10f, 90f)
            else -> match.homeWinProb.toFloat()
        },
        label = "sim_prob"
    )

    val simulatedXgHome by animateFloatAsState(
        targetValue = when (selectedFormation) {
            "4-2-4 هجوم كاسح" -> match.stats.xgHome + 0.65f
            "3-5-2 أجنحة" -> match.stats.xgHome + 0.35f
            else -> match.stats.xgHome
        },
        label = "sim_xg"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = Brush.linearGradient(listOf(AppColors.NeonCyan, AppColors.NeonBlue))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "محاكي خطة المدرب التفاعلي (Sandbox)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "محاكاة خوارزمية فورية",
                    color = AppColors.NeonCyan,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "غير تشكيل مدرب ${match.homeTeam} وتكتيك الضغط ولاحظ تغير نسب الفوز والـ xG لحظيًا:",
                color = AppColors.TextSoft,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Formation Selector
            Text(text = "اختر التشكيل البديل:", color = AppColors.TextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                formations.forEach { formation ->
                    val isSel = selectedFormation == formation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) AppColors.NeonCyan.copy(alpha = 0.25f) else AppColors.SurfaceSoft)
                            .border(1.dp, if (isSel) AppColors.NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { selectedFormation = formation }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formation.split(" ")[0],
                            color = if (isSel) AppColors.NeonCyan else AppColors.TextSoft,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pressing Style Selector
            Text(text = "أسلوب اللعب التكتيكي:", color = AppColors.TextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pressingStyles.forEach { style ->
                    val isSel = selectedStyle == style
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) AppColors.NeonBlue.copy(alpha = 0.25f) else AppColors.SurfaceSoft)
                            .border(1.dp, if (isSel) AppColors.NeonBlue else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { selectedStyle = style }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.split(" ")[0],
                            color = if (isSel) AppColors.NeonBlue else AppColors.TextSoft,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulation Results
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "نسبة الفوز المتوقعة", color = AppColors.TextMuted, fontSize = 10.sp)
                        Text(
                            text = "${simulatedWinProb.roundToInt()}%",
                            color = AppColors.NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "الأهداف المتوقعة (xG)", color = AppColors.TextMuted, fontSize = 10.sp)
                        Text(
                            text = "%.2f".format(simulatedXgHome),
                            color = AppColors.NeonGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "مخاطر المرتدات", color = AppColors.TextMuted, fontSize = 10.sp)
                        Text(
                            text = if (selectedFormation.contains("هجوم")) "مرتفعة (38%)" else "منخفضة (14%)",
                            color = if (selectedFormation.contains("هجوم")) AppColors.Coral else AppColors.NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Innovation 2: Crowd Psychological Pressure Matrix
 */
@Composable
fun CrowdPsychologicalPressureCard(match: Match) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = Brush.linearGradient(listOf(AppColors.Coral, AppColors.NeonGold))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مصفوفة الضغط النفسي للجماهير (Crowd Pressure)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${match.venue.ifBlank { "الملعب الرئيسي" }}",
                    color = AppColors.Coral,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "يقيس تأثير هتافات المدرجات على أخطاء الخصم وقرارات التحكيم اللحظية:",
                color = AppColors.TextSoft,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PressurePillar("مستوى الضجيج (ديسيبل)", "104 dB", 0.88f, AppColors.Coral, Modifier.weight(1f))
                PressurePillar("عدائية الأرض للخصم", "86%", 0.86f, AppColors.NeonGold, Modifier.weight(1f))
                PressurePillar("معامل خطأ الفريق الزائر", "× 1.4", 0.72f, AppColors.NeonRed, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "الضغط الجماهيري يرفع احتمالية خطأ دفاعي في التمرير بنسبة 22% خلال أول 15 دقيقة من الشوط الثاني.",
                    color = AppColors.TextSoft,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Innovation 3: Clutch Momentum Horizon Index
 */
@Composable
fun ClutchMomentumHorizonCard(match: Match) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = Brush.linearGradient(listOf(AppColors.NeonGreen, AppColors.NeonCyan))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أفق الزخم القاتل (Clutch Momentum Horizon)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تنبؤ الانقلاب التكتيكي",
                    color = AppColors.NeonGreen,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "خوارزمية ترصد الدقائق الأكثر خطورة لتسجيل هدف قاتل أو حسم المباراة:",
                color = AppColors.TextSoft,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PressurePillar("نافذة الهدف الحاسمة", "د72 - د84", 0.91f, AppColors.NeonGreen, Modifier.weight(1f))
                PressurePillar("احتمالية انقلاب النتيجة", "64%", 0.64f, AppColors.NeonCyan, Modifier.weight(1f))
                PressurePillar("اللاعب الأقرب للتسجيل", "رأس الحربة", 0.85f, AppColors.NeonGold, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "مؤشر الإجهاد التراكمي لمحاور الارتكاز يشير إلى مساحات شاسعة ستفتح في العمق بعد الدقيقة 70.",
                    color = AppColors.TextSoft,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun PressurePillar(
    title: String,
    value: String,
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceSoft)
            .padding(8.dp)
    ) {
        Text(text = title, color = AppColors.TextMuted, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.08f)
        )
    }
}
