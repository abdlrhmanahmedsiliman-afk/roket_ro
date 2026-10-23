package com.example.model

data class SportsNewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val url: String,
    val imageUrl: String = "",
    val publishedAt: String = "",
    val timeAgo: String = "الآن"
)
