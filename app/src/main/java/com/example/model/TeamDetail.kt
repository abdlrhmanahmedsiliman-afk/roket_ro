package com.example.model

data class TeamTransfer(
    val playerName: String,
    val type: String, // "انتقال نهائي", "إعارة", "تجديد عقد"
    val otherClub: String,
    val fee: String,
    val season: String = "2024/2025",
    val isIncoming: Boolean = true
)

data class TeamPlayerStat(
    val number: Int,
    val name: String,
    val position: String,
    val rating: Float,
    val goals: Int = 0,
    val assists: Int = 0,
    val appearances: Int = 0,
    val photoUrl: String = "",
    val athleteId: String = "",
    val nationality: String = "",
    val age: Int = 26
)

data class TeamDetail(
    val teamName: String,
    val logo: String = "",
    val league: String = "",
    val country: String = "",
    val stadium: String = "",
    val coach: String = "",
    val rank: Int = 0,
    val points: Int = 0,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val recentForm: List<String> = emptyList(),
    val squad: List<TeamPlayerStat> = emptyList(),
    val topScorers: List<TopScorer> = emptyList(),
    val transfers: List<TeamTransfer> = emptyList(),
    val news: List<SportsNewsArticle> = emptyList(),
    val recentMatches: List<Match> = emptyList(),
    val upcomingMatches: List<Match> = emptyList()
)
