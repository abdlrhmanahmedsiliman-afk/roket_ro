package com.example.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AuthManager
import com.example.ui.components.GlassCard
import com.example.ui.components.Rocket3D
import com.example.ui.theme.AppColors

@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var isSigningIn by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF14213D), AppColors.Bg),
                    radius = 1100f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = Brush.linearGradient(
                colors = listOf(AppColors.NeonBlue, AppColors.NeonCyan)
            ),
            contentPadding = 22.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rocket 3D icon
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Rocket3D(size = 90.dp, showFlame = true, enableTilt = true)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "مرحباً بك في roket_ro",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "منصة التحليل التكتيكي الرياضي الذكي 3D",
                    color = AppColors.TextSoft,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // One-Tap Google Sign-In Button
                Button(
                    onClick = {
                        isSigningIn = true
                        authManager.signInWithGoogle(
                            displayName = "مستخدم Google الرسمي",
                            email = "user@gmail.com"
                        )
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Google",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تسجيل الدخول عبر Google",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
                    Text(
                        text = "أو متابعة سريعة",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Name Input
                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = { userNameInput = it },
                    placeholder = { Text("الاسم المستعار (اختياري)", color = AppColors.TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.NeonBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = AppColors.SurfaceSoft,
                        unfocusedContainerColor = AppColors.Surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Input (Optional)
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    placeholder = { Text("رقم الهاتف لتنبيهات SMS (اختياري)", color = AppColors.TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = AppColors.SurfaceSoft,
                        unfocusedContainerColor = AppColors.Surface
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Continue Button
                Button(
                    onClick = {
                        authManager.completeOnboarding(userNameInput, phoneInput)
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.NeonBlue,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "دخول التطبيق واستكشاف المباريات ⚽",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
