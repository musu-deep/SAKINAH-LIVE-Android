package com.sakinah.mobile

import android.content.Context

data class FeedItem(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val author: String,
    val tags: List<String>,
    var likes: Int,
    var saved: Boolean = false
)

data class MatchItem(
    val id: String,
    val name: String,
    val role: String,
    val reason: String,
    val trust: Int,
    var connected: Boolean = false
)

data class CommunityItem(
    val id: String,
    val name: String,
    val description: String,
    val members: Int,
    var joined: Boolean = false
)

data class ProjectItem(
    val id: String,
    val name: String,
    val outcome: String,
    var progress: Int,
    var nextAction: String
)

data class OpportunityItem(
    val id: String,
    val title: String,
    val match: Int,
    val why: String,
    var accepted: Boolean = false
)

class OutcomeStore private constructor(private val context: Context) {
    companion object {
        @Volatile private var INSTANCE: OutcomeStore? = null
        fun get(context: Context): OutcomeStore = INSTANCE ?: synchronized(this) {
            INSTANCE ?: OutcomeStore(context.applicationContext).also { INSTANCE = it }
        }
    }

    private val prefs = context.getSharedPreferences("athar_outcome_store", Context.MODE_PRIVATE)

    var goal: String = prefs.getString("goal", "بناء شبكة أثر ومعرفة") ?: "بناء شبكة أثر ومعرفة"
        set(value) { field = value; prefs.edit().putString("goal", value).apply() }

    var connections: Int = prefs.getInt("connections", 12)
        private set
    var actions: Int = prefs.getInt("actions", 7)
        private set
    var outcomes: Int = prefs.getInt("outcomes", 3)
        private set
    var knowledgeSaved: Int = prefs.getInt("knowledge", 8)
        private set
    var peopleHelped: Int = prefs.getInt("helped", 5)
        private set

    val feed = mutableListOf(
        FeedItem("f1", "KNOWLEDGE", "من المشاهدة إلى الفعل", "أفضل محتوى ليس ما يلفت الانتباه فقط، بل ما يغيّر قراراً أو يفتح علاقة أو يحرك مشروعاً.", "ATHAR Intelligence", listOf("أثر", "معرفة"), 148),
        FeedItem("f2", "OPPORTUNITY", "فرصة تعاون: تطوير مبادرة صحية", "جهة غير ربحية تبحث عن شخص لديه خبرة في التخطيط، بناء النماذج وقياس الأثر.", "Opportunity Graph", listOf("الصحة", "الرياض", "تعاون"), 96),
        FeedItem("f3", "QUESTION", "ما الذي ينبغي قياسه بعد البث؟", "المشاهدات مهمة، لكن العودة، الالتزام، المعرفة المكتسبة والنتيجة اللاحقة تكشف القيمة الحقيقية.", "Impact Lab", listOf("LIVE", "قياس"), 211),
        FeedItem("f4", "PROJECT", "مجلس سكينة الأسبوعي", "جلسة تلاوة هادئة تتحول بعد البث إلى ملخص، ورد مقترح، متابعة اختيارية وقياس للعودة.", "SAKINAH", listOf("قرآن", "مجتمع"), 318)
    )

    val matches = mutableListOf(
        MatchItem("m1", "د. مها السالم", "استراتيجية صحية", "تقاطع قوي مع هدفك في تطوير مبادرات صحية ذات أثر قابل للقياس.", 93),
        MatchItem("m2", "عبدالله الحربي", "تقنية ومنتجات رقمية", "لديه خبرة في بناء منتجات مجتمعية ويمكن أن يكمل جانب التنفيذ التقني.", 89),
        MatchItem("m3", "مبادرة وصل", "مجتمع مهني", "شبكة تجمع خبراء ومنظمات وتبحث عن نماذج تشبيك قائمة على النتائج.", 86)
    )

    val communities = mutableListOf(
        CommunityItem("c1", "سكينة", "مجتمع التلاوة والبث الهادئ والمتابعة القيمية.", 1240),
        CommunityItem("c2", "مختبر الأثر", "تجارب قياس النتائج والتحول من التفاعل إلى الإنجاز.", 682),
        CommunityItem("c3", "مؤسسون وتنمية", "بناء المبادرات والمنتجات والشراكات ذات القيمة.", 911)
    )

    val projects = mutableListOf(
        ProjectItem("p1", "SAKINAH LIVE", "بث قرآني مستقل قابل للتوسع", 68, "تفعيل قناة بث مستقلة ومشاركة عامة"),
        ProjectItem("p2", "Outcome Network", "شبكة تربط المعرفة بالعلاقات والنتائج", 42, "اختبار Match Engine مع أول 20 مستخدماً"),
        ProjectItem("p3", "Impact Profile", "ملف شخصي يعرض المساهمات والنتائج الموثقة", 27, "إضافة أول نتيجة موثقة")
    )

    val opportunities = mutableListOf(
        OpportunityItem("o1", "تطوير نموذج قياس أثر لجمعية صحية", 94, "خبرتك + اهتمامك بالصحة + شبكة الرياض"),
        OpportunityItem("o2", "شريك تقني لبناء مجتمع معرفي", 91, "تقاطع مباشر مع مشروع Outcome Network"),
        OpportunityItem("o3", "جلسة خبراء عن مستقبل العمل المجتمعي", 84, "اهتمامك بالتحول المؤسسي والذكاء الاصطناعي")
    )

    fun like(item: FeedItem) { item.likes += 1; actions += 1; persistCounters() }
    fun save(item: FeedItem) { item.saved = !item.saved; if (item.saved) knowledgeSaved += 1; persistCounters() }
    fun connect(item: MatchItem) {
        if (!item.connected) { item.connected = true; connections += 1; actions += 1; persistCounters() }
    }
    fun join(item: CommunityItem) {
        if (!item.joined) { item.joined = true; actions += 1; persistCounters() }
    }
    fun advance(project: ProjectItem) {
        project.progress = (project.progress + 10).coerceAtMost(100)
        actions += 1
        if (project.progress == 100) outcomes += 1
        persistCounters()
    }
    fun accept(item: OpportunityItem) {
        if (!item.accepted) { item.accepted = true; actions += 1; outcomes += 1; persistCounters() }
    }
    fun addContribution() { peopleHelped += 1; actions += 1; persistCounters() }

    fun impactScore(): Int {
        val raw = 35 + connections + actions * 2 + outcomes * 7 + peopleHelped * 3 + knowledgeSaved
        return raw.coerceIn(0, 100)
    }

    fun agentRecommendation(): String {
        val opportunity = opportunities.firstOrNull { !it.accepted }
        val match = matches.firstOrNull { !it.connected }
        return buildString {
            append("هدفك الحالي: $goal\n\n")
            if (opportunity != null) append("أقوى خطوة الآن: ${opportunity.title} (${opportunity.match}% تطابق). ${opportunity.why}\n\n")
            if (match != null) append("علاقة مقترحة: ${match.name} — ${match.reason}\n\n")
            append("مؤشر الأثر الحالي ${impactScore()}/100. أفضل تحسين قصير الأجل: نفّذ خطوة واحدة قابلة للإثبات ثم وثّق نتيجتها، بدلاً من زيادة الاستهلاك فقط.")
        }
    }

    private fun persistCounters() {
        prefs.edit()
            .putInt("connections", connections)
            .putInt("actions", actions)
            .putInt("outcomes", outcomes)
            .putInt("knowledge", knowledgeSaved)
            .putInt("helped", peopleHelped)
            .apply()
    }
}
