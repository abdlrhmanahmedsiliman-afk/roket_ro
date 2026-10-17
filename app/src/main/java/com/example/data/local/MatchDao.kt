package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY updatedAt DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE sport = :sport ORDER BY updatedAt DESC")
    fun getMatchesBySport(sport: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE status IN ('LIVE', 'HT', 'ET', 'PEN') ORDER BY updatedAt DESC")
    fun getLiveMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    fun getMatchById(id: String): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    suspend fun getMatchByIdSync(id: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("UPDATE matches SET isFavorite = :isFavorite WHERE id = :matchId")
    suspend fun updateFavoriteStatus(matchId: String, isFavorite: Boolean)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatchById(id: String)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()

    @Query("SELECT COUNT(*) FROM matches")
    suspend fun getMatchesCount(): Int
}
