package com.example.data.provider

import com.example.BuildConfig
import com.example.model.EventType
import com.example.model.MatchEvent
import com.example.model.PlayerLineup
import com.example.model.TeamLineup
import com.example.model.TeamTransfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * On-Demand Client for API-Football (RapidAPI / api-sports.io).
 * - Enforces daily quota preservation (100 requests/day limit).
 * - Reads `x-ratelimit-requests-remaining` and halts non-essential requests if quota is critical.
 * - Called ONLY when opening a match page or team hub.
 * - 24-hour cache for responses; 30-day cache for team name -> ID search resolution.
 * - Strictly displays real confirmed transfers or "غير متاح".
 */
object ApiFootballClient {

    private const val BASE_URL = "https://v3.football.api-sports.io"
    private const val SOURCE_NAME = "API-Football"

    // Team search name mapping cached for 30 days
    private val teamNameToIdCache = ConcurrentHashMap<String, Pair<Int, Long>>()

    // Last recorded quota remaining
    var lastRecordedQuotaRemaining: Int? = null
        private set

    private fun getValidApiKey(): String? {
        val key = BuildConfig.API_FOOTBALL_KEY.trim()
        if (key.isBlank() || key == "YOUR_API_FOOTBALL_KEY" || key.startsWith("YOUR_")) {
            return null
        }
        return key
    }

    private fun isQuotaDepleted(): Boolean {
        val remaining = lastRecordedQuotaRemaining
        return remaining != null && remaining <= 3
    }

    private suspend fun executeApiFootballRequest(
        endpoint: String,
        cacheKey: String,
        cacheTtlHours: Long = 24
    ): DataResult<Pair<String, Map<String, String>>> = withContext(Dispatchers.IO) {
        val apiKey = getValidApiKey()
            ?: return@withContext DataResult.Error(
                errorType = ErrorType.MISSING_OR_INVALID_KEY,
                message = "مفتاح API_FOOTBALL_KEY غير مهيأ. يرجى إضافته في إعدادات الأسرار (100 طلب/يوم مجاني).",
                source = SOURCE_NAME
            )

        if (isQuotaDepleted()) {
            return@withContext DataResult.Error(
                errorType = ErrorType.QUOTA_EXHAUSTED,
                message = "تم استهلاك الحصة اليومية المتاحة لـ API-Football (100 طلب/يوم). سيتم استئناف الطلبات غداً.",
                source = SOURCE_NAME
            )
        }

        val url = "$BASE_URL/$endpoint"
        val request = Request.Builder()
            .url(url)
            .header("x-apisports-key", apiKey)
            .build()

        val result = ProviderNetworkClient.executeSafe(
            request = request,
            sourceName = SOURCE_NAME,
            cacheKey = cacheKey,
            cacheTtlMillis = TimeUnit.HOURS.toMillis(cacheTtlHours)
        )

        if (result is DataResult.Success) {
            val remaining = result.data.second["x-ratelimit-requests-remaining"]?.toIntOrNull()
            if (remaining != null) {
                lastRecordedQuotaRemaining = remaining
            }
        }
        result
    }

    /**
     * Search team ID by name and cache for 30 days
     */
    suspend fun resolveTeamId(teamName: String): Int? = withContext(Dispatchers.IO) {
        val norm = teamName.trim().lowercase()
        val cached = teamNameToIdCache[norm]
        val now = System.currentTimeMillis()
        if (cached != null && now < cached.second) {
            return@withContext cached.first
        }

        val encoded = try { URLEncoder.encode(teamName, "UTF-8") } catch (_: Exception) { teamName }
        val result = executeApiFootballRequest(
            endpoint = "teams?search=$encoded",
            cacheKey = "apif_team_search_$norm",
            cacheTtlHours = 24 * 30 // 30 days
        )

        when (result) {
            is DataResult.Success -> {
                try {
                    val root = JSONObject(result.data.first)
                    val response = root.optJSONArray("response")
                    if (response != null && response.length() > 0) {
                        val teamObj = response.getJSONObject(0).getJSONObject("team")
                        val id = teamObj.getInt("id")
                        teamNameToIdCache[norm] = id to (now + TimeUnit.DAYS.toMillis(30))
                        return@withContext id
                    }
                } catch (_: Exception) {}
            }
            else -> {}
        }
        null
    }

    /**
     * Fetch confirmed transfers for a team
     */
    suspend fun fetchConfirmedTransfers(teamName: String): DataResult<List<TeamTransfer>> = withContext(Dispatchers.IO) {
        val teamId = resolveTeamId(teamName)
            ?: return@withContext DataResult.Empty(
                source = SOURCE_NAME,
                message = "لم يتم العثور على سجل انتقالات رسمي مؤكد لهذا النادي في API-Football"
            )

        val result = executeApiFootballRequest(
            endpoint = "transfers?team=$teamId",
            cacheKey = "apif_transfers_$teamId",
            cacheTtlHours = 24
        )

        when (result) {
            is DataResult.Success -> {
                try {
                    val root = JSONObject(result.data.first)
                    val responseArray = root.optJSONArray("response")
                    if (responseArray == null || responseArray.length() == 0) {
                        return@withContext DataResult.Empty(source = SOURCE_NAME, message = "لا توجد انتقالات رسمية مسجلة حالياً")
                    }

                    val transfers = mutableListOf<TeamTransfer>()
                    for (i in 0 until responseArray.length()) {
                        val playerEntry = responseArray.getJSONObject(i)
                        val playerObj = playerEntry.getJSONObject("player")
                        val playerName = playerObj.optString("name", "لاعب")
                        val transfersList = playerEntry.optJSONArray("transfers") ?: continue

                        for (j in 0 until transfersList.length()) {
                            val tObj = transfersList.getJSONObject(j)
                            val date = tObj.optString("date", "")
                            val type = tObj.optString("type", "انتقال")
                            val teamsObj = tObj.optJSONObject("teams")
                            val inTeam = teamsObj?.optJSONObject("in")?.optString("name", "") ?: ""
                            val outTeam = teamsObj?.optJSONObject("out")?.optString("name", "") ?: ""

                            val isIncoming = inTeam.contains(teamName, ignoreCase = true)
                            val otherClub = if (isIncoming) outTeam else inTeam

                            transfers.add(
                                TeamTransfer(
                                    playerName = playerName,
                                    type = when {
                                        type.contains("Loan", true) -> "إعارة"
                                        type.contains("Free", true) -> "انتقال حر"
                                        type.contains("N/A", true) || type.isBlank() -> "غير متاح"
                                        else -> type
                                    },
                                    otherClub = otherClub.ifBlank { "غير متاح" },
                                    fee = type,
                                    season = date.take(4).ifBlank { "غير متاح" },
                                    isIncoming = isIncoming
                                )
                            )
                        }
                    }

                    if (transfers.isEmpty()) {
                        DataResult.Empty(source = SOURCE_NAME, message = "لا توجد انتقالات رسمية مسجلة")
                    } else {
                        DataResult.Success(transfers.take(20), source = SOURCE_NAME, cached = result.cached)
                    }
                } catch (e: Exception) {
                    DataResult.Error(ErrorType.MALFORMED_JSON, e.message ?: "", SOURCE_NAME)
                }
            }
            is DataResult.Empty -> result
            is DataResult.Error -> result
        }
    }

    /**
     * Fetch confirmed match events (goals, cards, substitutions)
     */
    suspend fun fetchFixtureEvents(fixtureId: String): DataResult<List<MatchEvent>> = withContext(Dispatchers.IO) {
        val result = executeApiFootballRequest(
            endpoint = "fixtures/events?fixture=$fixtureId",
            cacheKey = "apif_events_$fixtureId",
            cacheTtlHours = 1
        )

        when (result) {
            is DataResult.Success -> {
                try {
                    val root = JSONObject(result.data.first)
                    val resp = root.optJSONArray("response") ?: return@withContext DataResult.Empty(SOURCE_NAME)
                    val events = mutableListOf<MatchEvent>()

                    for (i in 0 until resp.length()) {
                        val e = resp.getJSONObject(i)
                        val timeObj = e.optJSONObject("time")
                        val elapsed = timeObj?.optInt("elapsed", 0) ?: 0
                        val extra = timeObj?.optInt("extra", 0) ?: 0
                        val minuteStr = if (extra > 0) "$elapsed+$extra'" else "$elapsed'"

                        val typeStr = e.optString("type", "")
                        val detailStr = e.optString("detail", "")
                        val player = e.optJSONObject("player")?.optString("name", "لاعب") ?: "لاعب"

                        val eventType = when {
                            typeStr.equals("Goal", true) -> EventType.GOAL
                            typeStr.equals("Card", true) && detailStr.contains("Yellow", true) -> EventType.YELLOW_CARD
                            typeStr.equals("Card", true) -> EventType.RED_CARD
                            typeStr.equals("subst", true) -> EventType.SUBSTITUTION
                            else -> EventType.GOAL
                        }

                        events.add(
                            MatchEvent(
                                minute = minuteStr.toIntOrNull() ?: 0,
                                type = eventType,
                                teamIsHome = true, // refined by caller via team name match
                                player = player,
                                detail = detailStr
                            )
                        )
                    }
                    DataResult.Success(events, SOURCE_NAME, result.cached)
                } catch (e: Exception) {
                    DataResult.Error(ErrorType.MALFORMED_JSON, e.message ?: "", SOURCE_NAME)
                }
            }
            is DataResult.Empty -> result
            is DataResult.Error -> result
        }
    }

    /**
     * Fetch match injuries
     */
    suspend fun fetchInjuries(fixtureId: String): DataResult<List<String>> = withContext(Dispatchers.IO) {
        val result = executeApiFootballRequest(
            endpoint = "injuries?fixture=$fixtureId",
            cacheKey = "apif_injuries_$fixtureId",
            cacheTtlHours = 4
        )

        when (result) {
            is DataResult.Success -> {
                try {
                    val root = JSONObject(result.data.first)
                    val resp = root.optJSONArray("response") ?: return@withContext DataResult.Empty(SOURCE_NAME)
                    val list = mutableListOf<String>()
                    for (i in 0 until resp.length()) {
                        val item = resp.getJSONObject(i)
                        val player = item.optJSONObject("player")?.optString("name", "") ?: ""
                        val reason = item.optString("type", "إصابة")
                        if (player.isNotBlank()) list.add("$player ($reason)")
                    }
                    if (list.isEmpty()) DataResult.Empty(SOURCE_NAME, "لا توجد إصابات مسجلة للمباراة")
                    else DataResult.Success(list, SOURCE_NAME, result.cached)
                } catch (e: Exception) {
                    DataResult.Error(ErrorType.MALFORMED_JSON, e.message ?: "", SOURCE_NAME)
                }
            }
            is DataResult.Empty -> result
            is DataResult.Error -> result
        }
    }
}
