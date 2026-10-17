package com.example.model

enum class StreamFormat(val labelAr: String, val badgeColor: Long) {
    UHD_4K("4K Ultra HD", 0xFFFFD700),
    FULL_HD("1080p HD", 0xFF5BC8FF),
    OTT_STREAM("بث رقمي OTT", 0xFF4FE3C1),
    FREE_TO_AIR("مفتوح مجاني", 0xFF69F0AE)
}

enum class RegionZone(val code: String, val nameAr: String, val flag: String) {
    ALL("ALL", "جميع المناطق", "🌐"),
    MENA("MENA", "الشرق الأوسط وشمال إفريقيا", "🇸🇦"),
    UK("UK", "المملكة المتحدة", "🇬🇧"),
    USA("USA", "الولايات المتحدة", "🇺🇸"),
    SPAIN("ES", "إسبانيا", "🇪🇸"),
    GERMANY("DE", "ألمانيا", "🇩🇪"),
    FRANCE("FR", "فرنسا", "🇫🇷"),
    INTERNATIONAL("GLOBAL", "العالمي", "🌍")
}

data class Broadcaster(
    val id: String,
    val name: String,
    val channelNumberOrPackage: String,
    val region: RegionZone,
    val format: StreamFormat,
    val commentators: List<String> = emptyList(),
    val language: String = "العربية",
    val officialPlatformUrl: String = "https://www.beinsports.com",
    val appPackage: String = "tv.beinsports.connect",
    val isOfficialLicenseVerified: Boolean = true
)
