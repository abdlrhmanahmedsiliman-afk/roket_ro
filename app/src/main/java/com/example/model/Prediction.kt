package com.example.model

enum class PredictionStatus(val labelAr: String) {
    PENDING("قيد الانتظار"),
    WON_EXACT("فوز بنتيجة دقيقة (+10 نقاط)"),
    WON_OUTCOME("فوز بنتيجة المباراة (+5 نقاط)"),
    LOST("توقع خاطئ (0 نقطة)"),
    REJECTED_STARTED("مرفوض - بدأت المباراة")
}

data class UserPrediction(
    val predictionId: String,
    val userId: String,
    val matchId: String,
    val matchTitle: String,
    val predictedHomeScore: Int,
    val predictedAwayScore: Int,
    val submittedAt: Long = System.currentTimeMillis(),
    val status: PredictionStatus = PredictionStatus.PENDING,
    val pointsAwarded: Int = 0,
    val idempotencyKey: String = ""
)
