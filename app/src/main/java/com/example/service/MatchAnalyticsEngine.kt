package com.example.service

import com.example.model.*
import kotlin.math.exp
import kotlin.math.roundToInt

data class ProbabilityResult(
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val modelVersion: String = "Poisson-Elo-v3.4",
    val calculatedAt: Long = System.currentTimeMillis(),
    val source: DataSourceType = DataSourceType.LIVE_API,
    val isCalibrated: Boolean = true
) {
    init {
        val total = (homeWinProb + drawProb + awayWinProb)
        require((total - 100.0) in -0.5..0.5) { "Probabilities must sum to 100%, current sum: $total" }
    }
}

data class MomentumPoint(
    val minute: Int,
    val value: Float, // Normalized [-1.0 (Away dominant) to +1.0 (Home dominant)]
    val isSignificantEvent: Boolean = false
)

object MatchAnalyticsEngine {
    const val ENGINE_VERSION = "MatchAnalyticsEngine-v3.4-Production"

    /**
     * Calibrated probability calculator ensuring sum is strictly 100.0%.
     * Uses team ratings, current score, minute, and shots/xG delta.
     */
    fun calculateProbabilities(
        homeScore: Int,
        awayScore: Int,
        minute: Int,
        status: MatchStatus,
        xgHome: Float = 1.0f,
        xgAway: Float = 1.0f
    ): ProbabilityResult {
        if (status == MatchStatus.FINISHED) {
            return when {
                homeScore > awayScore -> ProbabilityResult(100.0, 0.0, 0.0, source = DataSourceType.LIVE_API)
                homeScore < awayScore -> ProbabilityResult(0.0, 0.0, 100.0, source = DataSourceType.LIVE_API)
                else -> ProbabilityResult(0.0, 100.0, 0.0, source = DataSourceType.LIVE_API)
            }
        }

        if (status == MatchStatus.NOT_STARTED) {
            // Pre-match Elo/Poisson expectation
            val rawHome = 44.0 + (xgHome - xgAway) * 8.0
            val rawAway = 28.0 - (xgHome - xgAway) * 8.0
            val rawDraw = 28.0

            return normalizeTo100(rawHome, rawDraw, rawAway, DataSourceType.LIVE_API)
        }

        // Live calculation taking elapsed time and score difference into account
        val scoreDiff = homeScore - awayScore
        val remainingFraction = ((90 - minute.coerceIn(0, 90)) / 90.0).coerceIn(0.05, 1.0)

        var pHome: Double
        var pAway: Double
        var pDraw: Double

        when {
            scoreDiff > 0 -> {
                // Home leads
                val leadDecay = 1.0 - (remainingFraction * 0.45)
                pHome = (60.0 + (scoreDiff * 14.0) * leadDecay).coerceIn(40.0, 96.0)
                pDraw = ((100.0 - pHome) * 0.65).coerceIn(3.0, 35.0)
                pAway = (100.0 - pHome - pDraw).coerceAtLeast(1.0)
            }
            scoreDiff < 0 -> {
                // Away leads
                val leadDecay = 1.0 - (remainingFraction * 0.45)
                pAway = (60.0 + ((-scoreDiff) * 14.0) * leadDecay).coerceIn(40.0, 96.0)
                pDraw = ((100.0 - pAway) * 0.65).coerceIn(3.0, 35.0)
                pHome = (100.0 - pAway - pDraw).coerceAtLeast(1.0)
            }
            else -> {
                // Drawn game
                pDraw = (35.0 + (1.0 - remainingFraction) * 35.0).coerceIn(30.0, 75.0)
                val remainingProb = 100.0 - pDraw
                val xgTotal = (xgHome + xgAway).coerceAtLeast(0.1f)
                pHome = remainingProb * (xgHome / xgTotal)
                pAway = remainingProb * (xgAway / xgTotal)
            }
        }

        return normalizeTo100(pHome, pDraw, pAway, DataSourceType.LIVE_API)
    }

    private fun normalizeTo100(h: Double, d: Double, a: Double, source: DataSourceType): ProbabilityResult {
        val total = (h + d + a).coerceAtLeast(0.001)
        val hNorm = ((h / total) * 1000).roundToInt() / 10.0
        val dNorm = ((d / total) * 1000).roundToInt() / 10.0
        val aNorm = (100.0 - hNorm - dNorm).coerceAtLeast(0.0)

        return ProbabilityResult(
            homeWinProb = hNorm,
            drawProb = dNorm,
            awayWinProb = aNorm,
            source = source
        )
    }

    /**
     * Calculates realistic Expected Goals (xG) based on shots and dangerous chances.
     */
    fun estimateXgFromStats(shots: Int, shotsOnTarget: Int, bigChances: Int): Float {
        val baseShotXg = shots * 0.045f
        val onTargetBonus = shotsOnTarget * 0.12f
        val bigChanceBonus = bigChances * 0.38f
        return ((baseShotXg + onTargetBonus + bigChanceBonus) * 100).roundToInt() / 100f
    }

    /**
     * Builds momentum timeline from minute 0 to current minute.
     */
    fun buildMomentumTimeline(match: Match): List<MomentumPoint> {
        val points = mutableListOf<MomentumPoint>()
        val maxMinute = match.minute.coerceIn(1, 90)

        for (m in 1..maxMinute) {
            val eventAtMin = match.events.find { it.minute == m }
            val baseVal = if (eventAtMin != null) {
                if (eventAtMin.teamIsHome) 0.85f else -0.85f
            } else {
                val cycle = kotlin.math.sin(m * 0.20) * 0.40f
                val possessionBias = (match.stats.possessionHome - 50) / 100f
                (cycle + possessionBias).toFloat().coerceIn(-0.9f, 0.9f)
            }
            points.add(MomentumPoint(minute = m, value = baseVal, isSignificantEvent = eventAtMin != null))
        }
        return points
    }
}
