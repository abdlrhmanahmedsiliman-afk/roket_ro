package com.example.model

data class PlayerDetail(
    val id: String,
    val name: String,
    val teamName: String,
    val teamLogo: String = "",
    val number: Int,
    val position: String,
    val nationality: String,
    val age: Int,
    val height: String = "غير متاح",
    val preferredFoot: String = "غير متاح",
    val rating: Float = 0f,
    val photoUrl: String = "",
    val athleteId: String = "",
    val goalsSeason: Int = 0,
    val assistsSeason: Int = 0,
    val matchesPlayed: Int = 0,
    val minutesPlayed: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val marketValue: String = "غير متاح",
    val contractUntil: String = "غير متاح",
    val pciCondition: PlayerCondition? = null,
    val biographyAr: String = ""
)
