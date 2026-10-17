package com.example.data.local

import com.example.model.PlayerLineup
import com.example.model.TeamLineup

/**
 * Mapper to convert between Room TeamRosterEntity and domain TeamLineup.
 * Uses a robust, zero-overhead serialized representation for players.
 */
object TeamRosterMapper {

    fun serializePlayers(players: List<PlayerLineup>): String {
        return players.joinToString(";;") { player ->
            "${player.number}::${player.name}::${player.position}::${player.rating}::${player.isCaptain}::${player.x}::${player.y}"
        }
    }

    fun deserializePlayers(serialized: String): List<PlayerLineup> {
        if (serialized.isBlank()) return emptyList()
        return try {
            serialized.split(";;").mapNotNull { playerStr ->
                val tokens = playerStr.split("::")
                if (tokens.size >= 7) {
                    PlayerLineup(
                        number = tokens[0].toIntOrNull() ?: 0,
                        name = tokens[1],
                        position = tokens[2],
                        rating = tokens[3].toFloatOrNull() ?: 7.0f,
                        isCaptain = tokens[4].toBooleanStrictOrNull() ?: false,
                        x = tokens[5].toFloatOrNull() ?: 0.5f,
                        y = tokens[6].toFloatOrNull() ?: 0.5f
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toEntity(
        teamName: String,
        lineup: TeamLineup,
        league: String = "",
        sport: String = "كرة القدم"
    ): TeamRosterEntity {
        return TeamRosterEntity(
            teamName = teamName,
            sport = sport,
            league = league,
            formation = lineup.formation,
            coach = lineup.coach,
            startersJson = serializePlayers(lineup.starters),
            subsJson = serializePlayers(lineup.subs),
            totalPlayers = lineup.starters.size + lineup.subs.size,
            cachedAt = System.currentTimeMillis()
        )
    }

    fun toDomainModel(entity: TeamRosterEntity): TeamLineup {
        val starters = deserializePlayers(entity.startersJson)
        val subs = deserializePlayers(entity.subsJson)
        return TeamLineup(
            formation = entity.formation,
            coach = entity.coach,
            starters = starters,
            subs = subs
        )
    }
}
