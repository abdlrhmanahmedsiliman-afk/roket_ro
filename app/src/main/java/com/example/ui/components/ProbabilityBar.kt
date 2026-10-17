package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors

@Composable
fun ProbabilityBar(
    homeTeam: String,
    awayTeam: String,
    homeProb: Double,
    drawProb: Double,
    awayProb: Double,
    modifier: Modifier = Modifier
) {
    val total = (homeProb + drawProb + awayProb).coerceAtLeast(1.0)
    val hWeight = (homeProb / total).toFloat()
    val dWeight = (drawProb / total).toFloat()
    val aWeight = (awayProb / total).toFloat()

    val animH by animateFloatAsState(targetValue = hWeight, animationSpec = tween(700), label = "hProb")
    val animD by animateFloatAsState(targetValue = dWeight, animationSpec = tween(700), label = "dProb")
    val animA by animateFloatAsState(targetValue = aWeight, animationSpec = tween(700), label = "aProb")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$homeTeam ${homeProb.toInt()}%",
                color = AppColors.NeonBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "تعادل ${drawProb.toInt()}%",
                color = AppColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${awayProb.toInt()}% $awayTeam",
                color = AppColors.Coral,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.SurfaceSoft)
        ) {
            if (animH > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(animH)
                        .background(AppColors.NeonBlue)
                )
            }
            if (animD > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(animD)
                        .background(AppColors.TextMuted.copy(alpha = 0.5f))
                )
            }
            if (animA > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(animA)
                        .background(AppColors.Coral)
                )
            }
        }
    }
}
