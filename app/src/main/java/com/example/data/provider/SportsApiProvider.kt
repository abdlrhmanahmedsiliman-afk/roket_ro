package com.example.data.provider

import com.example.model.League
import com.example.model.Match
import com.example.model.PlayerCondition

enum class ProviderTier(val tierNumber: Int, val labelAr: String) {
    TIER_1_PRIMARY(1, "الطبقة الأولى: المزودات الأساسية (API-Football, TheSportsDB, SportRadar)"),
    TIER_2_SPECIALIZED(2, "الطبقة الثانية: مزودات الإحصاءات المتقدمة (Opta Stats Perform, Football-Data, ESPN)"),
    TIER_3_GLOBAL_SPORTS(3, "الطبقة الثالثة: الرياضات العالمية (NBA, ATP Tennis, Formula 1, CricAPI)"),
    CACHED_FALLBACK(4, "الطبقة الرابعة: الذاكرة الاحتياطية وقاعدة البيانات المحلية")
}

data class ProviderHealth(
    val providerId: String,
    val providerName: String,
    val tier: ProviderTier,
    val isCircuitOpen: Boolean = false,
    val successRatePercent: Int = 100,
    val latencyMs: Long = 120L,
    val remainingQuota: Int = 1000
)

/**
 * Standard Circuit Breaker for Sports APIs
 * Automatically prevents cascading failures and shifts to the next tier when threshold is crossed.
 */
class CircuitBreaker(
    val failureThreshold: Int = 3,
    val resetTimeoutMs: Long = 30_000L
) {
    private var failureCount = 0
    private var lastFailureTime = 0L
    var isOpen: Boolean = false
        private set

    @Synchronized
    fun recordSuccess() {
        failureCount = 0
        isOpen = false
    }

    @Synchronized
    fun recordFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        if (failureCount >= failureThreshold) {
            isOpen = true
        }
    }

    @Synchronized
    fun allowExecution(): Boolean {
        if (!isOpen) return true
        if (System.currentTimeMillis() - lastFailureTime > resetTimeoutMs) {
            // Half-open: allow one probe request
            return true
        }
        return false
    }
}

/**
 * Token Bucket Rate Limiter
 */
class TokenBucketRateLimiter(
    val maxTokens: Int = 60,
    val refillTokensPerSecond: Int = 5
) {
    private var tokens = maxTokens.toDouble()
    private var lastRefillTimestamp = System.currentTimeMillis()

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()
        val elapsedSec = (now - lastRefillTimestamp) / 1000.0
        tokens = (tokens + elapsedSec * refillTokensPerSecond).coerceAtMost(maxTokens.toDouble())
        lastRefillTimestamp = now

        return if (tokens >= 1.0) {
            tokens -= 1.0
            true
        } else {
            false
        }
    }
}

/**
 * Common Contract for all Sports Providers
 */
interface SportsApiProvider {
    val id: String
    val name: String
    val tier: ProviderTier
    val circuitBreaker: CircuitBreaker

    suspend fun fetchLiveMatches(): Result<List<Match>>
    suspend fun fetchMatchDetails(matchId: String): Result<Match?>
    suspend fun fetchLeagues(): Result<List<League>>
    suspend fun fetchPlayerConditions(matchId: String): Result<List<PlayerCondition>>
}
