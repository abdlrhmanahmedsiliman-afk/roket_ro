package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PciFactors
import com.example.model.PciStatus
import com.example.model.PciTimelinePoint
import com.example.model.PlayerCondition
import com.example.ui.theme.AppColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Player Condition Ring with pulsing glow and color status
 */
@Composable
fun PlayerConditionRing(
    score: Int,
    status: PciStatus,
    size: Dp = 72.dp,
    strokeWidth: Dp = 6.dp,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pci_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "pci_progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = this.size.minDimension
                val strokePx = strokeWidth.toPx()
                val radius = (canvasSize - strokePx) / 2f
                val center = Offset(this.size.width / 2f, this.size.height / 2f)

                // Background track
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokePx)
                )

                // Progress Arc
                val sweepAngle = animatedProgress * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            status.color.copy(alpha = 0.7f),
                            status.color,
                            AppColors.NeonBlue
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Pulsing indicator dot at end of arc
                if (sweepAngle > 5f) {
                    val angleRad = Math.toRadians((sweepAngle - 90.0)).toFloat()
                    val dotX = center.x + radius * cos(angleRad)
                    val dotY = center.y + radius * sin(angleRad)
                    drawCircle(
                        color = status.color.copy(alpha = pulseAlpha),
                        radius = strokePx * 0.9f,
                        center = Offset(dotX, dotY)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    color = Color.White,
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "PCI",
                    color = status.color,
                    fontSize = (size.value * 0.14f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(status.color.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = status.labelAr,
                    color = status.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 7-Axis Radar Chart for Player Condition Index Factors:
 * 1. Physical Fatigue (بدني)
 * 2. Mental & Psychological (نفسي)
 * 3. Recent Form (المستوى)
 * 4. Injury Risk (أمان الإصابة)
 * 5. Travel & Schedule (جدول وسفر)
 * 6. Tactical Fit (التوافق التكتيكي)
 * 7. Weather & Pitch (تأقلم الطقس)
 */
@Composable
fun PciRadarChart(
    factors: PciFactors,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val axisLabels = listOf(
        "بدني",
        "نفسي",
        "مستوى",
        "أمان",
        "جدول",
        "تكتيك",
        "طقس"
    )

    val values = listOf(
        factors.physicalFatigue,
        factors.mentalPsychological,
        factors.recentForm,
        factors.injuryRisk,
        factors.travelScheduleLoad,
        factors.tacticalFit,
        factors.weatherPitchAdaptation
    )

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "radar_growth"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val numAxes = 7
                val cX = this.size.width / 2f
                val cY = this.size.height / 2f
                val maxRadius = (this.size.minDimension / 2f) * 0.72f

                // Draw concentric guide rings (25%, 50%, 75%, 100%)
                val ringLevels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                ringLevels.forEach { lvl ->
                    val ringPath = Path()
                    for (i in 0 until numAxes) {
                        val angle = (i * (2 * Math.PI / numAxes) - Math.PI / 2).toFloat()
                        val r = maxRadius * lvl
                        val x = cX + r * cos(angle)
                        val y = cY + r * sin(angle)
                        if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                    }
                    ringPath.close()
                    drawPath(
                        path = ringPath,
                        color = Color.White.copy(alpha = if (lvl == 1.0f) 0.18f else 0.08f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw axis spokes
                for (i in 0 until numAxes) {
                    val angle = (i * (2 * Math.PI / numAxes) - Math.PI / 2).toFloat()
                    val x = cX + maxRadius * cos(angle)
                    val y = cY + maxRadius * sin(angle)
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(cX, cY),
                        end = Offset(x, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw player condition polygon
                val dataPath = Path()
                for (i in 0 until numAxes) {
                    val angle = (i * (2 * Math.PI / numAxes) - Math.PI / 2).toFloat()
                    val normalizedValue = (values[i] / 100f).coerceIn(0f, 1f) * animatedProgress
                    val r = maxRadius * normalizedValue
                    val x = cX + r * cos(angle)
                    val y = cY + r * sin(angle)
                    if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                }
                dataPath.close()

                // Polygon fill
                drawPath(
                    path = dataPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.NeonCyan.copy(alpha = 0.45f),
                            AppColors.NeonBlue.copy(alpha = 0.20f)
                        ),
                        center = Offset(cX, cY),
                        radius = maxRadius
                    )
                )

                // Polygon border
                drawPath(
                    path = dataPath,
                    color = AppColors.NeonCyan,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dots at vertices
                for (i in 0 until numAxes) {
                    val angle = (i * (2 * Math.PI / numAxes) - Math.PI / 2).toFloat()
                    val normalizedValue = (values[i] / 100f).coerceIn(0f, 1f) * animatedProgress
                    val r = maxRadius * normalizedValue
                    val x = cX + r * cos(angle)
                    val y = cY + r * sin(angle)
                    drawCircle(
                        color = AppColors.NeonCyan,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Legend of the 7 factors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            axisLabels.forEachIndexed { idx, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        color = AppColors.TextSoft,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${values[idx].toInt()}%",
                        color = AppColors.NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * PCI Minute-by-Minute Timeline Chart
 */
@Composable
fun PciTimelineChart(
    timeline: List<PciTimelinePoint>,
    modifier: Modifier = Modifier
) {
    if (timeline.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تطور الجاهزية البدنية دقيقة بدقيقة (90')",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "بداية ⬅ نهاية",
                color = AppColors.TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Surface)
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val points = timeline.mapIndexed { idx, point ->
                    val x = (idx.toFloat() / (timeline.size - 1).coerceAtLeast(1)) * w
                    val y = h - ((point.score / 100f) * h)
                    Offset(x, y)
                }

                val linePath = Path()
                points.forEachIndexed { i, pt ->
                    if (i == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
                }

                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(AppColors.NeonCyan, AppColors.NeonBlue, AppColors.NeonOrange)
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw points
                points.forEach { pt ->
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

/**
 * Interactive Player Condition Card
 */
@Composable
fun PlayerConditionCard(
    playerCondition: PlayerCondition,
    modifier: Modifier = Modifier,
    onView3D: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        contentPadding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Condition Ring with Player Avatar
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerConditionRing(
                        score = playerCondition.overallScore,
                        status = playerCondition.status,
                        size = 56.dp,
                        strokeWidth = 3.5.dp,
                        showLabel = false
                    )
                    if (playerCondition.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = playerCondition.photoUrl,
                            contentDescription = playerCondition.playerName,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "${playerCondition.overallScore}",
                            color = playerCondition.status.color,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Player info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${playerCondition.playerNumber}",
                            color = AppColors.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = playerCondition.playerName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${playerCondition.teamName} • ${playerCondition.position}",
                            color = AppColors.TextSoft,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(playerCondition.status.color.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = playerCondition.status.labelAr,
                                color = playerCondition.status.color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Expand indicator
                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = AppColors.TextSoft
                    )
                }
            }

            // Expanded deep analysis view
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "الرادار السباعي لتحليل الجاهزية (7-Axis PCI)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PciRadarChart(
                        factors = playerCondition.factors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (playerCondition.timeline.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PciTimelineChart(timeline = playerCondition.timeline)
                    }

                    if (playerCondition.aiConditionSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.BgElevated)
                                .border(1.dp, AppColors.NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = "AI",
                                    tint = AppColors.NeonBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = playerCondition.aiConditionSummary,
                                    color = AppColors.TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (onView3D != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, AppColors.NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onView3D() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("عرض المجسم 3D التفاعلي (360°)", color = AppColors.NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
