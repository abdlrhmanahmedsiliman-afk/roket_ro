package com.example.data.provider

import com.example.core.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** One past/upcoming match of a team, always from the team's point of view. */
data class HubMatch(
    val dateLabel: String,
    val opponent: String,
    val opponentLogo: String,
    val isHome: Boolean,
    val scoreText: String,
    val result: String,
    val competition: String
)

data class HubPlayer(
    val id: String,
    val name: String,
    val number: Int,
    val position: String,
    val age: Int,
    val nationality: String,
    val photoUrl: String
)

data class HubNews(val title: String, val source: String, val url: String, val timeAgo: String)

data class TeamHubData(
    val teamId: String,
    val name: String,
    val logo: String,
    val league: String,
    val stadium: String,
    val coach: String,
    val standing: String,
    val recent: List<HubMatch>,
    val upcoming: List<HubMatch>,
    val players: List<HubPlayer>,
    val news: List<HubNews>,
    val transferNews: List<HubNews>,
    /** false = the team could not be matched inside ESPN (only news is shown). */
    val resolved: Boolean
)

/**
 * Real team page data. Everything comes from the SAME ESPN team id (logo, players, photos, fixtures),
 * so a logo/photo can never belong to a different team/player. Nothing here is invented:
 * what the source does not provide is left empty and the UI says "unavailable".
 */
object EspnTeamApi {
    private const val BASE = "https://site.api.espn.com/apis/site/v2/sports/soccer"
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val teamsCache = ConcurrentHashMap<String, List<Triple<String, String, String>>>()
    private val hubCache = ConcurrentHashMap<String, Pair<Long, TeamHubData>>()

    private val allSlugs = listOf(
        "eng.1", "esp.1", "ita.1", "ger.1", "fra.1", "uefa.champions", "uefa.europa", "sau.1", "egy.1",
        "ned.1", "por.1", "tur.1", "caf.champions", "afc.champions", "sco.1", "bel.1", "usa.1", "bra.1", "arg.1"
    )

    private fun http(url: String): String? = try {
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Android; Mobile)").build()
        client.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
    } catch (_: Exception) {
        null
    }

    private fun parseObj(s: String): JSONObject? = try {
        JSONObject(s)
    } catch (_: Exception) {
        null
    }

    private fun norm(s: String): String {
        var x = s.lowercase()
        for (w in listOf(" fc", " cf", " afc", " sc", " ac", "fc ", "cf ", "ac ", "sc ")) x = x.replace(w, " ")
        return x.filter { it.isLetterOrDigit() }
    }

    private fun slugHints(leagueName: String): List<String> {
        val n = leagueName.lowercase()
        val hints = ArrayList<String>()
        fun add(s: String) {
            if (s !in hints) hints.add(s)
        }
        if (n.contains("premier") || n.contains("الإنجليزي")) add("eng.1")
        if (n.contains("laliga") || n.contains("الإسباني")) add("esp.1")
        if (n.contains("serie a") || n.contains("الإيطالي")) add("ita.1")
        if (n.contains("bundesliga") || n.contains("الألماني")) add("ger.1")
        if (n.contains("ligue 1") || n.contains("الفرنسي")) add("fra.1")
        if (n.contains("ucl") || n.contains("champions") || n.contains("أبطال أوروبا")) add("uefa.champions")
        if (n.contains("europa") || n.contains("الأوروبي")) add("uefa.europa")
        if (n.contains("روشن") || n.contains("السعودي")) add("sau.1")
        if (n.contains("المصري") || n.contains("egypt")) add("egy.1")
        for (s in allSlugs) add(s)
        return hints
    }

    /** (id, displayName, logo) of every team of a competition. */
    private fun teamsOf(slug: String): List<Triple<String, String, String>> {
        teamsCache[slug]?.let { return it }
        val body = http("$BASE/$slug/teams?limit=200") ?: return emptyList()
        val out = ArrayList<Triple<String, String, String>>()
        try {
            val leagues = JSONObject(body).optJSONArray("sports")?.optJSONObject(0)?.optJSONArray("leagues")
            val teams = leagues?.optJSONObject(0)?.optJSONArray("teams") ?: JSONArray()
            for (i in 0 until teams.length()) {
                val t = teams.optJSONObject(i)?.optJSONObject("team") ?: continue
                val logo = t.optJSONArray("logos")?.optJSONObject(0)?.optString("href", "") ?: ""
                out.add(Triple(t.optString("id"), t.optString("displayName"), logo))
            }
        } catch (_: Exception) {
            return emptyList()
        }
        if (out.isNotEmpty()) teamsCache[slug] = out
        return out
    }

    private data class Resolved(val slug: String, val id: String, val name: String, val logo: String)

    private fun resolve(teamName: String, leagueName: String): Resolved? {
        val slugs = slugHints(leagueName).take(8)
        val target = norm(teamName)
        if (target.isEmpty()) return null
        for (pass in 0..1) {
            for (slug in slugs) {
                for ((id, name, logo) in teamsOf(slug)) {
                    val n = norm(name)
                    val ok = if (pass == 0) n == target else (n.length >= 4 && target.length >= 4 && (n.contains(target) || target.contains(n)))
                    if (ok) return Resolved(slug, id, name, logo)
                }
            }
        }
        return null
    }

    private fun posOrder(pos: String): Int = when (pos.uppercase()) {
        "G", "GK" -> 0
        "D", "CB", "LB", "RB", "DF" -> 1
        "M", "CM", "DM", "AM", "MF" -> 2
        "F", "FW", "ST", "A", "LW", "RW" -> 3
        else -> 4
    }

    private fun parsePlayers(roster: JSONObject?): List<HubPlayer> {
        val out = ArrayList<HubPlayer>()
        val arr = roster?.optJSONArray("athletes") ?: return out
        fun addOne(a: JSONObject) {
            val name = a.optString("fullName", a.optString("displayName", ""))
            if (name.isBlank()) return
            val id = a.optString("id", "")
            val head = a.optJSONObject("headshot")?.optString("href", "") ?: ""
            val photo = if (head.isNotBlank()) head else if (id.isNotEmpty() && id.all { it.isDigit() }) "https://a.espncdn.com/i/headshots/soccer/players/full/$id.png" else ""
            out.add(
                HubPlayer(
                    id = id,
                    name = name,
                    number = a.optString("jersey", "").toIntOrNull() ?: 0,
                    position = a.optJSONObject("position")?.optString("abbreviation", "") ?: "",
                    age = a.optInt("age", 0),
                    nationality = a.optString("citizenship", ""),
                    photoUrl = photo
                )
            )
        }
        for (i in 0 until arr.length()) {
            val el = arr.optJSONObject(i) ?: continue
            val items = el.optJSONArray("items")
            if (items != null) {
                for (j in 0 until items.length()) items.optJSONObject(j)?.let { addOne(it) }
            } else {
                addOne(el)
            }
        }
        return out.sortedWith(compareBy({ posOrder(it.position) }, { it.number }))
    }

    private fun scoreOf(c: JSONObject): Int? {
        val s = c.opt("score")
        return when (s) {
            is JSONObject -> s.optString("displayValue", "").toIntOrNull()
            is String -> s.toIntOrNull()
            is Number -> s.toInt()
            else -> null
        }
    }

    private fun dateLabel(millis: Long): String {
        if (millis <= 0L) return ""
        val k = TimeUtil.localDateKey(millis)
        return TimeUtil.dayNameAr(k) + " " + TimeUtil.dayOfMonth(k) + " " + TimeUtil.monthNameAr(k) + " • " + TimeUtil.formatTime(millis)
    }

    private fun parseSchedule(schedule: JSONObject?, teamId: String): Pair<List<HubMatch>, List<HubMatch>> {
        val events = schedule?.optJSONArray("events") ?: return Pair(emptyList(), emptyList())
        val done = ArrayList<Pair<Long, HubMatch>>()
        val next = ArrayList<Pair<Long, HubMatch>>()
        for (i in 0 until events.length()) {
            val ev = events.optJSONObject(i) ?: continue
            val comp = ev.optJSONArray("competitions")?.optJSONObject(0) ?: continue
            val comps = comp.optJSONArray("competitors") ?: continue
            var me: JSONObject? = null
            var opp: JSONObject? = null
            for (k in 0 until comps.length()) {
                val c = comps.optJSONObject(k) ?: continue
                if (c.optJSONObject("team")?.optString("id") == teamId) me = c else opp = c
            }
            if (me == null || opp == null) continue
            val millis = TimeUtil.parseIsoMillis(ev.optString("date", ""))
            val completed = comp.optJSONObject("status")?.optJSONObject("type")?.optBoolean("completed", false) ?: false
            val oppTeam = opp.optJSONObject("team")
            val oppName = oppTeam?.optString("displayName", "") ?: ""
            val oppLogo = oppTeam?.optJSONArray("logos")?.optJSONObject(0)?.optString("href", "") ?: oppTeam?.optString("logo", "") ?: ""
            val mine = scoreOf(me)
            val theirs = scoreOf(opp)
            val comp2 = ev.optJSONObject("league")?.optString("name", "") ?: ""
            val isHome = me.optString("homeAway", "") == "home"
            if (completed && mine != null && theirs != null) {
                val res = if (mine > theirs) "W" else if (mine == theirs) "D" else "L"
                done.add(Pair(millis, HubMatch(dateLabel(millis), oppName, oppLogo, isHome, "$mine - $theirs", res, comp2)))
            } else if (!completed) {
                next.add(Pair(millis, HubMatch(dateLabel(millis), oppName, oppLogo, isHome, "", "", comp2)))
            }
        }
        val recent = done.sortedByDescending { it.first }.take(6).map { it.second }
        val upcoming = next.sortedBy { it.first }.take(6).map { it.second }
        return Pair(recent, upcoming)
    }

    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val RFC = Regex("(\\d{1,2}) ([A-Za-z]{3}) (\\d{4}) (\\d{2}):(\\d{2}):(\\d{2})")

    private fun timeAgo(pub: String): String {
        val m = RFC.find(pub) ?: return ""
        val mo = MONTHS.indexOf(m.groupValues[2]) + 1
        if (mo <= 0) return ""
        val days = TimeUtil.daysFromCivil(m.groupValues[3].toInt(), mo, m.groupValues[1].toInt())
        val ms = days * 86_400_000L + m.groupValues[4].toLong() * 3_600_000L + m.groupValues[5].toLong() * 60_000L + m.groupValues[6].toLong() * 1_000L
        val diff = System.currentTimeMillis() - ms
        if (diff < 0L) return "الآن"
        val min = diff / 60_000L
        return when {
            min < 1L -> "الآن"
            min < 60L -> "قبل $min دقيقة"
            min < 1440L -> "قبل ${min / 60L} ساعة"
            else -> "قبل ${min / 1440L} يوم"
        }
    }

    /** Google News RSS (free, no key). Items link to the original publisher. */
    private fun fetchNews(query: String): List<HubNews> {
        val url = "https://news.google.com/rss/search?q=" + URLEncoder.encode(query, "UTF-8") + "&hl=ar&gl=EG&ceid=EG:ar"
        val xml = http(url) ?: return emptyList()
        val out = ArrayList<HubNews>()
        try {
            val p = android.util.Xml.newPullParser()
            p.setInput(StringReader(xml))
            var ev = p.eventType
            var inItem = false
            var tag = ""
            var title = ""
            var link = ""
            var pub = ""
            var src = ""
            while (ev != XmlPullParser.END_DOCUMENT && out.size < 12) {
                when (ev) {
                    XmlPullParser.START_TAG -> {
                        tag = p.name ?: ""
                        if (tag == "item") {
                            inItem = true; title = ""; link = ""; pub = ""; src = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inItem) {
                            val t = p.text ?: ""
                            when (tag) {
                                "title" -> title += t
                                "link" -> link += t
                                "pubDate" -> pub += t
                                "source" -> src += t
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (p.name == "item") {
                            inItem = false
                            if (title.isNotBlank() && link.isNotBlank()) {
                                val s = src.trim()
                                val clean = if (s.isNotEmpty() && title.endsWith(" - $s")) title.removeSuffix(" - $s") else title
                                out.add(HubNews(clean.trim(), s, link.trim(), timeAgo(pub)))
                            }
                        }
                        tag = ""
                    }
                }
                ev = p.next()
            }
        } catch (_: Exception) {
        }
        return out
    }

    suspend fun load(teamName: String, leagueName: String): TeamHubData = withContext(Dispatchers.IO) {
        val key = norm(teamName)
        val cached = hubCache[key]
        if (cached != null && System.currentTimeMillis() - cached.first < 10 * 60_000L && cached.second.resolved) {
            return@withContext cached.second
        }
        coroutineScope {
            val newsJob = async { fetchNews("$teamName كرة القدم") }
            val transferJob = async { fetchNews("$teamName انتقالات OR صفقة OR transfer") }
            val r = resolve(teamName, leagueName)
            var info: JSONObject? = null
            var roster: JSONObject? = null
            var schedule: JSONObject? = null
            if (r != null) {
                val infoJob = async { http("$BASE/${r.slug}/teams/${r.id}")?.let { parseObj(it) } }
                val rosterJob = async { http("$BASE/${r.slug}/teams/${r.id}/roster")?.let { parseObj(it) } }
                val schedJob = async { http("$BASE/${r.slug}/teams/${r.id}/schedule")?.let { parseObj(it) } }
                info = infoJob.await()
                roster = rosterJob.await()
                schedule = schedJob.await()
            }
            val teamObj = info?.optJSONObject("team")
            val coachObj = roster?.optJSONArray("coach")?.optJSONObject(0)
            val coach = if (coachObj != null) (coachObj.optString("firstName", "") + " " + coachObj.optString("lastName", "")).trim() else ""
            val (recent, upcoming) = if (r != null) parseSchedule(schedule, r.id) else Pair(emptyList<HubMatch>(), emptyList<HubMatch>())
            val data = TeamHubData(
                teamId = r?.id ?: "",
                name = r?.name ?: teamName,
                logo = r?.logo ?: "",
                league = leagueName,
                stadium = teamObj?.optJSONObject("venue")?.optString("fullName", "") ?: "",
                coach = coach,
                standing = teamObj?.optString("standingSummary", "") ?: "",
                recent = recent,
                upcoming = upcoming,
                players = parsePlayers(roster),
                news = newsJob.await(),
                transferNews = transferJob.await(),
                resolved = r != null
            )
            if (data.resolved) hubCache[key] = Pair(System.currentTimeMillis(), data)
            data
        }
    }
}
