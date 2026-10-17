package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AppColors

/**
 * 7 core factors evaluated by the Player Condition Index (PCI)
 */
data class PciFactors(
    val physicalFatigue: Float,       // 0 (exhausted) to 100 (peak freshness)
    val mentalPsychological: Float,   // 0 (high stress) to 100 (locked in / confident)
    val recentForm: Float,            // 0 (slump) to 100 (on fire / high xG/xA)
    val injuryRisk: Float,            // 0 (high danger) to 100 (fully safe / low load)
    val travelScheduleLoad: Float,    // 0 (dense 3-day travel) to 100 (well rested)
    val tacticalFit: Float,           // 0 (misaligned role) to 100 (ideal tactical system)
    val weatherPitchAdaptation: Float // 0 (hostile conditions) to 100 (accustomed)
) {
    /**
     * Weighted composite calculation for total PCI score
     */
    val compositeScore: Int
        get() {
            val score = (physicalFatigue * 0.22f) +
                    (recentForm * 0.20f) +
                    (injuryRisk * 0.18f) +
                    (tacticalFit * 0.14f) +
                    (mentalPsychological * 0.12f) +
                    (travelScheduleLoad * 0.08f) +
                    (weatherPitchAdaptation * 0.06f)
            return score.toInt().coerceIn(0, 100)
        }
}

data class PciTimelinePoint(
    val minute: Int,
    val score: Int,
    val note: String = ""
)

enum class PciStatus(val labelAr: String, val labelEn: String) {
    PEAK("جاهزية استثنائية", "Peak Condition"),
    OPTIMAL("حالة ممتازة", "Optimal"),
    MODERATE("إجهاد معتدل", "Moderate Fatigue"),
    HIGH_RISK("تحذير إجهاد عالي", "High Fatigue / Risk");

    val color: Color
        get() = when (this) {
            PEAK -> AppColors.NeonCyan
            OPTIMAL -> AppColors.NeonBlue
            MODERATE -> AppColors.NeonOrange
            HIGH_RISK -> AppColors.NeonRed
        }
}

data class PlayerCondition(
    val playerId: String,
    val playerName: String,
    val playerNumber: Int,
    val teamName: String,
    val position: String,
    val photoUrl: String = "",
    val factors: PciFactors,
    val timeline: List<PciTimelinePoint> = emptyList(),
    val keyStrengths: List<String> = emptyList(),
    val aiConditionSummary: String = ""
) {
    val overallScore: Int get() = factors.compositeScore

    val status: PciStatus
        get() = when {
            overallScore >= 88 -> PciStatus.PEAK
            overallScore >= 75 -> PciStatus.OPTIMAL
            overallScore >= 60 -> PciStatus.MODERATE
            else -> PciStatus.HIGH_RISK
        }
}
