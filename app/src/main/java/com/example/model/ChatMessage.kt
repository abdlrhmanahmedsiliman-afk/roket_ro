package com.example.model

data class AiChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val matchContextId: String? = null,
    val suggestedActions: List<String> = emptyList()
)
