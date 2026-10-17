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
    val detail: String = ""
)

data class MatchStats(
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
    val yellowCardsHome: Int = 2,
    val yellowCardsAway: Int = 3,
    val redCardsHome: Int = 0,
    val redCardsAway: Int = 0
)

data class PlayerLineup(
    val number: Int,
    val name: String,
    val position: String,
    val rating: Float = 7.0f,
    val isCaptain: Boolean = false,
    val x: Float = 0.5f, // Normalized tactical pitch coordinate [0.0 - 1.0]
    val y: Float = 0.5f  // Normalized tactical pitch coordinate [0.0 - 1.0]
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
    val playerConditions: List<PlayerCondition> = emptyList(),
    val broadcasters: List<Broadcaster> = emptyList()
) {
    val isLive: Boolean get() = status.isLive
    val isFinished: Boolean get() = status == MatchStatus.FINISHED
}
