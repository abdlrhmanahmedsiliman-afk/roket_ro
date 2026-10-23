package com.example.data.provider

import com.example.model.PlayerDetail
import com.example.model.TeamDetail

/**
 * Honest player/team helper.
 *
 * Older versions of this file contained hand-typed photo IDs (many wrong or duplicated), a pool of
 * random faces assigned to unknown players by name hash, and invented squads, stats and transfers.
 * All of that was removed on purpose: a wrong face or an invented number is worse than "unavailable".
 * Real team pages now come from [EspnTeamApi].
 */
object PlayerDatabaseProvider {
    @Suppress("unused")
    val curatedPlayerPhotos: Map<String, String> = emptyMap()

    /** Real photo only: the ESPN headshot of the SAME athlete id. Unknown -> "" (the UI shows initials). */
    fun getPlayerPhoto(playerName: String, athleteId: String = ""): String {
        val id = athleteId.trim()
        return if (id.isNotEmpty() && id.all { it.isDigit() }) {
            "https://a.espncdn.com/i/headshots/soccer/players/full/$id.png"
        } else {
            ""
        }
    }

    fun getPlayerDetail(name: String, teamName: String, position: String = "FWD", number: Int = 10): PlayerDetail =
        PlayerDetail(
            id = name,
            name = name,
            teamName = teamName,
            number = number,
            position = position,
            nationality = "غير متاح",
            age = 0,
            height = "غير متاح",
            preferredFoot = "غير متاح",
            rating = 0f,
            marketValue = "غير متاح",
            contractUntil = "غير متاح",
            biographyAr = "لا تتوفر بيانات تفصيلية موثوقة لهذا اللاعب في المصادر الحالية."
        )

    fun getTeamDetail(teamName: String, leagueName: String = ""): TeamDetail =
        TeamDetail(teamName = teamName, league = leagueName, rank = 0, recentForm = emptyList())
}
