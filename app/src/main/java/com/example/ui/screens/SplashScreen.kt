package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Rocket3D
import com.example.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startLaunchAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        startLaunchAnimation = true
        delay(2200)
        onSplashFinished()
    }

    val rocketOffsetY by animateFloatAsState(
        targetValue = if (startLaunchAnimation) 0f else 80f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "rocketSplashY"
    )

    val rocketAlpha by animateFloatAsState(
        targetValue = if (startLaunchAnimation) 1.0f else 0f,
        animationSpec = tween(900),
        label = "rocketAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF14213D), AppColors.Bg),
                    center = Offset(500f, 600f),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Starfield background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val random = java.util.Random(42)
            for (i in 0 until 40) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                val starRadius = 1.0f + random.nextFloat() * 2.0f
                val alpha = 0.2f + random.nextFloat() * 0.7f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = starRadius,
                    center = Offset(x, y)
                )
            }
        }

        // Center Rocket & Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y = rocketOffsetY.dp)
        ) {
            Rocket3D(
                size = 230.dp,
                showFlame = true,
                enableTilt = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "roket_ro",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "تحليل ذكي لكل الرياضات • 3D • بدون تسجيل",
                color = AppColors.TextSoft,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "v3.0 • AI-Powered Realtime Engine",
                color = AppColors.NeonCyan.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Footer note
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "نتائج مباشرة • إحصائيات عميقة • ذكاء اصطناعي",
                color = AppColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
