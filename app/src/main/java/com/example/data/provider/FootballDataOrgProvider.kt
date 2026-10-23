package com.example.data.provider

import com.example.BuildConfig
import com.example.model.StandingRow
import com.example.model.TopScorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Official client for football-data.org (Tier 1 Priority: Standings, Scorers, Squads).
 * - Enforces 10 requests / minute rate limiter.
 * - Respects HTTP 429 and rate-limiting headers.
 * - Extracts standings with last 5 match form badges, official scorers, and team profiles.
 * - Free competition codes supported: PL, ELC, PD, SA, BL1, FL1, DED, PPL, BSA, CL, EC, WC.
 */
object FootballDataOrgProvider {

    private const val BASE_URL = "https://api.football-data.org/v4"
    private const val SOURCE_NAME = "football-data.org"

    // Supported 12 Free Competition Codes
    val FREE_COMPETITIONS: Map<String, String> = mapOf(
        "PL" to "الدوري الإنجليزي الممتاز",
        "ELC" to "دوري البطولة الإنجليزية (Championship)",
        "PD" to "الدوري الإسباني (La Liga)",
        "SA" to "الدوري الإيطالي (Serie A)",
        "BL1" to "الدوري الألماني (Bundesliga)",
        "FL1" to "الدوري الفرنسي (Ligue 1)",
        "DED" to "الدوري الهولندي (Eredivisie)",
        "PPL" to "الدوري البرتغالي (Primeira Liga)",
        "BSA" to "الدوري البرازيلي (Brasileirão)",
        "CL" to "دوري أبطال أوروبا (Champions League)",
        "EC" to "كأس الأمم الأوروبية (Euro)",
        "WC" to "كأس العالم (FIFA World Cup)"
    )

    // Token Bucket for 10 requests / minute (1 token replenished every 6.0 seconds)
    private var tokens = 10.0
    private var lastTokenRefill = System.currentTimeMillis()
    private var rateLimitResetTime = 0L

    // Cache of team names to football-data.org ID (retained for 30 days)
    private val teamNameToIdCache = ConcurrentHashMap<String, Int>()

    @Synchronized
    private fun tryConsumeRateToken(): Boolean {
        val now = System.currentTimeMillis()
        if (now < rateLimitResetTime) return false

        val elapsedSeconds = (now - lastTokenRefill) / 1000.0
        // 10 tokens per 60s = 0.1667 tokens/sec
        tokens = (tokens + elapsedSeconds * (10.0 / 60.0)).coerceAtMost(10.0)
        lastTokenRefill = now

        return if (tokens >= 1.0) {
            tokens -= 1.0
            true
        } else {
            false
        }
    }

    @Synchronized
    fun recordRateLimit(resetSeconds: Long = 60L) {
        tokens = 0.0
        rateLimitResetTime = System.currentTimeMillis() + (resetSeconds * 1000L)
    }

    private fun getValidApiKey(): String? {
        val key = BuildConfig.FOOTBALL_DATA_KEY.trim()
        if (key.isBlank() || key == "YOUR_FOOTBALL_DATA_KEY" || key.startsWith("YOUR_")) {
            return null
        }
        return key
    }

    /**
     * Map League Name or ID to football-data.org free competition code
     */
    fun getCompetitionCode(leagueIdentifier: String): String? {
        val s = leagueIdentifier.trim().lowercase()
        return when {
            s.contains("إنجليزي") || s.contains("premier") || s == "pl" || s == "eng.1" -> "PL"
            s.contains("شامبيونشيب") || s.contains("championship") || s == "elc" -> "ELC"
            s.contains("إسباني") || s.contains("liga") || s == "pd" || s == "esp.1" -> "PD"
            s.contains("إيطالي") || s.contains("serie a") || s == "sa" || s == "ita.1" -> "SA"
            s.contains("ألماني") || s.contains("bundesliga") || s == "bl1" || s == "ger.1" -> "BL1"
            s.contains("فرنسي") || s.contains("ligue 1") || s == "fl1" || s == "fra.1" -> "FL1"
            s.contains("هولندي") || s.contains("eredivisie") || s == "ded" || s == "ned.1" -> "DED"
            s.contains("برتغالي") || s.contains("primeira") || s == "ppl" || s == "por.1" -> "PPL"
            s.contains("برازيلي") || s.contains("brasileiro") || s == "bsa" || s == "bra.1" -> "BSA"
            s.contains("أبطال أوروبا") || s.contains("champions") || s == "cl" || s == "uefa.champions" -> "CL"
            s.contains("يورو") || s.contains("euro") || s == "ec" -> "EC"
            s.contains("كأس العالم") || s.contains("world cup") || s == "wc" -> "WC"
            else -> null
        }
    }

    /**
     * Fetch Standings table for a competition
     */
    suspend fun fetchStandings(competitionCode: String): DataResult<List<StandingRow>> = withContext(Dispatchers.IO) {
        val apiKey = getValidApiKey()
            ?: return@withContext DataResult.Error(
                errorType = ErrorType.MISSING_OR_INVALID_KEY,
                message = "مفتاح FOOTBALL_DATA_KEY غير مهيأ. يرجى إضافته في إعدادات الأسرار (GitHub Secrets / .env).",
                source = SOURCE_NAME
            )

        if (!tryConsumeRateToken()) {
            return@withContext DataResult.Error(
                errorType = ErrorType.RATE_LIMITED,
                message = "تم تجاوز حد 10 طلبات/دقيقة في football-data.org. يرجى الانتظار ثوانٍ معدودة.",
                source = SOURCE_NAME
            )
        }

        val url = "$BASE_URL/competitions/$competitionCode/standings"
        val request = Request.Builder()
            .url(url)
            .header("X-Auth-Token", apiKey)
            .build()

        val cacheKey = "fdo_standings_$competitionCode"
        // 30 minutes cache for standings
        val networkResult = ProviderNetworkClient.executeSafe(
            request = request,
            sourceName = SOURCE_NAME,
            cacheKey = cacheKey,
            cacheTtlMillis = TimeUnit.MINUTES.toMillis(30)
        )

        when (networkResult) {
            is DataResult.Success -> {
                val (jsonString, headers) = networkResult.data
                try {
                    val root = JSONObject(jsonString)
                    val standingsArray = root.optJSONArray("standings")
                    if (standingsArray == null || standingsArray.length() == 0) {
                        return@withContext DataResult.Empty(source = SOURCE_NAME, message = "لا يتوفر جدول ترتيب حالياً لهذه البطولة")
                    }

                    // Look for TOTAL table standings
                    var tableArray = standingsArray.optJSONObject(0)?.optJSONArray("table")
                    for (i in 0 until standingsArray.length()) {
                        val obj = standingsArray.optJSONObject(i)
                        if (obj?.optString("type") == "TOTAL") {
                            tableArray = obj.optJSONArray("table")
                            break
                        }
                    }

                    if (tableArray == null || tableArray.length() == 0) {
                        return@withContext DataResult.Empty(source = SOURCE_NAME, message = "جدول الترتيب فارغ في المصدر")
                    }

                    val rows = mutableListOf<StandingRow>()
                    for (i in 0 until tableArray.length()) {
                        val rowObj = tableArray.getJSONObject(i)
                        val teamObj = rowObj.getJSONObject("team")
                        val teamId = teamObj.getInt("id")
                        val teamName = teamObj.optString("shortName", teamObj.optString("name", "فريق"))
                        val crest = teamObj.optString("crest", "")

                        // Cache team ID mapping
                        teamNameToIdCache[teamName.lowercase()] = teamId
                        teamObj.optString("name").let { if (it.isNotBlank()) teamNameToIdCache[it.lowercase()] = teamId }

                        val formStr = rowObj.optString("form", "")
                        val formList = if (formStr.isNotBlank()) {
                            formStr.split(",", "").map { it.trim().uppercase() }.filter { it.isNotBlank() }
                        } else emptyList()

                        rows.add(
                            StandingRow(
                                rank = rowObj.optInt("position", i + 1),
                                teamName = teamName,
                                logo = crest,
                                played = rowObj.optInt("playedGames", 0),
                                won = rowObj.optInt("won", 0),
                                drawn = rowObj.optInt("draw", 0),
                                lost = rowObj.optInt("lost", 0),
                                goalsFor = rowObj.optInt("goalsFor", 0),
                                goalsAgainst = rowObj.optInt("goalsAgainst", 0),
                                goalDiff = rowObj.optInt("goalDifference", 0),
                                points = rowObj.optInt("points", 0),
                                form = formList,
                                teamId = teamId
                            )
                        )
                    }

                    DataResult.Success(rows, source = SOURCE_NAME, cached = networkResult.cached)
                } catch (e: Exception) {
                    DataResult.Error(
                        errorType = ErrorType.MALFORMED_JSON,
                        message = "خطأ في تحليل استجابة الترتيب: ${e.message}",
                        source = SOURCE_NAME,
                        throwable = e
                    )
                }
            }
            is DataResult.Empty -> networkResult
            is DataResult.Error -> {
                if (networkResult.httpCode == 429) recordRateLimit(60)
                networkResult
            }
        }
    }

    /**
     * Fetch real Top Scorers for a competition (limit 20)
     */
    suspend fun fetchTopScorers(competitionCode: String, limit: Int = 20): DataResult<List<TopScorer>> = withContext(Dispatchers.IO) {
        val apiKey = getValidApiKey()
            ?: return@withContext DataResult.Error(
                errorType = ErrorType.MISSING_OR_INVALID_KEY,
                message = "مفتاح FOOTBALL_DATA_KEY غير مهيأ. يرجى إضافته في إعدادات الأسرار.",
                source = SOURCE_NAME
            )

        if (!tryConsumeRateToken()) {
            return@withContext DataResult.Error(
                errorType = ErrorType.RATE_LIMITED,
                message = "تم تجاوز حد 10 طلبات/دقيقة في football-data.org.",
                source = SOURCE_NAME
            )
        }

        val url = "$BASE_URL/competitions/$competitionCode/scorers?limit=$limit"
        val request = Request.Builder()
            .url(url)
            .header("X-Auth-Token", apiKey)
            .build()

        val cacheKey = "fdo_scorers_${competitionCode}_$limit"
        val networkResult = ProviderNetworkClient.executeSafe(
            request = request,
            sourceName = SOURCE_NAME,
            cacheKey = cacheKey,
            cacheTtlMillis = TimeUnit.MINUTES.toMillis(30)
        )

        when (networkResult) {
            is DataResult.Success -> {
                val (jsonString, _) = networkResult.data
                try {
                    val root = JSONObject(jsonString)
                    val scorersArray = root.optJSONArray("scorers")
                    if (scorersArray == null || scorersArray.length() == 0) {
                        return@withContext DataResult.Empty(source = SOURCE_NAME, message = "قائمة الهدافين غير متوفرة حالياً في المصدر")
                    }

                    val list = mutableListOf<TopScorer>()
                    for (i in 0 until scorersArray.length()) {
                        val item = scorersArray.getJSONObject(i)
                        val playerObj = item.getJSONObject("player")
                        val playerName = playerObj.optString("name", "لاعب")
                        val teamObj = item.optJSONObject("team")
                        val teamName = teamObj?.optString("shortName", teamObj.optString("name", "غير متاح")) ?: "غير متاح"
                        val teamCrest = teamObj?.optString("crest", "") ?: ""

                        list.add(
                            TopScorer(
                                rank = i + 1,
                                playerName = playerName,
                                teamName = teamName,
                                goals = item.optInt("goals", 0),
                                assists = item.optInt("assists", 0),
                                teamLogo = teamCrest,
                                playerPhoto = "", // football-data does not serve player headshots; UI uses initial+number
                                penalties = item.optInt("penalties", 0),
                                playedMatches = item.optInt("playedMatches", 0)
                            )
                        )
                    }

                    DataResult.Success(list, source = SOURCE_NAME, cached = networkResult.cached)
                } catch (e: Exception) {
                    DataResult.Error(
                        errorType = ErrorType.MALFORMED_JSON,
                        message = "خطأ في تحليل استجابة الهدافين: ${e.message}",
                        source = SOURCE_NAME,
                        throwable = e
                    )
                }
            }
            is DataResult.Empty -> networkResult
            is DataResult.Error -> {
                if (networkResult.httpCode == 429) recordRateLimit(60)
                networkResult
            }
        }
    }

    /**
     * Resolve team ID from cached matches or standings
     */
    fun findTeamId(teamName: String): Int? {
        val norm = teamName.lowercase().trim()
        val direct = teamNameToIdCache[norm]
        if (direct != null) return direct

        for ((key, id) in teamNameToIdCache) {
            if (norm.contains(key) || key.contains(norm)) {
                return id
            }
        }
        return null
    }

    /**
     * Fetch Team squad and venue details from football-data.org
     */
    suspend fun fetchTeamSquad(teamId: Int): DataResult<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val apiKey = getValidApiKey() ?: return@withContext DataResult.Error(ErrorType.MISSING_OR_INVALID_KEY, "مفتاح API مفقود", SOURCE_NAME)
        if (!tryConsumeRateToken()) return@withContext DataResult.Error(ErrorType.RATE_LIMITED, "Rate limit", SOURCE_NAME)

        val url = "$BASE_URL/teams/$teamId"
        val request = Request.Builder().url(url).header("X-Auth-Token", apiKey).build()
        val cacheKey = "fdo_team_$teamId"

        when (val result = ProviderNetworkClient.executeSafe(request, SOURCE_NAME, cacheKey, TimeUnit.HOURS.toMillis(12))) {
            is DataResult.Success -> {
                try {
                    val root = JSONObject(result.data.first)
                    val squad = root.optJSONArray("squad") ?: return@withContext DataResult.Empty(SOURCE_NAME)
                    val list = mutableListOf<Pair<String, String>>()
                    for (i in 0 until squad.length()) {
                        val p = squad.getJSONObject(i)
                        list.add(p.optString("name") to p.optString("position", "MID"))
                    }
                    DataResult.Success(list, SOURCE_NAME, result.cached)
                } catch (e: Exception) {
                    DataResult.Error(ErrorType.MALFORMED_JSON, e.message ?: "", SOURCE_NAME)
                }
            }
            is DataResult.Empty -> result
            is DataResult.Error -> result
        }
    }
}
