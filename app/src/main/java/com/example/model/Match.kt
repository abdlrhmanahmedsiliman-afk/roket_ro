package com.example.model

enum class MatchStatus(val code: String, val labelAr: String, val isLive: Boolean) {
    NOT_STARTED("NS", "لم تبدأ", false),
    LIVE("LIVE", "مباشر", true),
    HALF_TIME("HT", "استراحة", true),
    FINISHED("FT", "انتهت", false),
    EXTRA_TIME("ET", "وقت إضافي", true),
    PENALTIES("PEN", "ركلات ترجيح", true)
}

enum class EventType {
    GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION, VAR, CORNER
}

data class MatchEvent(
    val minute: Int,
    val type: EventType,
    val teamIsHome: Boolean,
    val player: String,
    val detail: String = "",
    val assistPlayer: String = ""
)

data class MatchStats(
    val possessionHome: Int = 50,
    val possessionAway: Int = 50,
    val xgHome: Float = 0.0f,
    val xgAway: Float = 0.0f,
    val shotsHome: Int = 0,
    val shotsAway: Int = 0,
    val shotsOnTargetHome: Int = 0,
    val shotsOnTargetAway: Int = 0,
    val shotsOffTargetHome: Int = 0,
    val shotsOffTargetAway: Int = 0,
    val bigChancesHome: Int = 0,
    val bigChancesAway: Int = 0,
    val totalPassesHome: Int = 0,
    val totalPassesAway: Int = 0,
    val passAccuracyHome: Int = 0,
    val passAccuracyAway: Int = 0,
    val cornersHome: Int = 0,
    val cornersAway: Int = 0,
    val foulsHome: Int = 0,
    val foulsAway: Int = 0,
    val offsidesHome: Int = 0,
    val offsidesAway: Int = 0,
    val yellowCardsHome: Int = 0,
    val yellowCardsAway: Int = 0,
    val redCardsHome: Int = 0,
    val redCardsAway: Int = 0,
    val manOfTheMatch: String = "",
    val manOfTheMatchRating: Float = 0.0f,
    val manOfTheMatchPhoto: String = ""
)

data class PlayerLineup(
    val number: Int,
    val name: String,
    val position: String,
    val rating: Float = 7.0f,
    val isCaptain: Boolean = false,
    val x: Float = 0.5f, // Normalized tactical pitch coordinate [0.0 - 1.0]
    val y: Float = 0.5f, // Normalized tactical pitch coordinate [0.0 - 1.0]
    val photoUrl: String = "",
    val athleteId: String = ""
)

data class TeamLineup(
    val formation: String,
    val coach: String,
    val starters: List<PlayerLineup>,
    val subs: List<PlayerLineup>
)

data class HeadToHead(
    val pastMatchesCount: Int = 5,
    val homeWins: Int = 3,
    val draws: Int = 1,
    val awayWins: Int = 1,
    val recentFormHome: List<String> = listOf("W", "W", "D", "W", "L"),
    val recentFormAway: List<String> = listOf("L", "W", "D", "L", "W")
)

data class Match(
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
    val status: MatchStatus = MatchStatus.NOT_STARTED,
    val minute: Int = 0,
    val shortDetail: String = "",
    val kickoffTime: String = "20:00",
    val venue: String = "",
    val referee: String = "",
    val homeWinProb: Double = 45.0,
    val drawProb: Double = 27.0,
    val awayWinProb: Double = 28.0,
    val aiPreview: String = "",
    val aiLiveTactical: String = "",
    val aiPostReport: String = "",
    val stats: MatchStats = MatchStats(),
    val events: List<MatchEvent> = emptyList(),
    val homeLineup: TeamLineup? = null,
    val awayLineup: TeamLineup? = null,
    val h2h: HeadToHead = HeadToHead(),
    val isFavorite: Boolean = false,
    val dateKey: String = "",
    val playerConditions: List<PlayerCondition> = emptyList(),
    val broadcasters: List<Broadcaster> = emptyList(),
    val kickoffMillis: Long = System.currentTimeMillis(),
    val dataSource: DataSourceType = DataSourceType.LIVE_API,
    val statsSource: DataSourceType = DataSourceType.LIVE_API,
    val lineupSource: DataSourceType = DataSourceType.ESTIMATED_DEMO,
    val eventsSource: DataSourceType = DataSourceType.LIVE_API,
    val broadcastersSource: DataSourceType = DataSourceType.LIVE_API,
    val predictionModelInfo: String = "Poisson/Elo Model v3.2 (Calibrated 100%)",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    val isLive: Boolean get() = status.isLive
    val isFinished: Boolean get() = status == MatchStatus.FINISHED
}
