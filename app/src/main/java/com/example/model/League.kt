package com.example.model

enum class LeagueTier(val labelAr: String, val badgeColor: Long, val descriptionAr: String) {
    TIER_1_ELITE("النخبة الصف الأول (Tier 1)", 0xFFFFD700, "أقوى بطولات ودوريات العالم الكبرى"),
    TIER_2_TOP("الدوريات الكبرى (Tier 2)", 0xFF5BC8FF, "دوريات قارية وأوروبية ممتازة"),
    TIER_3_MAJOR("دوريات متميزة (Tier 3)", 0xFF4FE3C1, "دوريات إقليمية ولاتينية وكؤوس هامة"),
    TIER_4_ALL_SPORTS("متعدد الرياضات (Tier 4)", 0xFFFF8A65, "NBA، تنس، كريكيت، فورمولا 1 وملاكمة")
}

data class StandingRow(
    val rank: Int,
    val teamName: String,
    val logo: String = "",
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDiff: Int,
    val points: Int,
    val form: List<String> = emptyList(),
    val teamId: Int = 0
)

data class TopScorer(
    val rank: Int,
    val playerName: String,
    val teamName: String,
    val goals: Int,
    val assists: Int = 0,
    val teamLogo: String = "",
    val playerPhoto: String = "",
    val penalties: Int = 0,
    val playedMatches: Int = 0
)

data class League(
    val id: String,
    val name: String,
    val country: String,
    val sport: String,
    val tier: LeagueTier = LeagueTier.TIER_1_ELITE,
    val logo: String = "",
    val season: String = "2024/2025",
    val activeTeamsCount: Int = 20,
    val standings: List<StandingRow> = emptyList(),
    val topScorers: List<TopScorer> = emptyList()
)
