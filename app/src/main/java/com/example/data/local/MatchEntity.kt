package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey
    val id: String,
    val sport: String,
    val league: String,
    val country: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeLogo: String = "",
    val awayLogo: String = "",
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val status: String = "NS",
    val minute: Int = 0,
    val kickoffTime: String = "20:00",
    val venue: String = "",
    val referee: String = "",
    val homeWinProb: Double = 45.0,
    val drawProb: Double = 27.0,
    val awayWinProb: Double = 28.0,
    val aiPreview: String = "",
    val aiLiveTactical: String = "",
    val aiPostReport: String = "",
    val possessionHome: Int = 50,
    val possessionAway: Int = 50,
    val xgHome: Float = 1.45f,
    val xgAway: Float = 1.10f,
    val shotsHome: Int = 12,
    val shotsAway: Int = 8,
    val shotsOnTargetHome: Int = 5,
    val shotsOnTargetAway: Int = 3,
    val bigChancesHome: Int = 3,
    val bigChancesAway: Int = 1,
    val passAccuracyHome: Int = 86,
    val passAccuracyAway: Int = 81,
    val cornersHome: Int = 6,
    val cornersAway: Int = 4,
    val foulsHome: Int = 9,
    val foulsAway: Int = 12,
    val isFavorite: Boolean = false,
    val dateKey: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
