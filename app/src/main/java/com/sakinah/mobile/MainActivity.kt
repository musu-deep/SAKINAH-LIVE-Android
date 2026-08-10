package com.sakinah.mobile

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var store: OutcomeStore
    private lateinit var contentHost: LinearLayout
    private lateinit var titleView: TextView
    private var section = "feed"
    private val bg = 0xFF0B0D18.toInt()
    private val surface = 0xFF151827.toInt()
    private val surface2 = 0xFF1C2033.toInt()
    private val textColor = 0xFFF6F7FB.toInt()
    private val muted = 0xFFADB4CC.toInt()
    private val accent = 0xFF8B7CFF.toInt()
    private val cyan = 0xFF66D9FF.toInt()
    private val green = 0xFF77E3B2.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = OutcomeStore.get(this)
        buildShell()
        showFeed()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(10))
        }
        header.addView(TextView(this).apply {
            text = "أثر  ATHAR"
            textSize = 28f
            setTextColor(textColor)
            setTypeface(typeface, Typeface.BOLD)
        })
        titleView = TextView(this).apply {
            text = "شبكة اجتماعية مبنية على النتائج"
            textSize = 13f
            setTextColor(muted)
        }
        header.addView(titleView)
        root.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        contentHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(22))
        }
        scroll.addView(contentHost)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomNav())
        setContentView(root)
    }

    private fun bottomNav(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(6), dp(4), dp(8))
            setBackgroundColor(surface)
        }
        val items = listOf("⌂\nلك" to "feed", "◎\nشبكتي" to "network", "●\nLIVE" to "live", "↗\nأثري" to "impact", "✦\nالوكيل" to "agent")
        items.forEach { (label, key) ->
            val b = MaterialButton(this).apply {
                text = label
                textSize = 11f
                setTextColor(if (key == section) accent else muted)
                setBackgroundColor(0x00000000)
                insetTop = 0
                insetBottom = 0
                setOnClickListener { section = key; rebuildBottomAndShow(key) }
            }
            bar.addView(b, LinearLayout.LayoutParams(0, dp(58), 1f))
        }
        return bar
    }

    private fun rebuildBottomAndShow(key: String) {
        val root = contentHost.parent.parent as LinearLayout
        root.removeViewAt(root.childCount - 1)
        root.addView(bottomNav())
        when (key) {
            "feed" -> showFeed()
            "network" -> showNetwork()
            "live" -> showLive()
            "impact" -> showImpact()
            "agent" -> showAgent()
        }
    }

    private fun clear(title: String, subtitle: String) {
        contentHost.removeAllViews()
        titleView.text = "$title  •  $subtitle"
    }

    private fun showFeed() {
        clear("For You", "من الانتباه إلى الفعل")
        goalHeader()
        feedModes()
        addInsightBanner("محرك اليوم", "لا نرتب المحتوى على المشاهدة وحدها؛ نرفع ما يطابق هدفك، يزيد معرفتك، يفتح علاقة أو يقود لنتيجة.")
        store.feed.forEach { item -> contentHost.addView(feedCard(item)) }
        contentHost.addView(primaryButton("＋ أنشئ Knowledge Object") { createKnowledgeObject() })
    }

    private fun goalHeader() {
        val c = card()
        val box = vertical()
        box.addView(kicker("PURPOSE GRAPH"))
        box.addView(h2("هدفي الآن"))
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("بناء شبكة أثر ومعرفة", "تطوير SAKINAH LIVE", "تأسيس مشروع جديد", "التشبيك مع خبراء", "اكتساب معرفة مركزة"))
            val current = (adapter as ArrayAdapter<String>).getPosition(store.goal)
            if (current >= 0) setSelection(current)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { store.goal = parent?.getItemAtPosition(position).toString() }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        box.addView(spinner)
        c.addView(box)
        contentHost.addView(c)
    }

    private fun feedModes() {
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("لك", "لهدفك", "لشبكتك", "لنموك", "لأثرك").forEachIndexed { i, s ->
            row.addView(MaterialButton(this).apply {
                text = s
                setTextColor(if (i == 0) textColor else muted)
                setBackgroundColor(if (i == 0) surface2 else 0x00000000)
                setOnClickListener { toast("تم تفعيل موجز: $s") }
            })
        }
        hs.addView(row)
        contentHost.addView(hs)
    }

    private fun feedCard(item: FeedItem): View {
        val c = card()
        val box = vertical()
        box.addView(kicker(item.type + "  •  " + item.author))
        box.addView(h2(item.title))
        box.addView(body(item.body))
        box.addView(tags(item.tags.joinToString("   ") { "#$it" }))
        val stats = TextView(this).apply {
            text = "♡ ${item.likes}    ↗ مشاركة    ${if (item.saved) "★ محفوظ" else "☆ حفظ"}"
            setTextColor(muted)
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        }
        box.addView(stats)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(smallButton("♡ مفيد") { store.like(item); stats.text = "♡ ${item.likes}    ↗ مشاركة    ${if (item.saved) "★ محفوظ" else "☆ حفظ"}" }, LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(smallButton(if (item.saved) "★ محفوظ" else "☆ حفظ") { store.save(item); showFeed() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(smallButton("↗ حوّله لفعل") { store.addContribution(); toast("تم تسجيل فعل مرتبط بالمحتوى") }, LinearLayout.LayoutParams(0, dp(46), 1f))
        box.addView(actions)
        c.addView(box)
        return c
    }

    private fun showNetwork() {
        clear("شبكتي", "Interest + Social + Knowledge + Outcome Graph")
        metricsRow()
        sectionTitle("مطابقات ذكية", "أشخاص وجهات يكملون هدفك الحالي")
        store.matches.forEach { m ->
            val c = card(); val box = vertical()
            box.addView(kicker("TRUST ${m.trust}%")); box.addView(h2(m.name)); box.addView(body(m.role)); box.addView(body(m.reason))
            box.addView(primaryButton(if (m.connected) "✓ متصل" else "＋ تواصل") { store.connect(m); showNetwork() })
            c.addView(box); contentHost.addView(c)
        }
        sectionTitle("المجتمعات", "الانتماء حول هدف لا حول المتابعة فقط")
        store.communities.forEach { community ->
            val c = card(); val box = vertical()
            box.addView(h2(community.name)); box.addView(body(community.description)); box.addView(tags("${community.members} عضو"))
            box.addView(smallButton(if (community.joined) "✓ منضم" else "انضم للمجتمع") { store.join(community); showNetwork() })
            c.addView(box); contentHost.addView(c)
        }
        sectionTitle("مشروعاتي", "من التفاعل إلى التنفيذ")
        store.projects.forEach { p -> contentHost.addView(projectCard(p)) }
        sectionTitle("فرص لي", "Opportunity Matching")
        store.opportunities.forEach { o -> contentHost.addView(opportunityCard(o)) }
    }

    private fun projectCard(p: ProjectItem): View {
        val c = card(); val box = vertical()
        box.addView(kicker("PROJECT  •  ${p.progress}%")); box.addView(h2(p.name)); box.addView(body(p.outcome))
        box.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = p.progress })
        box.addView(tags("الخطوة التالية: ${p.nextAction}"))
        box.addView(smallButton("أنجز خطوة +10%") { store.advance(p); showNetwork() })
        c.addView(box); return c
    }

    private fun opportunityCard(o: OpportunityItem): View {
        val c = card(); val box = vertical()
        box.addView(kicker("MATCH ${o.match}%")); box.addView(h2(o.title)); box.addView(body(o.why))
        box.addView(primaryButton(if (o.accepted) "✓ بدأت" else "ابدأ التعاون") { store.accept(o); showNetwork() })
        c.addView(box); return c
    }

    private fun showLive() {
        clear("LIVE", "Broadcast → Discussion → Action → Outcome")
        addInsightBanner("LIVE ليس نهاية الجلسة", "كل جلسة يمكن أن تتحول إلى ملخص، معرفة، أسئلة، علاقات، مهام، التزامات ونتائج قابلة للمتابعة.")
        val hero = card(); val box = vertical()
        box.addView(kicker("VERTICAL COMMUNITY")); box.addView(h2("SAKINAH • القرآن الحي"))
        box.addView(body("استوديو مستقل داخل الهاتف: مشهد قرآني، تلاوة محلية، RTMP/RTMPS، ووضع مشهد ملء الشاشة للبث عبر أي قناة تدعم مشاركة الشاشة."))
        box.addView(primaryButton("فتح SAKINAH Studio") { startActivity(Intent(this, SakinahLiveActivity::class.java)) })
        hero.addView(box); contentHost.addView(hero)
        sectionTitle("أنماط LIVE", "اختر ما الذي يجب أن يحدث بعد المشاهدة")
        listOf("Broadcast" to "بث أحادي مع تفاعل خفيف", "Discussion" to "نقاش متعدد الضيوف وأسئلة الجمهور", "Learning Room" to "درس + اختبار + حفظ تقدم", "Project Room" to "غرفة مشروع بقرارات ومهام", "Action Room" to "جلسة تنتهي بالتزام واضح لكل مشارك").forEach { (a,b) ->
            val c = card(); val bx = vertical(); bx.addView(h2(a)); bx.addView(body(b)); bx.addView(smallButton("إنشاء غرفة") { toast("تم إنشاء قالب $a محلياً") }); c.addView(bx); contentHost.addView(c)
        }
    }

    private fun showImpact() {
        clear("أثري", "Outcome-native profile")
        val score = store.impactScore()
        val c = card(); val box = vertical()
        box.addView(kicker("IMPACT SCORE"))
        box.addView(TextView(this).apply { text = "$score / 100"; textSize = 44f; setTypeface(typeface, Typeface.BOLD); setTextColor(green) })
        box.addView(body("المؤشر يجمع العلاقات، الأفعال، المعرفة المحفوظة، المساهمات والنتائج — وليس عدد المشاهدات فقط."))
        box.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = score })
        c.addView(box); contentHost.addView(c)
        metricsRow()
        sectionTitle("Outcome Graph", "نتائج يمكن إثباتها وربطها بمشروعات وأشخاص")
        listOf("نتيجة موثقة" to "SAKINAH أصبح تطبيق Android قابل للبناء والنشر.", "علاقة ذات معنى" to "مطابقة خبرة تقنية مع مشروع شبكة النتائج.", "معرفة تحولت لفعل" to "إنشاء نموذج قياس يتجاوز Likes إلى Action وOutcome.").forEach { (a,b) ->
            val cc = card(); val bx = vertical(); bx.addView(kicker("VERIFIED OUTCOME ✓")); bx.addView(h2(a)); bx.addView(body(b)); cc.addView(bx); contentHost.addView(cc)
        }
        contentHost.addView(primaryButton("＋ سجل مساهمة جديدة") { store.addContribution(); showImpact() })
    }

    private fun metricsRow() {
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("${store.connections}" to "علاقة", "${store.actions}" to "فعل", "${store.outcomes}" to "نتيجة", "${store.peopleHelped}" to "شخصاً ساعدت", "${store.knowledgeSaved}" to "معرفة محفوظة").forEach { (n,l) ->
            val c = MaterialCardView(this).apply { radius = dp(16).toFloat(); setCardBackgroundColor(surface2); setContentPadding(dp(16), dp(12), dp(16), dp(12)) }
            val b = vertical()
            b.addView(TextView(this).apply { text = n; textSize = 24f; setTextColor(textColor); setTypeface(typeface, Typeface.BOLD) })
            b.addView(TextView(this).apply { text = l; textSize = 11f; setTextColor(muted) })
            c.addView(b)
            row.addView(c, LinearLayout.LayoutParams(dp(132), dp(82)).apply { setMargins(dp(5), dp(4), dp(5), dp(8)) })
        }
        hs.addView(row); contentHost.addView(hs)
    }

    private fun showAgent() {
        clear("وكيل ATHAR", "من مساعد محادثة إلى وكيل متابعة")
        addInsightBanner("Agentic Layer", "الوكيل يقرأ هدفك وحالة شبكتك ومشروعاتك وفرصك، ثم يقترح الخطوة التالية ذات أعلى احتمال للنتيجة.")
        val c = card(); val box = vertical()
        box.addView(kicker("YOUR NEXT BEST ACTION")); box.addView(h2("تحليل سياقك الحالي"))
        val result = TextView(this).apply { text = store.agentRecommendation(); setTextColor(textColor); textSize = 15f; setPadding(0, dp(10), 0, dp(10)) }
        box.addView(result)
        box.addView(primaryButton("✦ أعد التحليل") { result.text = store.agentRecommendation() })
        c.addView(box); contentHost.addView(c)
        sectionTitle("اسأل الوكيل", "إجابات سياقية داخل التطبيق")
        val input = TextInputEditText(this).apply { hint = "مثال: ما أفضل خطوة لمشروع سكينة الآن؟"; setTextColor(textColor); setHintTextColor(muted); setBackgroundColor(surface2); setPadding(dp(14), dp(14), dp(14), dp(14)) }
        contentHost.addView(input, LinearLayout.LayoutParams(-1, dp(90)).apply { setMargins(0, dp(6), 0, dp(8)) })
        val answer = TextView(this).apply { setTextColor(textColor); textSize = 15f }
        contentHost.addView(primaryButton("اسأل") {
            val q = input.text?.toString().orEmpty()
            answer.text = if (q.isBlank()) "اكتب سؤالك أولاً." else "بناءً على هدفك «${store.goal}»: ${store.agentRecommendation()}"
        })
        contentHost.addView(answer)
    }

    private fun createKnowledgeObject() {
        val input = TextInputEditText(this).apply { hint = "اكتب فكرة، سؤالاً، فرصة أو نتيجة" }
        AlertDialog.Builder(this).setTitle("Knowledge Object جديد").setView(input).setNegativeButton("إلغاء", null).setPositiveButton("إضافة") { _, _ ->
            val value = input.text?.toString()?.trim().orEmpty()
            if (value.isNotEmpty()) {
                store.feed.add(0, FeedItem("local-${System.currentTimeMillis()}", "IDEA", value, "كائن معرفة أنشأته الآن ويمكن لاحقاً ربطه بهدف أو مشروع أو شخص.", "أنت", listOf("جديد"), 0))
                showFeed()
            }
        }.show()
    }

    private fun addInsightBanner(title: String, message: String) {
        val c = card(0xFF202442.toInt()); val box = vertical(); box.addView(kicker("ATHAR INTELLIGENCE")); box.addView(h2(title)); box.addView(body(message)); c.addView(box); contentHost.addView(c)
    }

    private fun sectionTitle(a: String, b: String) {
        contentHost.addView(TextView(this).apply { text = a; textSize = 22f; setTextColor(textColor); setTypeface(typeface, Typeface.BOLD); setPadding(dp(4), dp(18), dp(4), 0) })
        contentHost.addView(TextView(this).apply { text = b; textSize = 12f; setTextColor(muted); setPadding(dp(4), 0, dp(4), dp(8)) })
    }

    private fun card(color: Int = surface): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(20).toFloat(); setCardBackgroundColor(color); cardElevation = 0f; setContentPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(6), 0, dp(6)) }
    }
    private fun vertical() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
    private fun kicker(s: String) = TextView(this).apply { text = s; textSize = 10f; setTextColor(cyan); setTypeface(typeface, Typeface.BOLD) }
    private fun h2(s: String) = TextView(this).apply { text = s; textSize = 20f; setTextColor(textColor); setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(4), 0, dp(5)) }
    private fun body(s: String) = TextView(this).apply { text = s; textSize = 14f; setTextColor(textColor); setLineSpacing(2f, 1.15f) }
    private fun tags(s: String) = TextView(this).apply { text = s; textSize = 11f; setTextColor(muted); setPadding(0, dp(8), 0, 0) }
    private fun primaryButton(label: String, action: () -> Unit) = MaterialButton(this).apply {
        text = label; setTextColor(textColor); setBackgroundColor(accent); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(8), 0, 0) }
    }
    private fun smallButton(label: String, action: () -> Unit) = MaterialButton(this).apply {
        text = label; textSize = 12f; setTextColor(textColor); setBackgroundColor(surface2); setOnClickListener { action() }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
