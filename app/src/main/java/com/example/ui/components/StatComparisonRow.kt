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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors

@Composable
fun StatComparisonRow(
    title: String,
    homeValueStr: String,
    awayValueStr: String,
    homeRatio: Float,
    awayRatio: Float,
    modifier: Modifier = Modifier
) {
    val total = (homeRatio + awayRatio).coerceAtLeast(0.001f)
    val hNorm = (homeRatio / total).coerceIn(0.05f, 0.95f)
    val aNorm = (awayRatio / total).coerceIn(0.05f, 0.95f)

    val animH by animateFloatAsState(targetValue = hNorm, animationSpec = tween(600), label = "hStat")
    val animA by animateFloatAsState(targetValue = aNorm, animationSpec = tween(600), label = "aStat")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = homeValueStr,
                color = AppColors.NeonBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Start
            )
            Text(
                text = title,
                color = AppColors.TextSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = awayValueStr,
                color = AppColors.Coral,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppColors.SurfaceSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animH)
                    .background(AppColors.NeonBlue)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animA)
                    .background(AppColors.Coral)
            )
        }
    }
}
