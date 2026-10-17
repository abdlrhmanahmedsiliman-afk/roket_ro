package com.example.model

enum class SportType(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val emoji: String,
    val iconName: String
) {
    ALL("all", "الكل", "All", "🌟", "all"),
    FOOTBALL("football", "كرة القدم", "Football", "⚽", "sports_soccer"),
    BASKETBALL("basketball", "كرة السلة", "Basketball", "🏀", "sports_basketball"),
    TENNIS("tennis", "التنس", "Tennis", "🎾", "sports_tennis"),
    CRICKET("cricket", "الكريكيت", "Cricket", "🏏", "sports_cricket"),
    NFL("american-football", "NFL", "NFL", "🏈", "sports_football"),
    NHL("hockey", "الهوكي", "NHL", "🏒", "sports_hockey"),
    BASEBALL("baseball", "البيسبول", "Baseball", "⚾", "sports_baseball"),
    MMA("mma", "فنون قتالية", "MMA", "🥊", "sports_mma"),
    VOLLEYBALL("volleyball", "الكرة الطائرة", "Volleyball", "🏐", "sports_volleyball"),
    HANDBALL("handball", "كرة اليد", "Handball", "🤾", "sports_handball"),
    RUGBY("rugby", "الرجبي", "Rugby", "🏉", "sports_rugby"),
    F1("formula-1", "فورمولا 1", "Formula 1", "🏎️", "sports_motorsports");

    companion object {
        fun fromId(id: String): SportType {
            return entries.firstOrNull { it.id == id } ?: FOOTBALL
        }
    }
}
