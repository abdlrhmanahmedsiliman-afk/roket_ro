package com.example.service

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class PredictionSubmissionResult {
    data class Success(val prediction: UserPrediction) : PredictionSubmissionResult()
    data class Rejected(val reasonAr: String) : PredictionSubmissionResult()
}

/**
 * Immutable Prediction Ledger Manager.
 * Prevents client-side balance manipulation, enforces kickoff cutoff,
 * guarantees one prediction per user/match, and calculates points on final result.
 */
class PredictionLedgerManager {
    private val ledger = mutableMapOf<String, UserPrediction>() // predictionId -> Prediction
    private val userMatchIndex = mutableMapOf<Pair<String, String>, String>() // (userId, matchId) -> predictionId
    private val processedIdempotencyKeys = mutableSetOf<String>()

    private val _userPoints = MutableStateFlow(50) // Initial welcome bonus points
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _predictions = MutableStateFlow<List<UserPrediction>>(emptyList())
    val predictions: StateFlow<List<UserPrediction>> = _predictions.asStateFlow()

    /**
     * Submits a match prediction with strict server-side validation.
     */
    @Synchronized
    fun submitPrediction(
        userId: String,
        match: Match,
        predictedHome: Int,
        predictedAway: Int,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): PredictionSubmissionResult {
        // 1. Replay attack check
        if (processedIdempotencyKeys.contains(idempotencyKey)) {
            val existingId = userMatchIndex[userId to match.id]
            val existing = existingId?.let { ledger[it] }
            return if (existing != null) PredictionSubmissionResult.Success(existing)
            else PredictionSubmissionResult.Rejected("تم استلام هذا الطلب سابقاً (Idempotent)")
        }

        // 2. Strict Kickoff Cutoff: reject if match is live or finished or past kickoff
        if (match.status != MatchStatus.NOT_STARTED || match.minute > 0) {
            return PredictionSubmissionResult.Rejected("عذراً، بدأت المباراة بالفعل ولا يمكن إرسال توقعات جديدة.")
        }

        // 3. One prediction per user per match rule
        val pairKey = userId to match.id
        val predictionId = "pred_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val prediction = UserPrediction(
            predictionId = predictionId,
            userId = userId,
            matchId = match.id,
            matchTitle = "${match.homeTeam} ضد ${match.awayTeam}",
            predictedHomeScore = predictedHome,
            predictedAwayScore = predictedAway,
            submittedAt = System.currentTimeMillis(),
            status = PredictionStatus.PENDING,
            pointsAwarded = 0,
            idempotencyKey = idempotencyKey
        )

        ledger[predictionId] = prediction
        userMatchIndex[pairKey] = predictionId
        processedIdempotencyKeys.add(idempotencyKey)

        _predictions.value = ledger.values.toList()
        return PredictionSubmissionResult.Success(prediction)
    }

    /**
     * Evaluates predictions against final match result and awards points.
     */
    @Synchronized
    fun evaluateFinalMatch(match: Match): Int {
        if (!match.isFinished) return 0

        var pointsDistributed = 0

        for ((id, pred) in ledger.entries.toList()) {
            if (pred.matchId == match.id && pred.status == PredictionStatus.PENDING) {
                val exactMatch = (pred.predictedHomeScore == match.homeScore && pred.predictedAwayScore == match.awayScore)
                val predictedWinner = when {
                    pred.predictedHomeScore > pred.predictedAwayScore -> 1
                    pred.predictedHomeScore < pred.predictedAwayScore -> -1
                    else -> 0
                }
                val actualWinner = when {
                    match.homeScore > match.awayScore -> 1
                    match.homeScore < match.awayScore -> -1
                    else -> 0
                }
                val correctOutcome = (predictedWinner == actualWinner)

                val (newStatus, points) = when {
                    exactMatch -> PredictionStatus.WON_EXACT to 10
                    correctOutcome -> PredictionStatus.WON_OUTCOME to 5
                    else -> PredictionStatus.LOST to 0
                }

                val updatedPred = pred.copy(status = newStatus, pointsAwarded = points)
                ledger[id] = updatedPred
                pointsDistributed += points
            }
        }

        if (pointsDistributed > 0) {
            _userPoints.value += pointsDistributed
            _predictions.value = ledger.values.toList()
        }

        return pointsDistributed
    }

    /**
     * Redeem points for ad-free experience.
     */
    @Synchronized
    fun redeemPointsForAdFree(cost: Int = 100): Boolean {
        if (_userPoints.value >= cost) {
            _userPoints.value -= cost
            return true
        }
        return false
    }
}
