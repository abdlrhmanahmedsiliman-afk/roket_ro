package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for team rosters and player squads.
 * Provides offline-first access and cache synchronization methods.
 */
@Dao
interface TeamRosterDao {
    @Query("SELECT * FROM team_rosters WHERE teamName = :teamName LIMIT 1")
    fun getRosterByTeam(teamName: String): Flow<TeamRosterEntity?>

    @Query("SELECT * FROM team_rosters WHERE teamName = :teamName LIMIT 1")
    suspend fun getRosterByTeamSync(teamName: String): TeamRosterEntity?

    @Query("SELECT * FROM team_rosters WHERE league = :league")
    fun getRostersByLeague(league: String): Flow<List<TeamRosterEntity>>

    @Query("SELECT * FROM team_rosters ORDER BY cachedAt DESC")
    fun getAllRosters(): Flow<List<TeamRosterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoster(roster: TeamRosterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRosters(rosters: List<TeamRosterEntity>)

    @Update
    suspend fun updateRoster(roster: TeamRosterEntity)

    @Query("DELETE FROM team_rosters WHERE teamName = :teamName")
    suspend fun deleteRosterByTeam(teamName: String)

    @Query("DELETE FROM team_rosters")
    suspend fun deleteAllRosters()

    @Query("SELECT COUNT(*) FROM team_rosters")
    suspend fun getRostersCount(): Int
}
