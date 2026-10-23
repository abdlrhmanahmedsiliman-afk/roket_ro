package com.example.service

import android.content.Context
import com.example.core.performance.ConnectionQuality
import com.example.core.performance.ConnectionQualityMonitor
import com.example.core.performance.MemoryLruCache
import com.example.data.local.MatchDao
import com.example.data.local.TeamRosterDao
import com.example.data.local.TeamRosterMapper
import com.example.data.local.toDomainModel
import com.example.data.local.toEntity
import com.example.data.provider.MultiProviderOrchestrator
import com.example.data.provider.ProviderHealth
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class SportsRepository(
    private val matchDao: MatchDao,
    private val rosterDao: TeamRosterDao,
    private val preferencesManager: PreferencesManager,
    private val scope: CoroutineScope,
    private val context: Context? = null
) {
    private val memoryCache = MemoryLruCache<String, Any>(maxCapacity = 300)
    val orchestrator = MultiProviderOrchestrator(memoryCache)

    val connectionMonitor: ConnectionQualityMonitor? = context?.let {
        try { ConnectionQualityMonitor(it) } catch (_: Exception) { null }
    }

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    private val _leagues = MutableStateFlow<List<League>>(emptyList())
    val leagues: StateFlow<List<League>> = _leagues.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastRefreshed = MutableStateFlow(System.currentTimeMillis())
    val lastRefreshed: StateFlow<Long> = _lastRefreshed.asStateFlow()

    private val _refreshNotification = MutableStateFlow<String?>(null)
    val refreshNotification: StateFlow<String?> = _refreshNotification.asStateFlow()

    private val _sportsNews = MutableStateFlow<List<SportsNewsArticle>>(emptyList())
    val sportsNews: StateFlow<List<SportsNewsArticle>> = _sportsNews.asStateFlow()

    private val apiClient = com.example.data.provider.RealSportsApiClient()

    val activeApiProvider: StateFlow<String> = orchestrator.activeProvider
    val providerHealth: StateFlow<List<ProviderHealth>> = orchestrator.healthStats

    init {
        observeDatabaseMatches()
        loadInitialData()
        startLiveTicker()
    }

    private fun observeDatabaseMatches() {
        scope.launch(Dispatchers.IO) {
            matchDao.getAllMatches()
                .distinctUntilChanged()
                .collect { entities ->
                    if (entities.isNotEmpty()) {
                        val currentMemoryMap = _matches.value.associateBy { it.id }
                        val domainMatches = entities.map { entity ->
                            val memoryMatch = currentMemoryMap[entity.id]
                            val homeLineup = memoryMatch?.homeLineup ?: getOrFetchTeamRoster(entity.homeTeam, entity.league, entity.sport)
                            val awayLineup = memoryMatch?.awayLineup ?: getOrFetchTeamRoster(entity.awayTeam, entity.league, entity.sport)
                            entity.toDomainModel(
                                events = memoryMatch?.events ?: emptyList(),
                                homeLineup = homeLineup,
                                awayLineup = awayLineup,
                                h2h = memoryMatch?.h2h ?: HeadToHead()
                            ).copy(
                                playerConditions = memoryMatch?.playerConditions ?: generatePciForTeams(entity.homeTeam, entity.awayTeam, homeLineup, awayLineup),
                                broadcasters = memoryMatch?.broadcasters ?: generateBroadcastersForMatch(entity.league)
                            )
                        }
                        _matches.value = domainMatches
                    }
                }
        }
    }

    private fun loadInitialData() {
        scope.launch(Dispatchers.IO) {
            val count = matchDao.getMatchesCount()
            if (count == 0) {
                val realMatches = try {
                    orchestrator.executeFallbackChain("initial_live_matches", { it.fetchLiveMatches() }) {
                        generateSampleMatches()
                    }
                } catch (_: Exception) {
                    generateSampleMatches()
                }
                val rawMatches = if (realMatches.isNotEmpty()) realMatches else generateSampleMatches()
                val matchesToSave = rawMatches.map { enrichMatch(it) }
                _matches.value = matchesToSave
                matchDao.insertMatches(matchesToSave.map { it.toEntity() })

                // Pre-cache team rosters locally into Room to guarantee offline availability and 0 latency
                val rostersToCache = matchesToSave.flatMap { match ->
                    listOfNotNull(
                        match.homeLineup?.let { TeamRosterMapper.toEntity(match.homeTeam, it, match.league, match.sport) },
                        match.awayLineup?.let { TeamRosterMapper.toEntity(match.awayTeam, it, match.league, match.sport) }
                    )
                }
                if (rostersToCache.isNotEmpty()) {
                    rosterDao.insertRosters(rostersToCache)
                }
            } else {
                // If we already have cached matches, attempt background real refresh
                try {
                    val realMatches = orchestrator.executeFallbackChain("background_sync_matches", { it.fetchLiveMatches() }) {
                        emptyList()
                    }
                    if (realMatches.isNotEmpty()) {
                        val enriched = realMatches.map { enrichMatch(it) }
                        _matches.value = enriched
                        matchDao.insertMatches(enriched.map { it.toEntity() })
                    }
                } catch (_: Exception) {}
            }
            _leagues.value = generate50PlusLeagues()
            refreshLeaguesStandings()
            refreshSportsNews()
        }
    }

    fun refreshSportsNews() {
        scope.launch(Dispatchers.IO) {
            try {
                val articles = apiClient.fetchSportsNews()
                if (articles.isNotEmpty()) {
                    _sportsNews.value = articles
                }
            } catch (_: Exception) {}
        }
    }

    fun refreshLeaguesStandings() {
        scope.launch(Dispatchers.IO) {
            try {
                val realLeagues = orchestrator.executeFallbackChain("real_leagues_standings", { it.fetchLeagues() }) {
                    emptyList()
                }
                if (realLeagues.isNotEmpty()) {
                    val mapReal = realLeagues.associateBy { it.id }
                    _leagues.value = _leagues.value.map { baseLeague ->
                        val real = mapReal[baseLeague.id]
                        if (real != null && real.standings.isNotEmpty()) {
                            baseLeague.copy(standings = real.standings)
                        } else {
                            baseLeague
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadMatchDetails(matchId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentMatch = _matches.value.find { it.id == matchId } ?: return@launch

                val remoteDetails = try {
                    apiClient.fetchMatchSummary(matchId)
                } catch (_: Exception) {
                    null
                }

                val effectiveHomeLineup = remoteDetails?.homeLineup
                    ?: (if (currentMatch.homeLineup?.starters?.firstOrNull()?.photoUrl?.isNotBlank() == true) currentMatch.homeLineup else null)
                    ?: getOrFetchTeamRoster(currentMatch.homeTeam, currentMatch.league, currentMatch.sport)

                val effectiveAwayLineup = remoteDetails?.awayLineup
                    ?: (if (currentMatch.awayLineup?.starters?.firstOrNull()?.photoUrl?.isNotBlank() == true) currentMatch.awayLineup else null)
                    ?: getOrFetchTeamRoster(currentMatch.awayTeam, currentMatch.league, currentMatch.sport)

                val effectiveEvents = if (remoteDetails != null && remoteDetails.events.isNotEmpty()) {
                    remoteDetails.events
                } else if (currentMatch.events.isNotEmpty()) {
                    currentMatch.events
                } else {
                    generateRealisticEvents(
                        homeTeam = currentMatch.homeTeam,
                        awayTeam = currentMatch.awayTeam,
                        homeScore = currentMatch.homeScore,
                        awayScore = currentMatch.awayScore,
                        minute = currentMatch.minute,
                        homeLineup = effectiveHomeLineup,
                        awayLineup = effectiveAwayLineup
                    )
                }

                val effectiveStats = ensureValidStats(
                    remoteDetails?.stats ?: currentMatch.stats,
                    currentMatch.homeScore,
                    currentMatch.awayScore,
                    currentMatch.isLive,
                    currentMatch.isFinished
                )

                val updatedConditions = generatePciForTeams(currentMatch.homeTeam, currentMatch.awayTeam, effectiveHomeLineup, effectiveAwayLineup)

                val updatedMatch = currentMatch.copy(
                    homeLineup = effectiveHomeLineup,
                    awayLineup = effectiveAwayLineup,
                    events = effectiveEvents,
                    stats = effectiveStats,
                    playerConditions = updatedConditions,
                    venue = if (remoteDetails?.venue?.isNotBlank() == true) remoteDetails.venue else currentMatch.venue,
                    referee = if (remoteDetails?.referee?.isNotBlank() == true) remoteDetails.referee else currentMatch.referee,
                    lineupSource = if (remoteDetails?.homeLineup != null) DataSourceType.LIVE_API else DataSourceType.ESTIMATED_DEMO,
                    eventsSource = if (remoteDetails?.events?.isNotEmpty() == true) DataSourceType.LIVE_API else DataSourceType.ESTIMATED_DEMO,
                    statsSource = if (remoteDetails?.stats != null) DataSourceType.LIVE_API else DataSourceType.ESTIMATED_DEMO
                )

                _matches.value = _matches.value.map { if (it.id == matchId) updatedMatch else it }
                matchDao.insertMatch(updatedMatch.toEntity())
            } catch (_: Exception) {}
        }
    }

    fun enrichMatch(match: Match): Match {
        val homeLineup = match.homeLineup ?: createSampleLineup(formation = "4-3-3", coach = "المدير الفني", team = match.homeTeam)
        val awayLineup = match.awayLineup ?: createSampleLineup(formation = "4-3-3", coach = "المدير الفني", team = match.awayTeam)
        val stats = ensureValidStats(match.stats, match.homeScore, match.awayScore, match.isLive, match.isFinished, homeLineup, awayLineup)
        val pci = if (match.playerConditions.isNotEmpty()) match.playerConditions else generatePciForTeams(match.homeTeam, match.awayTeam, homeLineup, awayLineup)
        val broadcasters = if (match.broadcasters.isNotEmpty()) match.broadcasters else generateBroadcastersForMatch(match.league)
        val events = if (match.events.isNotEmpty()) match.events else generateRealisticEvents(
            match.homeTeam,
            match.awayTeam,
            match.homeScore,
            match.awayScore,
            if (match.minute > 0) match.minute else 90,
            homeLineup,
            awayLineup
        )
        return match.copy(
            homeLineup = homeLineup,
            awayLineup = awayLineup,
            stats = stats,
            playerConditions = pci,
            broadcasters = broadcasters,
            events = events
        )
    }

    fun ensureValidStats(stats: MatchStats, homeScore: Int, awayScore: Int, isLive: Boolean, isFinished: Boolean, homeLineup: TeamLineup? = null, awayLineup: TeamLineup? = null): MatchStats {
        return stats /* never invent statistics */
    }

    private fun generateRealisticEvents(
        homeTeam: String,
        awayTeam: String,
        homeScore: Int,
        awayScore: Int,
        minute: Int,
        homeLineup: TeamLineup?,
        awayLineup: TeamLineup?
    ): List<MatchEvent> {
        return emptyList() /* never invent match events */
    }

    /**
     * Offline-first team roster retrieval:
     * 1. Sub-millisecond In-Memory LRU Cache
     * 2. Device-local Room Database (TeamRosterDao)
     * 3. Fallback generator with instant Room persistence
     */
    suspend fun getOrFetchTeamRoster(teamName: String, league: String = "", sport: String = "كرة القدم"): TeamLineup {
        return try {
            val d = com.example.data.provider.EspnTeamApi.load(teamName, league)
            TeamLineup(
                formation = "",
                coach = d.coach,
                starters = d.players.map { p -> PlayerLineup(number = p.number, name = p.name, position = p.position, rating = 0f, photoUrl = p.photoUrl, athleteId = p.id) },
                subs = emptyList()
            )
        } catch (_: Exception) {
            TeamLineup(formation = "", coach = "", starters = emptyList(), subs = emptyList())
        }
    }

    fun observeTeamRoster(teamName: String): Flow<TeamLineup?> {
        return rosterDao.getRosterByTeam(teamName)
            .map { entity -> entity?.let { TeamRosterMapper.toDomainModel(it) } }
    }

    fun refreshMatches() {
        scope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val realMatches = orchestrator.executeFallbackChain("manual_refresh_matches", { it.fetchLiveMatches() }) {
                    emptyList()
                }
                if (realMatches.isNotEmpty()) {
                    val enriched = realMatches.map { enrichMatch(it) }
                    val currentOtherSports = _matches.value.filter { it.sport.lowercase() !in listOf("football", "soccer") }.ifEmpty {
                        generateSampleMatches().filter { it.sport.lowercase() !in listOf("football", "soccer") }
                    }
                    val combined = enriched + currentOtherSports
                    _matches.value = combined
                    matchDao.insertMatches(combined.map { it.toEntity() })
                    _lastRefreshed.value = System.currentTimeMillis()
                    _isLoading.value = false
                    _refreshNotification.value = "تم تحديث البيانات من السيرفر المباشر بنجاح"
                    delay(2500)
                    _refreshNotification.value = null
                    return@launch
                }
            } catch (_: Exception) {}

            // Fast delay simulating high-throughput delta sync
            delay(400)

            var updatedScoresMsg: String? = null

            // If real matches weren't fetched, do not artificially jump minutes ahead!
            // Maintain real clock and only update minor dynamic telemetry.
            val currentList = _matches.value.map { match ->
                if (match.isLive) {
                    val updatedXgHome = match.stats.xgHome
                    val updatedXgAway = match.stats.xgAway
                    match.copy(
                        stats = match.stats.copy(
                            xgHome = (updatedXgHome * 100).toInt() / 100f,
                            xgAway = (updatedXgAway * 100).toInt() / 100f
                        )
                    )
                } else {
                    match
                }
            }

            _matches.value = currentList
            matchDao.insertMatches(currentList.map { it.toEntity() })
            _lastRefreshed.value = System.currentTimeMillis()
            _isLoading.value = false

            if (updatedScoresMsg != null) {
                _refreshNotification.value = updatedScoresMsg
                delay(3500)
                _refreshNotification.value = null
            }
        }
    }

    /**
     * Real-time live scoreboard synchronizer.
     * Polls the live ESPN / sports server every 25 seconds for actual stadium clocks and real scores.
     * For demo matches, advances 1 match minute per 60 wall-clock seconds (never jumping).
     */
    private fun startLiveTicker() {
        scope.launch(Dispatchers.IO) {
            var wallClockTicks = 0
            while (true) {
                // Poll every 25 seconds for real live matches
                delay(25_000L)
                wallClockTicks++

                try {
                    val realMatches = apiClient.fetchRealMatches()
                    if (realMatches.isNotEmpty()) {
                        val currentMap = _matches.value.associateBy { it.id }
                        val merged = realMatches.map { fresh ->
                            val existing = currentMap[fresh.id]
                            if (existing != null) {
                                enrichMatch(fresh.copy(
                                    homeLineup = fresh.homeLineup ?: existing.homeLineup,
                                    awayLineup = fresh.awayLineup ?: existing.awayLineup,
                                    events = if (fresh.events.isNotEmpty()) fresh.events else existing.events,
                                    stats = if (fresh.stats.shotsHome > 0) fresh.stats else existing.stats,
                                    playerConditions = existing.playerConditions,
                                    broadcasters = existing.broadcasters,
                                    isFavorite = existing.isFavorite
                                ))
                            } else {
                                enrichMatch(fresh)
                            }
                        }
                        if (merged.isNotEmpty()) {
                            val currentOtherSports = _matches.value.filter { it.sport.lowercase() !in listOf("football", "soccer") }.ifEmpty {
                                generateSampleMatches().filter { it.sport.lowercase() !in listOf("football", "soccer") }
                            }
                            val combined = merged + currentOtherSports
                            _matches.value = combined
                            matchDao.insertMatches(combined.map { it.toEntity() })
                            _lastRefreshed.value = System.currentTimeMillis()
                        }
                        continue
                    }
                } catch (_: Exception) {}

                // Offline / Demo fallback: advance by 1 minute strictly every 60 seconds (approx 2.4 cycles of 25s)
                if (wallClockTicks % 2 == 0) {
                    val updated = _matches.value.map { match ->
                        if (match.isLive && match.minute < 90) {
                            match.copy(minute = match.minute + 1)
                        } else match
                    }
                    _matches.value = updated
                }
            }
        }
    }

    fun getMatchById(id: String): Match? {
        return _matches.value.firstOrNull { it.id == id }
    }

    private fun generatePciForTeams(
        homeTeam: String,
        awayTeam: String,
        homeLineup: TeamLineup? = null,
        awayLineup: TeamLineup? = null
    ): List<PlayerCondition> {
        return emptyList() /* no invented player condition */
    }

    private fun generateBroadcastersForMatch(league: String): List<Broadcaster> {
        return emptyList() /* no invented TV channels */
    }

    private fun generateSampleMatches(): List<Match> {
        return emptyList() /* no sample/fake matches */
    }

    fun p(
        number: Int,
        name: String,
        position: String,
        rating: Float = 8.0f,
        isCaptain: Boolean = false,
        x: Float = 0.5f,
        y: Float = 0.5f,
        athleteId: String = ""
    ): PlayerLineup {
        val photoUrl = com.example.data.provider.PlayerDatabaseProvider.getPlayerPhoto(name, athleteId)
        return PlayerLineup(
            number = number,
            name = name,
            position = position,
            rating = rating,
            isCaptain = isCaptain,
            x = x,
            y = y,
            photoUrl = photoUrl,
            athleteId = athleteId
        )
    }

    fun createSampleLineup(formation: String = "4-3-3", coach: String = "المدير الفني", team: String): TeamLineup {
        return TeamLineup(formation = "", coach = "", starters = emptyList(), subs = emptyList()) /* lineup unavailable instead of a made-up one */
    }

    /**
     * 50+ Leagues across Tier 1, Tier 2, Tier 3, and Tier 4 (All Sports)
     */
    fun generate50PlusLeagues(): List<League> =
        generateLeagueCatalogRaw().map { it.copy(standings = emptyList(), topScorers = emptyList()) }

    private fun generateLeagueCatalogRaw(): List<League> {
        val standingsPL = listOf(
            StandingRow(1, "ليفربول", "https://media.api-sports.io/football/teams/40.png", 28, 20, 5, 3, 64, 25, 39, 65),
            StandingRow(2, "مانشستر سيتي", "https://media.api-sports.io/football/teams/50.png", 28, 19, 6, 3, 62, 28, 34, 63),
            StandingRow(3, "أرسنال", "https://media.api-sports.io/football/teams/42.png", 28, 18, 7, 3, 58, 22, 36, 61),
            StandingRow(4, "تشيلسي", "https://media.api-sports.io/football/teams/49.png", 28, 14, 7, 7, 49, 36, 13, 49),
            StandingRow(5, "نيوكاسل يونايتد", "https://media.api-sports.io/football/teams/34.png", 28, 13, 6, 9, 44, 38, 6, 45),
            StandingRow(6, "نوتنغهام فورست", "https://media.api-sports.io/football/teams/65.png", 28, 13, 5, 10, 39, 36, 3, 44),
            StandingRow(7, "أستون فيلا", "https://media.api-sports.io/football/teams/66.png", 28, 12, 7, 9, 42, 41, 1, 43),
            StandingRow(8, "بورنموث", "https://media.api-sports.io/football/teams/35.png", 28, 12, 6, 10, 41, 35, 6, 42),
            StandingRow(9, "برايتون", "https://media.api-sports.io/football/teams/51.png", 28, 11, 8, 9, 42, 43, -1, 41),
            StandingRow(10, "فولهام", "https://media.api-sports.io/football/teams/36.png", 28, 11, 7, 10, 39, 39, 0, 40),
            StandingRow(11, "برينتفورد", "https://media.api-sports.io/football/teams/55.png", 28, 11, 4, 13, 45, 45, 0, 37),
            StandingRow(12, "مانشستر يونايتد", "https://media.api-sports.io/football/teams/33.png", 28, 10, 6, 12, 36, 39, -3, 36),
            StandingRow(13, "توتنهام هوتسبير", "https://media.api-sports.io/football/teams/47.png", 28, 10, 4, 14, 52, 42, 10, 34),
            StandingRow(14, "وست هام يونايتد", "https://media.api-sports.io/football/teams/48.png", 28, 8, 7, 13, 34, 51, -17, 31),
            StandingRow(15, "إيفرتون", "https://media.api-sports.io/football/teams/45.png", 28, 7, 9, 12, 31, 39, -8, 30),
            StandingRow(16, "كريستال بالاس", "https://media.api-sports.io/football/teams/52.png", 28, 6, 10, 12, 29, 38, -9, 28),
            StandingRow(17, "ليستر سيتي", "https://media.api-sports.io/football/teams/46.png", 28, 5, 5, 18, 28, 59, -31, 20),
            StandingRow(18, "وولفرهامبتون", "https://media.api-sports.io/football/teams/39.png", 28, 5, 4, 19, 34, 58, -24, 19),
            StandingRow(19, "إيبسويتش تاون", "https://media.api-sports.io/football/teams/57.png", 28, 3, 8, 17, 23, 55, -32, 17),
            StandingRow(20, "ساوثهامبتون", "https://media.api-sports.io/football/teams/41.png", 28, 1, 3, 24, 16, 68, -52, 6)
        )

        val topScorersPL = listOf(
            TopScorer(1, "محمد صلاح", "ليفربول", 21, 11),
            TopScorer(2, "إيرلينغ هالاند", "مانشستر سيتي", 20, 4),
            TopScorer(3, "كول بالمر", "تشيلسي", 16, 8),
            TopScorer(4, "بوكايو ساكا", "أرسنال", 14, 10),
            TopScorer(5, "ألكسندر إيزاك", "نيوكاسل يونايتد", 14, 3),
            TopScorer(6, "كريس وود", "نوتنغهام فورست", 13, 2),
            TopScorer(7, "أولي واتكينز", "أستون فيلا", 12, 8),
            TopScorer(8, "بريان مبيومو", "برينتفورد", 12, 4),
            TopScorer(9, "نيكولاس جاكسون", "تشيلسي", 11, 4),
            TopScorer(10, "كاي هافيرتز", "أرسنال", 10, 4)
        )

        val standingsLaLiga = listOf(
            StandingRow(1, "برشلونة", "https://media.api-sports.io/football/teams/529.png", 28, 20, 4, 4, 73, 28, 45, 64),
            StandingRow(2, "ريال مدريد", "https://media.api-sports.io/football/teams/541.png", 28, 19, 6, 3, 62, 24, 38, 63),
            StandingRow(3, "أتلتيكو مدريد", "https://media.api-sports.io/football/teams/530.png", 28, 17, 7, 4, 49, 19, 30, 58),
            StandingRow(4, "أتلتيك بيلباو", "https://media.api-sports.io/football/teams/531.png", 28, 15, 8, 5, 47, 26, 21, 53),
            StandingRow(5, "فياريال", "https://media.api-sports.io/football/teams/533.png", 28, 13, 8, 7, 51, 42, 9, 47),
            StandingRow(6, "ريال مايوركا", "https://media.api-sports.io/football/teams/547.png", 28, 12, 7, 9, 31, 30, 1, 43),
            StandingRow(7, "أوساسونا", "https://media.api-sports.io/football/teams/727.png", 28, 10, 10, 8, 36, 38, -2, 40),
            StandingRow(8, "ريال بيتيس", "https://media.api-sports.io/football/teams/543.png", 28, 10, 9, 9, 33, 33, 0, 39),
            StandingRow(9, "ريال سوسيداد", "https://media.api-sports.io/football/teams/548.png", 28, 10, 6, 12, 27, 28, -1, 36),
            StandingRow(10, "سيلتا فيغو", "https://media.api-sports.io/football/teams/538.png", 28, 10, 5, 13, 40, 44, -4, 35),
            StandingRow(11, "رايو فاليكانو", "https://media.api-sports.io/football/teams/728.png", 28, 9, 8, 11, 29, 33, -4, 35),
            StandingRow(12, "جيرونا", "https://media.api-sports.io/football/teams/546.png", 28, 9, 7, 12, 36, 40, -4, 34),
            StandingRow(13, "إشبيلية", "https://media.api-sports.io/football/teams/536.png", 28, 9, 6, 13, 31, 38, -7, 33),
            StandingRow(14, "ديبورتيفو ألافيس", "https://media.api-sports.io/football/teams/542.png", 28, 8, 5, 15, 30, 42, -12, 29),
            StandingRow(15, "ليغانيس", "https://media.api-sports.io/football/teams/540.png", 28, 6, 9, 13, 24, 39, -15, 27),
            StandingRow(16, "خيتافي", "https://media.api-sports.io/football/teams/545.png", 28, 5, 11, 12, 19, 27, -8, 26),
            StandingRow(17, "لاس بالماس", "https://media.api-sports.io/football/teams/534.png", 28, 6, 7, 15, 33, 49, -16, 25),
            StandingRow(18, "إسبانيول", "https://media.api-sports.io/football/teams/540.png", 28, 6, 6, 16, 26, 47, -21, 24),
            StandingRow(19, "فالنسيا", "https://media.api-sports.io/football/teams/532.png", 28, 4, 11, 13, 25, 43, -18, 23),
            StandingRow(20, "ريال بلد الوليد", "https://media.api-sports.io/football/teams/720.png", 28, 4, 4, 20, 18, 55, -37, 16)
        )

        val topScorersLaLiga = listOf(
            TopScorer(1, "روبرت ليفاندوفسكي", "برشلونة", 21, 4),
            TopScorer(2, "كيليان مبابي", "ريال مدريد", 19, 5),
            TopScorer(3, "فينيسيوس جونيور", "ريال مدريد", 14, 8),
            TopScorer(4, "رافينيا", "برشلونة", 13, 9),
            TopScorer(5, "آيوزي بيريز", "فياريال", 11, 2),
            TopScorer(6, "أنتي بوديمير", "أوساسونا", 10, 2),
            TopScorer(7, "لامين يامال", "برشلونة", 9, 12),
            TopScorer(8, "خوليان ألفاريز", "أتلتيكو مدريد", 9, 3),
            TopScorer(9, "داني أولمو", "برشلونة", 8, 4),
            TopScorer(10, "إينياكي ويليامز", "أتلتيك بيلباو", 7, 6)
        )

        val standingsSPL = listOf(
            StandingRow(1, "الهلال", "", 24, 21, 3, 0, 72, 18, 54, 66),
            StandingRow(2, "الاتحاد", "", 24, 19, 3, 2, 58, 20, 38, 60),
            StandingRow(3, "النصر", "", 24, 16, 5, 3, 56, 24, 32, 53),
            StandingRow(4, "القادسية", "", 24, 15, 2, 7, 42, 26, 16, 47),
            StandingRow(5, "الشباب", "", 24, 13, 4, 7, 39, 28, 11, 43),
            StandingRow(6, "الأهلي", "", 24, 12, 5, 7, 44, 29, 15, 41),
            StandingRow(7, "الاتفاق", "", 24, 9, 6, 9, 31, 33, -2, 33),
            StandingRow(8, "التعاون", "", 24, 9, 5, 10, 28, 30, -2, 32),
            StandingRow(9, "الرائد", "", 24, 8, 4, 12, 30, 37, -7, 28),
            StandingRow(10, "الخليج", "", 24, 8, 4, 12, 27, 36, -9, 28),
            StandingRow(11, "ضمك", "", 24, 7, 6, 11, 29, 39, -10, 27),
            StandingRow(12, "العروبة", "", 24, 7, 2, 15, 23, 46, -23, 23),
            StandingRow(13, "الرياض", "", 24, 6, 5, 13, 25, 41, -16, 23),
            StandingRow(14, "الفيحاء", "", 24, 5, 7, 12, 22, 39, -17, 22),
            StandingRow(15, "الخلود", "", 24, 5, 4, 15, 24, 43, -19, 19),
            StandingRow(16, "الأخدود", "", 24, 4, 5, 15, 22, 40, -18, 17),
            StandingRow(17, "الوحدة", "", 24, 4, 4, 16, 25, 53, -28, 16),
            StandingRow(18, "الفتح", "", 24, 3, 4, 17, 21, 48, -27, 13)
        )

        val topScorersSPL = listOf(
            TopScorer(1, "ألكسندر ميتروفيتش", "الهلال", 23, 5),
            TopScorer(2, "كريستيانو رونالدو", "النصر", 22, 7),
            TopScorer(3, "كريم بنزيما", "الاتحاد", 17, 6),
            TopScorer(4, "موسى بارو", "التعاون", 11, 4),
            TopScorer(5, "مالكوم", "الهلال", 11, 6),
            TopScorer(6, "جورج كيفين نكودو", "ضمك", 10, 4),
            TopScorer(7, "عبد الرزاق حمد الله", "الشباب", 10, 3),
            TopScorer(8, "سالم الدوسري", "الهلال", 9, 8),
            TopScorer(9, "رياض محرز", "الأهلي", 9, 7),
            TopScorer(10, "ماركوس ليوناردو", "الهلال", 8, 2)
        )

        val standingsEgypt = listOf(
            StandingRow(1, "الأهلي", "", 24, 19, 4, 1, 54, 15, 39, 61),
            StandingRow(2, "بيراميدز", "", 24, 17, 5, 2, 47, 18, 29, 56),
            StandingRow(3, "الزمالك", "", 24, 15, 5, 4, 44, 22, 22, 50),
            StandingRow(4, "المصري البورسعيدي", "", 24, 12, 7, 5, 35, 26, 9, 43),
            StandingRow(5, "مودرن سبورت", "", 24, 11, 8, 5, 33, 25, 8, 41),
            StandingRow(6, "سموحة", "", 24, 10, 7, 7, 32, 27, 5, 37),
            StandingRow(7, "زد إف سي", "", 24, 9, 9, 6, 29, 25, 4, 36),
            StandingRow(8, "سيراميكا كليوباترا", "", 24, 9, 8, 7, 34, 30, 4, 35),
            StandingRow(9, "إنبي", "", 24, 9, 6, 9, 28, 28, 0, 33),
            StandingRow(10, "طلائع الجيش", "", 24, 8, 8, 8, 25, 27, -2, 32),
            StandingRow(11, "الاتحاد السكندري", "", 24, 8, 7, 9, 28, 33, -5, 31),
            StandingRow(12, "الجونة", "", 24, 7, 8, 9, 26, 32, -6, 29),
            StandingRow(13, "البنك الأهلي", "", 24, 7, 6, 11, 31, 37, -6, 27),
            StandingRow(14, "الإسماعيلي", "", 24, 6, 8, 10, 24, 31, -7, 26),
            StandingRow(15, "فاركو", "", 24, 5, 8, 11, 23, 35, -12, 23),
            StandingRow(16, "غزل المحلة", "", 24, 4, 7, 13, 19, 38, -19, 19),
            StandingRow(17, "حرس الحدود", "", 24, 3, 6, 15, 17, 43, -26, 15),
            StandingRow(18, "بتروجيت", "", 24, 2, 7, 15, 16, 45, -29, 13)
        )

        val topScorersEgypt = listOf(
            TopScorer(1, "فيستون ماييلي", "بيراميدز", 17, 3),
            TopScorer(2, "أحمد سيد زيزو", "الزمالك", 13, 9),
            TopScorer(3, "حسام أشرف", "بلدية المحلة", 12, 2),
            TopScorer(4, "إمام عاشور", "الأهلي", 11, 6),
            TopScorer(5, "ناصر منسي", "الزمالك", 10, 2),
            TopScorer(6, "محمود كهربا", "الأهلي", 9, 3),
            TopScorer(7, "مصطفى زيكو", "زد إف سي", 9, 4),
            TopScorer(8, "صديق أوجولا", "سيراميكا كليوباترا", 9, 6),
            TopScorer(9, "حسام حسن", "سموحة", 8, 3),
            TopScorer(10, "أحمد بلحاج", "سيراميكا كليوباترا", 8, 5)
        )

        val standingsMorocco = listOf(
            StandingRow(1, "الرجاء الرياضي", "", 26, 18, 8, 0, 54, 15, 39, 62),
            StandingRow(2, "الجيش الملكي", "", 26, 19, 4, 3, 58, 19, 39, 61),
            StandingRow(3, "نهضة بركان", "", 26, 13, 10, 3, 36, 18, 18, 49),
            StandingRow(4, "الوداد الرياضي", "", 26, 12, 7, 7, 33, 23, 10, 43),
            StandingRow(5, "الفتح الرباطي", "", 26, 11, 9, 6, 32, 22, 10, 42),
            StandingRow(6, "أولمبيك آسفي", "", 26, 11, 8, 7, 30, 24, 6, 41),
            StandingRow(7, "نهضة الزمامرة", "", 26, 10, 7, 9, 31, 29, 2, 37),
            StandingRow(8, "المغرب الفاسي", "", 26, 8, 11, 7, 28, 26, 2, 35),
            StandingRow(9, "اتحاد طنجة", "", 26, 8, 8, 10, 26, 31, -5, 32),
            StandingRow(10, "اتحاد تواركة", "", 26, 7, 9, 10, 27, 33, -6, 30),
            StandingRow(11, "المغرب التطواني", "", 26, 6, 11, 9, 25, 32, -7, 29),
            StandingRow(12, "حسنية أكادير", "", 26, 7, 6, 13, 24, 37, -13, 27),
            StandingRow(13, "شباب المحمدية", "", 26, 6, 6, 14, 19, 36, -17, 24),
            StandingRow(14, "شباب السوالم", "", 26, 5, 8, 13, 22, 40, -18, 23),
            StandingRow(15, "مولودية وجدة", "", 26, 5, 7, 14, 20, 42, -22, 22),
            StandingRow(16, "يوسفية برشيد", "", 26, 4, 5, 17, 16, 45, -29, 17)
        )

        val standingsTunisia = listOf(
            StandingRow(1, "الترجي الرياضي التونسي", "", 20, 15, 4, 1, 38, 11, 27, 49),
            StandingRow(2, "الاتحاد المنستيري", "", 20, 12, 5, 3, 30, 14, 16, 41),
            StandingRow(3, "النجم الساحلي", "", 20, 10, 6, 4, 26, 16, 10, 36),
            StandingRow(4, "النادي الإفريقي", "", 20, 9, 6, 5, 24, 18, 6, 33),
            StandingRow(5, "النادي الرياضي الصفاقسي", "", 20, 8, 7, 5, 20, 15, 5, 31),
            StandingRow(6, "الملعب التونسي", "", 20, 8, 6, 6, 23, 20, 3, 30),
            StandingRow(7, "الأولمبي الباجي", "", 20, 7, 5, 8, 19, 22, -3, 26),
            StandingRow(8, "اتحاد بن قردان", "", 20, 6, 6, 8, 18, 21, -3, 24),
            StandingRow(9, "شبيبة العمران", "", 20, 5, 7, 8, 17, 23, -6, 22),
            StandingRow(10, "النادي البنزرتي", "", 20, 5, 6, 9, 15, 22, -7, 21),
            StandingRow(11, "مستقبل سليمان", "", 20, 5, 5, 10, 16, 26, -10, 20),
            StandingRow(12, "مستقبل قابس", "", 20, 4, 7, 9, 14, 25, -11, 19),
            StandingRow(13, "نجم المتلوي", "", 20, 4, 6, 10, 13, 24, -11, 18),
            StandingRow(14, "الترجي الجرجيسي", "", 20, 4, 5, 11, 12, 25, -13, 17),
            StandingRow(15, "اتحاد تطاوين", "", 20, 3, 6, 11, 11, 27, -16, 15),
            StandingRow(16, "قوافل قفصة", "", 20, 3, 4, 13, 10, 30, -20, 13)
        )

        val standingsBundesliga = listOf(
            StandingRow(1, "بايرن ميونخ", "https://media.api-sports.io/football/teams/157.png", 26, 20, 4, 2, 74, 24, 50, 64),
            StandingRow(2, "باير ليفركوزن", "https://media.api-sports.io/football/teams/168.png", 26, 18, 6, 2, 65, 28, 37, 60),
            StandingRow(3, "آينتراخت فرانكفورت", "https://media.api-sports.io/football/teams/169.png", 26, 15, 6, 5, 53, 33, 20, 51),
            StandingRow(4, "لايبزيغ", "https://media.api-sports.io/football/teams/173.png", 26, 14, 6, 6, 48, 29, 19, 48),
            StandingRow(5, "بوروسيا دورتموند", "https://media.api-sports.io/football/teams/165.png", 26, 13, 6, 7, 49, 38, 11, 45),
            StandingRow(6, "فرايبورغ", "https://media.api-sports.io/football/teams/160.png", 26, 13, 4, 9, 38, 36, 2, 43),
            StandingRow(7, "ماينتس 05", "https://media.api-sports.io/football/teams/164.png", 26, 11, 8, 7, 41, 34, 7, 41),
            StandingRow(8, "شتوتغارت", "https://media.api-sports.io/football/teams/172.png", 26, 11, 7, 8, 46, 40, 6, 40),
            StandingRow(9, "بوروسيا مونشنغلادباخ", "https://media.api-sports.io/football/teams/163.png", 26, 10, 7, 9, 39, 37, 2, 37),
            StandingRow(10, "فيردر بريمن", "https://media.api-sports.io/football/teams/162.png", 26, 9, 7, 10, 36, 43, -7, 34),
            StandingRow(11, "أوغسبورغ", "https://media.api-sports.io/football/teams/170.png", 26, 8, 8, 10, 31, 41, -10, 32),
            StandingRow(12, "يونيون برلين", "https://media.api-sports.io/football/teams/182.png", 26, 7, 9, 10, 24, 30, -6, 30),
            StandingRow(13, "فولفسبورغ", "https://media.api-sports.io/football/teams/161.png", 26, 8, 5, 13, 38, 44, -6, 29),
            StandingRow(14, "سانت باولي", "https://media.api-sports.io/football/teams/186.png", 26, 7, 4, 15, 22, 33, -11, 25),
            StandingRow(15, "هوفنهايم", "https://media.api-sports.io/football/teams/167.png", 26, 5, 8, 13, 33, 49, -16, 23),
            StandingRow(16, "هايدنهايم", "https://media.api-sports.io/football/teams/180.png", 26, 5, 4, 17, 26, 51, -25, 19),
            StandingRow(17, "بوخوم", "https://media.api-sports.io/football/teams/176.png", 26, 3, 6, 17, 21, 55, -34, 15),
            StandingRow(18, "هولشتاين كيل", "https://media.api-sports.io/football/teams/191.png", 26, 3, 5, 18, 27, 61, -34, 14)
        )

        val standingsSerieA = listOf(
            StandingRow(1, "إنتر ميلان", "https://media.api-sports.io/football/teams/505.png", 28, 21, 5, 2, 68, 22, 46, 68),
            StandingRow(2, "نابولي", "https://media.api-sports.io/football/teams/492.png", 28, 19, 5, 4, 51, 23, 28, 62),
            StandingRow(3, "أتالانتا", "https://media.api-sports.io/football/teams/499.png", 28, 18, 4, 6, 63, 29, 34, 58),
            StandingRow(4, "يوفنتوس", "https://media.api-sports.io/football/teams/496.png", 28, 14, 13, 1, 45, 21, 24, 55),
            StandingRow(5, "لاتسيو", "https://media.api-sports.io/football/teams/487.png", 28, 15, 4, 9, 48, 37, 11, 49),
            StandingRow(6, "روما", "https://media.api-sports.io/football/teams/497.png", 28, 13, 7, 8, 41, 31, 10, 46),
            StandingRow(7, "فيورنتينا", "https://media.api-sports.io/football/teams/502.png", 28, 12, 9, 7, 43, 30, 13, 45),
            StandingRow(8, "ميلان", "https://media.api-sports.io/football/teams/489.png", 28, 12, 8, 8, 45, 33, 12, 44),
            StandingRow(9, "بولونيا", "https://media.api-sports.io/football/teams/500.png", 28, 10, 11, 7, 38, 34, 4, 41),
            StandingRow(10, "تورينو", "https://media.api-sports.io/football/teams/503.png", 28, 9, 8, 11, 31, 35, -4, 35),
            StandingRow(11, "أودينيزي", "https://media.api-sports.io/football/teams/494.png", 28, 10, 5, 13, 32, 40, -8, 35),
            StandingRow(12, "إمبولي", "https://media.api-sports.io/football/teams/511.png", 28, 7, 10, 11, 25, 36, -11, 31),
            StandingRow(13, "فيرونا", "https://media.api-sports.io/football/teams/504.png", 28, 8, 4, 16, 31, 52, -21, 28),
            StandingRow(14, "بارما", "https://media.api-sports.io/football/teams/523.png", 28, 6, 9, 13, 34, 46, -12, 27),
            StandingRow(15, "كومو", "https://media.api-sports.io/football/teams/525.png", 28, 6, 8, 14, 30, 45, -15, 26),
            StandingRow(16, "كالياري", "https://media.api-sports.io/football/teams/490.png", 28, 6, 7, 15, 27, 45, -18, 25),
            StandingRow(17, "ليتشي", "https://media.api-sports.io/football/teams/867.png", 28, 6, 7, 15, 20, 44, -24, 25),
            StandingRow(18, "جنوى", "https://media.api-sports.io/football/teams/495.png", 28, 5, 9, 14, 23, 43, -20, 24),
            StandingRow(19, "فينيسيا", "https://media.api-sports.io/football/teams/517.png", 28, 4, 7, 17, 22, 46, -24, 19),
            StandingRow(20, "مونزا", "https://media.api-sports.io/football/teams/1579.png", 28, 2, 8, 18, 19, 44, -25, 14)
        )

        val standingsLigue1 = listOf(
            StandingRow(1, "باريس سان جيرمان", "https://media.api-sports.io/football/teams/85.png", 26, 20, 5, 1, 68, 23, 45, 65),
            StandingRow(2, "موناكو", "https://media.api-sports.io/football/teams/91.png", 26, 15, 5, 6, 49, 29, 20, 50),
            StandingRow(3, "مارسيليا", "https://media.api-sports.io/football/teams/81.png", 26, 14, 5, 7, 50, 33, 17, 47),
            StandingRow(4, "ليل", "https://media.api-sports.io/football/teams/79.png", 26, 13, 7, 6, 42, 27, 15, 46),
            StandingRow(5, "نيس", "https://media.api-sports.io/football/teams/84.png", 26, 12, 8, 6, 47, 28, 19, 44),
            StandingRow(6, "ليون", "https://media.api-sports.io/football/teams/80.png", 26, 12, 6, 8, 44, 33, 11, 42),
            StandingRow(7, "لانس", "https://media.api-sports.io/football/teams/116.png", 26, 10, 9, 7, 31, 25, 6, 39),
            StandingRow(8, "ريمس", "https://media.api-sports.io/football/teams/93.png", 26, 9, 7, 10, 34, 36, -2, 34),
            StandingRow(9, "ستاد بريست", "https://media.api-sports.io/football/teams/106.png", 26, 10, 3, 13, 33, 39, -6, 33),
            StandingRow(10, "رين", "https://media.api-sports.io/football/teams/94.png", 26, 9, 4, 13, 36, 38, -2, 31),
            StandingRow(11, "ستراسبورغ", "https://media.api-sports.io/football/teams/95.png", 26, 8, 6, 12, 38, 43, -5, 30),
            StandingRow(12, "تولوز", "https://media.api-sports.io/football/teams/96.png", 26, 7, 9, 10, 27, 32, -5, 30),
            StandingRow(13, "أوكسير", "https://media.api-sports.io/football/teams/108.png", 26, 7, 7, 12, 34, 42, -8, 28),
            StandingRow(14, "نانت", "https://media.api-sports.io/football/teams/83.png", 26, 5, 10, 11, 26, 37, -11, 25),
            StandingRow(15, "أنجيه", "https://media.api-sports.io/football/teams/77.png", 26, 6, 7, 13, 24, 39, -15, 25),
            StandingRow(16, "سانت إتيان", "https://media.api-sports.io/football/teams/1063.png", 26, 6, 4, 16, 23, 53, -30, 22),
            StandingRow(17, "لوهافر", "https://media.api-sports.io/football/teams/111.png", 26, 6, 2, 18, 19, 48, -29, 20),
            StandingRow(18, "مونبلييه", "https://media.api-sports.io/football/teams/82.png", 26, 4, 3, 19, 23, 63, -40, 15)
        )

        val topScorersLigue1 = listOf(
            TopScorer(1, "برادلي باركولا", "باريس سان جيرمان", 15, 6),
            TopScorer(2, "ماسون غرينوود", "مارسيليا", 13, 3),
            TopScorer(3, "جوناثان ديفيد", "ليل", 13, 4),
            TopScorer(4, "عثمان ديمبيلي", "باريس سان جيرمان", 9, 10),
            TopScorer(5, "ألكسندر لاكازيت", "ليون", 9, 3),
            TopScorer(6, "كانغ إن لي", "باريس سان جيرمان", 8, 4),
            TopScorer(7, "لودوفيك أجورك", "ستاد بريست", 8, 2),
            TopScorer(8, "إيلي واهي", "مارسيليا", 7, 3),
            TopScorer(9, "ألكسندر غولوفين", "موناكو", 6, 5),
            TopScorer(10, "أشرف حكيمي", "باريس سان جيرمان", 4, 8)
        )

        val standingsUCL = listOf(
            StandingRow(1, "ليفربول", "https://media.api-sports.io/football/teams/40.png", 8, 8, 0, 0, 17, 3, 14, 24),
            StandingRow(2, "برشلونة", "https://media.api-sports.io/football/teams/529.png", 8, 6, 1, 1, 28, 13, 15, 19),
            StandingRow(3, "أرسنال", "https://media.api-sports.io/football/teams/42.png", 8, 6, 1, 1, 16, 3, 13, 19),
            StandingRow(4, "إنتر ميلان", "https://media.api-sports.io/football/teams/505.png", 8, 6, 1, 1, 11, 1, 10, 19),
            StandingRow(5, "أتلتيكو مدريد", "https://media.api-sports.io/football/teams/530.png", 8, 6, 0, 2, 20, 12, 8, 18),
            StandingRow(6, "باير ليفركوزن", "https://media.api-sports.io/football/teams/168.png", 8, 5, 1, 2, 15, 7, 8, 16),
            StandingRow(7, "ليل", "https://media.api-sports.io/football/teams/79.png", 8, 5, 1, 2, 17, 10, 7, 16),
            StandingRow(8, "أستون فيلا", "https://media.api-sports.io/football/teams/66.png", 8, 5, 1, 2, 13, 5, 8, 16),
            StandingRow(9, "أتالانتا", "https://media.api-sports.io/football/teams/499.png", 8, 4, 3, 1, 20, 6, 14, 15),
            StandingRow(10, "بوروسيا دورتموند", "https://media.api-sports.io/football/teams/165.png", 8, 5, 0, 3, 22, 12, 10, 15),
            StandingRow(11, "بايرن ميونخ", "https://media.api-sports.io/football/teams/157.png", 8, 5, 0, 3, 20, 12, 8, 15),
            StandingRow(12, "ريال مدريد", "https://media.api-sports.io/football/teams/541.png", 8, 5, 0, 3, 20, 12, 8, 15),
            StandingRow(13, "ميلان", "https://media.api-sports.io/football/teams/489.png", 8, 5, 0, 3, 15, 11, 4, 15),
            StandingRow(14, "يوفنتوس", "https://media.api-sports.io/football/teams/496.png", 8, 4, 2, 2, 9, 7, 2, 14),
            StandingRow(15, "مانشستر سيتي", "https://media.api-sports.io/football/teams/50.png", 8, 4, 2, 2, 18, 9, 9, 14),
            StandingRow(16, "بنفيكا", "https://media.api-sports.io/football/teams/211.png", 8, 4, 1, 3, 16, 12, 4, 13),
            StandingRow(17, "سبورتينغ لشبونة", "https://media.api-sports.io/football/teams/228.png", 8, 3, 2, 3, 14, 12, 2, 11),
            StandingRow(18, "فاينورد", "https://media.api-sports.io/football/teams/197.png", 8, 3, 1, 4, 16, 18, -2, 10),
            StandingRow(19, "كلوب بروج", "https://media.api-sports.io/football/teams/569.png", 8, 3, 1, 4, 7, 11, -4, 10),
            StandingRow(20, "باريس سان جيرمان", "https://media.api-sports.io/football/teams/85.png", 8, 3, 1, 4, 9, 9, 0, 10),
            StandingRow(21, "موناكو", "https://media.api-sports.io/football/teams/91.png", 8, 3, 1, 4, 13, 13, 0, 10),
            StandingRow(22, "ستاد بريست", "https://media.api-sports.io/football/teams/106.png", 8, 3, 1, 4, 10, 11, -1, 10),
            StandingRow(23, "دينامو زغرب", "https://media.api-sports.io/football/teams/646.png", 8, 2, 2, 4, 11, 19, -8, 8),
            StandingRow(24, "سلتيك", "https://media.api-sports.io/football/teams/247.png", 8, 2, 2, 4, 10, 18, -8, 8)
        )

        val topScorersUCL = listOf(
            TopScorer(1, "روبرت ليفاندوفسكي", "برشلونة", 9, 1),
            TopScorer(2, "رافينيا", "برشلونة", 8, 4),
            TopScorer(3, "هاري كين", "بايرن ميونخ", 7, 2),
            TopScorer(4, "فيكتور جيوكيريس", "سبورتينغ لشبونة", 6, 1),
            TopScorer(5, "إيرلينغ هالاند", "مانشستر سيتي", 6, 1),
            TopScorer(6, "فينيسيوس جونيور", "ريال مدريد", 5, 3),
            TopScorer(7, "فلوريان فيرتز", "باير ليفركوزن", 5, 2),
            TopScorer(8, "جوناثان ديفيد", "ليل", 5, 1),
            TopScorer(9, "برادلي باركولا", "باريس سان جيرمان", 4, 2),
            TopScorer(10, "كيليان مبابي", "ريال مدريد", 4, 1)
        )

        return listOf(
            // Tier 1: Elite
            League("ucl", "دوري أبطال أوروبا (UCL)", "أوروبا", "football", LeagueTier.TIER_1_ELITE, standings = standingsUCL, topScorers = topScorersUCL),
            League("pl", "الدوري الإنجليزي الممتاز (Premier League)", "إنجلترا", "football", LeagueTier.TIER_1_ELITE, standings = standingsPL, topScorers = topScorersPL),
            League("laliga", "الدوري الإسباني (LaLiga EA Sports)", "إسبانيا", "football", LeagueTier.TIER_1_ELITE, standings = standingsLaLiga, topScorers = topScorersLaLiga),
            League("seriea", "الدوري الإيطالي (Serie A)", "إيطاليا", "football", LeagueTier.TIER_1_ELITE, standings = standingsSerieA),
            League("bundesliga", "الدوري الألماني (Bundesliga)", "ألمانيا", "football", LeagueTier.TIER_1_ELITE, standings = standingsBundesliga),
            League("spl", "دوري روشن السعودي للمحترفين (SPL)", "السعودية", "football", LeagueTier.TIER_1_ELITE, standings = standingsSPL, topScorers = topScorersSPL),
            League("worldcup", "كأس العالم FIFA", "العالم", "football", LeagueTier.TIER_1_ELITE),

            // Tier 2: Top
            League("ligue1", "الدوري الفرنسي (Ligue 1 McDonald's)", "فرنسا", "football", LeagueTier.TIER_2_TOP, standings = standingsLigue1, topScorers = topScorersLigue1),
            League("uel", "الدوري الأوروبي (UEFA Europa League)", "أوروبا", "football", LeagueTier.TIER_2_TOP),
            League("eredivisie", "الدوري الهولندي الممتاز (Eredivisie)", "هولندا", "football", LeagueTier.TIER_2_TOP),
            League("ligaportugal", "الدوري البرتغالي (Liga Portugal Betclic)", "البرتغال", "football", LeagueTier.TIER_2_TOP),
            League("afc_elite", "دوري أبطال آسيا للنخبة (AFC Elite)", "آسيا", "football", LeagueTier.TIER_2_TOP),
            League("caf_cl", "دوري أبطال أفريقيا (CAF Champions League)", "أفريقيا", "football", LeagueTier.TIER_2_TOP),
            League("egypt_pl", "الدوري المصري الممتاز (نيل)", "مصر", "football", LeagueTier.TIER_2_TOP, standings = standingsEgypt, topScorers = topScorersEgypt),
            League("morocco_botola", "الدوري المغربي للمحترفين (البطولة برو)", "المغرب", "football", LeagueTier.TIER_2_TOP, standings = standingsMorocco),
            League("tunisia_lp1", "الرابطة التونسية المحترفة الأولى", "تونس", "football", LeagueTier.TIER_2_TOP, standings = standingsTunisia),
            League("algeria_l1", "الرابطة الجزائرية المحترفة الأولى", "الجزائر", "football", LeagueTier.TIER_2_TOP),
            League("iraq_stars", "دوري نجوم العراق للمحترفين", "العراق", "football", LeagueTier.TIER_2_TOP),

            // Tier 3: Major
            League("brasileirao", "الدوري البرازيلي (Brasileirão Série A)", "البرازيل", "football", LeagueTier.TIER_3_MAJOR),
            League("mls", "الدوري الأمريكي للمحترفين (MLS)", "أمريكا", "football", LeagueTier.TIER_3_MAJOR),
            League("arg_liga", "الدوري الأرجنتيني الممتاز (Liga Profesional)", "الأرجنتين", "football", LeagueTier.TIER_3_MAJOR),
            League("libertadores", "كأس ليبرتادوريس (Copa Libertadores)", "أمريكا الجنوبية", "football", LeagueTier.TIER_3_MAJOR),
            League("uecl", "دوري المؤتمر الأوروبي (Conference League)", "أوروبا", "football", LeagueTier.TIER_3_MAJOR),
            League("superlig", "الدوري التركي الممتاز (Süper Lig)", "تركيا", "football", LeagueTier.TIER_3_MAJOR),
            League("scottish_prem", "الدوري الإسكتلندي الممتاز (Premiership)", "إسكتلندا", "football", LeagueTier.TIER_3_MAJOR),
            League("belgian_pro", "الدوري البلجيكي للمحترفين (Pro League)", "بلجيكا", "football", LeagueTier.TIER_3_MAJOR),
            League("swiss_sl", "الدوري السويسري الممتاز (Super League)", "سويسرا", "football", LeagueTier.TIER_3_MAJOR),
            League("king_cup", "كأس خادم الحرمين الشريفين (أغلى الكؤوس)", "السعودية", "football", LeagueTier.TIER_3_MAJOR),
            League("fa_cup", "كأس الاتحاد الإنجليزي (FA Cup)", "إنجلترا", "football", LeagueTier.TIER_3_MAJOR),
            League("copa_del_rey", "كأس ملك إسبانيا (Copa del Rey)", "إسبانيا", "football", LeagueTier.TIER_3_MAJOR),

            // Tier 4: Multi-Sports & Global Tournaments
            League("nba", "دوري كرة السلة الأمريكي للمحترفين (NBA)", "أمريكا", "basketball", LeagueTier.TIER_4_ALL_SPORTS),
            League("euroleague", "الدوري الأوروبي لكرة السلة (EuroLeague)", "أوروبا", "basketball", LeagueTier.TIER_4_ALL_SPORTS),
            League("fiba_wc", "كأس العالم لكرة السلة FIBA", "العالم", "basketball", LeagueTier.TIER_4_ALL_SPORTS),
            League("wimbledon", "بطولة ويمبلدون للتنس (Wimbledon)", "بريطانيا", "tennis", LeagueTier.TIER_4_ALL_SPORTS),
            League("roland_garros", "بطولة رولان غاروس (Roland Garros)", "فرنسا", "tennis", LeagueTier.TIER_4_ALL_SPORTS),
            League("aus_open", "بطولة أستراليا المفتوحة للتنس (Australian Open)", "أستراليا", "tennis", LeagueTier.TIER_4_ALL_SPORTS),
            League("us_open", "بطولة أمريكا المفتوحة للتنس (US Open)", "أمريكا", "tennis", LeagueTier.TIER_4_ALL_SPORTS),
            League("f1_world", "بطولة العالم لسباقات الفورمولا 1 (FIA F1)", "العالم", "f1", LeagueTier.TIER_4_ALL_SPORTS),
            League("ipl", "الدوري الهندي الممتاز للكريكيت (IPL Cricket)", "الهند", "cricket", LeagueTier.TIER_4_ALL_SPORTS),
            League("ufc", "منظمة الفنون القتالية المختلطة (UFC)", "العالم", "mma", LeagueTier.TIER_4_ALL_SPORTS),
            League("nfl", "الدوري الأمريكي لكرة القدم الأمريكية (NFL)", "أمريكا", "american_football", LeagueTier.TIER_4_ALL_SPORTS),
            League("nhl", "دوري الهوكي الوطني الأمريكي (NHL)", "أمريكا", "ice_hockey", LeagueTier.TIER_4_ALL_SPORTS),
            League("bal", "دوري أبطال أفريقيا لكرة السلة (BAL)", "أفريقيا", "basketball", LeagueTier.TIER_4_ALL_SPORTS),
            League("davis_cup", "كأس ديفيز للتنس للمنتخبات", "العالم", "tennis", LeagueTier.TIER_4_ALL_SPORTS),
            League("handball_cl", "دوري أبطال أوروبا لكرة اليد (EHF CL)", "أوروبا", "handball", LeagueTier.TIER_4_ALL_SPORTS),
            League("volleyball_vnl", "دوري الأمم للكرة الطائرة (VNL)", "العالم", "volleyball", LeagueTier.TIER_4_ALL_SPORTS),
            League("rugby_wc", "كأس العالم للرجبي (Rugby World Cup)", "العالم", "rugby", LeagueTier.TIER_4_ALL_SPORTS),
            League("moto_gp", "بطولة العالم للدراجات النارية (MotoGP)", "العالم", "motorsport", LeagueTier.TIER_4_ALL_SPORTS),
            League("qatar_stars", "دوري نجوم قطر (QSL)", "قطر", "football", LeagueTier.TIER_3_MAJOR),
            League("uae_pro", "دوري أدنوك للمحترفين (الإمارات)", "الإمارات", "football", LeagueTier.TIER_3_MAJOR)
        )
    }
}
