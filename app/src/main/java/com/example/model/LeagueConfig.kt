package com.example.model

/**
 * Real Multi-Tier League Configuration Catalog.
 * Contains 40+ leagues across Arab regions and world sports.
 */
data class LeagueItemConfig(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val countryAr: String,
    val sport: String,
    val tier: LeagueTier,
    val logoUrl: String = "",
    val espnLeagueCode: String = "",
    val theSportsDbId: String = ""
)

object LeagueCatalog {
    val allLeagues: List<LeagueItemConfig> = listOf(
        // Tier 1: Elite International
        LeagueItemConfig("ucl", "دوري أبطال أوروبا", "UEFA Champions League", "أوروبا", "football", LeagueTier.TIER_1_ELITE, espnLeagueCode = "uefa.champions"),
        LeagueItemConfig("pl", "الدوري الإنجليزي الممتاز", "Premier League", "إنجلترا", "football", LeagueTier.TIER_1_ELITE, espnLeagueCode = "eng.1"),
        LeagueItemConfig("laliga", "الدوري الإسباني (LaLiga)", "La Liga", "إسبانيا", "football", LeagueTier.TIER_1_ELITE, espnLeagueCode = "esp.1"),
        LeagueItemConfig("seriea", "الدوري الإيطالي (Serie A)", "Serie A", "إيطاليا", "football", LeagueTier.TIER_1_ELITE, espnLeagueCode = "ita.1"),
        LeagueItemConfig("bundesliga", "الدوري الألماني (Bundesliga)", "Bundesliga", "ألمانيا", "football", LeagueTier.TIER_1_ELITE, espnLeagueCode = "ger.1"),
        LeagueItemConfig("spl", "دوري روشن السعودي للمحترفين", "Saudi Pro League", "السعودية", "football", LeagueTier.TIER_1_ELITE, espnLeagueCode = "sau.1"),
        LeagueItemConfig("worldcup", "كأس العالم FIFA", "FIFA World Cup", "العالم", "football", LeagueTier.TIER_1_ELITE),

        // Tier 2: Arab & Top Regional Leagues
        LeagueItemConfig("egypt_pl", "الدوري المصري الممتاز (نيل)", "Egyptian Premier League", "مصر", "football", LeagueTier.TIER_2_TOP, espnLeagueCode = "egy.1"),
        LeagueItemConfig("morocco_botola", "الدوري المغربي للمحترفين (البطولة برو)", "Botola Pro", "المغرب", "football", LeagueTier.TIER_2_TOP),
        LeagueItemConfig("tunisia_lp1", "الرابطة التونسية المحترفة الأولى", "Tunisian Ligue Professionnelle 1", "تونس", "football", LeagueTier.TIER_2_TOP),
        LeagueItemConfig("algeria_l1", "الرابطة الجزائرية المحترفة الأولى", "Algerian Ligue 1", "الجزائر", "football", LeagueTier.TIER_2_TOP),
        LeagueItemConfig("qatar_stars", "دوري نجوم قطر (QSL)", "Qatar Stars League", "قطر", "football", LeagueTier.TIER_2_TOP),
        LeagueItemConfig("uae_pro", "دوري أدنوك للمحترفين (الإمارات)", "UAE Pro League", "الإمارات", "football", LeagueTier.TIER_2_TOP),
        LeagueItemConfig("iraq_stars", "دوري نجوم العراق للمحترفين", "Iraq Stars League", "العراق", "football", LeagueTier.TIER_2_TOP),
        LeagueItemConfig("afc_elite", "دوري أبطال آسيا للنخبة", "AFC Champions League Elite", "آسيا", "football", LeagueTier.TIER_2_TOP),
        LeagueItemConfig("caf_cl", "دوري أبطال أفريقيا", "CAF Champions League", "أفريقيا", "football", LeagueTier.TIER_2_TOP),
        LeagueItemConfig("ligue1", "الدوري الفرنسي (Ligue 1)", "Ligue 1", "فرنسا", "football", LeagueTier.TIER_2_TOP, espnLeagueCode = "fra.1"),
        LeagueItemConfig("uel", "الدوري الأوروبي (Europa League)", "UEFA Europa League", "أوروبا", "football", LeagueTier.TIER_2_TOP, espnLeagueCode = "uefa.europa"),
        LeagueItemConfig("eredivisie", "الدوري الهولندي الممتاز", "Eredivisie", "هولندا", "football", LeagueTier.TIER_2_TOP, espnLeagueCode = "ned.1"),
        LeagueItemConfig("ligaportugal", "الدوري البرتغالي الممتاز", "Liga Portugal", "البرتغال", "football", LeagueTier.TIER_2_TOP, espnLeagueCode = "por.1"),

        // Tier 3: Major Continental & Cups
        LeagueItemConfig("brasileirao", "الدوري البرازيلي (Brasileirão)", "Brasileirão Série A", "البرازيل", "football", LeagueTier.TIER_3_MAJOR, espnLeagueCode = "bra.1"),
        LeagueItemConfig("mls", "الدوري الأمريكي للمحترفين (MLS)", "Major League Soccer", "أمريكا", "football", LeagueTier.TIER_3_MAJOR, espnLeagueCode = "usa.1"),
        LeagueItemConfig("arg_liga", "الدوري الأرجنتيني الممتاز", "Liga Profesional Argentina", "الأرجنتين", "football", LeagueTier.TIER_3_MAJOR, espnLeagueCode = "arg.1"),
        LeagueItemConfig("libertadores", "كأس ليبرتادوريس", "Copa Libertadores", "أمريكا الجنوبية", "football", LeagueTier.TIER_3_MAJOR),
        LeagueItemConfig("uecl", "دوري المؤتمر الأوروبي", "UEFA Conference League", "أوروبا", "football", LeagueTier.TIER_3_MAJOR),
        LeagueItemConfig("superlig", "الدوري التركي الممتاز", "Süper Lig", "تركيا", "football", LeagueTier.TIER_3_MAJOR, espnLeagueCode = "tur.1"),
        LeagueItemConfig("scottish_prem", "الدوري الإسكتلندي الممتاز", "Scottish Premiership", "إسكتلندا", "football", LeagueTier.TIER_3_MAJOR),
        LeagueItemConfig("king_cup", "كأس خادم الحرمين الشريفين", "King Cup", "السعودية", "football", LeagueTier.TIER_3_MAJOR),
        LeagueItemConfig("fa_cup", "كأس الاتحاد الإنجليزي (FA Cup)", "FA Cup", "إنجلترا", "football", LeagueTier.TIER_3_MAJOR),
        LeagueItemConfig("copa_del_rey", "كأس ملك إسبانيا", "Copa del Rey", "إسبانيا", "football", LeagueTier.TIER_3_MAJOR),

        // Tier 4: Multi-Sports
        LeagueItemConfig("nba", "دوري كرة السلة الأمريكي (NBA)", "NBA Basketball", "أمريكا", "basketball", LeagueTier.TIER_4_ALL_SPORTS, espnLeagueCode = "basketball/nba"),
        LeagueItemConfig("euroleague", "الدوري الأوروبي لكرة السلة", "EuroLeague", "أوروبا", "basketball", LeagueTier.TIER_4_ALL_SPORTS),
        LeagueItemConfig("fiba_wc", "كأس العالم لكرة السلة FIBA", "FIBA Basketball World Cup", "العالم", "basketball", LeagueTier.TIER_4_ALL_SPORTS),
        LeagueItemConfig("wimbledon", "بطولة ويمبلدون للتنس", "Wimbledon", "بريطانيا", "tennis", LeagueTier.TIER_4_ALL_SPORTS),
        LeagueItemConfig("roland_garros", "بطولة رولان غاروس", "Roland Garros", "فرنسا", "tennis", LeagueTier.TIER_4_ALL_SPORTS),
        LeagueItemConfig("f1_world", "بطولة العالم للفورمولا 1 (FIA F1)", "Formula 1 World Championship", "العالم", "f1", LeagueTier.TIER_4_ALL_SPORTS),
        LeagueItemConfig("ipl", "الدوري الهندي للمحترفين للكريكيت (IPL)", "IPL Cricket", "الهند", "cricket", LeagueTier.TIER_4_ALL_SPORTS),
        LeagueItemConfig("ufc", "منظمة الفنون القتالية المختلطة (UFC)", "Ultimate Fighting Championship", "العالم", "mma", LeagueTier.TIER_4_ALL_SPORTS),
        LeagueItemConfig("nfl", "الدوري الأمريكي لكرة القدم (NFL)", "NFL", "أمريكا", "american_football", LeagueTier.TIER_4_ALL_SPORTS),
        LeagueItemConfig("nhl", "دوري الهوكي الوطني (NHL)", "NHL", "أمريكا", "ice_hockey", LeagueTier.TIER_4_ALL_SPORTS)
    )
}
