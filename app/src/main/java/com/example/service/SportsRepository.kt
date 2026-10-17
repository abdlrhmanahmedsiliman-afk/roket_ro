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
                                playerConditions = memoryMatch?.playerConditions ?: generatePciForTeams(entity.homeTeam, entity.awayTeam),
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
                val initial = generateSampleMatches()
                _matches.value = initial
                matchDao.insertMatches(initial.map { it.toEntity() })

                // Pre-cache team rosters locally into Room to guarantee offline availability and 0 latency
                val rostersToCache = initial.flatMap { match ->
                    listOfNotNull(
                        match.homeLineup?.let { TeamRosterMapper.toEntity(match.homeTeam, it, match.league, match.sport) },
                        match.awayLineup?.let { TeamRosterMapper.toEntity(match.awayTeam, it, match.league, match.sport) }
                    )
                }
                if (rostersToCache.isNotEmpty()) {
                    rosterDao.insertRosters(rostersToCache)
                }
            }
            _leagues.value = generate50PlusLeagues()
        }
    }

    /**
     * Offline-first team roster retrieval:
     * 1. Sub-millisecond In-Memory LRU Cache
     * 2. Device-local Room Database (TeamRosterDao)
     * 3. Fallback generator with instant Room persistence
     */
    suspend fun getOrFetchTeamRoster(teamName: String, league: String = "", sport: String = "كرة القدم"): TeamLineup {
        val cacheKey = "roster_$teamName"
        val inMemory = memoryCache.get(cacheKey) as? TeamLineup
        if (inMemory != null) return inMemory

        val localCached = rosterDao.getRosterByTeamSync(teamName)
        if (localCached != null && !localCached.isExpired) {
            val domainLineup = TeamRosterMapper.toDomainModel(localCached)
            memoryCache.put(cacheKey, domainLineup)
            return domainLineup
        }

        val freshLineup = createSampleLineup(
            formation = if (teamName.contains("سيتي")) "4-2-3-1" else "4-3-3",
            coach = "المدرب الفني",
            team = teamName
        )
        val entity = TeamRosterMapper.toEntity(teamName, freshLineup, league, sport)
        rosterDao.insertRoster(entity)
        memoryCache.put(cacheKey, freshLineup)
        return freshLineup
    }

    fun observeTeamRoster(teamName: String): Flow<TeamLineup?> {
        return rosterDao.getRosterByTeam(teamName)
            .map { entity -> entity?.let { TeamRosterMapper.toDomainModel(it) } }
    }

    fun refreshMatches() {
        scope.launch(Dispatchers.IO) {
            _isLoading.value = true
            // Fast delay simulating high-throughput delta sync
            delay(400)

            var newEventsCount = 0
            var updatedScoresMsg: String? = null

            val currentList = _matches.value.map { match ->
                if (match.isLive) {
                    val minuteStep = Random.nextInt(1, 3)
                    val newMinute = (match.minute + minuteStep).coerceAtMost(95)
                    val goalChance = Random.nextInt(100) < 18
                    var newHomeScore = match.homeScore
                    var newAwayScore = match.awayScore
                    val newEvents = match.events.toMutableList()

                    val updatedXgHome = match.stats.xgHome + (Random.nextFloat() * 0.08f)
                    val updatedXgAway = match.stats.xgAway + (Random.nextFloat() * 0.08f)
                    val updatedShotsHome = match.stats.shotsHome + if (Random.nextBoolean()) 1 else 0
                    val updatedShotsAway = match.stats.shotsAway + if (Random.nextBoolean()) 1 else 0

                    if (goalChance && newMinute < 90) {
                        val homeScores = Random.nextBoolean()
                        newEventsCount++
                        if (homeScores) {
                            newHomeScore += 1
                            val scorer = if (match.homeTeam.contains("مدريد")) "فينيسيوس جونيور"
                            else if (match.homeTeam.contains("ليفربول")) "محمد صلاح"
                            else "هدف مباغت!"

                            newEvents.add(
                                MatchEvent(
                                    minute = newMinute,
                                    type = EventType.GOAL,
                                    teamIsHome = true,
                                    player = scorer,
                                    detail = "$newHomeScore - $newAwayScore"
                                )
                            )
                            updatedScoresMsg = "⚽ هدف لـ ${match.homeTeam} (د${newMinute}')"
                        } else {
                            newAwayScore += 1
                            val scorer = if (match.awayTeam.contains("سيتي")) "إيرلينغ هالاند"
                            else if (match.awayTeam.contains("أرسنال")) "بوكايو ساكا"
                            else "هدف التعادل!"

                            newEvents.add(
                                MatchEvent(
                                    minute = newMinute,
                                    type = EventType.GOAL,
                                    teamIsHome = false,
                                    player = scorer,
                                    detail = "$newHomeScore - $newAwayScore"
                                )
                            )
                            updatedScoresMsg = "⚽ هدف لـ ${match.awayTeam} (د${newMinute}')"
                        }
                    }

                    match.copy(
                        minute = newMinute,
                        homeScore = newHomeScore,
                        awayScore = newAwayScore,
                        events = newEvents,
                        stats = match.stats.copy(
                            xgHome = (updatedXgHome * 100).toInt() / 100f,
                            xgAway = (updatedXgAway * 100).toInt() / 100f,
                            shotsHome = updatedShotsHome,
                            shotsAway = updatedShotsAway
                        ),
                        // Dynamic Win Probabilities re-calculated live
                        homeWinProb = if (newHomeScore > newAwayScore) 62.0 else if (newHomeScore < newAwayScore) 22.0 else 38.0,
                        drawProb = if (newHomeScore == newAwayScore) 38.0 else 20.0,
                        awayWinProb = if (newAwayScore > newHomeScore) 58.0 else if (newAwayScore < newHomeScore) 18.0 else 24.0
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

    private fun startLiveTicker() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                // Adaptive delay based on network quality
                val interval = connectionMonitor?.quality?.value?.refreshIntervalMs ?: 3000L
                delay(interval)

                val hasLive = _matches.value.any { it.isLive }
                if (hasLive && !_isLoading.value) {
                    val updated = _matches.value.map { match ->
                        if (match.isLive && match.minute < 90) {
                            val nextMin = match.minute + 1
                            match.copy(minute = nextMin)
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

    private fun generatePciForTeams(homeTeam: String, awayTeam: String): List<PlayerCondition> {
        val conditions = mutableListOf<PlayerCondition>()

        // Home team star 1
        conditions.add(
            PlayerCondition(
                playerId = "p_1",
                playerName = if (homeTeam.contains("مدريد")) "فينيسيوس جونيور" else "محمد صلاح",
                playerNumber = if (homeTeam.contains("مدريد")) 7 else 11,
                teamName = homeTeam,
                position = "جناح هجومي",
                factors = PciFactors(
                    physicalFatigue = 94f,
                    mentalPsychological = 92f,
                    recentForm = 96f,
                    injuryRisk = 90f,
                    travelScheduleLoad = 85f,
                    tacticalFit = 95f,
                    weatherPitchAdaptation = 90f
                ),
                timeline = listOf(
                    PciTimelinePoint(0, 96, "بداية المباراة بكامل اللياقة"),
                    PciTimelinePoint(30, 94, "سرعات عالية وتفوق 1v1"),
                    PciTimelinePoint(60, 90, "ثبات في التحولات"),
                    PciTimelinePoint(85, 87, "جهد بدني كبير")
                ),
                keyStrengths = listOf("تسارع فائق", "إنهاء قاتل", "تمركز ذكي"),
                aiConditionSummary = "جاهزية بدنية وذهنية استثنائية (PCI 92)؛ معدل استرجاع طاقة مرتفع وفرص صناعة الفارق تتجاوز 88%."
            )
        )

        // Home team midfield leader
        conditions.add(
            PlayerCondition(
                playerId = "p_2",
                playerName = if (homeTeam.contains("مدريد")) "جود بيلينغهام" else "ألكسيس ماك أليستر",
                playerNumber = if (homeTeam.contains("مدريد")) 5 else 10,
                teamName = homeTeam,
                position = "وسط هجومي",
                factors = PciFactors(
                    physicalFatigue = 86f,
                    mentalPsychological = 90f,
                    recentForm = 88f,
                    injuryRisk = 85f,
                    travelScheduleLoad = 78f,
                    tacticalFit = 92f,
                    weatherPitchAdaptation = 88f
                ),
                timeline = listOf(
                    PciTimelinePoint(0, 92),
                    PciTimelinePoint(45, 87),
                    PciTimelinePoint(75, 82)
                ),
                aiConditionSummary = "أداء بدني متوازن مع تفوق في استخلاص الكرات الثانية والتغطية الدفاعية في العمق."
            )
        )

        // Away team star 1
        conditions.add(
            PlayerCondition(
                playerId = "p_3",
                playerName = if (awayTeam.contains("سيتي")) "إيرلينغ هالاند" else "بوكايو ساكا",
                playerNumber = if (awayTeam.contains("سيتي")) 9 else 7,
                teamName = awayTeam,
                position = if (awayTeam.contains("سيتي")) "قلب هجوم" else "جناح أيمن",
                factors = PciFactors(
                    physicalFatigue = 91f,
                    mentalPsychological = 95f,
                    recentForm = 93f,
                    injuryRisk = 88f,
                    travelScheduleLoad = 82f,
                    tacticalFit = 94f,
                    weatherPitchAdaptation = 89f
                ),
                timeline = listOf(
                    PciTimelinePoint(0, 95),
                    PciTimelinePoint(45, 91),
                    PciTimelinePoint(80, 88)
                ),
                keyStrengths = listOf("قوة جسدية خارقة", "إنهاء هجمات من لمسة واحدة"),
                aiConditionSummary = "مؤشر PCI يبلغ 91؛ استغلال ممتاز للمساحات بين قلبي الدفاع مع خطورة فائقة في الكرات الهوائية."
            )
        )

        // Away team playmaker (moderate fatigue example)
        conditions.add(
            PlayerCondition(
                playerId = "p_4",
                playerName = if (awayTeam.contains("سيتي")) "كيفين دي بروين" else "مارتن أوديغارد",
                playerNumber = if (awayTeam.contains("سيتي")) 17 else 8,
                teamName = awayTeam,
                position = "صانع ألعاب",
                factors = PciFactors(
                    physicalFatigue = 72f,
                    mentalPsychological = 92f,
                    recentForm = 85f,
                    injuryRisk = 68f, // Moderate injury risk / recovery
                    travelScheduleLoad = 65f,
                    tacticalFit = 95f,
                    weatherPitchAdaptation = 84f
                ),
                timeline = listOf(
                    PciTimelinePoint(0, 85),
                    PciTimelinePoint(45, 78),
                    PciTimelinePoint(70, 68)
                ),
                aiConditionSummary = "رؤية استثنائية للملعب مع تنبيه بضرورة تقنين المجهود في الربع ساعة الأخير لتفادي الإجهاد العضلي."
            )
        )

        return conditions
    }

    private fun generateBroadcastersForMatch(league: String): List<Broadcaster> {
        return listOf(
            Broadcaster(
                id = "b_1",
                name = "beIN SPORTS 1 HD",
                channelNumberOrPackage = "قناة 1 (باقة beIN Ultimate)",
                region = RegionZone.MENA,
                format = StreamFormat.UHD_4K,
                commentators = listOf("عصام الشوالي", "خليل البلوشي"),
                language = "العربية",
                officialPlatformUrl = "https://www.tod.tv",
                appPackage = "tv.tod.app"
            ),
            Broadcaster(
                id = "b_2",
                name = "SSC 1 HD",
                channelNumberOrPackage = "باقة الرياضة السعودية",
                region = RegionZone.MENA,
                format = StreamFormat.FULL_HD,
                commentators = listOf("فهد العتيبي", "فارس عوض"),
                language = "العربية",
                officialPlatformUrl = "https://shahid.mbc.net",
                appPackage = "com.mbc.shahid"
            ),
            Broadcaster(
                id = "b_3",
                name = "Sky Sports Main Event",
                channelNumberOrPackage = "Sky TV / NOW UK",
                region = RegionZone.UK,
                format = StreamFormat.UHD_4K,
                commentators = listOf("Peter Drury", "Gary Neville"),
                language = "English",
                officialPlatformUrl = "https://www.skysports.com",
                appPackage = "com.bskyb.sportnews"
            ),
            Broadcaster(
                id = "b_4",
                name = "DAZN Spain / Global",
                channelNumberOrPackage = "DAZN Pro OTT",
                region = RegionZone.SPAIN,
                format = StreamFormat.OTT_STREAM,
                commentators = listOf("Miguel Ángel Román"),
                language = "Español",
                officialPlatformUrl = "https://www.dazn.com",
                appPackage = "com.dazn"
            ),
            Broadcaster(
                id = "b_5",
                name = "ESPN+ / USA",
                channelNumberOrPackage = "ESPN Select",
                region = RegionZone.USA,
                format = StreamFormat.OTT_STREAM,
                commentators = listOf("Jon Champion", "Stewart Robson"),
                language = "English",
                officialPlatformUrl = "https://plus.espn.com",
                appPackage = "com.espn.score_center"
            ),
            Broadcaster(
                id = "b_6",
                name = "Canal+ Foot",
                channelNumberOrPackage = "Canal+ France",
                region = RegionZone.FRANCE,
                format = StreamFormat.FULL_HD,
                commentators = listOf("Paul Le Guen"),
                language = "Français",
                officialPlatformUrl = "https://www.canalplus.com",
                appPackage = "com.canal.android.canalplus"
            )
        )
    }

    private fun generateSampleMatches(): List<Match> {
        val favs = preferencesManager.favoriteTeams.value
        return listOf(
            Match(
                id = "m_1",
                sport = SportType.FOOTBALL.id,
                league = "دوري أبطال أوروبا",
                country = "أوروبا",
                homeTeam = "ريال مدريد",
                awayTeam = "مانشستر سيتي",
                homeLogo = "https://media.api-sports.io/football/teams/541.png",
                awayLogo = "https://media.api-sports.io/football/teams/50.png",
                homeScore = 2,
                awayScore = 1,
                status = MatchStatus.LIVE,
                minute = 67,
                venue = "سانتياغو برنابيو، مدريد",
                referee = "سيمون مارتشينياك",
                homeWinProb = 48.0,
                drawProb = 24.0,
                awayWinProb = 28.0,
                aiPreview = "قمة أوروبية نارية؛ تفوق واضح للريال في التحولات السريعة مع حذر تكتيكي من جوارديولا في عمق الوسط.",
                aiLiveTactical = "ضغط عالي مكثف من السيتي في آخر 10 دقائق (68% استحواذ)، بينما يعتمد الريال على مرتدات فينيسيوس ومبابي السريعة مع تألق كورتوا.",
                aiPostReport = "مباراة ملحمية حسمتها تفاصيل صغيرة في خط الوسط وجودة إنهاء الهجمات في اللحظات الحاسمة.",
                stats = MatchStats(
                    possessionHome = 46,
                    possessionAway = 54,
                    xgHome = 1.82f,
                    xgAway = 1.45f,
                    shotsHome = 14,
                    shotsAway = 11,
                    shotsOnTargetHome = 6,
                    shotsOnTargetAway = 4,
                    bigChancesHome = 3,
                    bigChancesAway = 2,
                    passAccuracyHome = 88,
                    passAccuracyAway = 91,
                    cornersHome = 5,
                    cornersAway = 7,
                    foulsHome = 8,
                    foulsAway = 11
                ),
                events = listOf(
                    MatchEvent(12, EventType.GOAL, true, "فينيسيوس جونيور", "صناعة بيلينغهام"),
                    MatchEvent(34, EventType.YELLOW_CARD, false, "رودري", "خطأ تكتيكي"),
                    MatchEvent(43, EventType.GOAL, false, "إيرلينغ هالاند", "تسديدة رأسية قوية"),
                    MatchEvent(58, EventType.GOAL, true, "كيليان مبابي", "انفراد بالمرمى")
                ),
                homeLineup = createSampleLineup("4-3-3", "كارلو أنشيلوتي", "ريال مدريد"),
                awayLineup = createSampleLineup("4-2-3-1", "بيب جوارديولا", "مانشستر سيتي"),
                isFavorite = favs.contains("ريال مدريد") || favs.contains("مانشستر سيتي"),
                playerConditions = generatePciForTeams("ريال مدريد", "مانشستر سيتي"),
                broadcasters = generateBroadcastersForMatch("دوري أبطال أوروبا")
            ),
            Match(
                id = "m_2",
                sport = SportType.FOOTBALL.id,
                league = "الدوري الإنجليزي الممتاز",
                country = "إنجلترا",
                homeTeam = "ليفربول",
                awayTeam = "أرسنال",
                homeLogo = "https://media.api-sports.io/football/teams/40.png",
                awayLogo = "https://media.api-sports.io/football/teams/42.png",
                homeScore = 1,
                awayScore = 1,
                status = MatchStatus.LIVE,
                minute = 34,
                venue = "آنفيلد، ليفربول",
                referee = "أنتوني تايلور",
                homeWinProb = 39.0,
                drawProb = 31.0,
                awayWinProb = 30.0,
                aiPreview = "صراع الصدارة في آنفيلد. صلاح يقود هجوم الريدز أمام أقوى خط دفاع في الدوري بقيادة ساليبا وغابرييل.",
                aiLiveTactical = "إيقاع سريع وتدخلات بدنية قوية في منتصف الملعب، وسيطرة متوازنة على الكرات الثانية.",
                events = listOf(
                    MatchEvent(18, EventType.GOAL, true, "محمد صلاح", "تسديدة يسارية مميزة"),
                    MatchEvent(29, EventType.GOAL, false, "بوكايو ساكا", "متابعة داخل المنطقة")
                ),
                homeLineup = createSampleLineup("4-3-3", "أرني سلوت", "ليفربول"),
                awayLineup = createSampleLineup("4-3-3", "ميكيل أرتيتا", "أرسنال"),
                playerConditions = generatePciForTeams("ليفربول", "أرسنال"),
                broadcasters = generateBroadcastersForMatch("الدوري الإنجليزي الممتاز")
            ),
            Match(
                id = "m_3",
                sport = SportType.FOOTBALL.id,
                league = "الدوري الإسباني",
                country = "إسبانيا",
                homeTeam = "برشلونة",
                awayTeam = "أتلتيكو مدريد",
                homeLogo = "https://media.api-sports.io/football/teams/529.png",
                awayLogo = "https://media.api-sports.io/football/teams/530.png",
                homeScore = 0,
                awayScore = 0,
                status = MatchStatus.NOT_STARTED,
                kickoffTime = "22:00",
                venue = "كامب نو / الأولمبي، برشلونة",
                referee = "خيل مانزانو",
                homeWinProb = 52.0,
                drawProb = 25.0,
                awayWinProb = 23.0,
                aiPreview = "برشلونة فليك بأسلوب مصيدة التسلل والضغط العالي أمام تكتل سيميوني الدفاعي والهجمات المرتدة الخاطفة.",
                homeLineup = createSampleLineup("4-2-3-1", "هانسي فليك", "برشلونة"),
                awayLineup = createSampleLineup("5-3-2", "دييغو سيميوني", "أتلتيكو مدريد"),
                playerConditions = generatePciForTeams("برشلونة", "أتلتيكو مدريد"),
                broadcasters = generateBroadcastersForMatch("الدوري الإسباني")
            ),
            Match(
                id = "m_4",
                sport = SportType.FOOTBALL.id,
                league = "دوري روشن السعودي",
                country = "السعودية",
                homeTeam = "الهلال",
                awayTeam = "النصر",
                homeLogo = "https://media.api-sports.io/football/teams/2939.png",
                awayLogo = "https://media.api-sports.io/football/teams/2932.png",
                homeScore = 3,
                awayScore = 1,
                status = MatchStatus.FINISHED,
                venue = "المملكة أرينا، الرياض",
                homeWinProb = 55.0,
                drawProb = 22.0,
                awayWinProb = 23.0,
                aiPreview = "ديربي الرياض المشتعل؛ كريستيانو رونالدو وماني في مواجهة ميتروفيتش وسافيتش.",
                aiPostReport = "فوز مستحق للزعيم بثلاثية حسمها الاستحواذ الفعال وجودة الكرات العرضية في منطقة العمليات.",
                playerConditions = generatePciForTeams("الهلال", "النصر"),
                broadcasters = generateBroadcastersForMatch("دوري روشن السعودي")
            ),
            Match(
                id = "m_5",
                sport = SportType.BASKETBALL.id,
                league = "دوري السلة الأمريكي NBA",
                country = "أمريكا",
                homeTeam = "لوس أنجلوس ليكرز",
                awayTeam = "غولدن ستيت واريورز",
                homeScore = 114,
                awayScore = 110,
                status = MatchStatus.LIVE,
                minute = 4,
                kickoffTime = "الربع الرابع",
                venue = "Crypto.com Arena",
                homeWinProb = 62.0,
                drawProb = 0.0,
                awayWinProb = 38.0,
                aiPreview = "ليبرون جيمس وأنثوني ديفيس في مواجهة ستيف كوري وثلاثياته الحاسمة في اللحظات الأخيرة."
            ),
            Match(
                id = "m_6",
                sport = SportType.TENNIS.id,
                league = "بطولة ويمبلدون للتنس",
                country = "بريطانيا",
                homeTeam = "كارلوس ألكاراز",
                awayTeam = "يانيك سينر",
                homeScore = 3,
                awayScore = 2,
                status = MatchStatus.FINISHED,
                venue = "الملعب الرئيسي، ويمبلدون",
                homeWinProb = 51.0,
                drawProb = 0.0,
                awayWinProb = 49.0,
                aiPreview = "نهائي ناري ومثير من خمس مجموعات بين عملاقي الجيل الجديد على الأراضي العشبية الخضراء."
            ),
            Match(
                id = "m_7",
                sport = SportType.F1.id,
                league = "سباق جائزة موناكو الكبرى",
                country = "موناكو",
                homeTeam = "ماكس فيرستابين",
                awayTeam = "شارل لوكلير",
                homeScore = 1,
                awayScore = 2,
                status = MatchStatus.FINISHED,
                venue = "حلبة موناكو الأسطورية",
                homeWinProb = 40.0,
                drawProb = 0.0,
                awayWinProb = 60.0,
                aiPreview = "فوز عاطفي وتاريخي لشارل لوكلير على أرضه وبين جماهيره بعد انطلاقة مثالية واستراتيجية توقفات ممتازة."
            )
        )
    }

    private fun createSampleLineup(formation: String, coach: String, team: String): TeamLineup {
        return when (team) {
            "ريال مدريد" -> {
                val starters = listOf(
                    PlayerLineup(1, "كورتوا", "GK", 8.8f, false, 0.5f, 0.90f),
                    PlayerLineup(2, "كارفاخال", "RB", 8.2f, false, 0.85f, 0.72f),
                    PlayerLineup(3, "إيدير ميليتاو", "CB", 8.4f, false, 0.62f, 0.76f),
                    PlayerLineup(22, "أنطونيو روديغر", "CB", 8.7f, false, 0.38f, 0.76f),
                    PlayerLineup(23, "فيرلاند ميندي", "LB", 8.1f, false, 0.15f, 0.72f),
                    PlayerLineup(14, "أوريليان تشواميني", "CDM", 8.4f, false, 0.5f, 0.54f),
                    PlayerLineup(8, "فيديريكو فالفيردي", "CM", 9.1f, false, 0.74f, 0.42f),
                    PlayerLineup(5, "جود بيلينغهام", "CAM", 9.3f, false, 0.5f, 0.32f),
                    PlayerLineup(11, "رودريغو", "RW", 8.6f, false, 0.84f, 0.20f),
                    PlayerLineup(9, "كيليان مبابي", "ST", 9.4f, false, 0.5f, 0.14f),
                    PlayerLineup(7, "فينيسيوس جونيور", "LW", 9.6f, true, 0.16f, 0.20f)
                )
                val subs = listOf(
                    PlayerLineup(13, "أندريه لونين", "GK", 8.0f),
                    PlayerLineup(17, "لوكاس فاسكيز", "RB", 7.9f),
                    PlayerLineup(20, "فران غارسيا", "LB", 7.6f),
                    PlayerLineup(6, "إدواردو كامافينغا", "CM", 8.5f),
                    PlayerLineup(10, "لوكا مودريتش", "CM", 8.7f),
                    PlayerLineup(15, "أردا غولر", "CAM", 8.2f),
                    PlayerLineup(16, "إندريك", "ST", 8.1f)
                )
                TeamLineup("4-3-3 هجومي", "كارلو أنشيلوتي", starters, subs)
            }
            "مانشستر سيتي" -> {
                val starters = listOf(
                    PlayerLineup(31, "إيدرسون", "GK", 8.6f, false, 0.5f, 0.90f),
                    PlayerLineup(82, "ريكو لويس", "RB", 8.0f, false, 0.85f, 0.72f),
                    PlayerLineup(25, "مانويل أكانجي", "CB", 8.3f, false, 0.62f, 0.76f),
                    PlayerLineup(3, "روبن دياز", "CB", 8.8f, true, 0.38f, 0.76f),
                    PlayerLineup(24, "يوشكو جفارديول", "LB", 8.7f, false, 0.15f, 0.72f),
                    PlayerLineup(16, "رودري", "CDM", 9.5f, false, 0.5f, 0.55f),
                    PlayerLineup(8, "ماتيو كوفاسيتش", "CM", 8.2f, false, 0.68f, 0.44f),
                    PlayerLineup(17, "كيفين دي بروين", "CAM", 9.4f, false, 0.5f, 0.32f),
                    PlayerLineup(20, "برناردو سيلفا", "RW", 8.9f, false, 0.84f, 0.22f),
                    PlayerLineup(9, "إيرلينغ هالاند", "ST", 9.6f, false, 0.5f, 0.14f),
                    PlayerLineup(47, "فيل فودين", "LW", 9.2f, false, 0.16f, 0.22f)
                )
                val subs = listOf(
                    PlayerLineup(18, "ستيفان أورتيغا", "GK", 7.9f),
                    PlayerLineup(5, "جون ستونز", "CB", 8.4f),
                    PlayerLineup(2, "كايل ووكر", "RB", 8.2f),
                    PlayerLineup(27, "ماتيوس نونيز", "CM", 7.8f),
                    PlayerLineup(11, "جيريمي دوكو", "LW", 8.4f),
                    PlayerLineup(10, "جاك غريليش", "LW", 8.3f),
                    PlayerLineup(52, "أوسكار بوب", "RW", 7.9f)
                )
                TeamLineup("4-2-3-1 تكتيكي", "بيب جوارديولا", starters, subs)
            }
            "ليفربول" -> {
                val starters = listOf(
                    PlayerLineup(1, "أليسون بيكر", "GK", 9.0f, false, 0.5f, 0.90f),
                    PlayerLineup(66, "ترينت أرنولد", "RB", 8.9f, false, 0.85f, 0.72f),
                    PlayerLineup(5, "إبراهيما كوناتي", "CB", 8.5f, false, 0.62f, 0.76f),
                    PlayerLineup(4, "فيرجيل فان دايك", "CB", 9.2f, true, 0.38f, 0.76f),
                    PlayerLineup(26, "أندي روبرتسون", "LB", 8.3f, false, 0.15f, 0.72f),
                    PlayerLineup(38, "ريان غرافنبرخ", "CDM", 8.6f, false, 0.5f, 0.55f),
                    PlayerLineup(10, "أليكسيس ماك أليستر", "CM", 8.8f, false, 0.70f, 0.44f),
                    PlayerLineup(8, "دومينيك سوبوسلاي", "CAM", 8.5f, false, 0.5f, 0.32f),
                    PlayerLineup(11, "محمد صلاح", "RW", 9.5f, false, 0.84f, 0.20f),
                    PlayerLineup(9, "داروين نونيز", "ST", 8.4f, false, 0.5f, 0.14f),
                    PlayerLineup(7, "لويس دياز", "LW", 9.0f, false, 0.16f, 0.20f)
                )
                val subs = listOf(
                    PlayerLineup(62, "كاويمين كيليهير", "GK", 8.1f),
                    PlayerLineup(2, "جو غوميز", "CB", 7.8f),
                    PlayerLineup(21, "كوستاس تسيميكاس", "LB", 7.7f),
                    PlayerLineup(17, "كورتيس جونز", "CM", 8.2f),
                    PlayerLineup(19, "هارفي إليوت", "CAM", 8.0f),
                    PlayerLineup(18, "كودي خاكبو", "LW", 8.5f),
                    PlayerLineup(14, "فيديريكو كييزا", "RW", 8.1f)
                )
                TeamLineup("4-3-3 هجومي سريع", "أرني سلوت", starters, subs)
            }
            "أرسنال" -> {
                val starters = listOf(
                    PlayerLineup(22, "دافيد رايا", "GK", 8.8f, false, 0.5f, 0.90f),
                    PlayerLineup(4, "بن وايت", "RB", 8.4f, false, 0.85f, 0.72f),
                    PlayerLineup(2, "ويليام ساليبا", "CB", 9.2f, false, 0.62f, 0.76f),
                    PlayerLineup(6, "غابرييل ماغالهايس", "CB", 9.0f, false, 0.38f, 0.76f),
                    PlayerLineup(12, "يوريان تيمبر", "LB", 8.5f, false, 0.15f, 0.72f),
                    PlayerLineup(5, "توماس بارتي", "CDM", 8.3f, false, 0.5f, 0.55f),
                    PlayerLineup(41, "ديكلان رايس", "CM", 9.1f, false, 0.70f, 0.44f),
                    PlayerLineup(8, "مارتن أوديغارد", "CAM", 9.3f, true, 0.5f, 0.32f),
                    PlayerLineup(7, "بوكايو ساكا", "RW", 9.4f, false, 0.84f, 0.20f),
                    PlayerLineup(29, "كاي هافيرتز", "ST", 8.7f, false, 0.5f, 0.14f),
                    PlayerLineup(11, "غابرييل مارتينيلي", "LW", 8.6f, false, 0.16f, 0.20f)
                )
                val subs = listOf(
                    PlayerLineup(32, "نيتو", "GK", 7.8f),
                    PlayerLineup(15, "ياكوب كيويور", "CB", 7.7f),
                    PlayerLineup(17, "أولكسندر زينتشينكو", "LB", 8.0f),
                    PlayerLineup(20, "جورجينيو", "CM", 8.1f),
                    PlayerLineup(23, "ميكيل ميرينو", "CM", 8.3f),
                    PlayerLineup(19, "لياندرو تروسار", "LW", 8.6f),
                    PlayerLineup(9, "غابرييل جيسوس", "ST", 8.2f)
                )
                TeamLineup("4-3-3 ضغط عالي", "ميكيل أرتيتا", starters, subs)
            }
            "برشلونة" -> {
                val starters = listOf(
                    PlayerLineup(13, "إينياكي بينيا", "GK", 8.1f, false, 0.5f, 0.90f),
                    PlayerLineup(23, "جول كوندي", "RB", 8.8f, false, 0.85f, 0.72f),
                    PlayerLineup(2, "باو كوبارسي", "CB", 8.7f, false, 0.62f, 0.76f),
                    PlayerLineup(5, "إينيغو مارتينيز", "CB", 8.6f, false, 0.38f, 0.76f),
                    PlayerLineup(3, "أليخاندرو بالدي", "LB", 8.5f, false, 0.15f, 0.72f),
                    PlayerLineup(17, "مارك كاسادو", "CDM", 8.5f, false, 0.5f, 0.55f),
                    PlayerLineup(8, "بيدري غونزاليس", "CM", 9.3f, false, 0.70f, 0.44f),
                    PlayerLineup(20, "داني أولمو", "CAM", 9.0f, false, 0.5f, 0.32f),
                    PlayerLineup(19, "لامين يامال", "RW", 9.5f, false, 0.84f, 0.20f),
                    PlayerLineup(9, "روبرت ليفاندوفسكي", "ST", 9.4f, false, 0.5f, 0.14f),
                    PlayerLineup(11, "رافينيا دياز", "LW", 9.4f, true, 0.16f, 0.20f)
                )
                val subs = listOf(
                    PlayerLineup(25, "فويتشيك تشيزني", "GK", 8.2f),
                    PlayerLineup(24, "إريك غارسيا", "CB", 7.8f),
                    PlayerLineup(35, "جيرارد مارتن", "LB", 7.5f),
                    PlayerLineup(6, "غافي", "CM", 8.9f),
                    PlayerLineup(21, "فرينكي دي يونغ", "CM", 8.8f),
                    PlayerLineup(18, "باو فيكتور", "ST", 7.9f),
                    PlayerLineup(10, "أنسو فاتي", "LW", 8.0f)
                )
                TeamLineup("4-2-3-1 هجومي مباشر", "هانسي فليك", starters, subs)
            }
            "أتلتيكو مدريد" -> {
                val starters = listOf(
                    PlayerLineup(13, "يان أوبلاك", "GK", 9.1f, true, 0.5f, 0.90f),
                    PlayerLineup(16, "ناويل مولينا", "RWB", 8.2f, false, 0.85f, 0.68f),
                    PlayerLineup(24, "روبين لو نورماند", "CB", 8.6f, false, 0.68f, 0.78f),
                    PlayerLineup(2, "خوسيه خيمينيز", "CB", 8.7f, false, 0.5f, 0.82f),
                    PlayerLineup(23, "رينيلدو ماندافا", "CB", 8.3f, false, 0.32f, 0.78f),
                    PlayerLineup(12, "صامويل لينو", "LWB", 8.4f, false, 0.15f, 0.68f),
                    PlayerLineup(5, "رودريغو دي بول", "CM", 8.6f, false, 0.68f, 0.48f),
                    PlayerLineup(6, "كوكي", "CDM", 8.4f, false, 0.5f, 0.56f),
                    PlayerLineup(4, "كونور غالاغير", "CM", 8.5f, false, 0.32f, 0.48f),
                    PlayerLineup(7, "أنطوان غريزمان", "ST", 9.2f, false, 0.65f, 0.22f),
                    PlayerLineup(19, "جوليان ألفاريز", "ST", 8.9f, false, 0.35f, 0.22f)
                )
                val subs = listOf(
                    PlayerLineup(1, "خوان موسو", "GK", 7.8f),
                    PlayerLineup(3, "سيزار أزبيليكويتا", "CB", 7.9f),
                    PlayerLineup(14, "ماركوس يورينتي", "RWB", 8.5f),
                    PlayerLineup(8, "بابلو باريوس", "CM", 8.2f),
                    PlayerLineup(17, "رودريغو ريكيلمي", "LW", 8.1f),
                    PlayerLineup(9, "ألكسندر سورلوث", "ST", 8.4f),
                    PlayerLineup(10, "أنخيل كوريا", "ST", 8.3f)
                )
                TeamLineup("5-3-2 تكتل ومرتدات", "دييغو سيميوني", starters, subs)
            }
            "الهلال" -> {
                val starters = listOf(
                    PlayerLineup(37, "ياسين بونو", "GK", 9.0f, false, 0.5f, 0.90f),
                    PlayerLineup(20, "جواو كانسيلو", "RB", 8.9f, false, 0.85f, 0.72f),
                    PlayerLineup(3, "خاليدو كوليبالي", "CB", 8.7f, false, 0.62f, 0.76f),
                    PlayerLineup(5, "علي البليهي", "CB", 8.3f, false, 0.38f, 0.76f),
                    PlayerLineup(6, "رينان لودي", "LB", 8.2f, false, 0.15f, 0.72f),
                    PlayerLineup(8, "روبن نيفيز", "CDM", 9.0f, false, 0.5f, 0.55f),
                    PlayerLineup(22, "سيرجي سافيتش", "CM", 9.1f, false, 0.70f, 0.44f),
                    PlayerLineup(77, "مالكوم فيليبي", "RW", 8.9f, false, 0.84f, 0.20f),
                    PlayerLineup(11, "ماركوس ليوناردو", "CAM", 8.5f, false, 0.5f, 0.32f),
                    PlayerLineup(29, "سالم الدوسري", "LW", 9.2f, true, 0.16f, 0.20f),
                    PlayerLineup(9, "ألكسندر ميتروفيتش", "ST", 9.3f, false, 0.5f, 0.14f)
                )
                val subs = listOf(
                    PlayerLineup(21, "محمد العويس", "GK", 8.0f),
                    PlayerLineup(12, "ياسر الشهراني", "LB", 8.1f),
                    PlayerLineup(87, "حسان تمبكتي", "CB", 8.3f),
                    PlayerLineup(16, "ناصر الدوسري", "CM", 8.2f),
                    PlayerLineup(28, "محمد كنو", "CM", 8.3f),
                    PlayerLineup(99, "عبد الله الحمدان", "ST", 7.9f),
                    PlayerLineup(15, "محمد القحطاني", "RW", 7.8f)
                )
                TeamLineup("4-2-3-1 هجوم شامل", "جورجي جيسوس", starters, subs)
            }
            "النصر" -> {
                val starters = listOf(
                    PlayerLineup(24, "بينتو ماتيوس", "GK", 8.4f, false, 0.5f, 0.90f),
                    PlayerLineup(2, "سلطان الغنام", "RB", 8.5f, false, 0.85f, 0.72f),
                    PlayerLineup(3, "محمد سيماكان", "CB", 8.6f, false, 0.62f, 0.76f),
                    PlayerLineup(27, "إيمريك لابورت", "CB", 8.9f, false, 0.38f, 0.76f),
                    PlayerLineup(12, "نواف بوشل", "LB", 8.0f, false, 0.15f, 0.72f),
                    PlayerLineup(17, "عبد الله الخيبري", "CDM", 8.2f, false, 0.5f, 0.55f),
                    PlayerLineup(25, "أوتافيو مونتيرو", "CM", 8.8f, false, 0.70f, 0.44f),
                    PlayerLineup(94, "أندرسون تاليسكا", "RW", 8.8f, false, 0.84f, 0.20f),
                    PlayerLineup(20, "أنجيلو غابرييل", "CAM", 8.3f, false, 0.5f, 0.32f),
                    PlayerLineup(10, "ساديو ماني", "LW", 9.0f, false, 0.16f, 0.20f),
                    PlayerLineup(7, "كريستيانو رونالدو", "ST", 9.6f, true, 0.5f, 0.14f)
                )
                val subs = listOf(
                    PlayerLineup(36, "راغد النجار", "GK", 7.6f),
                    PlayerLineup(4, "محمد آل فتيل", "CB", 7.7f),
                    PlayerLineup(78, "علي لاجامي", "CB", 8.0f),
                    PlayerLineup(19, "علي الحسن", "CM", 7.8f),
                    PlayerLineup(29, "عبد الرحمن غريب", "LW", 8.2f),
                    PlayerLineup(16, "مروان الصحفي", "RW", 7.9f),
                    PlayerLineup(30, "محمد مران", "ST", 7.7f)
                )
                TeamLineup("4-2-3-1 عمودي سريع", "ستيفانو بيولي", starters, subs)
            }
            else -> {
                val starters = listOf(
                    PlayerLineup(1, "حارس المرمى", "GK", 7.8f, false, 0.5f, 0.90f),
                    PlayerLineup(2, "الظهير الأيمن", "RB", 7.5f, false, 0.85f, 0.72f),
                    PlayerLineup(4, "قلب الدفاع 1", "CB", 7.9f, true, 0.62f, 0.76f),
                    PlayerLineup(3, "قلب الدفاع 2", "CB", 7.8f, false, 0.38f, 0.76f),
                    PlayerLineup(12, "الظهير الأيسر", "LB", 7.6f, false, 0.15f, 0.72f),
                    PlayerLineup(6, "محور الارتكاز", "CDM", 8.0f, false, 0.5f, 0.55f),
                    PlayerLineup(8, "لاعب الوسط", "CM", 8.2f, false, 0.70f, 0.44f),
                    PlayerLineup(10, "صانع الألعاب", "CAM", 8.5f, false, 0.5f, 0.32f),
                    PlayerLineup(7, "الجناح الأيمن", "RW", 8.4f, false, 0.84f, 0.20f),
                    PlayerLineup(9, "المهاجم الصريح", "ST", 8.7f, false, 0.5f, 0.14f),
                    PlayerLineup(11, "الجناح الأيسر", "LW", 8.4f, false, 0.16f, 0.20f)
                )
                val subs = listOf(
                    PlayerLineup(13, "حارس بديل", "GK", 7.5f),
                    PlayerLineup(14, "مدافع بديل", "CB", 7.4f),
                    PlayerLineup(15, "ظهير بديل", "RB", 7.3f),
                    PlayerLineup(16, "وسط بديل", "CM", 7.6f),
                    PlayerLineup(17, "جناح بديل", "RW", 7.8f),
                    PlayerLineup(18, "مهاجم بديل", "ST", 7.9f)
                )
                TeamLineup(formation, coach, starters, subs)
            }
        }
    }

    /**
     * 50+ Leagues across Tier 1, Tier 2, Tier 3, and Tier 4 (All Sports)
     */
    fun generate50PlusLeagues(): List<League> {
        val standingsPL = listOf(
            StandingRow(1, "ليفربول", "", 28, 20, 5, 3, 64, 25, 39, 65),
            StandingRow(2, "مانشستر سيتي", "", 28, 19, 6, 3, 62, 28, 34, 63),
            StandingRow(3, "أرسنال", "", 28, 18, 7, 3, 58, 22, 36, 61),
            StandingRow(4, "تشيلسي", "", 28, 14, 7, 7, 49, 36, 13, 49),
            StandingRow(5, "نيوكاسل", "", 28, 13, 6, 9, 44, 38, 6, 45),
            StandingRow(6, "مانشستر يونايتد", "", 28, 11, 7, 10, 38, 39, -1, 40)
        )

        val topScorersPL = listOf(
            TopScorer(1, "محمد صلاح", "ليفربول", 21, 11),
            TopScorer(2, "إيرلينغ هالاند", "مانشستر سيتي", 20, 4),
            TopScorer(3, "كول بالمر", "تشيلسي", 16, 8),
            TopScorer(4, "بوكايو ساكا", "أرسنال", 14, 10)
        )

        val standingsLaLiga = listOf(
            StandingRow(1, "ريال مدريد", "", 28, 21, 4, 3, 66, 21, 45, 67),
            StandingRow(2, "برشلونة", "", 28, 20, 4, 4, 71, 28, 43, 64),
            StandingRow(3, "أتلتيكو مدريد", "", 28, 17, 7, 4, 50, 20, 30, 58),
            StandingRow(4, "أتلتيك بيلباو", "", 28, 14, 8, 6, 45, 29, 16, 50)
        )

        val topScorersLaLiga = listOf(
            TopScorer(1, "كيليان مبابي", "ريال مدريد", 19, 5),
            TopScorer(2, "روبرت ليفاندوفسكي", "برشلونة", 18, 4),
            TopScorer(3, "فينيسيوس جونيور", "ريال مدريد", 14, 8),
            TopScorer(4, "لامين يامال", "برشلونة", 9, 12)
        )

        return listOf(
            // Tier 1: Elite
            League("ucl", "دوري أبطال أوروبا (UCL)", "أوروبا", "football", LeagueTier.TIER_1_ELITE, standings = standingsPL.take(4)),
            League("pl", "الدوري الإنجليزي الممتاز (Premier League)", "إنجلترا", "football", LeagueTier.TIER_1_ELITE, standings = standingsPL, topScorers = topScorersPL),
            League("laliga", "الدوري الإسباني (LaLiga EA Sports)", "إسبانيا", "football", LeagueTier.TIER_1_ELITE, standings = standingsLaLiga, topScorers = topScorersLaLiga),
            League("seriea", "الدوري الإيطالي (Serie A)", "إيطاليا", "football", LeagueTier.TIER_1_ELITE, standings = standingsPL.take(4)),
            League("bundesliga", "الدوري الألماني (Bundesliga)", "ألمانيا", "football", LeagueTier.TIER_1_ELITE, standings = standingsPL.take(4)),
            League("spl", "دوري روشن السعودي للمحترفين (SPL)", "السعودية", "football", LeagueTier.TIER_1_ELITE, standings = standingsPL.take(4)),
            League("worldcup", "كأس العالم FIFA", "العالم", "football", LeagueTier.TIER_1_ELITE),

            // Tier 2: Top
            League("ligue1", "الدوري الفرنسي (Ligue 1 McDonald's)", "فرنسا", "football", LeagueTier.TIER_2_TOP),
            League("uel", "الدوري الأوروبي (UEFA Europa League)", "أوروبا", "football", LeagueTier.TIER_2_TOP),
            League("eredivisie", "الدوري الهولندي الممتاز (Eredivisie)", "هولندا", "football", LeagueTier.TIER_2_TOP),
            League("ligaportugal", "الدوري البرتغالي (Liga Portugal Betclic)", "البرتغال", "football", LeagueTier.TIER_2_TOP),
            League("afc_elite", "دوري أبطال آسيا للنخبة (AFC Elite)", "آسيا", "football", LeagueTier.TIER_2_TOP),
            League("caf_cl", "دوري أبطال أفريقيا (CAF Champions League)", "أفريقيا", "football", LeagueTier.TIER_2_TOP),
            League("egypt_pl", "الدوري المصري الممتاز (نيل)", "مصر", "football", LeagueTier.TIER_2_TOP),
            League("morocco_botola", "الدوري المغربي للمحترفين (البطولة برو)", "المغرب", "football", LeagueTier.TIER_2_TOP),
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
