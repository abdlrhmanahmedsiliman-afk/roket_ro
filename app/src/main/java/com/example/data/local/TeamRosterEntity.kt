package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity to cache Team Rosters and Lineups locally.
 * Ensures instant offline availability and eliminates redundant API latency.
 */
@Entity(tableName = "team_rosters")
data class TeamRosterEntity(
    @PrimaryKey
    val teamName: String,
    val sport: String = "كرة القدم",
    val league: String = "",
    val formation: String = "4-3-3",
    val coach: String = "",
    val startersJson: String = "", // Serialized list of starting players
    val subsJson: String = "",     // Serialized list of substitute players
    val totalPlayers: Int = 11,
    val cachedAt: Long = System.currentTimeMillis(),
    val ttlMillis: Long = 86_400_000L // 24-hour cache TTL
) {
    val isExpired: Boolean
        get() = (System.currentTimeMillis() - cachedAt) > ttlMillis
}
