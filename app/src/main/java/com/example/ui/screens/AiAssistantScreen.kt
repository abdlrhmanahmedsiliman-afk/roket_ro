package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
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
import com.example.model.AiChatMessage
import com.example.model.Match
import com.example.service.GeminiAiService
import com.example.ui.components.Rocket3D
import com.example.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AiAssistantScreen(
    aiService: GeminiAiService,
    matches: List<Match>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputMessage by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                id = "welcome",
                text = "أهلاً بك 🚀 أنا roket_ro AI!\nمساعدك الذكي الشامل — اسألني عن أي مباراة، تحليل تكتيكي، إحصائيات، أو أي سؤال عام أو تقني.",
                isUser = false
            )
        )
    }

    val quickPrompts = remember {
        listOf(
            "حلل قمة ريال مدريد ومانشستر سيتي",
            "اشرح لي قانون التسلل بالتفصيل",
            "قارن بين ميسي ورونالدو بالأرقام",
            "اكتبلي كود بايثون يقرأ JSON",
            "ما هي أفضل خطة لمواجهة الضغط العالي؟"
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isThinking) return
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            isUser = true
        )
        messages.add(userMsg)
        inputMessage = ""
        isThinking = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size)
            val replyText = aiService.askAssistant(text, matches)
            messages.add(
                AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = replyText,
                    isUser = false
                )
            )
            isThinking = false
            listState.animateScrollToItem(messages.size)
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.TextPrimary
                    )
                }

                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Rocket3D(size = 38.dp, showFlame = false, enableTilt = false)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "roket_ro AI",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.NeonBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = AppColors.NeonBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(
                        text = "مساعد ذكي شامل • تحليل مباريات وأسئلة عامة",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp)
            ) {
                // Quick prompt suggestions
                if (messages.size <= 3) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickPrompts) { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppColors.Surface)
                                    .border(1.dp, AppColors.NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .clickable { sendMessage(prompt) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    color = AppColors.TextSoft,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppColors.Surface)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = {
                            Text(
                                "اسأل أي سؤال رياضي أو عام...",
                                color = AppColors.TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = AppColors.TextPrimary,
                            unfocusedTextColor = AppColors.TextPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = { sendMessage(inputMessage) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AppColors.NeonGradient)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg = msg)
            }

            if (isThinking) {
                item {
                    TypingIndicator()
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: AiChatMessage) {
    val isUser = msg.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) AppColors.NeonGradient
                    else Brush.linearGradient(
                        colors = listOf(AppColors.Surface, AppColors.SurfaceSoft)
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) Color.Transparent else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(14.dp)
        ) {
            SelectionContainer {
                Text(
                    text = msg.text,
                    color = if (isUser) Color.White else AppColors.TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val dotAlpha1 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    val dotAlpha2 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d2")
    val dotAlpha3 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = AppColors.NeonBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "roket_ro يفكر...", color = AppColors.TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AppColors.NeonBlue.copy(alpha = dotAlpha1)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AppColors.NeonBlue.copy(alpha = dotAlpha2)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AppColors.NeonBlue.copy(alpha = dotAlpha3)))
        }
    }
}
