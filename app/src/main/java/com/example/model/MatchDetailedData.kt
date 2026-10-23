package com.example.model

data class MatchDetailedData(
    val matchId: String,
    val homeLineup: TeamLineup?,
    val awayLineup: TeamLineup?,
    val events: List<MatchEvent> = emptyList(),
    val stats: MatchStats? = null,
    val venue: String = "",
    val attendance: String = "",
    val referee: String = ""
)
