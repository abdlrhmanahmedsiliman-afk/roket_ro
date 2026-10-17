package com.example.data.local

import com.example.model.*

fun MatchEntity.toDomainModel(
    events: List<MatchEvent> = emptyList(),
    homeLineup: TeamLineup? = null,
    awayLineup: TeamLineup? = null,
    h2h: HeadToHead = HeadToHead()
): Match {
    val matchStatus = when (status) {
        "LIVE" -> MatchStatus.LIVE
        "HT" -> MatchStatus.HALF_TIME
        "FT" -> MatchStatus.FINISHED
        "ET" -> MatchStatus.EXTRA_TIME
        "PEN" -> MatchStatus.PENALTIES
        else -> MatchStatus.NOT_STARTED
    }

    return Match(
        id = id,
        sport = sport,
        league = league,
        country = country,
        homeTeam = homeTeam,
        awayTeam = awayTeam,
        homeLogo = homeLogo,
        awayLogo = awayLogo,
        homeScore = homeScore,
        awayScore = awayScore,
        status = matchStatus,
        minute = minute,
        kickoffTime = kickoffTime,
        venue = venue,
        referee = referee,
        homeWinProb = homeWinProb,
        drawProb = drawProb,
        awayWinProb = awayWinProb,
        aiPreview = aiPreview,
        aiLiveTactical = aiLiveTactical,
        aiPostReport = aiPostReport,
        stats = MatchStats(
            possessionHome = possessionHome,
            possessionAway = possessionAway,
            xgHome = xgHome,
            xgAway = xgAway,
            shotsHome = shotsHome,
            shotsAway = shotsAway,
            shotsOnTargetHome = shotsOnTargetHome,
            shotsOnTargetAway = shotsOnTargetAway,
            bigChancesHome = bigChancesHome,
            bigChancesAway = bigChancesAway,
            passAccuracyHome = passAccuracyHome,
            passAccuracyAway = passAccuracyAway,
            cornersHome = cornersHome,
            cornersAway = cornersAway,
            foulsHome = foulsHome,
            foulsAway = foulsAway
        ),
        events = events,
        homeLineup = homeLineup,
        awayLineup = awayLineup,
        h2h = h2h,
        isFavorite = isFavorite
    )
}

fun Match.toEntity(): MatchEntity {
    return MatchEntity(
        id = id,
        sport = sport,
        league = league,
        country = country,
        homeTeam = homeTeam,
        awayTeam = awayTeam,
        homeLogo = homeLogo,
        awayLogo = awayLogo,
        homeScore = homeScore,
        awayScore = awayScore,
        status = status.code,
        minute = minute,
        kickoffTime = kickoffTime,
        venue = venue,
        referee = referee,
        homeWinProb = homeWinProb,
        drawProb = drawProb,
        awayWinProb = awayWinProb,
        aiPreview = aiPreview,
        aiLiveTactical = aiLiveTactical,
        aiPostReport = aiPostReport,
        possessionHome = stats.possessionHome,
        possessionAway = stats.possessionAway,
        xgHome = stats.xgHome,
        xgAway = stats.xgAway,
        shotsHome = stats.shotsHome,
        shotsAway = stats.shotsAway,
        shotsOnTargetHome = stats.shotsOnTargetHome,
        shotsOnTargetAway = stats.shotsOnTargetAway,
        bigChancesHome = stats.bigChancesHome,
        bigChancesAway = stats.bigChancesAway,
        passAccuracyHome = stats.passAccuracyHome,
        passAccuracyAway = stats.passAccuracyAway,
        cornersHome = stats.cornersHome,
        cornersAway = stats.cornersAway,
        foulsHome = stats.foulsHome,
        foulsAway = stats.foulsAway,
        isFavorite = isFavorite,
        updatedAt = System.currentTimeMillis()
    )
}
