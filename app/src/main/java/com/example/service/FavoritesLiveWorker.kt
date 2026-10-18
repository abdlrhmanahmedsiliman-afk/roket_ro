package com.example.service

import com.example.ui.theme.AppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background Live Monitor that tracks favorite teams, goals, and match events.
 * Dispatches real-time alerts into NotificationCenter every 30-90 seconds.
 */
class FavoritesLiveWorker(
    private val sportsRepository: SportsRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationCenter: NotificationCenter,
    private val scope: CoroutineScope
) {
    fun startTracking() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val favTeams = preferencesManager.favoriteTeams.value
                    val currentMatches = sportsRepository.matches.value

                    val liveFavorites = currentMatches.filter { match ->
                        match.isLive && (favTeams.contains(match.homeTeam) || favTeams.contains(match.awayTeam))
                    }

                    for (match in liveFavorites) {
                        val latestGoal = match.events.lastOrNull { it.type == com.example.model.EventType.GOAL }
                        if (latestGoal != null && latestGoal.minute == match.minute) {
                            notificationCenter.postNotification(
                                title = "⚽ هدف لفريقك المفضل (${latestGoal.player})",
                                message = "${match.homeTeam} ${match.homeScore} - ${match.awayScore} ${match.awayTeam} (د${latestGoal.minute}')",
                                color = AppColors.NeonCyan,
                                matchId = match.id
                            )
                        }
                    }
                } catch (_: Exception) {}

                // Check interval for live matches
                delay(45_000L)
            }
        }
    }
}
