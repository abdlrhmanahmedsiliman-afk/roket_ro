package com.example.model

/**
 * Explicit Data Source Attribution for Transparency.
 * In accordance with strict integrity guidelines: Demo/Estimated data is never
 * disguised as official.
 */
enum class DataSourceType(val labelAr: String, val badgeColor: Long) {
    LIVE_API("Live API • مصدر مباشر", 0xFF4FE3C1),
    CACHED("Cached • مخزن محلياً", 0xFF5BC8FF),
    ESTIMATED_DEMO("تقديري/تجريبي — نموذج توضيحي", 0xFFFFB74D),
    UNAVAILABLE("غير متاح رسمياً من المصدر", 0xFFFF5252)
}
