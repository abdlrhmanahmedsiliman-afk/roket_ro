package com.example.core

import java.util.Locale
import java.util.TimeZone

/**
 * Thread-safe date/time helpers.
 *
 * Why this exists: SimpleDateFormat is NOT thread-safe, and the app parses ESPN/TheSportsDB
 * dates from ~40 parallel coroutines. Everything here is pure arithmetic (no shared mutable
 * state), and the device time zone is read on every call so the user always sees match times in
 * the zone the phone is currently in (Egypt, USA, ...). No location permission is needed.
 */
object TimeUtil {
    private const val DAY_MS = 86_400_000L
    private const val HOUR_MS = 3_600_000L
    private const val MIN_MS = 60_000L

    private val ISO = Regex(
        "(\\d{4})-(\\d{2})-(\\d{2})(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2})(?:[.,](\\d{1,9}))?)?)?\\s*(Z|z|[+-]\\d{2}(?::?\\d{2})?)?"
    )

    private val DAY_NAMES_AR = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    private val MONTH_NAMES_AR = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private fun floorDiv(a: Long, b: Long): Long {
        var q = a / b
        if ((a % b != 0L) && ((a < 0L) != (b < 0L))) q -= 1L
        return q
    }

    private fun floorMod(a: Long, b: Long): Long {
        var r = a % b
        if (r < 0L) r += b
        return r
    }

    // ───────────── civil date <-> days since 1970-01-01 (proleptic Gregorian) ─────────────

    fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = if (month > 2) month - 3 else month + 9
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * 146097L + doe.toLong() - 719468L
    }

    /** Returns [year, month, day]. */
    fun civilFromDays(days: Long): IntArray {
        val z = days + 719468L
        val era = floorDiv(z, 146097L)
        val doe = (z - era * 146097L).toInt()
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe.toLong() + era * 400L
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val year = (if (m <= 2) y + 1L else y).toInt()
        return intArrayOf(year, m, d)
    }

    fun keyOf(year: Int, month: Int, day: Int): String =
        "%04d-%02d-%02d".format(Locale.US, year, month, day)

    /** Parses "yyyy-MM-dd" → [y, m, d] or null. */
    fun parseKey(key: String): IntArray? {
        if (key.length != 10 || key[4] != '-' || key[7] != '-') return null
        val y = key.substring(0, 4).toIntOrNull() ?: return null
        val m = key.substring(5, 7).toIntOrNull() ?: return null
        val d = key.substring(8, 10).toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        return intArrayOf(y, m, d)
    }

    // ───────────────────────────── ISO-8601 parsing ─────────────────────────────

    /**
     * Parses ISO-8601 ("2026-10-17T14:00Z", "...:00:00Z", "...:00.000+03:00", "...T14:00:00").
     * A missing zone is treated as UTC (TheSportsDB timestamps are UTC). Returns 0 on failure.
     */
    fun parseIsoMillis(text: String?): Long {
        if (text.isNullOrBlank()) return 0L
        val m = ISO.matchEntire(text.trim()) ?: return 0L
        val g = m.groupValues
        val y = g[1].toIntOrNull() ?: return 0L
        val mo = g[2].toIntOrNull() ?: return 0L
        val d = g[3].toIntOrNull() ?: return 0L
        if (mo !in 1..12 || d !in 1..31) return 0L
        val hh = if (g[4].isEmpty()) 0 else g[4].toInt()
        val mi = if (g[5].isEmpty()) 0 else g[5].toInt()
        val ss = if (g[6].isEmpty()) 0 else g[6].toInt()
        if (hh > 23 || mi > 59 || ss > 60) return 0L
        val ms = if (g[7].isEmpty()) 0 else (g[7] + "00").substring(0, 3).toInt()
        var offsetMin = 0
        val z = g[8]
        if (z.isNotEmpty() && z != "Z" && z != "z") {
            val sign = if (z[0] == '-') -1 else 1
            val digits = z.substring(1).replace(":", "")
            val oh = digits.substring(0, 2).toInt()
            val om = if (digits.length >= 4) digits.substring(2, 4).toInt() else 0
            offsetMin = sign * (oh * 60 + om)
        }
        val days = daysFromCivil(y, mo, d)
        return days * DAY_MS + hh * HOUR_MS + mi * MIN_MS + ss * 1_000L + ms.toLong() - offsetMin * MIN_MS
    }

    // ───────────────────── local time (device zone, read fresh each call) ─────────────────────

    private fun offsetAt(utcMillis: Long): Long = TimeZone.getDefault().getOffset(utcMillis).toLong()

    /** Local calendar day ("yyyy-MM-dd") of an instant, in the phone's current time zone. */
    fun localDateKey(utcMillis: Long): String {
        if (utcMillis <= 0L) return ""
        val c = civilFromDays(floorDiv(utcMillis + offsetAt(utcMillis), DAY_MS))
        return keyOf(c[0], c[1], c[2])
    }

    fun todayKey(): String = localDateKey(System.currentTimeMillis())

    fun shiftKey(dateKey: String, days: Int): String {
        val p = parseKey(dateKey) ?: return dateKey
        val c = civilFromDays(daysFromCivil(p[0], p[1], p[2]) + days.toLong())
        return keyOf(c[0], c[1], c[2])
    }

    /** UTC instant of local midnight (start of that day) in the phone's time zone. */
    fun localDayStartMillis(dateKey: String): Long {
        val p = parseKey(dateKey) ?: return 0L
        val localMidnight = daysFromCivil(p[0], p[1], p[2]) * DAY_MS
        val tz = TimeZone.getDefault()
        var utc = localMidnight - tz.getOffset(localMidnight).toLong()
        utc = localMidnight - tz.getOffset(utc).toLong()
        return utc
    }

    /** 0 = Sunday … 6 = Saturday. */
    fun dayOfWeek(dateKey: String): Int {
        val p = parseKey(dateKey) ?: return 0
        return floorMod(daysFromCivil(p[0], p[1], p[2]) + 4L, 7L).toInt()
    }

    fun dayNameAr(dateKey: String): String = DAY_NAMES_AR[dayOfWeek(dateKey)]

    fun dayOfMonth(dateKey: String): Int = parseKey(dateKey)?.get(2) ?: 0

    fun monthNameAr(dateKey: String): String {
        val m = parseKey(dateKey)?.get(1) ?: return ""
        return MONTH_NAMES_AR[m - 1]
    }

    /** Local time of an instant. 12h → "8:00 م" / "8:00 PM", 24h → "20:00". */
    fun formatTime(utcMillis: Long, arabic: Boolean = true, use24h: Boolean = false): String {
        if (utcMillis <= 0L) return "--:--"
        val msOfDay = floorMod(utcMillis + offsetAt(utcMillis), DAY_MS)
        val h = (msOfDay / HOUR_MS).toInt()
        val m = ((msOfDay / MIN_MS) % 60L).toInt()
        if (use24h) return "%02d:%02d".format(Locale.US, h, m)
        val h12 = if (h % 12 == 0) 12 else h % 12
        val suffix = if (arabic) (if (h < 12) "ص" else "م") else (if (h < 12) "AM" else "PM")
        return "%d:%02d %s".format(Locale.US, h12, m, suffix)
    }

    /** "GMT+03:00 • Africa/Cairo" */
    fun timezoneLabel(): String {
        val tz = TimeZone.getDefault()
        val offMin = tz.getOffset(System.currentTimeMillis()) / 60_000
        val sign = if (offMin < 0) "-" else "+"
        val a = if (offMin < 0) -offMin else offMin
        return "GMT" + sign + "%02d:%02d".format(Locale.US, a / 60, a % 60) + " • " + tz.id
    }

    /** True when [dateKey] is within the last [days] days (including today). */
    fun isFutureOrToday(dateKey: String): Boolean = dateKey >= todayKey()
}
