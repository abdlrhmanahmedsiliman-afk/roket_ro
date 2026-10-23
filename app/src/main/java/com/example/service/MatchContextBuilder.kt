package com.example.service

import com.example.model.DataSourceType
import com.example.model.Match
import com.example.model.MatchStatus

/**
 * Builds verified match context for AI generation.
 * Enforces zero-hallucination rules: explicitly distinguishes verified API facts
 * from unverified fields, specifies data sources, and prevents making up events.
 */
data class MatchVerifiedContext(
    val matchId: String,
    val contextVersion: String,
    val promptContext: String,
    val generatedAt: Long = System.currentTimeMillis()
)

object MatchContextBuilder {
    fun buildContext(match: Match, phase: String): MatchVerifiedContext {
        val version = "${match.id}_${match.status.code}_${match.homeScore}-${match.awayScore}_${match.minute}_${match.events.size}"

        val eventsSummary = if (match.events.isNotEmpty()) {
            match.events.joinToString("; ") { event ->
                "د${event.minute}': ${event.type.name} لـ ${event.player} (${if (event.teamIsHome) match.homeTeam else match.awayTeam}) [${event.detail}]"
            }
        } else {
            "لا توجد أحداث مسجلة بعد"
        }

        val startersHome = match.homeLineup?.starters?.joinToString(", ") { "#${it.number} ${it.name} (${it.position})" }
            ?: "التشكيل الرسمي غير متاح رسمياً (نموذج توضيحي)"

        val startersAway = match.awayLineup?.starters?.joinToString(", ") { "#${it.number} ${it.name} (${it.position})" }
            ?: "التشكيل الرسمي غير متاح رسمياً (نموذج توضيحي)"

        val prompt = """
            [معلومات المباراة الرسمية المؤكدة]:
            - المعرف: ${match.id}
            - البطولة: ${match.league} (${match.country})
            - الفريقان: ${match.homeTeam} (المستضيف) ضد ${match.awayTeam} (الضيف)
            - الحالة الحالية: ${match.status.labelAr} (الدقيقة: ${match.minute}')
            - النتيجة: ${match.homeScore} - ${match.awayScore}
            - الملعب: ${match.venue.ifBlank { "غير محدد" }}
            - الحكم: ${match.referee.ifBlank { "غير محدد" }}
            - مصدر البيانات: ${match.dataSource.labelAr} (آخر تحديث: ${match.lastUpdatedTimestamp})

            [الإحصائيات المؤكدة]:
            - الاستحواذ: ${match.stats.possessionHome}% لـ ${match.homeTeam} مقابل ${match.stats.possessionAway}% لـ ${match.awayTeam} (المصدر: ${match.statsSource.labelAr})
            - الأهداف المتوقعة (xG): ${match.stats.xgHome} مقابل ${match.stats.xgAway}
            - إجمالي التسديدات: ${match.stats.shotsHome} مقابل ${match.stats.shotsAway} (على المرمى: ${match.stats.shotsOnTargetHome} مقابل ${match.stats.shotsOnTargetAway})
            - الفرص الخطيرة: ${match.stats.bigChancesHome} مقابل ${match.stats.bigChancesAway}
            - البطاقات: صفراء (${match.stats.yellowCardsHome}-${match.stats.yellowCardsAway})، حمراء (${match.stats.redCardsHome}-${match.stats.redCardsAway})

            [قائمة الأحداث المؤكدة]:
            $eventsSummary

            [التشكيل]:
            - ${match.homeTeam}: $startersHome
            - ${match.awayTeam}: $startersAway

            [تعليمات الدقة والنزاهة الفنية]:
            1. حلل بناءً على الأرقام والأحداث المسجلة أعلاه فقط.
            2. لا تخترع أهدافاً أو بطاقات أو إصابات لم تذكر في السياق.
            3. إذا كان التشكيل أو رقم معين غير متاح، اذكر صراحة أنه غير متاح ولا تجزم به.
            4. اكتب تحليلاً استراتيجياً باللغة العربية بأسلوب محلل رياضي احترافي موضوعي، يتناول نقاط القوة، مناطق الضغط، والسيناريو المتوقع للمرحلة: $phase.
        """.trimIndent()

        return MatchVerifiedContext(
            matchId = match.id,
            contextVersion = version,
            promptContext = prompt
        )
    }
}
